package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.entity.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailNotificationSenderTest {

    @Mock
    JavaMailSender mailSender;

    EmailNotificationSender sender;

    private static final OrderCompletedEvent EVENT = new OrderCompletedEvent(
            "evt-1", "ORDER_COMPLETED", Instant.now(), 100L, 42L, "customer@example.com",
            new BigDecimal("500.00"), List.of(new OrderCompletedEvent.Item(1L, "Chocolate Truffle", 1, new BigDecimal("500.00"))));

    @BeforeEach
    void setUp() {
        sender = new EmailNotificationSender(mailSender);
        ReflectionTestUtils.setField(sender, "fromAddress", "cakedelight.donotreply@gmail.com");
    }

    @Test
    void send_buildsAndSendsMessageToUserEmail() {
        sender.send(EVENT);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();

        assertThat(message.getFrom()).isEqualTo("cakedelight.donotreply@gmail.com");
        assertThat(message.getTo()).containsExactly("customer@example.com");
        assertThat(message.getSubject()).contains("100");
        assertThat(message.getText()).contains("Chocolate Truffle").contains("500.00");
    }

    @Test
    void send_whenUserEmailMissing_throwsWithoutCallingMailSender() {
        OrderCompletedEvent noEmail = new OrderCompletedEvent(
                "evt-2", "ORDER_COMPLETED", Instant.now(), 101L, 42L, null,
                new BigDecimal("100.00"), List.of());

        assertThatThrownBy(() -> sender.send(noEmail)).isInstanceOf(IllegalStateException.class);

        verify(mailSender, never()).send((SimpleMailMessage) org.mockito.ArgumentMatchers.any());
    }

    @Test
    void channel_returnsEmail() {
        assertThat(sender.channel()).isEqualTo(NotificationChannel.EMAIL);
    }
}
