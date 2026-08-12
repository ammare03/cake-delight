package com.cakedelight.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCompletedListenerTest {

    @Mock
    NotificationService notificationService;

    @InjectMocks
    OrderCompletedListener listener;

    @Test
    void onOrderCompleted_delegatesToNotificationService() {
        OrderCompletedEvent event = new OrderCompletedEvent(
                "evt-1", "ORDER_COMPLETED", Instant.now(), 100L, 42L, "user@example.com",
                new BigDecimal("500.00"), List.of(new OrderCompletedEvent.Item(1L, "Chocolate Truffle", 1, new BigDecimal("500.00"))));

        listener.onOrderCompleted(event);

        verify(notificationService).recordOrderCompleted(event);
    }
}
