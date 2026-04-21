package com.petsupplies.core.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} for encrypting sensitive column values at rest.
 * Encryption uses AES-GCM via {@link Cipher} (see {@link AesGcmEncryptionService}).
 */
@Component
@Converter(autoApply = false)
public class AesEncryptionConverter implements AttributeConverter<String, String> {
  private final AesGcmEncryptionService encryptionService;

  public AesEncryptionConverter(AesGcmEncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null || attribute.isBlank()) {
      return null;
    }
    return encryptionService.encrypt(attribute);
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return null;
    }
    return encryptionService.decrypt(dbData);
  }
}
