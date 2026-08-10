package com.cakedelight.catalogservice.controller;

import com.cakedelight.catalogservice.dto.response.CakeResponse;
import com.cakedelight.catalogservice.exception.CakeNotFoundException;
import com.cakedelight.catalogservice.exception.ForbiddenException;
import com.cakedelight.catalogservice.exception.GlobalExceptionHandler;
import com.cakedelight.catalogservice.service.CakeService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// GlobalExceptionHandler isn't picked up automatically by the @WebMvcTest
// slice unless imported — it lives in the exception package, outside the
// default component scan this slice restricts itself to.
@WebMvcTest(controllers = CakeController.class)
@Import(GlobalExceptionHandler.class)
class CakeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    CakeService cakeService;

    @Test
    void list_returns200_withFilteredCakes() throws Exception {
        CakeResponse cake = new CakeResponse(1L, "Chocolate Truffle", "desc", "chocolate",
                new BigDecimal("500.00"), true, null, null);
        when(cakeService.search(isNull(), eq("chocolate"), isNull(), any())).thenReturn(List.of(cake));

        mockMvc.perform(get("/catalog/cakes").param("category", "chocolate").param("maxPrice", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Chocolate Truffle"));
    }

    @Test
    void getCake_returns200_withCakeJson() throws Exception {
        when(cakeService.getCakeById(1L)).thenReturn(
                new CakeResponse(1L, "Chocolate Truffle", "desc", "chocolate", new BigDecimal("500.00"), true, null, null));

        mockMvc.perform(get("/catalog/cakes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Chocolate Truffle"))
                .andExpect(jsonPath("$.price").value(500.00));
    }

    @Test
    void getCake_whenNotFound_returns404() throws Exception {
        when(cakeService.getCakeById(999L)).thenThrow(new CakeNotFoundException(999L));

        mockMvc.perform(get("/catalog/cakes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAKE_NOT_FOUND"));
    }

    @Test
    void createCake_withBlankName_returns400() throws Exception {
        String body = """
                { "name": "", "description": "yum", "category": "chocolate", "price": 500 }
                """;

        mockMvc.perform(post("/catalog/cakes")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void createCake_whenCallerNotAdmin_returns403() throws Exception {
        when(cakeService.createCake(any(), eq("CUSTOMER"))).thenThrow(new ForbiddenException("Only an admin can perform this action"));
        String body = """
                { "name": "Cake", "description": "yum", "category": "chocolate", "price": 500 }
                """;

        mockMvc.perform(post("/catalog/cakes")
                        .header("X-User-Role", "CUSTOMER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createCake_whenCallerIsAdmin_returns201() throws Exception {
        CakeResponse created = new CakeResponse(1L, "Cake", "yum", "chocolate", new BigDecimal("500.00"), true, null, null);
        when(cakeService.createCake(any(), eq("ADMIN"))).thenReturn(created);
        String body = """
                { "name": "Cake", "description": "yum", "category": "chocolate", "price": 500 }
                """;

        mockMvc.perform(post("/catalog/cakes")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteCake_whenCallerIsAdmin_returns204() throws Exception {
        mockMvc.perform(delete("/catalog/cakes/1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());
    }
}
