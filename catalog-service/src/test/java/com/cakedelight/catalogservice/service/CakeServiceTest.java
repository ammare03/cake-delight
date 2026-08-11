package com.cakedelight.catalogservice.service;

import com.cakedelight.catalogservice.dto.CakeResponse;
import com.cakedelight.catalogservice.dto.CreateCakeRequest;
import com.cakedelight.catalogservice.dto.UpdateCakeRequest;
import com.cakedelight.catalogservice.entity.Cake;
import com.cakedelight.catalogservice.exception.CakeNotFoundException;
import com.cakedelight.catalogservice.exception.ForbiddenException;
import com.cakedelight.catalogservice.repository.CakeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CakeServiceTest {

    @Mock
    CakeRepository cakeRepository;

    @InjectMocks
    CakeService cakeService;

    @Test
    void search_passesFiltersThroughToRepositoryAndMapsResults() {
        Cake cake = new Cake();
        cake.setId(1L);
        cake.setName("Chocolate Truffle");
        when(cakeRepository.search("choc", "chocolate", new BigDecimal("100"), new BigDecimal("500")))
                .thenReturn(List.of(cake));

        List<CakeResponse> result = cakeService.search("choc", "chocolate", new BigDecimal("100"), new BigDecimal("500"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Chocolate Truffle");
    }

    @Test
    void getCakeById_whenCakeExists_returnsMappedResponse() {
        Cake cake = new Cake();
        cake.setId(1L);
        cake.setName("Chocolate Truffle");
        when(cakeRepository.findById(1L)).thenReturn(Optional.of(cake));

        CakeResponse result = cakeService.getCakeById(1L);

        assertThat(result.name()).isEqualTo("Chocolate Truffle");
    }

    @Test
    void getCakeById_whenNotFound_throwsCakeNotFoundException() {
        when(cakeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cakeService.getCakeById(999L))
                .isInstanceOf(CakeNotFoundException.class);
    }

    @Test
    void createCake_whenCallerIsNotAdmin_throwsForbiddenException() {
        CreateCakeRequest request = new CreateCakeRequest("Cake", "desc", "chocolate", new BigDecimal("500.00"), true, null);

        assertThatThrownBy(() -> cakeService.createCake(request, "CUSTOMER"))
                .isInstanceOf(ForbiddenException.class);

        verify(cakeRepository, never()).save(any(Cake.class));
    }

    @Test
    void createCake_whenCallerRoleIsNull_throwsForbiddenException() {
        CreateCakeRequest request = new CreateCakeRequest("Cake", "desc", "chocolate", new BigDecimal("500.00"), true, null);

        assertThatThrownBy(() -> cakeService.createCake(request, null))
                .isInstanceOf(ForbiddenException.class);

        verify(cakeRepository, never()).save(any(Cake.class));
    }

    @Test
    void createCake_whenCallerIsAdmin_savesAndReturnsCake() {
        CreateCakeRequest request = new CreateCakeRequest("Cake", "desc", "chocolate", new BigDecimal("500.00"), true, null);
        when(cakeRepository.save(any(Cake.class))).thenAnswer(inv -> {
            Cake c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CakeResponse result = cakeService.createCake(request, "ADMIN");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Cake");
    }

    @Test
    void updateCake_whenCallerIsNotAdmin_throwsForbiddenExceptionWithoutTouchingRepository() {
        UpdateCakeRequest request = new UpdateCakeRequest("Cake", "desc", "chocolate", new BigDecimal("500.00"), true, null);

        assertThatThrownBy(() -> cakeService.updateCake(1L, request, "CUSTOMER"))
                .isInstanceOf(ForbiddenException.class);

        verify(cakeRepository, never()).findById(any());
    }

    @Test
    void updateCake_whenCakeDoesNotExist_throwsCakeNotFoundException() {
        UpdateCakeRequest request = new UpdateCakeRequest("Cake", "desc", "chocolate", new BigDecimal("500.00"), true, null);
        when(cakeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cakeService.updateCake(999L, request, "ADMIN"))
                .isInstanceOf(CakeNotFoundException.class);
    }

    @Test
    void deleteCake_whenCallerIsAdminAndCakeExists_deletesCake() {
        Cake cake = new Cake();
        cake.setId(1L);
        when(cakeRepository.findById(1L)).thenReturn(Optional.of(cake));

        cakeService.deleteCake(1L, "ADMIN");

        verify(cakeRepository).delete(cake);
    }

    @Test
    void deleteCake_whenCallerIsNotAdmin_throwsForbiddenExceptionWithoutDeleting() {
        assertThatThrownBy(() -> cakeService.deleteCake(1L, "CUSTOMER"))
                .isInstanceOf(ForbiddenException.class);

        verify(cakeRepository, never()).delete(any(Cake.class));
    }
}
