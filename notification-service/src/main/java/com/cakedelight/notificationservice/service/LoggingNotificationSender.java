package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.entity.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.notification", name = "channel", havingValue = "log")
@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(OrderCompletedEvent event) {
        log.info("Notification sent: order {} completed for user {} ({}), total {}",
                event.orderId(), event.userId(), event.userEmail(), event.totalAmount());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }
}
