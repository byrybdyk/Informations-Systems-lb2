package com.byrybdyk.lb1.controller;

import com.byrybdyk.lb1.model.User;
import com.byrybdyk.lb1.service.ImportHistoryService;
import com.byrybdyk.lb1.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class ImportHistoryController {

    private final ImportHistoryService importHistoryService;
    private final UserService userService;

    public ImportHistoryController(ImportHistoryService importHistoryService, UserService userService) {
        this.importHistoryService = importHistoryService;
        this.userService = userService;
    }

    @GetMapping("/import")
    public String viewImportHistory(Authentication authentication, Model model) {
        String userName = authentication.getName();

        User currentUser = userService.getUserByUsername(userName);
        boolean isAdmin = currentUser.getRole().equals(currentUser.getRole().ADMIN);
        if (isAdmin) {
            model.addAttribute("history", importHistoryService.getAllImportHistory());
        } else {
            model.addAttribute("history", importHistoryService.getImportHistoryForUser(userName));
        }

        model.addAttribute("username", userName);
        return "import";
    }
}
