package com.badereddine.demo.service;

import com.badereddine.demo.model.User;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

@Service
public class CsvExportService {

    public static final int MAX_EXPORT_ROWS = 10_000;

    private static final String HEADER =
            "ID,Username,Email,First Name,Last Name,Company,Job Position,City,Country,Mobile,Role,Status,Created At,Last Login\n";

    public byte[] createCsv(List<User> users) {
        StringBuilder csv = new StringBuilder(HEADER);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (User user : users) {
            csv.append(toCsvCell(user.getId() != null ? user.getId().toString() : "")).append(",");
            csv.append(toCsvCell(user.getUsername())).append(",");
            csv.append(toCsvCell(user.getEmail())).append(",");
            csv.append(toCsvCell(user.getFirstName())).append(",");
            csv.append(toCsvCell(user.getLastName())).append(",");
            csv.append(toCsvCell(user.getCompany())).append(",");
            csv.append(toCsvCell(user.getJobPosition())).append(",");
            csv.append(toCsvCell(user.getCity())).append(",");
            csv.append(toCsvCell(user.getCountry())).append(",");
            csv.append(toCsvCell(user.getMobile())).append(",");
            csv.append(toCsvCell(user.getRole() != null ? user.getRole().getName().name() : "")).append(",");
            csv.append(toCsvCell(user.isEnabled() ? "Active" : "Disabled")).append(",");
            csv.append(toCsvCell(user.getCreatedAt() != null ? dateFormat.format(user.getCreatedAt()) : "")).append(",");
            csv.append(toCsvCell(user.getLastLogin() != null ? dateFormat.format(user.getLastLogin()) : "Never")).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toCsvCell(String value) {
        if (value == null) {
            return "";
        }

        String safeValue = startsWithFormulaCharacter(value) ? "'" + value : value;
        if (safeValue.contains(",") || safeValue.contains("\"")
                || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }
        return safeValue;
    }

    private boolean startsWithFormulaCharacter(String value) {
        if (value.isEmpty()) {
            return false;
        }
        char firstCharacter = value.charAt(0);
        return firstCharacter == '=' || firstCharacter == '+'
                || firstCharacter == '-' || firstCharacter == '@';
    }
}
