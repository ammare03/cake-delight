package com.cakedelight.notificationservice.mapper;

import com.cakedelight.notificationservice.dto.response.NotificationResponse;
import com.cakedelight.notificationservice.entity.Notification;
import org.springframework.stereotype.Component;

// Manual mapping, not MapStruct — see catalog-service's CakeMapper for the
// same reasoning; consistent across services.
@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getOrderId(),
                notification.getChannel().name(),
                notification.getStatus().name(),
                notification.getPayload(),
                notification.getCreatedAt()
        );
    }
}
