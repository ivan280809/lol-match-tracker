package com.loltracker.app.match;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import com.loltracker.app.player.PlayerEntity;
import com.loltracker.app.player.PlayerRepository;
import com.loltracker.app.match.MatchEntity;
import com.loltracker.app.match.MatchRepository;
import com.loltracker.app.match.PlayerMatchEntity;
import com.loltracker.app.match.MatchResult;
import com.loltracker.lolmatchtracker.LolMatchTrackerApplication;

@SpringBootTest(classes = LolMatchTrackerApplication.class)
class QueueLabelFilterTest {
  @Autowired private TrackedMatchService trackedMatchService;
  @Autowired private PlayerMatchRepository playerMatchRepository;
  @Autowired private PlayerRepository playerRepository;
  @Autowired private MatchRepository matchRepository;

  @Test
  @Transactional
  void javaSpecificWhitespaceCharacterUsesUnknownQueueFallback() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("tabPlayer");
    player.setTagLine("T#0");
    playerRepository.save(player);

    MatchEntity match = new MatchEntity();
    match.setMatchId("m-1");
    match.setGameEndAt(Instant.now());
    match.setQueueId(998);
    match.setGameMode(String.valueOf((char) 0x001c));
    match.setDurationSeconds(123);
    match.setPlatform("1");
    matchRepository.save(match);

    PlayerMatchEntity pm = new PlayerMatchEntity();
    pm.setPlayer(player);
    pm.setMatch(match);
    pm.setPuuid("puuid1");
    pm.setChampionName("Ahri");
    pm.setResult(MatchResult.VICTORY);
    pm.setKills(10);
    pm.setDeaths(2);
    pm.setAssists(5);
    playerMatchRepository.save(pm);

    Set<Long> ids = trackedMatchService.findPlayerIdsMatchingHistory("queue 998", "", null, null);
    assertTrue(ids.contains(player.getId()));
  }

  @Test
  @Transactional
  void queueLabelMatchesWhenGameModeIsNonBlank() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("draftPlayer");
    player.setTagLine("D#0");
    playerRepository.save(player);

    MatchEntity match = new MatchEntity();
    match.setMatchId("m-2");
    match.setGameEndAt(Instant.now());
    match.setQueueId(400);
    match.setGameMode("CLASSIC");
    match.setDurationSeconds(123);
    match.setPlatform("1");
    matchRepository.save(match);

    PlayerMatchEntity pm = new PlayerMatchEntity();
    pm.setPlayer(player);
    pm.setMatch(match);
    pm.setPuuid("puuid2");
    pm.setChampionName("Sona");
    pm.setResult(MatchResult.VICTORY);
    pm.setKills(5);
    pm.setDeaths(3);
    pm.setAssists(7);
    playerMatchRepository.save(pm);

    Set<Long> ids = trackedMatchService.findPlayerIdsMatchingHistory("normal draft", "", null, null);
    assertTrue(ids.contains(player.getId()));
  }

  @Test
  @Transactional
  void tabOnlyGameModeUsesUnknownQueueIdFallback() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("unknownPlayer");
    player.setTagLine("U#0");
    playerRepository.save(player);

    MatchEntity match = new MatchEntity();
    match.setMatchId("m-3");
    match.setGameEndAt(Instant.now());
    match.setQueueId(999);
    match.setGameMode("\t");
    match.setDurationSeconds(123);
    match.setPlatform("1");
    matchRepository.save(match);

    PlayerMatchEntity pm = new PlayerMatchEntity();
    pm.setPlayer(player);
    pm.setMatch(match);
    pm.setPuuid("puuid3");
    pm.setChampionName("Fizz");
    pm.setResult(MatchResult.VICTORY);
    pm.setKills(8);
    pm.setDeaths(4);
    pm.setAssists(6);
    playerMatchRepository.save(pm);

    Set<Long> ids = trackedMatchService.findPlayerIdsMatchingHistory("queue 999", "", null, null);
    assertTrue(ids.contains(player.getId()));
  }

  @Test
  @Transactional
  void newlineOnlyGameModeUsesUnknownQueueIdFallback() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("newlinePlayer");
    player.setTagLine("N#0");
    playerRepository.save(player);

    MatchEntity match = new MatchEntity();
    match.setMatchId("m-4");
    match.setGameEndAt(Instant.now());
    match.setQueueId(997);
    match.setGameMode("\n");
    match.setDurationSeconds(123);
    match.setPlatform("1");
    matchRepository.save(match);

    PlayerMatchEntity pm = new PlayerMatchEntity();
    pm.setPlayer(player);
    pm.setMatch(match);
    pm.setPuuid("puuid4");
    pm.setChampionName("Ahri");
    pm.setResult(MatchResult.VICTORY);
    pm.setKills(10);
    pm.setDeaths(2);
    pm.setAssists(5);
    playerMatchRepository.save(pm);

    Set<Long> ids = trackedMatchService.findPlayerIdsMatchingHistory("queue 997", "", null, null);
    assertTrue(ids.contains(player.getId()));
  }
  @Test
  @Transactional
  void trimsJavaControlWhitespaceAroundNonBlankGameMode() {
    PlayerEntity player = new PlayerEntity();
    player.setGameName("wrappedModePlayer");
    player.setTagLine("W#0");
    playerRepository.save(player);

    MatchEntity match = new MatchEntity();
    match.setMatchId("m-5");
    match.setGameEndAt(Instant.now());
    match.setQueueId(998);
    match.setGameMode("\tCLASSIC\n");
    match.setDurationSeconds(123);
    match.setPlatform("1");
    matchRepository.save(match);

    PlayerMatchEntity pm = new PlayerMatchEntity();
    pm.setPlayer(player);
    pm.setMatch(match);
    pm.setPuuid("puuid5");
    pm.setChampionName("Ahri");
    pm.setResult(MatchResult.VICTORY);
    pm.setKills(10);
    pm.setDeaths(2);
    pm.setAssists(5);
    playerMatchRepository.save(pm);

    Set<Long> ids = trackedMatchService.findPlayerIdsMatchingHistory("classic", "", null, null);
    assertTrue(ids.contains(player.getId()));
  }
}
