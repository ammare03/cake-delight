package com.cakedelight.orderservice.event;

import com.cakedelight.orderservice.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutEventListenerTest {

    @Mock
    OrderEventPublisher eventPublisher;

    @InjectMocks
    OrderCheckoutEventListener listener;

    @Test
    void onOrderCheckedOut_delegatesToKafkaPublisher() {
        Order order = new Order();
        order.setId(100L);
        OrderCheckedOutEvent event = new OrderCheckedOutEvent(order, "user@example.com");

        listener.onOrderCheckedOut(event);

        verify(eventPublisher).publishOrderCompleted(order, "user@example.com");
    }
}
