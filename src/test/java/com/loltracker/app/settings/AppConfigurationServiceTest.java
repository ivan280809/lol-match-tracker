package com.loltracker.app.settings;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppConfigurationServiceTest {

  @Mock private AppConfigurationRepository appConfigurationRepository;
  @Mock private SecretCryptoService secretCryptoService;

  @InjectMocks private AppConfigurationService appConfigurationService;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(appConfigurationService, "fallbackRiotApiKey", "env-riot-key");
    ReflectionTestUtils.setField(appConfigurationService, "fallbackRiotRegion", "EUROPE");
    ReflectionTestUtils.setField(appConfigurationService, "fallbackTelegramBotToken", "env-telegram-token");
    ReflectionTestUtils.setField(appConfigurationService, "fallbackTelegramChatId", "env-chat-id");
  }

  @Test
  void runtimeConfigurationUsesEnvironmentFallbacksWhenDatabaseIsEmpty() {
    when(appConfigurationRepository.findById(1L)).thenReturn(Optional.empty());

    RuntimeAppConfiguration configuration = appConfigurationService.getRuntimeConfiguration();

    assertEquals("env-riot-key", configuration.riotApiKey());
    assertEquals(RiotRegion.EUROPE, configuration.riotRegion());
    assertEquals("env-telegram-token", configuration.telegramBotToken());
    assertEquals("env-chat-id", configuration.telegramChatId());
    verifyNoInteractions(secretCryptoService);
  }

  @Test
  void updateEncryptsProvidedSecretsAndKeepsBlankExistingSecrets() {
    AppConfigurationEntity entity = new AppConfigurationEntity();
    entity.setId(1L);
    entity.setRiotRegion(RiotRegion.EUROPE.name());
    entity.setTelegramBotTokenEncrypted("existing-telegram-token");
    when(appConfigurationRepository.findById(1L)).thenReturn(Optional.of(entity));
    when(secretCryptoService.encrypt("new-riot-key")).thenReturn("encrypted-riot-key");
    when(secretCryptoService.encrypt("chat-id")).thenReturn("encrypted-chat-id");

    appConfigurationService.update(
        new AppConfigurationForm(" new-riot-key ", RiotRegion.ASIA.name(), "", " chat-id "));

    assertEquals(RiotRegion.ASIA.name(), entity.getRiotRegion());
    assertEquals("encrypted-riot-key", entity.getRiotApiKeyEncrypted());
    assertEquals("existing-telegram-token", entity.getTelegramBotTokenEncrypted());
    assertEquals("encrypted-chat-id", entity.getTelegramChatIdEncrypted());
    verify(appConfigurationRepository).save(entity);
  }

  @Test
  void viewShowsConfiguredStatusWithoutDecryptingSecrets() {
    AppConfigurationEntity entity = new AppConfigurationEntity();
    entity.setId(1L);
    entity.setRiotRegion(RiotRegion.AMERICAS.name());
    entity.setRiotApiKeyEncrypted("encrypted-riot-key");
    when(appConfigurationRepository.findById(1L)).thenReturn(Optional.of(entity));
    when(secretCryptoService.isConfigured()).thenReturn(true);

    AppConfigurationView view = appConfigurationService.getView();

    assertTrue(view.riotApiKeyConfigured());
    assertEquals(RiotRegion.AMERICAS, view.riotRegion());
    assertTrue(view.telegramBotTokenConfigured());
    assertTrue(view.telegramChatIdConfigured());
    assertTrue(view.encryptionConfigured());
    verify(secretCryptoService, never()).decrypt(anyString());
  }
}
