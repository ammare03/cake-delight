package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalOrderController.class)
class InternalOrderControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    OrderService orderService;

    @Test
    void checkPurchase_whenPurchased_returnsTrue() throws Exception {
        when(orderService.hasPurchased(42L, 1L)).thenReturn(true);

        mockMvc.perform(get("/internal/orders/purchases").param("userId", "42").param("cakeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(true));
    }

    @Test
    void checkPurchase_whenNeverPurchased_returnsFalse() throws Exception {
        when(orderService.hasPurchased(42L, 1L)).thenReturn(false);

        mockMvc.perform(get("/internal/orders/purchases").param("userId", "42").param("cakeId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(false));
    }
}
