package com.loltracker.app.settings;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "app_configuration")
@Getter
@Setter
public class AppConfigurationEntity {

  @Id private Long id;

  @Column(name = "riot_api_key", length = 1200)
  private String riotApiKeyEncrypted;

  @Column(name = "riot_region", nullable = false, length = 32)
  private String riotRegion = RiotRegion.EUROPE.name();

  @Column(name = "telegram_bot_token", length = 1200)
  private String telegramBotTokenEncrypted;

  @Column(name = "telegram_chat_id", length = 1200)
  private String telegramChatIdEncrypted;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }
}
