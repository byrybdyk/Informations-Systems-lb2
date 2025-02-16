package com.byrybdyk.lb1.controller;

import ch.qos.logback.core.model.Model;
import com.byrybdyk.lb1.service.ImportHistoryService;
import com.byrybdyk.lb1.service.ImportService;
import com.byrybdyk.lb1.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.Future;

import static java.util.UUID.randomUUID;


@RestController
@RequestMapping("/import")
public class ImportController {

    @Value("${minio.bucket.name}")
    private String minioBucketName;

    @Autowired
    private ImportService importService;
    @Autowired
    private  MinioService minioService;
    @Autowired
    private ImportHistoryService importHistoryService;

    @GetMapping("/download/{uuid}")
    public ResponseEntity<String> downloadFile(@PathVariable String uuid) {
        String fileName = importHistoryService.getFullFileNameByUuid(uuid);
        String url = minioService.generateUrl(minioBucketName, fileName);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }


    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            Future<ImportService.ImportResult> resultFuture = importService.importFile(file, authentication);
            Boolean isSuccess = false;
            String result = "Ошибка обработки файла, произведена отмена транзакции";
            try {
                result = String.valueOf(resultFuture.get().getMessage());
                isSuccess = resultFuture.get().isSuccess();
            }
            catch (Exception e) {
                String username = "";
                if (authentication.getPrincipal() instanceof OAuth2User) {
                    OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
                    username = oauth2User.getAttribute("preferred_username");
                } else {
                    username = authentication.getName();
                }
                String UUID = randomUUID().toString();
                String hash = importHistoryService.generateHash(UUID);
                importHistoryService.addImportHistory(username, 0, false, hash, UUID, file.getOriginalFilename());
            }



            if (!isSuccess) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка обработки файла: " + e.getMessage());
        }
    }


}
