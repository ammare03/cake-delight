package com.cakedelight.notificationservice.controller;

import com.cakedelight.notificationservice.dto.NotificationResponse;
import com.cakedelight.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "List the current user's notifications")
    @GetMapping
    public List<NotificationResponse> listNotifications(@RequestHeader(value = "X-User-Id", required = false) String userId) {
        return notificationService.listNotifications(userId);
    }
}
