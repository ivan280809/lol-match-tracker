package com.loltracker.matchhistoryservice.infrastructure.models;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Challenges")
@NoArgsConstructor
@AllArgsConstructor
public class ChallengesMO {
  @Id @GeneratedValue private Long id;

  private int _12AssistStreakCount;
  private int baronBuffGoldAdvantageOverThreshold;
  private float controlWardTimeCoverageInRiverOrEnemyHalf;
  private int earliestBaron;
  private int earliestDragonTakedown;
  private int earliestElderDragon;
  private int earlyLaningPhaseGoldExpAdvantage;
  private int fasterSupportQuestCompletion;
  private int fastestLegendary;
  private int hadAfkTeammate;
  private int highestChampionDamage;
  private int highestCrowdControlScore;
  private int highestWardKills;
  private int junglerKillsEarlyJungle;
  private int killsOnLanersEarlyJungleAsJungler;
  private int laningPhaseGoldExpAdvantage;
  private int legendaryCount;
  private float maxCsAdvantageOnLaneOpponent;
  private int maxLevelLeadLaneOpponent;
  private int mostWardsDestroyedOneSweeper;
  private int mythicItemUsed;
  private int playedChampSelectPosition;
  private int soloTurretsLategame;
  private int takedownsFirst25Minutes;
  private int teleportTakedowns;
  private int thirdInhibitorDestroyedTime;
  private int threeWardsOneSweeperCount;
  private float visionScoreAdvantageLaneOpponent;
  private int infernalScalePickup;
  private int fistBumpParticipation;
  private int voidMonsterKill;
  private int abilityUses;
  private int acesBefore15Minutes;
  private float alliedJungleMonsterKills;
  private int baronTakedowns;
  private int blastConeOppositeOpponentCount;
  private int bountyGold;
  private int buffsStolen;
  private int completeSupportQuestInTime;
  private int controlWardsPlaced;
  private float damagePerMinute;
  private float damageTakenOnTeamPercentage;
  private int dancedWithRiftHerald;
  private int deathsByEnemyChamps;
  private int dodgeSkillShotsSmallWindow;
  private int doubleAces;
  private int dragonTakedowns;
  private List<Integer> legendaryItemUsed;
  private float effectiveHealAndShielding;
  private int elderDragonKillsWithOpposingSoul;
  private int elderDragonMultikills;
  private int enemyChampionImmobilizations;
  private float enemyJungleMonsterKills;
  private int epicMonsterKillsNearEnemyJungler;
  private int epicMonsterKillsWithin30SecondsOfSpawn;
  private int epicMonsterSteals;
  private int epicMonsterStolenWithoutSmite;
  private int firstTurretKilled;
  private float firstTurretKilledTime;
  private int flawlessAces;
  private int fullTeamTakedown;
  private float gameLength;
}
