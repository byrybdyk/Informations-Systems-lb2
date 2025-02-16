package com.byrybdyk.lb1.service;

import com.byrybdyk.lb1.model.ImportHistory;
import com.byrybdyk.lb1.repository.ImportHistoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class ImportHistoryService {

    private final ImportHistoryRepository importHistoryRepository;
    private final UserService userService;

    public ImportHistoryService(ImportHistoryRepository importHistoryRepository, UserService userService) {
        this.importHistoryRepository = importHistoryRepository;
        this.userService = userService;
    }

    public void addImportHistory(String userName, int addedObjectsCount, boolean isSuccessful, String hash, String uuid, String fileName) {
        ImportHistory importHistory = new ImportHistory();
        importHistory.setHash(hash);
        importHistory.setUser(userService.findByUsername(userName).orElseThrow(() -> new IllegalArgumentException("User not found")));
        importHistory.setAddedObjectsCount(addedObjectsCount);
        importHistory.setIsSuccessful(isSuccessful);
        importHistory.setUuid(uuid);
        importHistory.setFileName(fileName);
        importHistoryRepository.save(importHistory);
    }

    public List<ImportHistory> getAllImportHistory() {
        return importHistoryRepository.findAll(Sort.by(Sort.Order.desc("creationDate")));
    }

    public List<ImportHistory> getImportHistoryForUser(String name) {
        return importHistoryRepository.findByUserUsername(name, Sort.by(Sort.Order.desc("creationDate")));
    }

    public String generateHash(String uuid) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(String.valueOf(uuid).getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error generating hash", e);
        }
    }

    public String  getFullFileNameByUuid(String uuid) {
        String hash = generateHash(uuid);
        ImportHistory importHistory = importHistoryRepository.findByHash(hash).orElseThrow(() -> new IllegalArgumentException("Import history not found"));
        String fullFileName = importHistory.getUser().getUsername() + "/" + importHistory.getHash() + "/" + importHistory.getFileName();
        return fullFileName;
    }
}
