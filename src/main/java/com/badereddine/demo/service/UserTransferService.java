package com.badereddine.demo.service;

import com.badereddine.demo.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class UserTransferService {

    private final FakeDataService fakeDataService;
    private final UserImportService userImportService;
    private final UserService userService;
    private final CsvExportService csvExportService;
    private final ObjectMapper objectMapper;

    public UserTransferService(
            FakeDataService fakeDataService,
            UserImportService userImportService,
            UserService userService,
            CsvExportService csvExportService,
            ObjectMapper objectMapper
    ) {
        this.fakeDataService = fakeDataService;
        this.userImportService = userImportService;
        this.userService = userService;
        this.csvExportService = csvExportService;
        this.objectMapper = objectMapper;
    }

    public byte[] generateUsersJson(int count, int adminCount) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(fakeDataService.generateFakeUsers(count, adminCount));
    }

    @Transactional
    public UserImportService.UserImportResult importUsers(MultipartFile file) {
        return userImportService.importUsers(file);
    }

    @Transactional(readOnly = true)
    public byte[] exportUsersCsv(String search) {
        Pageable exportPage = PageRequest.of(0, CsvExportService.MAX_EXPORT_ROWS);
        List<User> users = hasSearchTerm(search)
                ? userService.searchUsers(search.trim(), exportPage).getContent()
                : userService.findAll(exportPage).getContent();
        return csvExportService.createCsv(users);
    }

    private boolean hasSearchTerm(String search) {
        return search != null && !search.trim().isEmpty();
    }
}
