package com.byrybdyk.lb1.controller;

import ch.qos.logback.core.model.Model;
import com.byrybdyk.lb1.service.ImportService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/import")
public class ImportController {
    private ImportService importService;

    public ImportController(ImportService importService) {
        this.importService = importService;
    }


    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file, Model model,Authentication authentication) throws Exception {
        importService.importFile(file,authentication);

        return "import";
    }
}
