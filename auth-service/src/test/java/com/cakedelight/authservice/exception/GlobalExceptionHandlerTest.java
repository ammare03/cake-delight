package com.cakedelight.authservice.exception;

import com.cakedelight.authservice.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    HttpServletRequest request;

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleEmailExists_returnsConflictAndErrorBody() {
        when(request.getRequestURI()).thenReturn("/auth/register");
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("taken@example.com");

        ResponseEntity<ErrorResponse> response = handler.handleEmailExists(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().status()).isEqualTo(409);
        assertThat(response.getBody().path()).isEqualTo("/auth/register");
    }

    @Test
    void handleInvalidCredentials_returnsUnauthorizedAndErrorBody() {
        when(request.getRequestURI()).thenReturn("/auth/login");
        InvalidCredentialsException ex = new InvalidCredentialsException();

        ResponseEntity<ErrorResponse> response = handler.handleInvalidCredentials(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().status()).isEqualTo(401);
    }

    @Test
    void handleValidation_returnsBadRequestWithFieldErrors() {
        when(request.getRequestURI()).thenReturn("/auth/register");
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("registerRequest", "email", "must be a valid email address");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().fieldErrors()).hasSize(1);
        assertThat(response.getBody().fieldErrors().get(0).field()).isEqualTo("email");
        assertThat(response.getBody().fieldErrors().get(0).message()).isEqualTo("must be a valid email address");
    }

    @Test
    void handleUnexpected_returnsGenericMessage_notLeakingExceptionDetails() {
        when(request.getRequestURI()).thenReturn("/auth/login");
        String sensitiveMessage = "database connection string: secret-stuff";
        RuntimeException ex = new RuntimeException(sensitiveMessage);

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Something went wrong");
        assertThat(response.getBody().message()).doesNotContain(sensitiveMessage);
        assertThat(response.getBody().message()).doesNotContain("secret-stuff");
    }
}
