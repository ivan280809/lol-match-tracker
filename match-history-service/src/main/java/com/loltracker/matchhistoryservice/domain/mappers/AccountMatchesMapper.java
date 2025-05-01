package com.loltracker.matchhistoryservice.domain.mappers;

import com.loltracker.matchhistoryservice.controllers.model.AccountMatchesDTO;
import com.loltracker.matchhistoryservice.controllers.model.account.AccountDTO;
import com.loltracker.matchhistoryservice.controllers.model.matches.*;
import com.loltracker.matchhistoryservice.infrastructure.models.*;
import java.util.stream.Collectors;

public interface AccountMatchesMapper {

  static AccountMatchesMO toEntity(AccountMatchesDTO dto) {
    if (dto == null) return null;
    return AccountMatchesMO.builder()
        .accountMO(toAccountMO(dto.getAccountDTO()))
        .matchesMO(toMatchesMO(dto.getMatchesDTO()))
        .build();
  }

  static AccountMatchesDTO toDto(AccountMatchesMO entity) {
    if (entity == null) return null;
    AccountMatchesDTO dto = new AccountMatchesDTO();
    dto.setAccountDTO(toAccountDTO(entity.getAccountMO()));
    dto.setMatchesDTO(toMatchesDTO(entity.getMatchesMO()));
    return dto;
  }

  static AccountMO toAccountMO(AccountDTO dto) {
    if (dto == null) return null;
    return AccountMO.builder()
        .puuid(dto.getPuuid())
        .gameName(dto.getGameName())
        .tagLine(dto.getTagLine())
        .build();
  }

  static AccountDTO toAccountDTO(AccountMO entity) {
    if (entity == null) return null;
    AccountDTO dto = new AccountDTO();
    dto.setPuuid(entity.getPuuid());
    dto.setGameName(entity.getGameName());
    dto.setTagLine(entity.getTagLine());
    return dto;
  }

  static MatchesMO toMatchesMO(MatchesDTO dto) {
    if (dto == null) return null;
    return MatchesMO.builder()
        .matches(
            dto.getMatches() != null
                ? dto.getMatches().stream()
                    .map(AccountMatchesMapper::toMatchMO)
                    .collect(Collectors.toList())
                : null)
        .build();
  }

  static MatchesDTO toMatchesDTO(MatchesMO entity) {
    if (entity == null) return null;
    MatchesDTO dto = new MatchesDTO();
    dto.setMatches(
        entity.getMatches() != null
            ? entity.getMatches().stream()
                .map(AccountMatchesMapper::toMatchDto)
                .collect(Collectors.toList())
            : null);
    return dto;
  }

  static MatchMO toMatchMO(MatchDto dto) {
    if (dto == null) return null;
    return MatchMO.builder()
        .metadata(toMetadataMO(dto.getMetadata()))
        .info(toInfoMO(dto.getInfo()))
        .build();
  }

  static MatchDto toMatchDto(MatchMO entity) {
    if (entity == null) return null;
    MatchDto dto = new MatchDto();
    dto.setMetadata(toMetadataDto(entity.getMetadata()));
    dto.setInfo(toInfoDto(entity.getInfo()));
    return dto;
  }

  static MetadataMO toMetadataMO(MetadataDto dto) {
    if (dto == null) return null;
    return MetadataMO.builder()
        .dataVersion(dto.getDataVersion())
        .matchId(dto.getMatchId())
        .participants(dto.getParticipants())
        .build();
  }

  static MetadataDto toMetadataDto(MetadataMO entity) {
    if (entity == null) return null;
    MetadataDto dto = new MetadataDto();
    dto.setDataVersion(entity.getDataVersion());
    dto.setMatchId(entity.getMatchId());
    dto.setParticipants(entity.getParticipants());
    return dto;
  }

