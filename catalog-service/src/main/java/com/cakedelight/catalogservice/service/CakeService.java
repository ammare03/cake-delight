package com.cakedelight.catalogservice.service;

import com.cakedelight.catalogservice.dto.request.CreateCakeRequest;
import com.cakedelight.catalogservice.dto.request.UpdateCakeRequest;
import com.cakedelight.catalogservice.dto.response.CakeResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CakeService {

    List<CakeResponse> search(String name, String category, BigDecimal minPrice, BigDecimal maxPrice);

    CakeResponse getCakeById(Long id);

    CakeResponse createCake(CreateCakeRequest request, String callerRole);

    CakeResponse updateCake(Long id, UpdateCakeRequest request, String callerRole);

    void deleteCake(Long id, String callerRole);
}
