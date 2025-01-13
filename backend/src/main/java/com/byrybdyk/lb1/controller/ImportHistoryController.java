package com.byrybdyk.lb1.controller;

import com.byrybdyk.lb1.service.ImportHistoryService;
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

    public ImportHistoryController(ImportHistoryService importHistoryService) {
        this.importHistoryService = importHistoryService;
    }

    @GetMapping("/import")
    public String viewImportHistory(Authentication authentication, Model model) {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String userName = oauth2User.getAttribute("preferred_username");

        Map<String, Object> attributes = token.getPrincipal().getAttributes();

        List<String> roles = (List<String>) attributes.get("roles");
        boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
        if (isAdmin) {
            model.addAttribute("history", importHistoryService.getAllImportHistory());
        } else {
            model.addAttribute("history", importHistoryService.getImportHistoryForUser(userName));
        }

        model.addAttribute("username", userName);
        return "import";
    }
}
