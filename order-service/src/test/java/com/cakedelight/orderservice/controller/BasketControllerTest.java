package com.cakedelight.orderservice.controller;

import com.cakedelight.orderservice.dto.response.BasketResponse;
import com.cakedelight.orderservice.exception.BasketItemNotFoundException;
import com.cakedelight.orderservice.exception.CakeNotFoundException;
import com.cakedelight.orderservice.exception.CakeUnavailableException;
import com.cakedelight.orderservice.exception.GlobalExceptionHandler;
import com.cakedelight.orderservice.exception.UnauthenticatedException;
import com.cakedelight.orderservice.service.BasketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BasketController.class)
@Import(GlobalExceptionHandler.class)
class BasketControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BasketService basketService;

    @Test
    void getBasket_returns200_withBasketJson() throws Exception {
        when(basketService.getBasket("42")).thenReturn(
                new BasketResponse(1L, 42L, List.of(), BigDecimal.ZERO));

        mockMvc.perform(get("/orders/basket").header("X-User-Id", "42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(42));
    }

    @Test
    void addItem_withoutUserIdHeader_returns401() throws Exception {
        when(basketService.addItem(eq(null), any())).thenThrow(new UnauthenticatedException());
        String body = """
                { "cakeId": 1, "quantity": 2 }
                """;

        mockMvc.perform(post("/orders/basket/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void addItem_withNonPositiveQuantity_returns400() throws Exception {
        String body = """
                { "cakeId": 1, "quantity": 0 }
                """;

        mockMvc.perform(post("/orders/basket/items")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void addItem_whenCakeNotFound_returns404() throws Exception {
        when(basketService.addItem(eq("42"), any())).thenThrow(new CakeNotFoundException(999L));
        String body = """
                { "cakeId": 999, "quantity": 1 }
                """;

        mockMvc.perform(post("/orders/basket/items")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAKE_NOT_FOUND"));
    }

    @Test
    void addItem_whenCakeUnavailable_returns409() throws Exception {
        when(basketService.addItem(eq("42"), any())).thenThrow(new CakeUnavailableException(1L));
        String body = """
                { "cakeId": 1, "quantity": 1 }
                """;

        mockMvc.perform(post("/orders/basket/items")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAKE_UNAVAILABLE"));
    }

    @Test
    void addItem_whenValid_returns201() throws Exception {
        when(basketService.addItem(eq("42"), any())).thenReturn(
                new BasketResponse(1L, 42L, List.of(), new BigDecimal("1000.00")));
        String body = """
                { "cakeId": 1, "quantity": 2 }
                """;

        mockMvc.perform(post("/orders/basket/items")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateItem_whenNotFound_returns404() throws Exception {
        when(basketService.updateItem(eq("42"), eq(999L), any())).thenThrow(new BasketItemNotFoundException(999L));
        String body = """
                { "quantity": 3 }
                """;

        mockMvc.perform(put("/orders/basket/items/999")
                        .header("X-User-Id", "42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BASKET_ITEM_NOT_FOUND"));
    }

    @Test
    void removeItem_returns204() throws Exception {
        mockMvc.perform(delete("/orders/basket/items/5").header("X-User-Id", "42"))
                .andExpect(status().isNoContent());
    }
}
