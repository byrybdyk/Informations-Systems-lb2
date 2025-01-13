package com.byrybdyk.lb1.controller;

import com.byrybdyk.lb1.model.LabWork;
import com.byrybdyk.lb1.service.LabWorkService;
import com.byrybdyk.lb1.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/user")
public class UserController {

    private final LabWorkService labWorkService;
    private final UserService userService;

    @Autowired
    public UserController(LabWorkService labWorkService, UserService userService) {
        this.labWorkService = labWorkService;
        this.userService = userService;

    }

    @GetMapping("/info")
    public String getUserInfo(@AuthenticationPrincipal OAuth2User principal) {
        return principal.getAttributes().toString();
    }

    @GetMapping("/home")
    public String showUserHome(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "asc") String order,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) String ownerUsername,
            @RequestParam(required = false) String creationDate,
            @RequestParam(required = false) String disciplineName,
            @RequestParam(required = false) Integer disciplinePracticeHours,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Double coordinatesX,
            @RequestParam(required = false) Double coordinatesY,
            @RequestParam(required = false) Double minimalPoint,
            @RequestParam(required = false) Double personalQualitiesMaximum,
            @RequestParam(required = false) Double personalQualitiesMinimum,
            @RequestParam(required = false) Double authorWeight,
            @RequestParam(required = false) String authorEyeColor,
            @RequestParam(required = false) String authorHairColor,
            @RequestParam(required = false) Double authorLocationX,
            @RequestParam(required = false) Double authorLocationY,
            @RequestParam(required = false) String authorLocationName,
            @RequestParam(required = false) Integer authorPassportID,
            Model model,
            Authentication authentication) {


        Sort sorting = Sort.unsorted();
        if (sort != null && !sort.isEmpty()) {
            sorting = Sort.by(order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sort);
        }

        Pageable pageable = PageRequest.of(page - 1, size, sorting);

        Page<LabWork> labWorksPage = labWorkService.getLabWorksPage(
                id, name, description, authorName, ownerUsername, creationDate, disciplineName, disciplinePracticeHours,
                difficulty, coordinatesX, coordinatesY, minimalPoint, personalQualitiesMaximum, personalQualitiesMinimum,
                authorWeight, authorEyeColor, authorHairColor, authorLocationX, authorLocationY, authorLocationName,
                authorPassportID, pageable);

        model.addAttribute("labWorks", labWorksPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", labWorksPage.getTotalPages());
        model.addAttribute("sort", sort);
        model.addAttribute("order", order);

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String userName = oauth2User.getAttribute("preferred_username");


        model.addAttribute("username", userName);

        return "user-home";
    }

    @GetMapping("/requests")
    public String showUserRequests(Model model, Authentication authentication) {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String userName = oauth2User.getAttribute("preferred_username");

        model.addAttribute("username", userName);

        return "user-request";
    }

}
