package com.loltracker.app.settings;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppConfigurationService {

  private static final Long CONFIGURATION_ID = 1L;

  private final AppConfigurationRepository appConfigurationRepository;
  private final SecretCryptoService secretCryptoService;

  @Value("${riot.api.key:}")
  private String fallbackRiotApiKey;

  @Value("${riot.api.region:EUROPE}")
  private String fallbackRiotRegion;

  @Value("${telegram.bot.token:}")
  private String fallbackTelegramBotToken;

  @Value("${telegram.chat.id:}")
  private String fallbackTelegramChatId;

  @Value("${app.poll.enabled:true}")
  private boolean fallbackPollingEnabled;

  @Value("${app.poll.manual-only:false}")
  private boolean fallbackPollingManualOnly;

  @Value("${app.poll.fixed-delay:PT5M}")
  private Duration fallbackPollingFixedDelay;

  @Value("${app.poll.match-window-size:10}")
  private int fallbackPollingMatchWindowSize;

  @Value("${app.poll.pagination-limit:3}")
  private int fallbackPollingPaginationLimit;

  @Transactional(readOnly = true)
  public AppConfigurationView getView() {
    Optional<AppConfigurationEntity> entity = appConfigurationRepository.findById(CONFIGURATION_ID);
    String riotApiKeyEncrypted = entity.map(AppConfigurationEntity::getRiotApiKeyEncrypted).orElse(null);
    String persistedRegion = entity.map(AppConfigurationEntity::getRiotRegion).orElse(null);
    String telegramBotTokenEncrypted =
        entity.map(AppConfigurationEntity::getTelegramBotTokenEncrypted).orElse(null);
    String telegramChatIdEncrypted =
        entity.map(AppConfigurationEntity::getTelegramChatIdEncrypted).orElse(null);
    Boolean pollingEnabled = entity.map(AppConfigurationEntity::getPollingEnabled).orElse(null);
    Boolean pollingManualOnly = entity.map(AppConfigurationEntity::getPollingManualOnly).orElse(null);
    String pollingFixedDelay = entity.map(AppConfigurationEntity::getPollingFixedDelay).orElse(null);
    Integer pollingMatchWindowSize = entity.map(AppConfigurationEntity::getPollingMatchWindowSize).orElse(null);
    Integer pollingPaginationLimit = entity.map(AppConfigurationEntity::getPollingPaginationLimit).orElse(null);
    return new AppConfigurationView(
        hasConfiguredSecret(riotApiKeyEncrypted, fallbackRiotApiKey),
        resolveRegion(persistedRegion),
        hasConfiguredSecret(telegramBotTokenEncrypted, fallbackTelegramBotToken),
        hasConfiguredSecret(telegramChatIdEncrypted, fallbackTelegramChatId),
        secretCryptoService.isConfigured(),
        secretSource(riotApiKeyEncrypted, fallbackRiotApiKey),
        regionSource(persistedRegion),
        secretSource(telegramBotTokenEncrypted, fallbackTelegramBotToken),
        secretSource(telegramChatIdEncrypted, fallbackTelegramChatId),
        pollingEnabled == null ? fallbackPollingEnabled : pollingEnabled,
        pollingManualOnly == null ? fallbackPollingManualOnly : pollingManualOnly,
        resolveDurationString(pollingFixedDelay, fallbackPollingFixedDelay),
        resolveInt(pollingMatchWindowSize, fallbackPollingMatchWindowSize, 1, 100),
        resolveInt(pollingPaginationLimit, fallbackPollingPaginationLimit, 1, 20),
        pollingConfigSource(pollingEnabled, pollingManualOnly, pollingFixedDelay, pollingMatchWindowSize, pollingPaginationLimit));
  }

  @Transactional(readOnly = true)
  public AppConfigurationForm getForm() {
    RiotRegion region =
        appConfigurationRepository
            .findById(CONFIGURATION_ID)
            .map(AppConfigurationEntity::getRiotRegion)
            .map(this::resolveRegion)
            .orElse(resolveRegion(null));
    Optional<AppConfigurationEntity> entity = appConfigurationRepository.findById(CONFIGURATION_ID);
    return new AppConfigurationForm(
        "",
        region.name(),
        "",
        "",
        entity.map(AppConfigurationEntity::getPollingEnabled).orElse(fallbackPollingEnabled),
        entity.map(AppConfigurationEntity::getPollingManualOnly).orElse(fallbackPollingManualOnly),
        resolveDurationString(entity.map(AppConfigurationEntity::getPollingFixedDelay).orElse(null), fallbackPollingFixedDelay),
        resolveInt(entity.map(AppConfigurationEntity::getPollingMatchWindowSize).orElse(null), fallbackPollingMatchWindowSize, 1, 100),
        resolveInt(entity.map(AppConfigurationEntity::getPollingPaginationLimit).orElse(null), fallbackPollingPaginationLimit, 1, 20));
  }

  @Transactional(readOnly = true)
  public RuntimeAppConfiguration getRuntimeConfiguration() {
    Optional<AppConfigurationEntity> entity = appConfigurationRepository.findById(CONFIGURATION_ID);
    return new RuntimeAppConfiguration(
        resolveSecret(entity.map(AppConfigurationEntity::getRiotApiKeyEncrypted).orElse(null), fallbackRiotApiKey),
        resolveRegion(entity.map(AppConfigurationEntity::getRiotRegion).orElse(null)),
        resolveSecret(
            entity.map(AppConfigurationEntity::getTelegramBotTokenEncrypted).orElse(null),
            fallbackTelegramBotToken),
        resolveSecret(
            entity.map(AppConfigurationEntity::getTelegramChatIdEncrypted).orElse(null),
            fallbackTelegramChatId),
        entity.map(AppConfigurationEntity::getPollingEnabled).orElse(fallbackPollingEnabled),
        entity.map(AppConfigurationEntity::getPollingManualOnly).orElse(fallbackPollingManualOnly),
        resolveDuration(entity.map(AppConfigurationEntity::getPollingFixedDelay).orElse(null), fallbackPollingFixedDelay),
        resolveInt(entity.map(AppConfigurationEntity::getPollingMatchWindowSize).orElse(null), fallbackPollingMatchWindowSize, 1, 100),
        resolveInt(entity.map(AppConfigurationEntity::getPollingPaginationLimit).orElse(null), fallbackPollingPaginationLimit, 1, 20));
  }

  @Transactional
  public void update(AppConfigurationForm form) {
    AppConfigurationEntity entity =
        appConfigurationRepository.findById(CONFIGURATION_ID).orElseGet(this::newConfiguration);
    entity.setRiotRegion(RiotRegion.from(form.riotRegion()).name());
    updateSecret(form.riotApiKey(), entity::setRiotApiKeyEncrypted);
    updateSecret(form.telegramBotToken(), entity::setTelegramBotTokenEncrypted);
    updateSecret(form.telegramChatId(), entity::setTelegramChatIdEncrypted);
    entity.setPollingEnabled(Boolean.TRUE.equals(form.pollingEnabled()));
    entity.setPollingManualOnly(Boolean.TRUE.equals(form.pollingManualOnly()));
    entity.setPollingFixedDelay(resolveDuration(form.pollingFixedDelay(), fallbackPollingFixedDelay).toString());
    entity.setPollingMatchWindowSize(resolveInt(form.pollingMatchWindowSize(), fallbackPollingMatchWindowSize, 1, 100));
    entity.setPollingPaginationLimit(resolveInt(form.pollingPaginationLimit(), fallbackPollingPaginationLimit, 1, 20));
    appConfigurationRepository.save(entity);
  }

  private AppConfigurationEntity newConfiguration() {
    AppConfigurationEntity entity = new AppConfigurationEntity();
    entity.setId(CONFIGURATION_ID);
    entity.setRiotRegion(resolveRegion(null).name());
    entity.setPollingEnabled(fallbackPollingEnabled);
    entity.setPollingManualOnly(fallbackPollingManualOnly);
    entity.setPollingFixedDelay(resolveDuration(null, fallbackPollingFixedDelay).toString());
    entity.setPollingMatchWindowSize(clamp(fallbackPollingMatchWindowSize, 1, 100));
    entity.setPollingPaginationLimit(clamp(fallbackPollingPaginationLimit, 1, 20));
    return entity;
  }

  private void updateSecret(String value, java.util.function.Consumer<String> setter) {
    if (value == null || value.isBlank()) {
      return;
    }
    setter.accept(secretCryptoService.encrypt(value.trim()));
  }

  private boolean hasConfiguredSecret(String encryptedValue, String fallbackValue) {
    return hasText(encryptedValue) || hasText(fallbackValue);
  }

  private String secretSource(String encryptedValue, String fallbackValue) {
    if (hasText(encryptedValue)) {
      return "DB";
    }
    if (hasText(fallbackValue)) {
      return "ENV";
    }
    return "No configurado";
  }

  private String regionSource(String persistedRegion) {
    if (hasText(persistedRegion)) {
      return "DB";
    }
    if (hasText(fallbackRiotRegion)) {
      return "ENV";
    }
    return "No configurado";
  }

  private String resolveSecret(String encryptedValue, String fallbackValue) {
    if (hasText(encryptedValue)) {
      return secretCryptoService.decrypt(encryptedValue);
    }
    return hasText(fallbackValue) ? fallbackValue.trim() : "";
  }

  private RiotRegion resolveRegion(String persistedRegion) {
    if (hasText(persistedRegion)) {
      return RiotRegion.from(persistedRegion);
    }
    if (hasText(fallbackRiotRegion)) {
      return RiotRegion.from(fallbackRiotRegion);
    }
    return RiotRegion.EUROPE;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private String pollingConfigSource(
      Boolean enabled, Boolean manualOnly, String fixedDelay, Integer windowSize, Integer paginationLimit) {
    if (enabled != null || manualOnly != null || hasText(fixedDelay) || windowSize != null || paginationLimit != null) {
      return "DB";
    }
    return "ENV";
  }

  private Duration resolveDuration(String value, Duration fallback) {
    Duration safeFallback = fallback == null ? Duration.ofMinutes(5) : fallback;
    if (!hasText(value)) {
      return safeFallback;
    }
    try {
      return Duration.parse(value.trim());
    } catch (RuntimeException e) {
      return safeFallback;
    }
  }

  private String resolveDurationString(String value, Duration fallback) {
    return resolveDuration(value, fallback).toString();
  }

  private int resolveInt(Integer value, int fallback, int min, int max) {
    return clamp(value == null ? fallback : value, min, max);
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }
}
