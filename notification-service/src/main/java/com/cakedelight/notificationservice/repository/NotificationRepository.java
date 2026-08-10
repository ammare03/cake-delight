package com.cakedelight.notificationservice.repository;

import com.cakedelight.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);

    boolean existsByEventId(String eventId);
}
