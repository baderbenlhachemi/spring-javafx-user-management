package com.badereddine.demo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationTest {

    private static final PostgreSQLContainer<?> POSTGRESQL =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("team_access_hub_flyway_test")
                    .withUsername("test_user")
                    .withPassword("test_password");

    @BeforeAll
    static void startDatabase() {
        POSTGRESQL.start();
    }

    @AfterAll
    static void stopDatabase() {
        POSTGRESQL.stop();
    }

    @Test
    void migratesEmptyDatabaseAndBootsTwiceAgainstTheSameSchema() {
        try (ConfigurableApplicationContext firstBoot = startApplication()) {
            JdbcTemplate jdbcTemplate = firstBoot.getBean(JdbcTemplate.class);

            assertThat(tableNames(jdbcTemplate)).contains("roles", "users");
            assertThat(constraintNames(jdbcTemplate)).contains(
                    "roles_pkey",
                    "chk_roles_name",
                    "users_pkey",
                    "uk_users_username",
                    "uk_users_email",
                    "fk_users_role"
            );
            assertThat(indexNames(jdbcTemplate)).contains(
                    "roles_pkey",
                    "users_pkey",
                    "uk_users_username",
                    "uk_users_email",
                    "idx_users_role_id",
                    "idx_users_created_at"
            );
            assertThat(appliedMigrationCount(jdbcTemplate)).isEqualTo(1);
        }

        try (ConfigurableApplicationContext secondBoot = startApplication()) {
            JdbcTemplate jdbcTemplate = secondBoot.getBean(JdbcTemplate.class);

            assertThat(appliedMigrationCount(jdbcTemplate)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM roles", Integer.class))
                    .isEqualTo(2);
        }
    }

    private ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(DemoApplication.class)
                .profiles("test")
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + POSTGRESQL.getJdbcUrl(),
                        "--spring.datasource.username=" + POSTGRESQL.getUsername(),
                        "--spring.datasource.password=" + POSTGRESQL.getPassword()
                );
    }

    private List<String> tableNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = current_schema()
                """, String.class);
    }

    private List<String> constraintNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("""
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = current_schema()
                  AND table_name IN ('roles', 'users')
                """, String.class);
    }

    private List<String> indexNames(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("""
                SELECT indexname
                FROM pg_indexes
                WHERE schemaname = current_schema()
                  AND tablename IN ('roles', 'users')
                """, String.class);
    }

    private Integer appliedMigrationCount(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = true
                  AND version = '1'
                """, Integer.class);
    }
}
