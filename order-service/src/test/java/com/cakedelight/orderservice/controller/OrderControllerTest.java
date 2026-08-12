package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.dto.OrderResponse;
import com.cakedelight.orderservice.exception.EmptyBasketException;
import com.cakedelight.orderservice.exception.GlobalExceptionHandler;
import com.cakedelight.orderservice.exception.OrderNotFoundException;
import com.cakedelight.orderservice.exception.UnauthenticatedException;
import com.cakedelight.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Test
    void checkout_withoutUserIdHeader_returns401() throws Exception {
        when(orderService.checkout(eq(null), eq(null))).thenThrow(new UnauthenticatedException());

        mockMvc.perform(post("/orders/checkout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void checkout_whenBasketEmpty_returns400() throws Exception {
        when(orderService.checkout(eq("42"), eq("user@example.com"))).thenThrow(new EmptyBasketException());

        mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", "42")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BASKET_EMPTY"));
    }

    @Test
    void checkout_whenValid_returns201() throws Exception {
        when(orderService.checkout(eq("42"), eq("user@example.com"))).thenReturn(
                new OrderResponse(1L, 42L, new BigDecimal("500.00"), "COMPLETED", List.of(), Instant.now()));

        mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", "42")
                        .header("X-User-Email", "user@example.com"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void checkout_withoutEmailHeader_stillSucceeds() throws Exception {
        when(orderService.checkout(eq("42"), eq(null))).thenReturn(
                new OrderResponse(1L, 42L, new BigDecimal("500.00"), "COMPLETED", List.of(), Instant.now()));

        mockMvc.perform(post("/orders/checkout").header("X-User-Id", "42"))
                .andExpect(status().isCreated());
    }

    @Test
    void listOrders_returns200_withList() throws Exception {
        when(orderService.listOrders("42")).thenReturn(
                List.of(new OrderResponse(1L, 42L, new BigDecimal("500.00"), "COMPLETED", List.of(), Instant.now())));

        mockMvc.perform(get("/orders").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getOrder_whenNotFound_returns404() throws Exception {
        when(orderService.getOrder("42", 999L)).thenThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/orders/999").header("X-User-Id", "42"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void getOrder_whenFound_returns200() throws Exception {
        when(orderService.getOrder("42", 1L)).thenReturn(
                new OrderResponse(1L, 42L, new BigDecimal("500.00"), "COMPLETED", List.of(), Instant.now()));

        mockMvc.perform(get("/orders/1").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
