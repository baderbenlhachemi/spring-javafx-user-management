package com.badereddine.demo.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializationNeverContainsPasswordOrHash() throws Exception {
        String bcryptHash = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());
        User user = new User("alice", "alice@example.com", bcryptHash);

        String serializedUser = objectMapper.writeValueAsString(user);
        JsonNode userJson = objectMapper.readTree(serializedUser);

        assertThat(userJson.has("password")).isFalse();
        assertThat(serializedUser).doesNotContain(bcryptHash);
    }

    @Test
    void deserializationStillAcceptsPasswordForLegacyInputCompatibility() throws Exception {
        String password = UUID.randomUUID().toString();
        String userJson = """
                {
                  "username": "alice",
                  "email": "alice@example.com",
                  "password": "%s"
                }
                """.formatted(password);

        User user = objectMapper.readValue(userJson, User.class);

        assertThat(user.getPassword()).isEqualTo(password);
    }
}
