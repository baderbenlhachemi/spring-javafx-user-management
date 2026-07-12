package com.badereddine.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgreSQLTestContainerConfiguration.class)
class DemoApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private PostgreSQLContainer<?> postgreSQLContainer;

    @Test
    void contextLoadsWithTestProfileAndContainerDatabase() throws SQLException {
        assertThat(environment.getActiveProfiles()).contains("test");
        assertThat(environment.getRequiredProperty("demo.jwtSecret"))
                .isNotBlank()
                .doesNotContain("${");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .isEqualTo(postgreSQLContainer.getJdbcUrl())
                    .doesNotContain("localhost:5432");
        }
    }

}
