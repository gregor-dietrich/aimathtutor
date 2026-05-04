package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import de.vptr.aimathtutor.dto.AiConfigDto.ConfigCategory;
import de.vptr.aimathtutor.dto.AiConfigDto.ConfigType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing AI configuration settings stored in the database.
 * Allows runtime management of AI provider, model, and prompt configuration
 * without requiring application restart.
 */
@Entity
@Table(name = "ai_config")
@NamedQueries({
        @NamedQuery(name = "AiConfig.findByKey", query = "FROM AiConfigEntity WHERE configKey = :key"),
        @NamedQuery(name = "AiConfig.findByCategory", query = "FROM AiConfigEntity WHERE category = :category ORDER BY configKey"),
        @NamedQuery(name = "AiConfig.findAll", query = "FROM AiConfigEntity ORDER BY category, configKey"),
})
public class AiConfigEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Version
    public Long version;

    @NotBlank
    @Column(name = "config_key", unique = true, nullable = false)
    public String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    public String configValue;

    @Column(name = "config_type", nullable = false)
    @Enumerated(EnumType.STRING)
    public ConfigType configType; // "STRING", "INTEGER", "DOUBLE", "BOOLEAN", "TEXT"

    @Column(name = "is_optional", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    public boolean isOptional = false; // Whether this config can have empty/null values

    @Column(name = "category", nullable = false)
    @Enumerated(EnumType.STRING)
    public ConfigCategory category; // "GENERAL", "GEMINI", "OPENAI", "OLLAMA", "PROMPTS"

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Generated(event = EventType.INSERT)
    public LocalDateTime created;

    @Generated(event = EventType.UPDATE)
    @Column(name = "last_edit")
    public LocalDateTime lastEdit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by")
    public UserEntity lastUpdatedBy;

    /**
     * Default constructor for Hibernate.
     */
    public AiConfigEntity() {
    }

    /**
     * Constructor with required fields.
     */
    public AiConfigEntity(final String configKey, final String configValue, final ConfigType configType,
            final ConfigCategory category) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.isOptional = false;
    }

    /**
     * Constructor with all fields including optionality.
     */
    public AiConfigEntity(final String configKey, final String configValue, final ConfigType configType,
            final ConfigCategory category, final String description, final boolean isOptional) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.description = description;
        this.isOptional = isOptional;
    }

    /**
     * Constructor with description but default optionality (not optional).
     */
    public AiConfigEntity(final String configKey, final String configValue, final ConfigType configType,
            final ConfigCategory category, final String description) {
        this(configKey, configValue, configType, category, description, false);
    }
}
