package com.cakedelight.catalogservice.service;

import com.cakedelight.catalogservice.dto.CakeResponse;
import com.cakedelight.catalogservice.dto.CreateCakeRequest;
import com.cakedelight.catalogservice.dto.UpdateCakeRequest;
import com.cakedelight.catalogservice.entity.Cake;
import com.cakedelight.catalogservice.exception.CakeNotFoundException;
import com.cakedelight.catalogservice.exception.ForbiddenException;
import com.cakedelight.catalogservice.repository.CakeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CakeService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final CakeRepository cakeRepository;

    @Transactional(readOnly = true)
    public List<CakeResponse> search(String name, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        return cakeRepository.search(name, category, minPrice, maxPrice).stream()
                .map(CakeResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CakeResponse getCakeById(Long id) {
        return CakeResponse.from(findOrThrow(id));
    }

    @Transactional
    public CakeResponse createCake(CreateCakeRequest request, String callerRole) {
        requireAdmin(callerRole);

        Cake cake = new Cake();
        cake.setName(request.name());
        cake.setDescription(request.description());
        cake.setCategory(request.category());
        cake.setPrice(request.price());
        cake.setAvailable(request.available() != null ? request.available() : true);
        cake.setImageUrl(request.imageUrl());

        Cake saved = cakeRepository.save(cake);
        log.info("Created cake {} ({})", saved.getId(), saved.getName());
        return CakeResponse.from(saved);
    }

    @Transactional
    public CakeResponse updateCake(Long id, UpdateCakeRequest request, String callerRole) {
        requireAdmin(callerRole);
        Cake cake = findOrThrow(id);

        cake.setName(request.name());
        cake.setDescription(request.description());
        cake.setCategory(request.category());
        cake.setPrice(request.price());
        cake.setAvailable(request.available());
        cake.setImageUrl(request.imageUrl());

        log.info("Updated cake {}", id);
        return CakeResponse.from(cake);
    }

    @Transactional
    public void deleteCake(Long id, String callerRole) {
        requireAdmin(callerRole);
        Cake cake = findOrThrow(id);
        cakeRepository.delete(cake);
        log.info("Deleted cake {}", id);
    }

    private Cake findOrThrow(Long id) {
        return cakeRepository.findById(id).orElseThrow(() -> new CakeNotFoundException(id));
    }

    private void requireAdmin(String callerRole) {
        if (!ADMIN_ROLE.equalsIgnoreCase(callerRole)) {
            throw new ForbiddenException("Only an admin can perform this action");
        }
    }
}
