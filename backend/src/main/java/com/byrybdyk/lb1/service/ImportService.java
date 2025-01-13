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
    private final ImportHistoryService importHistoryService;

    @Autowired
    public ImportService(LabWorkService labWorkService, ImportHistoryService importHistoryService) {
        this.labWorkService = labWorkService;
        this.importHistoryService = importHistoryService;
    }

    @Transactional
    public void importFile(MultipartFile file, Authentication authentication) throws Exception {
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new IllegalArgumentException("Допустимы только файлы формата XLSX.");
        }

        List<Map<String, String>> rows = readXlsxFile(file);

        for (Map<String, String> row : rows) {
            System.out.println(row);
        }
        Integer addedObjectsCount = 0;
        double duplicatePercentage = calculateDuplicatePercentage(rows);
        Boolean isSuccessful = false;
        if (duplicatePercentage > 20) {
            System.out.println("Более 20% строк являются дубликатами. Импорт отменен.");

        }
        else {
            try {
                labWorkService.addLabWorksFromFile(rows, authentication);
            } catch (Exception e) {
                System.out.println("Некорректный файл ");
            }
            isSuccessful = true;
            addedObjectsCount = rows.size();
        }


        importHistoryService.addImportHistory(authentication, addedObjectsCount,isSuccessful);
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
