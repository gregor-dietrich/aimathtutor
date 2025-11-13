package de.vptr.aimathtutor.entity;

import java.time.LocalDateTime;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

/**
 * Entity representing AI configuration settings stored in the database.
 * Allows runtime management of AI provider, model, and prompt configuration
 * without requiring application restart.
 */
@Entity
@Table(name = "ai_config", uniqueConstraints = {
        @UniqueConstraint(columnNames = "config_key")
})
@NamedQueries({
        @NamedQuery(name = "AiConfig.findByKey", query = "FROM AiConfigEntity WHERE configKey = :key"),
        @NamedQuery(name = "AiConfig.findByCategory", query = "FROM AiConfigEntity WHERE category = :category ORDER BY configKey"),
        @NamedQuery(name = "AiConfig.findAll", query = "FROM AiConfigEntity ORDER BY category, configKey"),
})
public class AiConfigEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @NotBlank
    @Column(name = "config_key", unique = true, nullable = false)
    public String configKey;

    @Column(name = "config_value", columnDefinition = "TEXT")
    public String configValue;

    @Column(name = "config_type")
    public String configType; // "STRING", "INTEGER", "DOUBLE", "BOOLEAN", "TEXT"

    @Column(name = "is_optional", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    public Boolean isOptional = false; // Whether this config can have empty/null values

    @Column(name = "category")
    public String category; // "GENERAL", "GEMINI", "OPENAI", "OLLAMA", "PROMPTS"

    @Column(name = "description", columnDefinition = "TEXT")
    public String description;

    @Column(name = "last_updated_at")
    public LocalDateTime lastUpdatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by")
    public UserEntity lastUpdatedBy;

    /**
     * Default constructor for Hibernate.
     */
    public AiConfigEntity() {
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with required fields.
     */
    public AiConfigEntity(final String configKey, final String configValue, final String configType,
            final String category) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.isOptional = false;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with all fields including optionality.
     */
    public AiConfigEntity(final String configKey, final String configValue, final String configType,
            final String category, final String description, final Boolean isOptional) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.category = category;
        this.description = description;
        this.isOptional = isOptional != null ? isOptional : false;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Constructor with description but default optionality (not optional).
     */
    public AiConfigEntity(final String configKey, final String configValue, final String configType,
            final String category, final String description) {
        this(configKey, configValue, configType, category, description, false);
    }
}
