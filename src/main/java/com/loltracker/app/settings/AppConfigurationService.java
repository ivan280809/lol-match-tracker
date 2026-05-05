package com.loltracker.app.settings;

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

  @Transactional(readOnly = true)
  public AppConfigurationView getView() {
    Optional<AppConfigurationEntity> entity = appConfigurationRepository.findById(CONFIGURATION_ID);
    String riotApiKeyEncrypted = entity.map(AppConfigurationEntity::getRiotApiKeyEncrypted).orElse(null);
    String persistedRegion = entity.map(AppConfigurationEntity::getRiotRegion).orElse(null);
    String telegramBotTokenEncrypted =
        entity.map(AppConfigurationEntity::getTelegramBotTokenEncrypted).orElse(null);
    String telegramChatIdEncrypted =
        entity.map(AppConfigurationEntity::getTelegramChatIdEncrypted).orElse(null);
    return new AppConfigurationView(
        hasConfiguredSecret(riotApiKeyEncrypted, fallbackRiotApiKey),
        resolveRegion(persistedRegion),
        hasConfiguredSecret(telegramBotTokenEncrypted, fallbackTelegramBotToken),
        hasConfiguredSecret(telegramChatIdEncrypted, fallbackTelegramChatId),
        secretCryptoService.isConfigured(),
        secretSource(riotApiKeyEncrypted, fallbackRiotApiKey),
        regionSource(persistedRegion),
        secretSource(telegramBotTokenEncrypted, fallbackTelegramBotToken),
        secretSource(telegramChatIdEncrypted, fallbackTelegramChatId));
  }

  @Transactional(readOnly = true)
  public AppConfigurationForm getForm() {
    RiotRegion region =
        appConfigurationRepository
            .findById(CONFIGURATION_ID)
            .map(AppConfigurationEntity::getRiotRegion)
            .map(this::resolveRegion)
            .orElse(resolveRegion(null));
    return new AppConfigurationForm("", region.name(), "", "");
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
            fallbackTelegramChatId));
  }

  @Transactional
  public void update(AppConfigurationForm form) {
    AppConfigurationEntity entity =
        appConfigurationRepository.findById(CONFIGURATION_ID).orElseGet(this::newConfiguration);
    entity.setRiotRegion(RiotRegion.from(form.riotRegion()).name());
    updateSecret(form.riotApiKey(), entity::setRiotApiKeyEncrypted);
    updateSecret(form.telegramBotToken(), entity::setTelegramBotTokenEncrypted);
    updateSecret(form.telegramChatId(), entity::setTelegramChatIdEncrypted);
    appConfigurationRepository.save(entity);
  }

  private AppConfigurationEntity newConfiguration() {
    AppConfigurationEntity entity = new AppConfigurationEntity();
    entity.setId(CONFIGURATION_ID);
    entity.setRiotRegion(resolveRegion(null).name());
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
}
