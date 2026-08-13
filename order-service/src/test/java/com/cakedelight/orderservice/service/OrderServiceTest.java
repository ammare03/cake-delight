package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.dto.OrderResponse;
import com.cakedelight.orderservice.entity.Basket;
import com.cakedelight.orderservice.entity.BasketItem;
import com.cakedelight.orderservice.entity.Order;
import com.cakedelight.orderservice.entity.OrderStatus;
import com.cakedelight.orderservice.exception.EmptyBasketException;
import com.cakedelight.orderservice.exception.OrderNotFoundException;
import com.cakedelight.orderservice.exception.UnauthenticatedException;
import com.cakedelight.orderservice.repository.BasketRepository;
import com.cakedelight.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orderRepository;

    @Mock
    BasketRepository basketRepository;

    @Mock
    ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    OrderService orderService;

    @Test
    void checkout_whenUserIdHeaderMissing_throwsUnauthenticatedException() {
        assertThatThrownBy(() -> orderService.checkout(null, "user@example.com"))
                .isInstanceOf(UnauthenticatedException.class);

        verify(basketRepository, never()).findByUserId(any());
    }

    @Test
    void checkout_whenBasketMissing_throwsEmptyBasketException() {
        when(basketRepository.findByUserId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout("42", "user@example.com"))
                .isInstanceOf(EmptyBasketException.class);

        verify(orderRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkout_whenBasketEmpty_throwsEmptyBasketException() {
        Basket basket = new Basket();
        basket.setUserId(42L);
        when(basketRepository.findByUserId(42L)).thenReturn(Optional.of(basket));

        assertThatThrownBy(() -> orderService.checkout("42", "user@example.com"))
                .isInstanceOf(EmptyBasketException.class);
    }

    @Test
    void checkout_whenBasketHasItems_createsOrderPublishesEventAndClearsBasket() {
        Basket basket = new Basket();
        basket.setId(10L);
        basket.setUserId(42L);
        BasketItem item1 = basketItem(1L, "Chocolate Truffle", "500.00", 2);
        BasketItem item2 = basketItem(2L, "Vanilla Dream", "300.00", 1);
        basket.getItems().add(item1);
        basket.getItems().add(item2);

        when(basketRepository.findByUserId(42L)).thenReturn(Optional.of(basket));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderResponse result = orderService.checkout("42", "user@example.com");

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.totalAmount()).isEqualByComparingTo("1300.00");
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.items()).hasSize(2);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getUserId()).isEqualTo(42L);
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo("1300.00");
        assertThat(savedOrder.getItems()).hasSize(2);
        assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

        ArgumentCaptor<OrderCheckedOutEvent> eventCaptor = ArgumentCaptor.forClass(OrderCheckedOutEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userEmail()).isEqualTo("user@example.com");
        assertThat(eventCaptor.getValue().order()).isSameAs(savedOrder);

        assertThat(basket.getItems()).isEmpty();
        verify(basketRepository).save(basket);
    }

    @Test
    void hasPurchased_delegatesToRepository() {
        when(orderRepository.existsByUserIdAndItems_CakeId(42L, 1L)).thenReturn(true);

        assertThat(orderService.hasPurchased(42L, 1L)).isTrue();
    }

    @Test
    void hasPurchased_whenNeverPurchased_returnsFalse() {
        when(orderRepository.existsByUserIdAndItems_CakeId(42L, 1L)).thenReturn(false);

        assertThat(orderService.hasPurchased(42L, 1L)).isFalse();
    }

    @Test
    void listOrders_returnsMappedResponses() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(42L);
        order.setTotalAmount(BigDecimal.TEN);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByUserId(42L)).thenReturn(List.of(order));

        List<OrderResponse> result = orderService.listOrders("42");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }

    @Test
    void getOrder_whenNotFound_throwsOrderNotFoundException() {
        when(orderRepository.findByIdAndUserId(999L, 42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder("42", 999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrder_whenFound_returnsResponse() {
        Order order = new Order();
        order.setId(1L);
        order.setUserId(42L);
        order.setTotalAmount(BigDecimal.TEN);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndUserId(1L, 42L)).thenReturn(Optional.of(order));

        OrderResponse result = orderService.getOrder("42", 1L);

        assertThat(result.id()).isEqualTo(1L);
    }

    private BasketItem basketItem(Long cakeId, String name, String price, int quantity) {
        BasketItem item = new BasketItem();
        item.setCakeId(cakeId);
        item.setCakeNameSnapshot(name);
        item.setUnitPriceSnapshot(new BigDecimal(price));
        item.setQuantity(quantity);
        return item;
    }
}
