package com.cakedelight.orderservice.service;

import com.cakedelight.orderservice.dto.response.OrderResponse;
import com.cakedelight.orderservice.entity.Basket;
import com.cakedelight.orderservice.entity.BasketItem;
import com.cakedelight.orderservice.entity.Order;
import com.cakedelight.orderservice.entity.OrderItem;
import com.cakedelight.orderservice.entity.OrderStatus;
import com.cakedelight.orderservice.event.OrderCheckedOutEvent;
import com.cakedelight.orderservice.exception.EmptyBasketException;
import com.cakedelight.orderservice.exception.OrderNotFoundException;
import com.cakedelight.orderservice.exception.UnauthenticatedException;
import com.cakedelight.orderservice.mapper.OrderMapper;
import com.cakedelight.orderservice.repository.BasketRepository;
import com.cakedelight.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final BasketRepository basketRepository;
    private final OrderMapper orderMapper;
    // Publishes an *internal* Spring event, not the Kafka one directly —
    // OrderCheckoutEventListener picks it up with @TransactionalEventListener
    // (phase = AFTER_COMMIT) so the Kafka send can never happen for a
    // checkout that ends up rolling back. See OrderCheckedOutEvent's javadoc.
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public OrderResponse checkout(String rawUserId, String userEmail) {
        Long userId = parseUserId(rawUserId);
        Basket basket = basketRepository.findByUserId(userId)
                .filter(b -> !b.getItems().isEmpty())
                .orElseThrow(EmptyBasketException::new);

        Order order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;
        for (BasketItem basketItem : basket.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setCakeId(basketItem.getCakeId());
            orderItem.setCakeName(basketItem.getCakeNameSnapshot());
            // Checkout trusts the basket's snapshot price rather than calling
            // catalog-service again — see BasketItem's cakeNameSnapshot/
            // unitPriceSnapshot fields for the documented reasoning.
            orderItem.setUnitPrice(basketItem.getUnitPriceSnapshot());
            orderItem.setQuantity(basketItem.getQuantity());
            order.getItems().add(orderItem);
            total = total.add(basketItem.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(basketItem.getQuantity())));
        }
        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);

        basket.getItems().clear();
        basketRepository.save(basket);

        // The one real status transition (SD-O3) — created, then completed,
        // both within this transaction. Checkout is a stub with nothing left
        // to do after this (CLAUDE.md §12), so COMPLETED is terminal; the
        // point isn't a longer lifecycle, it's that the column isn't
        // structurally stuck on a single literal value.
        saved.setStatus(OrderStatus.COMPLETED);
        saved = orderRepository.save(saved);

        applicationEventPublisher.publishEvent(new OrderCheckedOutEvent(saved, userEmail));

        log.info("Checked out order {} for user {} (total {})", saved.getId(), userId, total);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders(String rawUserId) {
        Long userId = parseUserId(rawUserId);
        return orderRepository.findByUserId(userId).stream().map(orderMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String rawUserId, Long orderId) {
        Long userId = parseUserId(rawUserId);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPurchased(Long userId, Long cakeId) {
        return orderRepository.existsByUserIdAndItems_CakeId(userId, cakeId);
    }

    private Long parseUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new UnauthenticatedException();
        }
        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ex) {
            throw new UnauthenticatedException();
        }
    }
}
