package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.entity.NotificationChannel;

public interface NotificationSender {
    void send(OrderCompletedEvent event);

    NotificationChannel channel();
}
