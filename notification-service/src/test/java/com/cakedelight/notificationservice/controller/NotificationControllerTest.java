package com.cakedelight.notificationservice.controller;

import com.cakedelight.notificationservice.dto.NotificationResponse;
import com.cakedelight.notificationservice.exception.GlobalExceptionHandler;
import com.cakedelight.notificationservice.exception.UnauthenticatedException;
import com.cakedelight.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    NotificationService notificationService;

    @Test
    void listNotifications_withoutUserIdHeader_returns401() throws Exception {
        when(notificationService.listNotifications(null)).thenThrow(new UnauthenticatedException());

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void listNotifications_returns200_withList() throws Exception {
        when(notificationService.listNotifications("42")).thenReturn(
                List.of(new NotificationResponse(1L, 100L, "IN_APP", "SENT", "{}", Instant.now())));

        mockMvc.perform(get("/notifications").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SENT"));
    }
}
