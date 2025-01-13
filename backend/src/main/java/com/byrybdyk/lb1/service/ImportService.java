package com.byrybdyk.lb1.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImportService {
    private final LabWorkService labWorkService;

    @Autowired
    public ImportService(LabWorkService labWorkService) {
        this.labWorkService = labWorkService;
    }

//    private final DuplicateCheckService duplicateCheckService;
//    private final CollectionRepository collectionRepository; // Замените на ваш репозиторий для сохранения данных
//
//    public ImportService(DuplicateCheckService duplicateCheckService, CollectionRepository collectionRepository) {
//        this.duplicateCheckService = duplicateCheckService;
//        this.collectionRepository = collectionRepository;
//    }

    @Transactional
    public void importFile(MultipartFile file, Authentication authentication) throws Exception {
        // Проверка типа файла
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Допустимы только файлы формата XLSX.");
        }

        // Чтение данных из файла
        List<Map<String, String>> rows = readXlsxFile(file);

        for (Map<String, String> row : rows) {
            System.out.println(row);
        }

        // Вычисление хэшей строк
        Set<String> hashes = calculateHashes(rows);
        try {
            labWorkService.addLabWorksFromFile(rows, authentication);
        }catch (Exception e){
            System.out.println("Некорректный файл "+ e);
        }

//        // Проверка на дубликаты
//        long duplicateCount = hashes.stream()
//                .filter(duplicateCheckService::isDuplicate)
//                .count();

//        double duplicatePercentage = (double) duplicateCount / rows.size() * 100;
//
//        if (duplicatePercentage > 20) {
//            throw new IllegalArgumentException("Более 20% строк являются дубликатами. Импорт отменен.");
//        }

        // Сохранение данных
//        saveData(rows);

        // Сохранение хэшей в хранилище после успешного завершения транзакции
//        duplicateCheckService.addHashes(hashes);
    }

    private List<Map<String, String>> readXlsxFile(MultipartFile file) throws Exception {
        try {
            try (InputStream inputStream = file.getInputStream();
                 Workbook workbook = new XSSFWorkbook(inputStream)) {

                Sheet sheet = workbook.getSheetAt(0); // Первый лист в файле
                List<Map<String, String>> data = new ArrayList<>();

                // Чтение заголовков (первая строка)
                Row headerRow = sheet.getRow(0);
                List<String> headers = new ArrayList<>();
                for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                    System.out.println("Заголовок " + i + " = " + headerRow.getCell(i).getStringCellValue());
                    headers.add(headerRow.getCell(i).getStringCellValue());
                }

                // Чтение строк данных
                for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                    Row row = sheet.getRow(i);
                    Map<String, String> rowData = new HashMap<>();
                    for (int j = 0; j < headers.size(); j++) {
                        String value = row.getCell(j) != null ? row.getCell(j).toString() : "";
                        rowData.put(headers.get(j), value);
                    }
                    data.add(rowData);
                }
                return data;
            }

        } catch (Exception e) {
            System.out.println("Ошибка чтения файла "+ e);
            throw new Exception("Ошибка чтения файла", e);

        }
    }

    private Set<String> calculateHashes(List<Map<String, String>> rows) {
        return rows.stream()
                .map(row -> hashString(row.toString()))
                .collect(Collectors.toSet());
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хэширования строки", e);
        }
    }

//    private void saveData(List<Map<String, String>> rows) {
//        // Здесь сохранение данных в вашу коллекцию/БД
//        rows.forEach(row -> {
//            CollectionItem item = new CollectionItem();
//            item.setName(row.get("name")); // Замените на реальные поля
//            item.setDescription(row.get("description")); // Замените на реальные поля
//            collectionRepository.save(item);
//        });
//    }
}
