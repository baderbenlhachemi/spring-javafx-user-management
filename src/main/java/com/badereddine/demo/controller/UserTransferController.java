package com.badereddine.demo.controller;

import com.badereddine.demo.exception.UserImportException;
import com.badereddine.demo.payload.response.MessageResponse;
import com.badereddine.demo.service.UserImportService;
import com.badereddine.demo.service.UserTransferService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "User Transfers")
public class UserTransferController {

    private final UserTransferService userTransferService;

    public UserTransferController(UserTransferService userTransferService) {
        this.userTransferService = userTransferService;
    }

    @GetMapping("/users/generate/{count}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateUsers(
            @PathVariable int count,
            @RequestParam(defaultValue = "0") int adminCount
    ) throws IOException {
        try {
            byte[] json = userTransferService.generateUsersJson(count, adminCount);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.json")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(json);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }
    }

    @PostMapping("/users/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchUsers(@RequestParam("file") MultipartFile file) {
        try {
            UserImportService.UserImportResult result = userTransferService.importUsers(file);
            Map<String, Integer> response = new HashMap<>();
            response.put("totalRecords", result.totalRecords());
            response.put("successfulImports", result.successfulImports());
            response.put("failedImports", result.failedImports());
            return ResponseEntity.ok(response);
        } catch (UserImportException exception) {
            return ResponseEntity.badRequest()
                    .body(new MessageResponse(exception.getMessage()));
        }
    }

    @GetMapping("/users/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportUsersToCsv(@RequestParam(required = false) String search) {
        byte[] csvBytes = userTransferService.exportUsersCsv(search);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", "users_export.csv");
        headers.setContentLength(csvBytes.length);
        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }
}