  static InfoMO toInfoMO(InfoDto dto) {
    if (dto == null) return null;
    return InfoMO.builder()
        .endOfGameResult(dto.getEndOfGameResult())
        .gameCreation(dto.getGameCreation())
        .gameDuration(dto.getGameDuration())
        .gameEndTimestamp(dto.getGameEndTimestamp())
        .gameId(dto.getGameId())
        .gameMode(dto.getGameMode())
        .gameName(dto.getGameName())
        .gameStartTimestamp(dto.getGameStartTimestamp())
        .gameType(dto.getGameType())
        .gameVersion(dto.getGameVersion())
        .mapId(dto.getMapId())
        .participants(
            dto.getParticipants() != null
                ? dto.getParticipants().stream()
                    .map(AccountMatchesMapper::toParticipantMO)
                    .collect(Collectors.toList())
                : null)
        .platformId(dto.getPlatformId())
        .queueId(dto.getQueueId())
        .teams(
            dto.getTeams() != null
                ? dto.getTeams().stream()
                    .map(AccountMatchesMapper::toTeamMO)
                    .collect(Collectors.toList())
                : null)
        .tournamentCode(dto.getTournamentCode())
        .build();
  }

  static InfoDto toInfoDto(InfoMO entity) {
    if (entity == null) return null;
    InfoDto dto = new InfoDto();
    dto.setEndOfGameResult(entity.getEndOfGameResult());
    dto.setGameCreation(entity.getGameCreation());
    dto.setGameDuration(entity.getGameDuration());
    dto.setGameEndTimestamp(entity.getGameEndTimestamp());
    dto.setGameId(entity.getGameId());
    dto.setGameMode(entity.getGameMode());
    dto.setGameName(entity.getGameName());
    dto.setGameStartTimestamp(entity.getGameStartTimestamp());
    dto.setGameType(entity.getGameType());
    dto.setGameVersion(entity.getGameVersion());
    dto.setMapId(entity.getMapId());
    dto.setParticipants(
        entity.getParticipants() != null
            ? entity.getParticipants().stream()
                .map(AccountMatchesMapper::toParticipantDto)
                .collect(Collectors.toList())
            : null);
    dto.setPlatformId(entity.getPlatformId());
    dto.setQueueId(entity.getQueueId());
    dto.setTeams(
        entity.getTeams() != null
            ? entity.getTeams().stream()
                .map(AccountMatchesMapper::toTeamDto)
                .collect(Collectors.toList())
            : null);
    dto.setTournamentCode(entity.getTournamentCode());
    return dto;
  }

  static TeamMO toTeamMO(TeamDto dto) {
    if (dto == null) return null;
    return TeamMO.builder()
        .teamId(dto.getTeamId())
        .win(dto.isWin())
        .objectives(toObjectivesMO(dto.getObjectives()))
        .bans(
            dto.getBans() != null
                ? dto.getBans().stream()
                    .map(AccountMatchesMapper::toBanMO)
                    .collect(Collectors.toList())
                : null)
        .build();
  }

  static TeamDto toTeamDto(TeamMO entity) {
    if (entity == null) return null;
    TeamDto dto = new TeamDto();
    dto.setTeamId(entity.getTeamId());
    dto.setWin(entity.isWin());
    dto.setObjectives(toObjectivesDto(entity.getObjectives()));
    dto.setBans(
        entity.getBans() != null
            ? entity.getBans().stream()
                .map(AccountMatchesMapper::toBanDto)
                .collect(Collectors.toList())
            : null);
    return dto;
  }

  static BanMO toBanMO(BanDto dto) {
    if (dto == null) return null;
    return BanMO.builder().championId(dto.getChampionId()).pickTurn(dto.getPickTurn()).build();
  }

  static BanDto toBanDto(BanMO entity) {
    if (entity == null) return null;
    BanDto dto = new BanDto();
    dto.setChampionId(entity.getChampionId());
    dto.setPickTurn(entity.getPickTurn());
    return dto;
  }

  static ObjectivesMO toObjectivesMO(ObjectivesDto dto) {
    if (dto == null) return null;
    return ObjectivesMO.builder()
        .baron(toObjectiveMO(dto.getBaron()))
        .champion(toObjectiveMO(dto.getChampion()))
        .dragon(toObjectiveMO(dto.getDragon()))
        .horde(toObjectiveMO(dto.getHorde()))
        .inhibitor(toObjectiveMO(dto.getInhibitor()))
        .riftHerald(toObjectiveMO(dto.getRiftHerald()))
        .tower(toObjectiveMO(dto.getTower()))
        .build();
  }

