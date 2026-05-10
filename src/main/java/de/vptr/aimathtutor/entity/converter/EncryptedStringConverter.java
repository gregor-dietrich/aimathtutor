package de.vptr.aimathtutor.entity.converter;

import de.vptr.aimathtutor.service.security.EncryptionService;
import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that transparently encrypts {@link String} fields with AES-256-GCM on write and
 * decrypts on read. Apply via {@code @Convert(converter = EncryptedStringConverter.class)}.
 */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable final String attribute) {
        if (attribute == null) {
            return null;
        }
        return CDI.current().select(EncryptionService.class).get().encrypt(attribute);
    }

    @Override
    @Nullable
    public String convertToEntityAttribute(@Nullable final String dbData) {
        if (dbData == null) {
            return null;
        }
        return CDI.current().select(EncryptionService.class).get().decrypt(dbData);
    }
}
