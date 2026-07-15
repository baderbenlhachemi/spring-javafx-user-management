package com.badereddine.demo.service;

import com.badereddine.demo.model.ERole;
import com.badereddine.demo.model.Role;
import com.badereddine.demo.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private static final String EXPECTED_HEADER =
            "ID,Username,Email,First Name,Last Name,Company,Job Position,City,Country,Mobile,Role,Status,Created At,Last Login\n";

    private final CsvExportService csvExportService = new CsvExportService();

    @Test
    void preservesHeaderAndColumnOrder() {
        String csv = export(representativeUser());

        assertThat(csv).startsWith(EXPECTED_HEADER);
        assertThat(csv).contains("7,ada,ada@example.test,Ada,Lovelace,Analytical Engines,Programmer,London,UK,'+44 20,ROLE_ADMIN,Active,,Never\n");
    }

    @ParameterizedTest
    @ValueSource(strings = {"=2+2", "+SUM(A1:A2)", "-10+20", "@SUM(A1:A2)"})
    void neutralizesSpreadsheetFormulaPrefixes(String username) {
        User user = representativeUser();
        user.setUsername(username);

        assertThat(export(user)).contains("\n7,'" + username + ",");
    }

    @Test
    void preservesCommaQuoteAndNewlineEscaping() {
        User user = representativeUser();
        user.setCompany("Acme, \"R&D\"\r\nGlobal");

        assertThat(export(user)).contains("\"Acme, \"\"R&D\"\"\r\nGlobal\"");
    }

    @Test
    void encodesNonAsciiValuesAsUtf8() {
        User user = representativeUser();
        user.setFirstName("José");

        byte[] csvBytes = csvExportService.createCsv(List.of(user));

        assertThat(csvBytes).containsSequence((byte) 0x4a, (byte) 0x6f, (byte) 0x73, (byte) 0xc3, (byte) 0xa9);
        assertThat(new String(csvBytes, StandardCharsets.UTF_8)).contains("José");
    }

    private String export(User user) {
        return new String(csvExportService.createCsv(List.of(user)), StandardCharsets.UTF_8);
    }

    private User representativeUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("ada");
        user.setEmail("ada@example.test");
        user.setFirstName("Ada");
        user.setLastName("Lovelace");
        user.setCompany("Analytical Engines");
        user.setJobPosition("Programmer");
        user.setCity("London");
        user.setCountry("UK");
        user.setMobile("+44 20");
        user.setRole(new Role(ERole.ROLE_ADMIN));
        user.setEnabled(true);
        return user;
    }
}