  static ObjectivesDto toObjectivesDto(ObjectivesMO entity) {
    if (entity == null) return null;
    ObjectivesDto dto = new ObjectivesDto();
    dto.setBaron(toObjectiveDto(entity.getBaron()));
    dto.setChampion(toObjectiveDto(entity.getChampion()));
    dto.setDragon(toObjectiveDto(entity.getDragon()));
    dto.setHorde(toObjectiveDto(entity.getHorde()));
    dto.setInhibitor(toObjectiveDto(entity.getInhibitor()));
    dto.setRiftHerald(toObjectiveDto(entity.getRiftHerald()));
    dto.setTower(toObjectiveDto(entity.getTower()));
    return dto;
  }

  // Objective
  static ObjectiveMO toObjectiveMO(ObjectiveDto dto) {
    if (dto == null) return null;
    return ObjectiveMO.builder().first(dto.isFirst()).kills(dto.getKills()).build();
  }

  static ObjectiveDto toObjectiveDto(ObjectiveMO entity) {
    if (entity == null) return null;
    ObjectiveDto dto = new ObjectiveDto();
    dto.setFirst(entity.isFirst());
    dto.setKills(entity.getKills());
    return dto;
  }

  static ParticipantMO toParticipantMO(ParticipantDto dto) {
    if (dto == null) return null;
    return ParticipantMO.builder()
        .allInPings(dto.getAllInPings())
        .assistMePings(dto.getAssistMePings())
        .assists(dto.getAssists())
        .baronKills(dto.getBaronKills())
        .bountyLevel(dto.getBountyLevel())
        .champExperience(dto.getChampExperience())
        .champLevel(dto.getChampLevel())
        .championId(dto.getChampionId())
        .championName(dto.getChampionName())
        .championTransform(dto.getChampionTransform())
        .commandPings(dto.getCommandPings())
        .consumablesPurchased(dto.getConsumablesPurchased())
        .challenges(toChallengesMO(dto.getChallenges()))
        .damageDealtToBuildings(dto.getDamageDealtToBuildings())
        .damageDealtToObjectives(dto.getDamageDealtToObjectives())
        .damageDealtToTurrets(dto.getDamageDealtToTurrets())
        .damageSelfMitigated(dto.getDamageSelfMitigated())
        .deaths(dto.getDeaths())
        .detectorWardsPlaced(dto.getDetectorWardsPlaced())
        .doubleKills(dto.getDoubleKills())
        .dragonKills(dto.getDragonKills())
        .eligibleForProgression(dto.isEligibleForProgression())
        .enemyMissingPings(dto.getEnemyMissingPings())
        .enemyVisionPings(dto.getEnemyVisionPings())
        .firstBloodAssist(dto.isFirstBloodAssist())
        .firstBloodKill(dto.isFirstBloodKill())
        .firstTowerAssist(dto.isFirstTowerAssist())
        .firstTowerKill(dto.isFirstTowerKill())
        .gameEndedInEarlySurrender(dto.isGameEndedInEarlySurrender())
        .gameEndedInSurrender(dto.isGameEndedInSurrender())
        .holdPings(dto.getHoldPings())
        .getBackPings(dto.getGetBackPings())
        .goldEarned(dto.getGoldEarned())
        .goldSpent(dto.getGoldSpent())
        .individualPosition(dto.getIndividualPosition())
        .inhibitorKills(dto.getInhibitorKills())
        .inhibitorTakedowns(dto.getInhibitorTakedowns())
        .inhibitorsLost(dto.getInhibitorsLost())
        .item0(dto.getItem0())
        .item1(dto.getItem1())
        .item2(dto.getItem2())
        .item3(dto.getItem3())
        .item4(dto.getItem4())
        .item5(dto.getItem5())
        .item6(dto.getItem6())
        .itemsPurchased(dto.getItemsPurchased())
        .killingSprees(dto.getKillingSprees())
        .kills(dto.getKills())
        .lane(dto.getLane())
        .largestCriticalStrike(dto.getLargestCriticalStrike())
        .largestKillingSpree(dto.getLargestKillingSpree())
        .largestMultiKill(dto.getLargestMultiKill())
        .longestTimeSpentLiving(dto.getLongestTimeSpentLiving())
        .magicDamageDealt(dto.getMagicDamageDealt())
        .magicDamageDealtToChampions(dto.getMagicDamageDealtToChampions())
        .magicDamageTaken(dto.getMagicDamageTaken())
        .missions(toMissionsMO(dto.getMissions()))
        .neutralMinionsKilled(dto.getNeutralMinionsKilled())
        .nexusKills(dto.getNexusKills())
        .nexusTakedowns(dto.getNexusTakedowns())
        .nexusLost(dto.getNexusLost())
        .objectivesStolen(dto.getObjectivesStolen())
        .objectivesStolenAssists(dto.getObjectivesStolenAssists())
        .onMyWayPings(dto.getOnMyWayPings())
        .participantId(dto.getParticipantId())
        .playerScore0(dto.getPlayerScore0())
        .playerScore1(dto.getPlayerScore1())
        .playerScore2(dto.getPlayerScore2())
        .playerScore3(dto.getPlayerScore3())
        .playerScore4(dto.getPlayerScore4())
        .playerScore5(dto.getPlayerScore5())
        .playerScore6(dto.getPlayerScore6())
        .playerScore7(dto.getPlayerScore7())
        .playerScore8(dto.getPlayerScore8())
        .playerScore9(dto.getPlayerScore9())
        .playerScore10(dto.getPlayerScore10())
        .playerScore11(dto.getPlayerScore11())
        .pentaKills(dto.getPentaKills())
        .perks(toPerksMO(dto.getPerks()))
        .physicalDamageDealt(dto.getPhysicalDamageDealt())
        .physicalDamageDealtToChampions(dto.getPhysicalDamageDealtToChampions())
        .physicalDamageTaken(dto.getPhysicalDamageTaken())
        .profileIcon(dto.getProfileIcon())
        .puuid(dto.getPuuid())
        .quadraKills(dto.getQuadraKills())
        .riotIdGameName(dto.getRiotIdGameName())
        .riotIdTagline(dto.getRiotIdTagline())
        .role(dto.getRole())
        .sightWardsBoughtInGame(dto.getSightWardsBoughtInGame())
        .spell1Casts(dto.getSpell1Casts())
        .spell2Casts(dto.getSpell2Casts())
        .spell3Casts(dto.getSpell3Casts())
        .spell4Casts(dto.getSpell4Casts())
        .summoner1Casts(dto.getSummoner1Casts())
        .summoner1Id(dto.getSummoner1Id())
        .summoner2Casts(dto.getSummoner2Casts())
        .summoner2Id(dto.getSummoner2Id())
        .summonerId(dto.getSummonerId())
        .summonerLevel(dto.getSummonerLevel())
        .summonerName(dto.getSummonerName())
        .teamEarlySurrendered(dto.isTeamEarlySurrendered())
        .teamId(dto.getTeamId())
        .teamPosition(dto.getTeamPosition())
        .totalDamageDealt(dto.getTotalDamageDealt())
        .totalDamageDealtToChampions(dto.getTotalDamageDealtToChampions())
        .totalDamageShieldedOnTeammates(dto.getTotalDamageShieldedOnTeammates())
        .totalDamageTaken(dto.getTotalDamageTaken())
        .totalHeal(dto.getTotalHeal())
        .totalHealsOnTeammates(dto.getTotalHealsOnTeammates())
        .totalMinionsKilled(dto.getTotalMinionsKilled())
        .totalTimeCCDealt(dto.getTotalTimeCCDealt())
        .totalTimeSpentDead(dto.getTotalTimeSpentDead())
        .totalUnitsHealed(dto.getTotalUnitsHealed())
        .tripleKills(dto.getTripleKills())
        .trueDamageDealt(dto.getTrueDamageDealt())
        .trueDamageDealtToChampions(dto.getTrueDamageDealtToChampions())
        .trueDamageTaken(dto.getTrueDamageTaken())
        .turretKills(dto.getTurretKills())
        .turretTakedowns(dto.getTurretTakedowns())
        .turretsLost(dto.getTurretsLost())
        .unrealKills(dto.getUnrealKills())
        .visionScore(dto.getVisionScore())
        .visionClearedPings(dto.getVisionClearedPings())
        .visionWardsBoughtInGame(dto.getVisionWardsBoughtInGame())
        .wardsKilled(dto.getWardsKilled())
        .wardsPlaced(dto.getWardsPlaced())
        .win(dto.isWin())
        .build();
  }

