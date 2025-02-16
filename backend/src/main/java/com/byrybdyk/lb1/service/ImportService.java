package com.byrybdyk.lb1.service;


import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.util.UUID.randomUUID;

@Service
public class ImportService {
    private final LabWorkService labWorkService;
    private final ImportHistoryService importHistoryService;
    private final MinioService minioService;
    private final TransactionTemplate transactionTemplate;
    private final String bucketName = "imports";

    private final ConcurrentHashMap<String, ExecutorService> userExecutors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CompletionService<ImportResult>> userCompletionServices = new ConcurrentHashMap<>();

    private static final int MAX_CONCURRENT_FILES = 2;

    @Autowired
    public ImportService(LabWorkService labWorkService, ImportHistoryService importHistoryService, MinioService minioService, TransactionTemplate transactionTemplate) {
        this.labWorkService = labWorkService;
        this.importHistoryService = importHistoryService;
        this.minioService = minioService;
        this.transactionTemplate = transactionTemplate;
    }



    public Future<ImportResult> importFile(MultipartFile file, Authentication authentication) {
        String username;
        if (authentication.getPrincipal() instanceof OAuth2User) {
            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            username = oauth2User.getAttribute("preferred_username");
        } else {
            username = authentication.getName();
        }

        System.out.println("Файл получен от пользователя: " + username + ", файл: " + file.getOriginalFilename());


        userExecutors.putIfAbsent(username, Executors.newFixedThreadPool(MAX_CONCURRENT_FILES));
        userCompletionServices.putIfAbsent(username, new ExecutorCompletionService<>(userExecutors.get(username)));

        ExecutorService executorService = userExecutors.get(username);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) executorService;


        System.out.println("Текущие активные потоки: " + threadPoolExecutor.getActiveCount());
        System.out.println("Файлы в очереди: " + threadPoolExecutor.getQueue().size());

        CompletionService<ImportResult> completionService = userCompletionServices.get(username);
        System.out.println("Файл добавлен в очередь пользователя: " + username);

        try{
            return completionService.submit(() ->
                    transactionTemplate.execute(status -> processFile(file, username))
            );
        }
        catch (Exception e) {

        }

        return null;
    }



    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ImportResult processFile(MultipartFile file, String username) {
        String UUID = randomUUID().toString();
        String hash = importHistoryService.generateHash(UUID);
        try {
            boolean isSuccessful = false;
            int addedObjectsCount = 0;

            String message;

            String finalMinioFilePath = username + "/" + hash + "/" + file.getOriginalFilename();

            try (InputStream fileStream = file.getInputStream()) {
                minioService.uploadFile(finalMinioFilePath, fileStream, file.getSize(), file.getContentType());
                minioService.deleteFile(finalMinioFilePath);
                System.out.println("S3 готово");

                List<Map<String, String>> rows = readXlsxFile(file);
                double duplicatePercentage = calculateDuplicatePercentage(rows);
                if (duplicatePercentage > 20) {
                    message = "Ошибка. Импорт отменен: более 20% строк являются дубликатами.";
                    System.out.println(message);
                    throw new RuntimeException(message);
                }

                labWorkService.addLabWorksFromFile(rows, username);
                addedObjectsCount = rows.size();

                System.out.println("БД готова");

                System.out.println("Ставим поток на паузу на 15 секунд");
                Thread.sleep(15* 1000);
                System.out.println("Пауза окончена");

                try(InputStream newFileStream = file.getInputStream()) {
                    minioService.uploadFile(finalMinioFilePath, newFileStream, file.getSize(), file.getContentType());
                    System.out.println("Файл перемещен в S3: " + finalMinioFilePath);
                } catch (Exception e) {
                    System.out.println("Ошибка при загрузке файла в S3: " + e.getMessage());

                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

                    rollbackMinioFile(finalMinioFilePath);

                    throw new RuntimeException("Ошибка при загрузке в S3 Транзакция откатывается.", e);
                }

                importHistoryService.addImportHistory(username, addedObjectsCount, true, hash, UUID, file.getOriginalFilename());

                isSuccessful = true;
                message = "Импорт в БД успешен. Добавлено объектов: " + addedObjectsCount;
                System.out.println(message);
            } catch (Exception e) {
                message = "Ошибка при обработке файла: " + e.getMessage();
                System.out.println(message);

                rollbackMinioFile(finalMinioFilePath);

                throw new RuntimeException("Ошибка при обработке файла", e);
            }

            System.out.println("Импорт завершен");
            return new ImportResult(isSuccessful, addedObjectsCount, message);
        }
        catch (Exception e) {
            System.out.println("Ошибка при обработке файла: " + e.getMessage());
            importHistoryService.addImportHistory(username, 0, false, hash, UUID, file.getOriginalFilename());
            throw new RuntimeException("Ошибка при обработке файла", e);
        }
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

    private double calculateDuplicatePercentage(List<Map<String, String>> rows) {
        List<String> hashes = rows.stream().map(this::generateHashString).collect(Collectors.toList());
        long totalRows = hashes.size();
        long uniqueRows = hashes.stream().distinct().count();
        long duplicateCount = totalRows - uniqueRows;
        return ((double) duplicateCount / totalRows) * 100;
    }

    private String generateHashString(Map<String, String> row) {
        return row.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
    }

    public static class ImportResult {
        private final boolean success;
        private final int addedObjects;
        private final String message;

        public ImportResult(boolean success, int addedObjects, String message) {
            this.success = success;
            this.addedObjects = addedObjects;
            this.message = message;
        }

        public String getMessage() { return message; }

        public boolean isSuccess() {
            return success;
        }

        public int getAddedObjects() {
            return addedObjects;
        }
    }

    private void rollbackMinioFile(String fileName) {
        try {
            minioService.rollbackMinioFile(fileName);
        } catch (Exception e) {
            System.out.println("Ошибка при удалении файла из MinIO: " + e.getMessage());
        }
    }


}
