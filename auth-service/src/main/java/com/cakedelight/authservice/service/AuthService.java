package com.cakedelight.authservice.service;

import com.cakedelight.authservice.dto.AuthResponse;
import com.cakedelight.authservice.dto.LoginRequest;
import com.cakedelight.authservice.dto.RegisterRequest;
import com.cakedelight.authservice.entity.Role;
import com.cakedelight.authservice.entity.User;
import com.cakedelight.authservice.exception.EmailAlreadyExistsException;
import com.cakedelight.authservice.exception.InvalidCredentialsException;
import com.cakedelight.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    // No SecurityConfig/@Bean for this — the gateway is the trust boundary
    // (CLAUDE.md §4), this service has no endpoints to lock down, and BCrypt
    // needs no configuration beyond the default work factor.
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.CUSTOMER);

        User saved = userRepository.save(user);
        log.info("Registered new user id={}", saved.getId());

        return buildAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        // Same exception for "no such user" and "wrong password" — don't leak which one.
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        log.info("User id={} logged in", user.getId());
        return buildAuthResponse(user);
    }

    private AuthResponse buildAuthResponse(User user) {
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationMs(), user.getEmail(), user.getRole().name());
    }
}
