package com.byrybdyk.lb1.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ImportService {

    private final LabWorkService labWorkService;
    private final ImportHistoryService importHistoryService;

    private final ConcurrentHashMap<String, ExecutorService> userExecutors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BlockingQueue<MultipartFile>> userQueues = new ConcurrentHashMap<>();
    private static final int MAX_CONCURRENT_FILES = 2;

    @Autowired
    public ImportService(LabWorkService labWorkService, ImportHistoryService importHistoryService) {
        this.labWorkService = labWorkService;
        this.importHistoryService = importHistoryService;
    }

    public void addFileToQueue(MultipartFile file, Authentication authentication) {
        String username = authentication.getName();
        System.out.println("Файл получен от пользователя: " + username + ", файл: " + file.getOriginalFilename());

        userExecutors.putIfAbsent(username, Executors.newFixedThreadPool(MAX_CONCURRENT_FILES));
        userQueues.putIfAbsent(username, new LinkedBlockingQueue<>());

        BlockingQueue<MultipartFile> queue = userQueues.get(username);
        try {
            queue.put(file);
            System.out.println("Файл добавлен в очередь пользователя: " + username);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ошибка при добавлении файла в очередь", e);
        }

        processNextFile(username);
    }

    private void processNextFile(String username) {
        BlockingQueue<MultipartFile> queue = userQueues.get(username);
        ExecutorService executorService = userExecutors.get(username);

        if (queue != null && !queue.isEmpty() && getActiveTaskCount(username) < MAX_CONCURRENT_FILES) {
            try {
                MultipartFile file = queue.take();
                System.out.println("Запуск обработки файла: " + file.getOriginalFilename() + " для пользователя: " + username);

                executorService.submit(() -> {
                    try {
                        processFile(file, username);
                    } catch (Exception e) {
                        System.out.println("Ошибка обработки файла: " + e.getMessage());
                    } finally {
                        processNextFile(username);
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Ошибка при извлечении файла из очереди: " + e.getMessage());
            }
        }
    }


    private long getActiveTaskCount(String username) {
        ExecutorService executorService = userExecutors.get(username);
        if (executorService instanceof ThreadPoolExecutor) {
            System.out.println("Потоков занято: " + ((ThreadPoolExecutor) executorService).getActiveCount());
            return ((ThreadPoolExecutor) executorService).getActiveCount();
        }
        return 0;
    }

    @Transactional
    public void processFile(MultipartFile file, String username) {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Неправильный формат файла. Допустимы только файлы формата XLSX.");
        }

        List<Map<String, String>> rows;
        try {
            rows = readXlsxFile(file);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка чтения файла", e);
        }

        int addedObjectsCount = 0;
        double duplicatePercentage = calculateDuplicatePercentage(rows);

        if (duplicatePercentage > 20) {
            saveImportHistory(username, addedObjectsCount, false);
            throw new IllegalArgumentException ("Более 20% строк являются дубликатами");
        }

        labWorkService.addLabWorksFromFile(rows, username);
        addedObjectsCount = rows.size();

        saveImportHistory(username, addedObjectsCount, true);
    }

    @Transactional
    public void saveImportHistory(String username, int addedObjectsCount, boolean success) {
        importHistoryService.addImportHistory(username, addedObjectsCount, success);
        System.out.println("Запись истории импорта завершена.");
    }

    private List<Map<String, String>> readXlsxFile(MultipartFile file) throws Exception {
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            List<Map<String, String>> data = new ArrayList<>();

            Row headerRow = sheet.getRow(0);
            List<String> headers = new ArrayList<>();
            for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                headers.add(headerRow.getCell(i).getStringCellValue().trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                Map<String, String> rowData = new HashMap<>();
                for (int j = 0; j < headers.size(); j++) {
                    String value = row.getCell(j) != null ? row.getCell(j).toString().trim() : "";
                    rowData.put(headers.get(j), value);
                }
                data.add(rowData);
            }
            return data;

        } catch (Exception e) {
            throw new Exception("Ошибка чтения файла", e);
        }
    }

    private String generateHashString(Map<String, String> row) {
        return row.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    public double calculateDuplicatePercentage(List<Map<String, String>> rows) {
        List<String> hashes = rows.stream()
                .map(this::generateHashString)
                .collect(Collectors.toList());

        long totalRows = hashes.size();
        long uniqueRows = hashes.stream().distinct().count();

        long duplicateCount = totalRows - uniqueRows;

        return ((double) duplicateCount / totalRows) * 100;
    }
}

