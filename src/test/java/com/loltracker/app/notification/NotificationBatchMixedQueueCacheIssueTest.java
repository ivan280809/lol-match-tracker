package com.loltracker.app.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.loltracker.app.integration.riot.RiotRankPort;
import com.loltracker.app.integration.riot.RiotAccountPort;
import com.loltracker.app.integration.riot.RiotMatchPort;
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
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.sql.Timestamp;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.mockito.ArgumentCaptor;

@SpringBootTest(
    classes = {
        LolMatchTrackerApplication.class,
        NotificationBatchMixedQueueCacheIssueTest.TestPorts.class
    },
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
class NotificationBatchMixedQueueCacheIssueTest {

    @org.springframework.test.context.bean.override.mockito.MockitoBean(name = "dispatcherScheduler")
    private org.springframework.scheduling.TaskScheduler dispatcherScheduler;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TrackedMatchRepository trackedMatchRepository;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @MockitoSpyBean
    private PlayerRepository playerRepositorySpy;

    @Autowired
    private RiotRankPort riotRankPort;

    @Autowired
    private TelegramNotificationPort telegramNotificationPort;

    @MockitoSpyBean
    private NotificationStatsService notificationStatsService;

    @MockitoSpyBean
    private PlayerRankRepository playerRankRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        TelegramNotificationPort fakeTelegramNotificationPort() {
            return mock(TelegramNotificationPort.class);
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
    void dispatchMixedQueueCacheIssue() {
        // Arrange
        when(telegramNotificationPort.send(anyString())).thenReturn(new TelegramDeliveryReceipt(123));
        // Persist old primary rank (Gold)
        player.setRankTier("GOLD");
        player.setRankDivision("II");
        player.setRankLeaguePoints(45);
        player.setRankScore(1500);
        player.setRankUpdatedAt(Instant.parse("2026-10-04T23:58:00Z"));
        playerRepositorySpy.saveAndFlush(player);
        // Persist expired Solo snapshot
        PlayerRankEntity soloRank = new PlayerRankEntity();
        soloRank.setPlayer(player);
        soloRank.setQueueType(PlayerRankService.SOLO_QUEUE);
        soloRank.setTier("GOLD");
        soloRank.setDivision("II");
        soloRank.setLeaguePoints(45);
        soloRank.setScore(1500);
        soloRank.setUpdatedAt(Instant.parse("2026-10-04T23:00:00Z"));
        playerRankRepository.saveAndFlush(soloRank);
        // Fake Riot return Platinum for Solo
        when(riotRankPort.fetchRankEntries(any(), eq(player.getPuuid()))).thenReturn(
                List.of(
                        new com.loltracker.app.integration.riot.RiotRankEntry(
                                PlayerRankService.SOLO_QUEUE, "PLATINUM", "II", 45, 0, 0)));
        // Create matches: non-ranked, solo, non-ranked
        TrackedMatchEntity nr1 = new TrackedMatchEntity();
        nr1.setPlayer(player);
        nr1.setMatchId("NR1");
        nr1.setChampionName("Lux");
        nr1.setResult("VICTORY");
        nr1.setGameMode("CLASSIC");
        nr1.setQueueId(450); // ARAM, non-ranked
        nr1.setDurationSeconds(1800);
        nr1.setGameEndAt(Instant.parse("2026-10-01T10:00:00Z"));
        nr1 = trackedMatchRepository.saveAndFlush(nr1);
        NotificationOutboxEntity outbox1 = new NotificationOutboxEntity();
        outbox1.setTrackedMatch(nr1);
        outbox1.setStatus(NotificationDeliveryStatus.PENDING);
        outbox1.setNextAttemptAt(Instant.parse("2026-10-01T09:00:00Z"));
        outboxRepository.saveAndFlush(outbox1);
        setCreatedAt(outbox1, Instant.parse("2026-10-01T10:00:00Z"));

        TrackedMatchEntity solo = new TrackedMatchEntity();
        solo.setPlayer(player);
        solo.setMatchId("S1");
        solo.setChampionName("Lux");
        solo.setResult("VICTORY");
        solo.setGameMode("CLASSIC");
        solo.setQueueId(420); // solo
        solo.setDurationSeconds(1800);
        solo.setGameEndAt(Instant.parse("2026-10-01T10:30:00Z"));
        solo = trackedMatchRepository.saveAndFlush(solo);
        NotificationOutboxEntity outbox2 = new NotificationOutboxEntity();
        outbox2.setTrackedMatch(solo);
        outbox2.setStatus(NotificationDeliveryStatus.PENDING);
        outbox2.setNextAttemptAt(Instant.parse("2026-10-01T09:30:00Z"));
        outboxRepository.saveAndFlush(outbox2);
        setCreatedAt(outbox2, Instant.parse("2026-10-01T10:01:00Z"));

        TrackedMatchEntity nr2 = new TrackedMatchEntity();
        nr2.setPlayer(player);
        nr2.setMatchId("NR2");
        nr2.setChampionName("Lux");
        nr2.setResult("VICTORY");
        nr2.setGameMode("CLASSIC");
        nr2.setQueueId(450);
        nr2.setDurationSeconds(1800);
        nr2.setGameEndAt(Instant.parse("2026-10-01T11:00:00Z"));
        nr2 = trackedMatchRepository.saveAndFlush(nr2);
        NotificationOutboxEntity outbox3 = new NotificationOutboxEntity();
        outbox3.setTrackedMatch(nr2);
        outbox3.setStatus(NotificationDeliveryStatus.PENDING);
        outbox3.setNextAttemptAt(Instant.parse("2026-10-01T10:00:00Z"));
        outboxRepository.saveAndFlush(outbox3);
        setCreatedAt(outbox3, Instant.parse("2026-10-01T10:02:00Z"));

        // Act
        notificationService.dispatchPendingForPlayer(player);

        // Assert
        verify(riotRankPort, times(1)).fetchRankEntries(any(), eq(player.getPuuid()));
        verify(telegramNotificationPort, times(3)).send(anyString());
        ArgumentCaptor<com.loltracker.app.player.PlayerRankService.RankRefreshResult> captor =
                ArgumentCaptor.forClass(com.loltracker.app.player.PlayerRankService.RankRefreshResult.class);
        verify(notificationStatsService, times(3)).buildFor(any(), captor.capture(), any());
        List<com.loltracker.app.player.PlayerRankService.RankRefreshResult> ranks = captor.getAllValues();
        assertEquals(3, ranks.size());
        assertEquals(com.loltracker.app.player.PlayerRankService.RankAvailability.STORED, ranks.get(0).availability());
        assertEquals("GOLD", ranks.get(0).snapshot().get().tier());
        assertEquals(com.loltracker.app.player.PlayerRankService.RankAvailability.CURRENT, ranks.get(1).availability());
        assertEquals("PLATINUM", ranks.get(1).snapshot().get().tier());
        assertEquals(com.loltracker.app.player.PlayerRankService.RankAvailability.STORED, ranks.get(2).availability());
        assertEquals("PLATINUM", ranks.get(2).snapshot().get().tier());
    }

    private void setCreatedAt(NotificationOutboxEntity outbox, Instant createdAt) {
        jdbcTemplate.update(
                "UPDATE notification_outbox SET created_at = ? WHERE id = ?",
                Timestamp.from(createdAt), outbox.getId());
    }
}
