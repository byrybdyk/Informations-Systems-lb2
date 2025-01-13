package com.byrybdyk.lb1.service;

import com.byrybdyk.lb1.model.ImportHistory;
import com.byrybdyk.lb1.repository.ImportHistoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImportHistoryService {

    private final ImportHistoryRepository importHistoryRepository;
    private final UserService userService;

    public ImportHistoryService(ImportHistoryRepository importHistoryRepository, UserService userService) {
        this.importHistoryRepository = importHistoryRepository;
        this.userService = userService;
    }

    public void addImportHistory(Authentication authentication, int addedObjectsCount, boolean isSuccessful) {
        ImportHistory importHistory = new ImportHistory();

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String userName = oauth2User.getAttribute("preferred_username");

        importHistory.setUser(userService.findByUsername(userName).orElseThrow(() -> new IllegalArgumentException("User not found")));
        importHistory.setAddedObjectsCount(addedObjectsCount);
        importHistory.setIsSuccessful(isSuccessful);

        importHistoryRepository.save(importHistory);
    }

    public List<ImportHistory> getAllImportHistory() {
        return importHistoryRepository.findAll(Sort.by(Sort.Order.desc("creationDate")));
    }

    public List<ImportHistory> getImportHistoryForUser(String name) {
        return importHistoryRepository.findByUserUsername(name, Sort.by(Sort.Order.desc("creationDate")));
    }

}
