package com.cakedelight.catalogservice.service;

import com.cakedelight.catalogservice.dto.request.CreateCakeRequest;
import com.cakedelight.catalogservice.dto.request.UpdateCakeRequest;
import com.cakedelight.catalogservice.dto.response.CakeResponse;
import com.cakedelight.catalogservice.entity.Cake;
import com.cakedelight.catalogservice.exception.CakeNotFoundException;
import com.cakedelight.catalogservice.exception.ForbiddenException;
import com.cakedelight.catalogservice.mapper.CakeMapper;
import com.cakedelight.catalogservice.repository.CakeRepository;
import com.cakedelight.catalogservice.repository.CakeSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CakeService {

    // The role string the JWT carries for admins (see auth-service's Role
    // enum — CUSTOMER/ADMIN). No shared JAR (CLAUDE.md §10), so this is
    // duplicated as a literal rather than an imported enum.
    private static final String ADMIN_ROLE = "ADMIN";

    private final CakeRepository cakeRepository;
    private final CakeMapper cakeMapper;

    @Transactional(readOnly = true)
    public List<CakeResponse> search(String name, String category, BigDecimal minPrice, BigDecimal maxPrice) {
        // isAvailable() is unconditional, not one of the optional name/category/
        // price filters — browse never shows unavailable cakes (SD-C1). Admins
        // still reach an unavailable cake directly via getCakeById/update/delete,
        // which don't go through this specification.
        Specification<Cake> spec = Specification.allOf(
                CakeSpecifications.isAvailable(),
                CakeSpecifications.hasName(name),
                CakeSpecifications.hasCategory(category),
                CakeSpecifications.minPrice(minPrice),
                CakeSpecifications.maxPrice(maxPrice)
        );
        return cakeRepository.findAll(spec).stream().map(cakeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CakeResponse getCakeById(Long id) {
        return cakeMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public CakeResponse createCake(CreateCakeRequest request, String callerRole) {
        requireAdmin(callerRole);
        Cake saved = cakeRepository.save(cakeMapper.toEntity(request));
        log.info("Created cake {} ({})", saved.getId(), saved.getName());
        return cakeMapper.toResponse(saved);
    }

    @Transactional
    public CakeResponse updateCake(Long id, UpdateCakeRequest request, String callerRole) {
        requireAdmin(callerRole);
        Cake cake = findOrThrow(id);
        cakeMapper.updateEntity(cake, request);
        log.info("Updated cake {}", id);
        return cakeMapper.toResponse(cake);
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
