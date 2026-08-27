package io.github.renatoxico.extract.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.DeliveryClaim;
import io.github.renatoxico.extract.repo.AdminEmailOutboxRepository.WeeklyMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEmailServiceTest {
    @Mock
    private AdminEmailOutboxRepository repository;
    @Mock
    private TransactionTemplate transactions;
    @Mock
    private JavaMailSender mailSender;

    private ObjectMapper objectMapper;
    private AdminEmailService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        lenient().when(transactions.execute(any())).thenAnswer(invocation ->
            invocation.<org.springframework.transaction.support.TransactionCallback<?>>getArgument(0)
                .doInTransaction(null)
        );
        lenient().doAnswer(invocation -> {
            invocation.<java.util.function.Consumer<?>>getArgument(0).accept(null);
            return null;
        }).when(transactions).executeWithoutResult(any());
        Clock clock = Clock.fixed(
            Instant.parse("2026-08-29T11:00:00Z"),
            ZoneOffset.UTC
        );
        service = new AdminEmailService(
            repository,
            mailSender,
            objectMapper,
            transactions,
            "sender@example.com",
            List.of("admin@example.com"),
            clock
        );
    }

    @Test
    void dailyReportIsSuppressedWhenNothingIsUnresolved() {
        when(repository.findUnresolvedFailures()).thenReturn(List.of());

        service.enqueueDailyFailureReport();

        verify(repository, never()).enqueue(anyString(), anyString(), any(), any());
    }

    @Test
    void weeklyReportUsesCompletedSaturdayThroughFridayWindow() {
        when(repository.findWeeklyMetrics(any(), any())).thenReturn(
            new WeeklyMetrics(0, 0, 0, 0, 0, 0, 0, List.of())
        );

        service.enqueueWeeklyStatusReport();

        verify(repository).findWeeklyMetrics(
            Instant.parse("2026-08-22T03:00:00Z"),
            Instant.parse("2026-08-29T03:00:00Z")
        );
        verify(repository).enqueue(
            org.mockito.ArgumentMatchers.eq("WEEKLY_STATUS_REPORT"),
            org.mockito.ArgumentMatchers.eq("WEEKLY_STATUS_REPORT:2026-08-22"),
            any(),
            org.mockito.ArgumentMatchers.isNull()
        );
    }

    @Test
    void successfulDeliveryIncludesNotificationId() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("subject", "Subject");
        payload.put("summary", "Summary");
        DeliveryClaim claim = new DeliveryClaim(42L, "TEST", payload, 1);
        when(repository.claimNext(any())).thenReturn(Optional.of(claim), Optional.empty());
        when(repository.lockSending(42L, 1)).thenReturn(true);

        service.deliverPendingEmails();

        ArgumentCaptor<SimpleMailMessage> message =
            ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(message.capture());
        assertThat(message.getValue().getFrom()).isEqualTo("sender@example.com");
        assertThat(message.getValue().getText()).contains("Notification ID: 42");
        verify(repository).markSent(42L);
    }

    @Test
    void sixthDeliveryFailureBecomesTerminalAfterFiveRetries() {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("subject", "Subject");
        payload.put("summary", "Summary");
        DeliveryClaim claim = new DeliveryClaim(42L, "TEST", payload, 6);
        when(repository.claimNext(any())).thenReturn(Optional.of(claim), Optional.empty());
        when(repository.lockSending(42L, 6)).thenReturn(true);
        org.mockito.Mockito.doThrow(new IllegalStateException("smtp down"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        service.deliverPendingEmails();

        verify(repository).markFailed(
            org.mockito.ArgumentMatchers.eq(42L),
            org.mockito.ArgumentMatchers.contains("smtp down")
        );
    }
}