  static ParticipantDto toParticipantDto(ParticipantMO entity) {
    if (entity == null) return null;
    ParticipantDto dto = new ParticipantDto();
    dto.setAllInPings(entity.getAllInPings());
    dto.setAssistMePings(entity.getAssistMePings());
    // ...
    dto.setChallenges(toChallengesDto(entity.getChallenges()));
    dto.setMissions(toMissionsDto(entity.getMissions()));
    dto.setPerks(toPerksDto(entity.getPerks()));
    // ...
    dto.setWin(entity.isWin());
    return dto;
  }

  static ChallengesMO toChallengesMO(ChallengesDto dto) {
    return ChallengesMO.builder()
        ._12AssistStreakCount(dto.get_12AssistStreakCount())
        .baronBuffGoldAdvantageOverThreshold(dto.getBaronBuffGoldAdvantageOverThreshold())
        .controlWardTimeCoverageInRiverOrEnemyHalf(
            dto.getControlWardTimeCoverageInRiverOrEnemyHalf())
        .earliestBaron(dto.getEarliestBaron())
        .earliestDragonTakedown(dto.getEarliestDragonTakedown())
        .earliestElderDragon(dto.getEarliestElderDragon())
        .earlyLaningPhaseGoldExpAdvantage(dto.getEarlyLaningPhaseGoldExpAdvantage())
        .fasterSupportQuestCompletion(dto.getFasterSupportQuestCompletion())
        .fastestLegendary(dto.getFastestLegendary())
        .hadAfkTeammate(dto.getHadAfkTeammate())
        .highestChampionDamage(dto.getHighestChampionDamage())
        .highestCrowdControlScore(dto.getHighestCrowdControlScore())
        .highestWardKills(dto.getHighestWardKills())
        .junglerKillsEarlyJungle(dto.getJunglerKillsEarlyJungle())
        .killsOnLanersEarlyJungleAsJungler(dto.getKillsOnLanersEarlyJungleAsJungler())
        .laningPhaseGoldExpAdvantage(dto.getLaningPhaseGoldExpAdvantage())
        .legendaryCount(dto.getLegendaryCount())
        .maxCsAdvantageOnLaneOpponent(dto.getMaxCsAdvantageOnLaneOpponent())
        .maxLevelLeadLaneOpponent(dto.getMaxLevelLeadLaneOpponent())
        .mostWardsDestroyedOneSweeper(dto.getMostWardsDestroyedOneSweeper())
        .mythicItemUsed(dto.getMythicItemUsed())
        .playedChampSelectPosition(dto.getPlayedChampSelectPosition())
        .soloTurretsLategame(dto.getSoloTurretsLategame())
        .takedownsFirst25Minutes(dto.getTakedownsFirst25Minutes())
        .teleportTakedowns(dto.getTeleportTakedowns())
        .thirdInhibitorDestroyedTime(dto.getThirdInhibitorDestroyedTime())
        .threeWardsOneSweeperCount(dto.getThreeWardsOneSweeperCount())
        .visionScoreAdvantageLaneOpponent(dto.getVisionScoreAdvantageLaneOpponent())
        .infernalScalePickup(dto.getInfernalScalePickup())
        .fistBumpParticipation(dto.getFistBumpParticipation())
        .voidMonsterKill(dto.getVoidMonsterKill())
        .abilityUses(dto.getAbilityUses())
        .acesBefore15Minutes(dto.getAcesBefore15Minutes())
        .alliedJungleMonsterKills(dto.getAlliedJungleMonsterKills())
        .baronTakedowns(dto.getBaronTakedowns())
        .blastConeOppositeOpponentCount(dto.getBlastConeOppositeOpponentCount())
        .bountyGold(dto.getBountyGold())
        .buffsStolen(dto.getBuffsStolen())
        .completeSupportQuestInTime(dto.getCompleteSupportQuestInTime())
        .controlWardsPlaced(dto.getControlWardsPlaced())
        .damagePerMinute(dto.getDamagePerMinute())
        .damageTakenOnTeamPercentage(dto.getDamageTakenOnTeamPercentage())
        .dancedWithRiftHerald(dto.getDancedWithRiftHerald())
        .deathsByEnemyChamps(dto.getDeathsByEnemyChamps())
        .dodgeSkillShotsSmallWindow(dto.getDodgeSkillShotsSmallWindow())
        .doubleAces(dto.getDoubleAces())
        .dragonTakedowns(dto.getDragonTakedowns())
        .legendaryItemUsed(dto.getLegendaryItemUsed())
        .effectiveHealAndShielding(dto.getEffectiveHealAndShielding())
        .elderDragonKillsWithOpposingSoul(dto.getElderDragonKillsWithOpposingSoul())
        .enemyChampionImmobilizations(dto.getEnemyChampionImmobilizations())
        .enemyJungleMonsterKills(dto.getEnemyJungleMonsterKills())
        .epicMonsterKillsNearEnemyJungler(dto.getEpicMonsterKillsNearEnemyJungler())
        .epicMonsterKillsWithin30SecondsOfSpawn(dto.getEpicMonsterKillsWithin30SecondsOfSpawn())
        .firstTurretKilledTime(dto.getFirstTurretKilledTime())
        .firstTurretKilled(dto.getFirstTurretKilled())
        .flawlessAces(dto.getFlawlessAces())
        .fullTeamTakedown(dto.getFullTeamTakedown())
        .gameLength(dto.getGameLength())
        .build();
  }

