package com.byrybdyk.lb1.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
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
    private final ConcurrentHashMap<String, Queue<MultipartFile>> userQueues = new ConcurrentHashMap<>();

    private static final int MAX_CONCURRENT_FILES = 2;

    @Autowired
    public ImportService(LabWorkService labWorkService, ImportHistoryService importHistoryService) {
        this.labWorkService = labWorkService;
        this.importHistoryService = importHistoryService;
    }

    @Transactional
    public void importFile(MultipartFile file, Authentication authentication) throws Exception {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String username = oauth2User.getAttribute("preferred_username");
        System.out.println("Файл получен от пользователя: " + username + ", файл: " + file.getOriginalFilename());

        userExecutors.putIfAbsent(username, Executors.newFixedThreadPool(MAX_CONCURRENT_FILES));
        userQueues.putIfAbsent(username, new LinkedBlockingQueue<>());

        Queue<MultipartFile> queue = userQueues.get(username);
        queue.add(file);
        System.out.println("Файл добавлен в очередь пользователя: " + username + ". Размер очереди: " + queue.size());

        processNextFile(username);
    }


    private void processNextFile(String username) {
        Queue<MultipartFile> queue = userQueues.get(username);
        ExecutorService executorService = userExecutors.get(username);

        System.out.println("Попытка обработки следующего файла для пользователя: " + username + ". Очередь содержит: " + queue.size() + " файлов.");
        System.out.println("Количество активных процессов: " + getActiveTaskCount(username));

        if (queue != null && !queue.isEmpty()) {

            if (getActiveTaskCount(username) < MAX_CONCURRENT_FILES) {
                MultipartFile file = queue.poll();
                System.out.println("Запуск обработки файла: " + file.getOriginalFilename() + " для пользователя: " + username);
                executorService.submit(() -> processFile(file, username));
            } else {
                System.out.println("Максимальное количество параллельных задач достигнуто для пользователя: " + username);
            }
        }
    }


    private void processFile(MultipartFile file, String username) {
        try {
            System.out.println("Обработка файла " + file.getOriginalFilename() + " начата для пользователя: " + username);

            if (!file.getOriginalFilename().endsWith(".xlsx")) {
                throw new IllegalArgumentException("Допустимы только файлы формата XLSX.");
            }

            List<Map<String, String>> rows = readXlsxFile(file);
            Integer addedObjectsCount = 0;
            double duplicatePercentage = calculateDuplicatePercentage(rows);
            Boolean isSuccessful = false;

            if (duplicatePercentage > 20) {
                System.out.println("Более 20% строк являются дубликатами. Импорт отменен для файла: " + file.getOriginalFilename());
            } else {
                try {
                    labWorkService.addLabWorksFromFile(rows, username);
                    isSuccessful = true;
                    addedObjectsCount = rows.size();
                    System.out.println("Импорт файла " + file.getOriginalFilename() + " успешен для пользователя: " + username + ". Добавлено объектов: " + addedObjectsCount);
                } catch (Exception e) {
                    System.out.println("Ошибка при импорте файла " + file.getOriginalFilename() + " для пользователя " + username + ": " + e.getMessage());
                }
            }

            importHistoryService.addImportHistory(username, addedObjectsCount, isSuccessful);
            System.out.println("Запись истории импорта для пользователя " + username + " завершена.");
        } catch (Exception e) {
            System.out.println("Ошибка при обработке файла " + file.getOriginalFilename() + " для пользователя " + username + ": " + e.getMessage());
        } finally {
            processNextFile(username);
        }
    }

    private long getActiveTaskCount(String username) {
        ExecutorService executorService = userExecutors.get(username);
        if (executorService instanceof ThreadPoolExecutor) {
            long activeCount = ((ThreadPoolExecutor) executorService).getActiveCount();
            System.out.println("Количество активных процессов для пользователя " + username + ": " + activeCount);
            return activeCount;
        }
        return 0;
    }

    public double calculateDuplicatePercentage(List<Map<String, String>> rows) {
        List<String> hashes = rows.stream()
                .map(this::generateHashString)
                .collect(Collectors.toList());

        long totalRows = hashes.size();
        long uniqueRows = hashes.stream().distinct().count();

        long duplicateCount = totalRows - uniqueRows;

        System.out.println("Всего строк: " + totalRows);
        System.out.println("Уникальных строк: " + uniqueRows);
        System.out.println("Количество дубликатов: " + duplicateCount);

        double duplicatePercentage = ((double) duplicateCount / totalRows) * 100;

        System.out.println("Процент дубликатов: " + duplicatePercentage + "%");
        return duplicatePercentage;
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
}
