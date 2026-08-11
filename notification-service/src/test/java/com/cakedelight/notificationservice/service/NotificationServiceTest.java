package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.dto.NotificationResponse;
import com.cakedelight.notificationservice.entity.Notification;
import com.cakedelight.notificationservice.entity.NotificationChannel;
import com.cakedelight.notificationservice.entity.NotificationStatus;
import com.cakedelight.notificationservice.exception.UnauthenticatedException;
import com.cakedelight.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    NotificationRepository notificationRepository;

    @Mock
    NotificationSender notificationSender;

    final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    NotificationService notificationService;

    private static final OrderCompletedEvent EVENT = new OrderCompletedEvent(
            "evt-1", "ORDER_COMPLETED", Instant.now(), 100L, 42L, "user@example.com",
            new BigDecimal("500.00"), List.of(new OrderCompletedEvent.Item(1L, "Chocolate Truffle", 1, new BigDecimal("500.00"))));

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, notificationSender, objectMapper);
    }

    @Test
    void recordOrderCompleted_whenSendSucceeds_savesNotificationAsSent() {
        when(notificationRepository.existsByEventId("evt-1")).thenReturn(false);
        when(notificationSender.channel()).thenReturn(NotificationChannel.IN_APP);

        notificationService.recordOrderCompleted(EVENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getOrderId()).isEqualTo(100L);
        assertThat(saved.getEventId()).isEqualTo("evt-1");
        assertThat(saved.getChannel()).isEqualTo(NotificationChannel.IN_APP);
        assertThat(saved.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(saved.getPayload()).contains("evt-1");
        verify(notificationSender).send(EVENT);
    }

    @Test
    void recordOrderCompleted_whenSendThrows_savesNotificationAsFailed() {
        when(notificationRepository.existsByEventId("evt-1")).thenReturn(false);
        when(notificationSender.channel()).thenReturn(NotificationChannel.EMAIL);
        doThrow(new RuntimeException("SMTP unreachable")).when(notificationSender).send(EVENT);

        notificationService.recordOrderCompleted(EVENT);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }

    @Test
    void recordOrderCompleted_whenEventAlreadyProcessed_skipsWithoutSavingAgain() {
        when(notificationRepository.existsByEventId("evt-1")).thenReturn(true);

        notificationService.recordOrderCompleted(EVENT);

        verify(notificationRepository, never()).save(any());
        verify(notificationSender, never()).send(any());
    }

    @Test
    void listNotifications_whenUserIdHeaderMissing_throwsUnauthenticatedException() {
        assertThatThrownBy(() -> notificationService.listNotifications(null))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void listNotifications_whenUserIdHeaderNotNumeric_throwsUnauthenticatedException() {
        assertThatThrownBy(() -> notificationService.listNotifications("not-a-number"))
                .isInstanceOf(UnauthenticatedException.class);
    }

    @Test
    void listNotifications_returnsMappedResponses() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setOrderId(100L);
        notification.setChannel(NotificationChannel.IN_APP);
        notification.setStatus(NotificationStatus.SENT);
        notification.setPayload("{}");
        when(notificationRepository.findByUserId(42L)).thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.listNotifications("42");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(1L);
    }
}
