package com.cakedelight.catalogservice.controller;

import com.cakedelight.catalogservice.dto.CakeResponse;
import com.cakedelight.catalogservice.dto.CreateCakeRequest;
import com.cakedelight.catalogservice.dto.UpdateCakeRequest;
import com.cakedelight.catalogservice.service.CakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/catalog/cakes")
@RequiredArgsConstructor
public class CakeController {

    private final CakeService cakeService;

    @Operation(summary = "List cakes, optionally filtered by name/category/price range")
    @GetMapping
    public List<CakeResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice
    ) {
        return cakeService.search(name, category, minPrice, maxPrice);
    }

    @Operation(summary = "Get a cake by id")
    @ApiResponse(responseCode = "200", description = "Cake found")
    @ApiResponse(responseCode = "404", description = "Cake not found")
    @GetMapping("/{id}")
    public CakeResponse getCake(@PathVariable Long id) {
        return cakeService.getCakeById(id);
    }

    @Operation(summary = "Create a cake (admin only)")
    @ApiResponse(responseCode = "201", description = "Cake created")
    @ApiResponse(responseCode = "403", description = "Caller is not an admin")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CakeResponse createCake(
            @Valid @RequestBody CreateCakeRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        return cakeService.createCake(request, role);
    }

    @Operation(summary = "Replace a cake (admin only)")
    @ApiResponse(responseCode = "200", description = "Cake updated")
    @ApiResponse(responseCode = "403", description = "Caller is not an admin")
    @ApiResponse(responseCode = "404", description = "Cake not found")
    @PutMapping("/{id}")
    public CakeResponse updateCake(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCakeRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        return cakeService.updateCake(id, request, role);
    }

    @Operation(summary = "Delete a cake (admin only)")
    @ApiResponse(responseCode = "204", description = "Cake deleted")
    @ApiResponse(responseCode = "403", description = "Caller is not an admin")
    @ApiResponse(responseCode = "404", description = "Cake not found")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCake(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String role
    ) {
        cakeService.deleteCake(id, role);
        return ResponseEntity.noContent().build();
    }
}
