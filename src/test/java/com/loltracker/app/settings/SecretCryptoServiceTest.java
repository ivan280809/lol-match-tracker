package com.loltracker.app.settings;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SecretCryptoServiceTest {

  @Test
  void encryptAndDecryptRoundTripWithoutPlainTextStorage() {
    SecretCryptoService service = new SecretCryptoService();
    ReflectionTestUtils.setField(service, "encryptionKey", "test-master-key");

    String encrypted = service.encrypt("super-secret-token");

    assertNotNull(encrypted);
    assertTrue(encrypted.startsWith("v1:"));
    assertFalse(encrypted.contains("super-secret-token"));
    assertEquals("super-secret-token", service.decrypt(encrypted));
  }

  @Test
  void encryptRequiresMasterKey() {
    SecretCryptoService service = new SecretCryptoService();
    ReflectionTestUtils.setField(service, "encryptionKey", "");

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> service.encrypt("secret"));

    assertEquals("APP_CONFIG_ENCRYPTION_KEY is not configured", exception.getMessage());
  }
}
