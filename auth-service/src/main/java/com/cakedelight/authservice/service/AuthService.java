package com.cakedelight.authservice.service;

import com.cakedelight.authservice.dto.request.LoginRequest;
import com.cakedelight.authservice.dto.request.RegisterRequest;
import com.cakedelight.authservice.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
