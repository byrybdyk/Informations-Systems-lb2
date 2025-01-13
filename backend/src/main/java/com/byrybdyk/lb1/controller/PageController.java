package com.byrybdyk.lb1.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/admin/home")
    public String adminHome(Model model, Authentication authentication) {
        String userName = authentication.getName();


        model.addAttribute("username", userName);
        return "admin-home";
    }
}
