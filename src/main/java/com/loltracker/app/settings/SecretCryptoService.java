package com.loltracker.app.settings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretCryptoService {

  private static final String PREFIX = "v1";
  private static final int IV_BYTES = 12;
  private static final int GCM_TAG_BITS = 128;
  private static final SecureRandom RANDOM = new SecureRandom();

  @Value("${app.config.encryption-key:}")
  private String encryptionKey;

  public boolean isConfigured() {
    return encryptionKey != null && !encryptionKey.isBlank();
  }

  public String encrypt(String plainText) {
    if (plainText == null || plainText.isBlank()) {
      return null;
    }
    requireConfigured();
    try {
      byte[] iv = new byte[IV_BYTES];
      RANDOM.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      return PREFIX
          + ":"
          + Base64.getEncoder().encodeToString(iv)
          + ":"
          + Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to encrypt application secret", e);
    }
  }

  public String decrypt(String encryptedText) {
    if (encryptedText == null || encryptedText.isBlank()) {
      return null;
    }
    requireConfigured();
    try {
      String[] parts = encryptedText.split(":", 3);
      if (parts.length != 3 || !PREFIX.equals(parts[0])) {
        throw new IllegalArgumentException("Unsupported encrypted secret format");
      }
      byte[] iv = Base64.getDecoder().decode(parts[1]);
      byte[] encrypted = Base64.getDecoder().decode(parts[2]);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, keySpec(), new GCMParameterSpec(GCM_TAG_BITS, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to decrypt application secret", e);
    }
  }

  private void requireConfigured() {
    if (!isConfigured()) {
      throw new IllegalStateException("APP_CONFIG_ENCRYPTION_KEY is not configured");
    }
  }

  private SecretKeySpec keySpec() throws Exception {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] key = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
    return new SecretKeySpec(key, "AES");
  }
}
