package com.cakedelight.authservice.service;

import com.cakedelight.authservice.config.JwtProperties;
import com.cakedelight.authservice.dto.request.LoginRequest;
import com.cakedelight.authservice.dto.request.RegisterRequest;
import com.cakedelight.authservice.dto.response.AuthResponse;
import com.cakedelight.authservice.entity.Role;
import com.cakedelight.authservice.entity.User;
import com.cakedelight.authservice.exception.EmailAlreadyExistsException;
import com.cakedelight.authservice.exception.InvalidCredentialsException;
import com.cakedelight.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    JwtProperties jwtProperties;

    @InjectMocks
    AuthService authService;

    @Test
    void register_whenEmailAlreadyExists_throwsEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("existing@example.com", "password123");
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_whenEmailIsNew_savesUserWithEncodedPasswordAndCustomerRole() {
        RegisterRequest request = new RegisterRequest("new@example.com", "rawPassword1");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword1")).thenReturn("encoded-hash");
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("new@example.com");
        savedUser.setPasswordHash("encoded-hash");
        savedUser.setRole(Role.CUSTOMER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(capturedUser.getPasswordHash()).isNotEqualTo("rawPassword1");
        assertThat(capturedUser.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void register_whenSuccessful_returnsAuthResponseFromJwtService() {
        RegisterRequest request = new RegisterRequest("new@example.com", "rawPassword1");
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-hash");
        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("new@example.com");
        savedUser.setPasswordHash("encoded-hash");
        savedUser.setRole(Role.CUSTOMER);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(savedUser)).thenReturn("generated-jwt-token");
        when(jwtProperties.getExpirationMs()).thenReturn(3600000L);

        AuthResponse response = authService.register(request);

        assertThat(response.token()).isEqualTo("generated-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(3600000L);
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.role()).isEqualTo("CUSTOMER");
    }

    @Test
    void login_whenUserNotFound_throwsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsInvalidCredentialsException() {
        LoginRequest wrongPasswordRequest = new LoginRequest("user@example.com", "wrongPassword");
        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setEmail("user@example.com");
        existingUser.setPasswordHash("encoded-hash");
        existingUser.setRole(Role.CUSTOMER);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", "encoded-hash")).thenReturn(false);

        LoginRequest noSuchUserRequest = new LoginRequest("missing@example.com", "anyPassword");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(wrongPasswordRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
        assertThatThrownBy(() -> authService.login(noSuchUserRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid email or password");
    }

    @Test
    void login_whenCredentialsValid_returnsAuthResponse() {
        LoginRequest request = new LoginRequest("user@example.com", "correctPassword");
        User existingUser = new User();
        existingUser.setId(7L);
        existingUser.setEmail("user@example.com");
        existingUser.setPasswordHash("encoded-hash");
        existingUser.setRole(Role.ADMIN);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctPassword", "encoded-hash")).thenReturn(true);
        when(jwtService.generateToken(existingUser)).thenReturn("valid-jwt-token");
        when(jwtProperties.getExpirationMs()).thenReturn(7200000L);

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("valid-jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInMs()).isEqualTo(7200000L);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo("ADMIN");
    }
}
