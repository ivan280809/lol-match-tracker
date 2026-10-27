package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotAccountPort;
import com.loltracker.app.integration.riot.RiotMatchPort;
import com.loltracker.app.integration.riot.RiotRankPort;
import com.loltracker.app.integration.telegram.TelegramDeliveryReceipt;
import com.loltracker.app.integration.telegram.TelegramNotificationPort;
import com.loltracker.app.match.TrackedMatchEntity;
import com.loltracker.app.match.TrackedMatchRepository;
import com.loltracker.app.notification.NotificationOutboxEntity;
import com.loltracker.app.notification.NotificationOutboxRepository;
import com.loltracker.app.notification.NotificationDeliveryStatus;
import com.loltracker.app.notification.NotificationDispatchResult;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.app.player.PlayerRankEntity;
import com.loltracker.app.player.PlayerRankRepository;
import com.loltracker.app.player.PlayerRankService;
import com.loltracker.app.notification.NotificationStatsService;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest(
    classes = {LolMatchTrackerApplication.class, NotificationBatchIntegrationTest.TestPorts.class},
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.flyway.enabled=false",
        "app.poll.initial-delay=PT876000H",
        "app.poll.scheduler-tick=PT876000H"
    })
@DirtiesContext
class NotificationBatchIntegrationTest {

    // Isolate the measured batch from the dispatcher's automatic startup pass.
    // Production dispatch behavior remains covered by the scheduler flow tests.
    @org.springframework.test.context.bean.override.mockito.MockitoBean(name = "dispatcherScheduler")
    private org.springframework.scheduling.TaskScheduler dispatcherScheduler;

    @Autowired
    private NotificationService notificationService;
    @MockitoSpyBean
    private PlayerRepository playerRepositorySpy;
    @Autowired
    private TrackedMatchRepository trackedMatchRepository;
    @Autowired
    private NotificationOutboxRepository outboxRepository;
    @MockitoSpyBean
    private PlayerRankRepository playerRankRepository;
    @Autowired
    private EntityManagerFactory emf;

    @Autowired
    private RiotRankPort riotRankPort;

    @Autowired
    private TelegramNotificationPort telegramNotificationPort;

    @MockitoSpyBean
    private NotificationStatsService notificationStatsService;

    private PlayerEntity player;

    @TestConfiguration
    static class TestPorts {
        @Bean
        @Primary
        RiotRankPort fakeRiotRankPort() {
            return mock(RiotRankPort.class);
        }

        @Bean
        @Primary
        RiotAccountPort fakeRiotAccountPort() {
            return mock(RiotAccountPort.class);
        }

        @Bean
        @Primary
        RiotMatchPort fakeRiotMatchPort() {
            return mock(RiotMatchPort.class);
        }

        @Bean
        @Primary
        TelegramNotificationPort fakeTelegramNotificationPort() {
            return mock(TelegramNotificationPort.class);
        }

        @Bean
        @Primary
        Clock fixedTestClock() {
            return Clock.fixed(Instant.parse("2026-10-05T00:00:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void setUp() {
        player = new PlayerEntity();
        player.setGameName("Bazaga");
        player.setTagLine("ESP");
        player.setPuuid("puuid-1");
        player.setActive(true);
        player = playerRepositorySpy.saveAndFlush(player);
    }

    @Test
    void dispatchBatchProcessesTwentyNotificationsWithSingleRankAndRosterQuery() {
        when(telegramNotificationPort.send(anyString())).thenReturn(new TelegramDeliveryReceipt(123));

        PlayerRankEntity rank = new PlayerRankEntity();
        rank.setPlayer(player);
        rank.setQueueType(PlayerRankService.SOLO_QUEUE);
        rank.setTier("GOLD");
        rank.setDivision("II");
        rank.setLeaguePoints(45);
        rank.setScore(1_500);
        rank.setUpdatedAt(Instant.parse("2099-01-01T00:00:00Z"));
        playerRankRepository.saveAndFlush(rank);

        for (int i = 0; i < 20; i++) {
            TrackedMatchEntity match = new TrackedMatchEntity();
            match.setPlayer(player);
            match.setMatchId("EUW1_" + i);
            match.setChampionName("Lux");
            match.setResult("VICTORY");
            match.setGameMode("CLASSIC");
            match.setQueueId(420); // Solo queue
            match.setDurationSeconds(1800);
            match.setGameEndAt(Instant.parse("2026-04-03T18:00:00Z"));
            match = trackedMatchRepository.saveAndFlush(match);

            NotificationOutboxEntity outbox = new NotificationOutboxEntity();
            outbox.setTrackedMatch(match);
            outbox.setStatus(NotificationDeliveryStatus.PENDING);
            outbox.setNextAttemptAt(Instant.parse("2020-01-01T00:00:00Z"));
            outboxRepository.saveAndFlush(outbox);
        }

        // Clear Hibernate statistics before test
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        clearInvocations(playerRepositorySpy, playerRankRepository);

        long start = System.nanoTime();
        NotificationDispatchResult result = notificationService.dispatchPendingForPlayer(player);
        long durationNs = System.nanoTime() - start;

        assertEquals(20, result.sent());
        assertEquals(0, result.failed());

        // One actual persisted rank query and one roster query serve the whole batch.
        verify(playerRankRepository, times(1))
                .findByPlayerIdAndQueueType(player.getId(), PlayerRankService.SOLO_QUEUE);
        verify(playerRepositorySpy, times(1))
                .findAllByActiveTrueAndArchivedAtIsNullOrderByGameNameAsc();
        verify(riotRankPort, never()).fetchRankEntries(any(), anyString());
        verify(telegramNotificationPort, times(20)).send(anyString());

        long sqlCount = stats.getPrepareStatementCount();
        long queryCount = stats.getQueryExecutionCount();
        assertTrue(sqlCount > 0, "Expected the measured batch to execute SQL");
        assertTrue(queryCount > 0, "Expected the measured batch to execute queries");

        System.out.printf(
                java.util.Locale.ROOT,
                "NOTIFICATION_BATCH size=20 rank_db_reads=1 roster_db_reads=1 riot_calls=0 telegram_fake_sends=20 sql_statements=%d query_executions=%d elapsed_ms=%.3f p95=not-measured%n",
                sqlCount, queryCount, durationNs / 1_000_000.0);
    }
}
