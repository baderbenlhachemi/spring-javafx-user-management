package com.badereddine.demo.exception;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRestExceptionHandlerTest {

    private static final String SENSITIVE_DETAIL = "jdbc:postgresql://admin:secret@internal/users";

    private final UserRestExceptionHandler handler = new UserRestExceptionHandler();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsStableValidationErrorWithoutBindingDetails() throws Exception {
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getMessage()).thenReturn(SENSITIVE_DETAIL);

        assertErrorResponse(
                handler.handleValidationException(exception),
                HttpStatus.BAD_REQUEST,
                UserRestExceptionHandler.VALIDATION_ERROR_MESSAGE
        );
    }

    @Test
    void returnsStableMalformedJsonErrorWithoutParserDetails() throws Exception {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
        when(exception.getMessage()).thenReturn(SENSITIVE_DETAIL);

        assertErrorResponse(
                handler.handleMalformedJsonException(exception),
                HttpStatus.BAD_REQUEST,
                UserRestExceptionHandler.MALFORMED_JSON_ERROR_MESSAGE
        );
    }

    @Test
    void preservesNotFoundMappingWithoutExposingExceptionMessage() throws Exception {
        assertErrorResponse(
                handler.handleUserNotFoundException(new UserNotFoundException(SENSITIVE_DETAIL)),
                HttpStatus.NOT_FOUND,
                UserRestExceptionHandler.NOT_FOUND_ERROR_MESSAGE
        );
    }

    @Test
    void preservesAuthenticationMappingWithoutExposingExceptionMessage() throws Exception {
        assertErrorResponse(
                handler.handleInvalidPasswordException(new InvalidPasswordException(SENSITIVE_DETAIL)),
                HttpStatus.UNAUTHORIZED,
                UserRestExceptionHandler.AUTHENTICATION_ERROR_MESSAGE
        );
    }

    @Test
    void preservesAuthorizationMappingWithoutExposingExceptionMessage() throws Exception {
        assertErrorResponse(
                handler.handleAccessDeniedException(new AccessDeniedException(SENSITIVE_DETAIL)),
                HttpStatus.FORBIDDEN,
                UserRestExceptionHandler.AUTHORIZATION_ERROR_MESSAGE
        );
    }

    @Test
    void preservesConflictMappingWithStableMessage() throws Exception {
        assertErrorResponse(
                handler.handleLastActiveAdminException(new LastActiveAdminException()),
                HttpStatus.CONFLICT,
                LastActiveAdminException.MESSAGE
        );
    }

    @Test
    void preservesInternalServerErrorMappingWithoutExposingIOExceptionMessage() throws Exception {
        assertErrorResponse(
                handler.handleIOException(new IOException(SENSITIVE_DETAIL)),
                HttpStatus.INTERNAL_SERVER_ERROR,
                UserRestExceptionHandler.INTERNAL_ERROR_MESSAGE
        );
    }

    @Test
    void preservesUnexpectedFailureMappingAndLogsOnlyRedactedDiagnostic() throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(UserRestExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            assertErrorResponse(
                    handler.handleUnexpectedException(new IllegalStateException(SENSITIVE_DETAIL)),
                    HttpStatus.BAD_REQUEST,
                    UserRestExceptionHandler.BAD_REQUEST_ERROR_MESSAGE
            );

            assertThat(appender.list)
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .containsExactly("Unexpected request failure; exceptionType=java.lang.IllegalStateException")
                    .allMatch(message -> !message.contains(SENSITIVE_DETAIL));
            assertThat(appender.list)
                    .allMatch(event -> event.getThrowableProxy() == null);
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private void assertErrorResponse(
            ResponseEntity<UserErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) throws Exception {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(response.getBody()));
        assertThat(json.properties()).extracting(entry -> entry.getKey())
                .containsExactlyInAnyOrderElementsOf(Set.of("status", "message", "timeStamp"));
        assertThat(json.get("status").asInt()).isEqualTo(expectedStatus.value());
        assertThat(json.get("message").asText()).isEqualTo(expectedMessage);
        assertThat(json.get("message").asText()).doesNotContain(SENSITIVE_DETAIL);
        assertThat(json.get("timeStamp").isNumber()).isTrue();
        assertThat(json.get("timeStamp").asLong()).isPositive();
    }
}