  static ChallengesDto toChallengesDto(ChallengesMO entity) {
    return null;
  }

  static MissionsMO toMissionsMO(MissionsDto dto) {
    return MissionsMO.builder()
        .playerScore0(dto.getPlayerScore0())
        .playerScore1(dto.getPlayerScore1())
        .playerScore2(dto.getPlayerScore2())
        .playerScore3(dto.getPlayerScore3())
        .playerScore4(dto.getPlayerScore4())
        .playerScore5(dto.getPlayerScore5())
        .playerScore6(dto.getPlayerScore6())
        .playerScore7(dto.getPlayerScore7())
        .playerScore8(dto.getPlayerScore8())
        .playerScore9(dto.getPlayerScore9())
        .playerScore10(dto.getPlayerScore10())
        .playerScore11(dto.getPlayerScore11())
        .build();
  }

  static MissionsDto toMissionsDto(MissionsMO entity) {
    return null;
  }

  static PerksMO toPerksMO(PerksDto dto) {
    return PerksMO.builder()
        .statPerks(toPerkStatsMO(dto.getStatPerks()))
        .styles(
            dto.getStyles() != null
                ? dto.getStyles().stream()
                    .map(AccountMatchesMapper::toPerkStyleMO)
                    .collect(Collectors.toList())
                : null)
        .build();
  }

  static PerkStatsMO toPerkStatsMO(PerkStatsDto statPerks) {
    return PerkStatsMO.builder()
        .defense(statPerks.getDefense())
        .flex(statPerks.getFlex())
        .offense(statPerks.getOffense())
        .build();
  }

  static PerkStyleMO toPerkStyleMO(PerkStyleDto perkStyleDto) {
    return PerkStyleMO.builder()
        .description(perkStyleDto.getDescription())
        .selections(
            perkStyleDto.getSelections() != null
                ? perkStyleDto.getSelections().stream()
                    .map(AccountMatchesMapper::toPerkSelectionMO)
                    .collect(Collectors.toList())
                : null)
        .style(perkStyleDto.getStyle())
        .build();
  }

  static PerkStyleSelectionMO toPerkSelectionMO(PerkStyleSelectionDto perkStyleSelectionDto) {
    return PerkStyleSelectionMO.builder()
        .perk(perkStyleSelectionDto.getPerk())
        .var1(perkStyleSelectionDto.getVar1())
        .var2(perkStyleSelectionDto.getVar2())
        .var3(perkStyleSelectionDto.getVar3())
        .build();
  }

  static PerksDto toPerksDto(PerksMO entity) {
    return null;
  }
}
