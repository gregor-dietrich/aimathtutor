package de.vptr.aimathtutor.view.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.ProviderTestResultDto;
import de.vptr.aimathtutor.service.ai.AiConfigKeys;
import de.vptr.aimathtutor.service.ai.AiConfigService;
import de.vptr.aimathtutor.service.ai.ProviderTestService;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

/**
 * Admin view for managing AI tutor configuration at runtime. Allows admins to change AI provider, model, temperature,
 * prompts, and other settings without restarting the application.
 */
@Route(value = "admin/config", layout = AdminMainLayout.class)
@PageTitle("AI Configuration - AI Math Tutor")
public class AdminConfigView extends AbstractAdminView {

    private static final Logger LOG = Logger.getLogger(AdminConfigView.class);
    private static final String TEMPERATURE_HELPER =
            "Temperature (0.0-2.0): Lower = more focused, Higher = more creative";
    private static final String MAX_TOKENS_HELPER = "Maximum tokens in response (1-8192)";

    @Inject
    private transient AiConfigService aiConfigService;

    @Inject
    private transient ProviderTestService aiProviderTestService;

    @Inject
    private transient ManagedExecutor managedExecutor;

    @Override
    protected boolean isAuthorized() {
        final var userRank = this.userRankService.getCurrentUserRank();
        return userRank != null && (userRank.canAdminView() || userRank.hasAnyExercisePermission()
                || userRank.hasAnyLessonPermission());
    }

    /**
     * Create a new admin config view with default layout initialization.
     */
    public AdminConfigView() {
        this.setSizeFull();
        this.setPadding(true);
        this.setSpacing(true);
    }

    /**
     * Called before the view is shown. Ensures authentication and proper permissions. Configuration can only be managed
     * by users with exercise or lesson permissions.
     */
    @Override
    public void beforeEnter(final BeforeEnterEvent event) {
        if (!this.isAuthOk(event)) {
            return;
        }
        this.buildUi();
    }

    private void buildUi() {
        this.removeAll();

        final var title = new H2("AI Configuration");
        this.add(title);

        final var tabs = new Tabs();
        tabs.setWidthFull();

        final var providerTab = new Tab("Provider");
        final var providerPanel = this.buildProviderPanel();

        final var promptsTab = new Tab("Prompts");
        final var promptsPanel = this.buildPromptsPanel();

        tabs.add(providerTab, promptsTab);
        this.add(tabs);

        final var contentContainer = new VerticalLayout();
        contentContainer.setSpacing(true);
        contentContainer.setPadding(false);
        contentContainer.setWidthFull();
        contentContainer.add(providerPanel);
        this.add(contentContainer);

        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(promptsTab)) {
                contentContainer.add(promptsPanel);
            } else {
                contentContainer.add(providerPanel);
            }
        });
    }

    private VerticalLayout buildProviderPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var enabledCheckbox = new Checkbox("Enable AI Tutor");
        enabledCheckbox.setValue(
                "true".equalsIgnoreCase(this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_ENABLED, "true")));

        final var providerCombo = new ComboBox<String>("AI Provider");
        providerCombo.setItems("mock", "google", "ollama", "openai");
        providerCombo.setValue(this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_PROVIDER, "mock"));
        providerCombo.setWidthFull();

        // Google provider fields
        final var googleApiKeyField =
                this.createReadOnlyApiKeyField("GOOGLE_API_KEY", "https://aistudio.google.com/app/apikey");
        final var googleModelField =
                this.createTextConfigField("Model", AiConfigKeys.GOOGLE_MODEL, "gemini-3.1-flash-lite",
                        "Google AI model name (e.g., gemini-2.5-flash-lite, gemma-4-31b-it, gemini-3.1-pro, ...)");
        final var googleUrlField = this.createTextConfigField("API Base URL", AiConfigKeys.GOOGLE_API_BASE_URL,
                "https://generativelanguage.googleapis.com", null);
        final var googleTempField = this.createTemperatureField(AiConfigKeys.GOOGLE_PREFIX);
        final var googleMaxTokensField = this.createMaxTokensField(AiConfigKeys.GOOGLE_PREFIX);
        final var googleSection = new VerticalLayout(googleApiKeyField, googleModelField, googleUrlField,
                googleTempField, googleMaxTokensField);
        googleSection.setSpacing(true);
        googleSection.setPadding(false);

        // OpenAI provider fields
        final var openaiApiKeyField =
                this.createReadOnlyApiKeyField("OPENAI_API_KEY", "https://platform.openai.com/api-keys");
        final var openaiOrgIdField =
                this.createTextConfigField("Organization ID (Optional)", AiConfigKeys.OPENAI_ORGANIZATION_ID, "", null);
        final var openaiModelField = this.createTextConfigField("Model", AiConfigKeys.OPENAI_MODEL, "gpt-4.1-mini",
                "OpenAI model name (e.g., gpt-5-nano, gpt-5.4-mini, gpt-5.5, ...)");
        final var openaiUrlField = this.createTextConfigField("API Base URL", AiConfigKeys.OPENAI_API_BASE_URL,
                "https://api.openai.com/v1", null);
        final var openaiTempField = this.createTemperatureField(AiConfigKeys.OPENAI_PREFIX);
        final var openaiMaxTokensField = this.createMaxTokensField(AiConfigKeys.OPENAI_PREFIX);
        final var openaiSection = new VerticalLayout(openaiApiKeyField, openaiOrgIdField, openaiModelField,
                openaiUrlField, openaiTempField, openaiMaxTokensField);
        openaiSection.setSpacing(true);
        openaiSection.setPadding(false);

        // Ollama provider fields
        final var ollamaApiUrlField = this.createTextConfigField("API URL", AiConfigKeys.OLLAMA_API_URL,
                "http://ollama:11434", "Ollama API URL (e.g., http://localhost:11434)");
        final var ollamaModelField = this.createTextConfigField("Model", AiConfigKeys.OLLAMA_MODEL, "llama3.2:3b",
                "Ollama model name (e.g., gemma3:1b, llama3.2:3b, qwen3:8b, phi4:14b, ...)");
        final var ollamaTempField = this.createTemperatureField(AiConfigKeys.OLLAMA_PREFIX);
        final var ollamaMaxTokensField = this.createMaxTokensField(AiConfigKeys.OLLAMA_PREFIX);
        ollamaMaxTokensField
                .setHelperText("Maximum tokens in response (1-8192). Use 2000+ to prevent truncated responses.");
        final var ollamaSection =
                new VerticalLayout(ollamaApiUrlField, ollamaModelField, ollamaTempField, ollamaMaxTokensField);
        ollamaSection.setSpacing(true);
        ollamaSection.setPadding(false);

        final var saveBtn = new Button("Save");
        final var testBtn = new Button("Test Connection");
        final var resetBtn = new Button("Reset to Defaults");

        final var leftButtons = new HorizontalLayout(saveBtn, testBtn);
        final var buttonRow = new HorizontalLayout(leftButtons, resetBtn);
        buttonRow.setWidthFull();
        buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        final Runnable updateSectionVisibility = () -> {
            final String p = providerCombo.getValue();
            googleSection.setVisible("google".equals(p));
            openaiSection.setVisible("openai".equals(p));
            ollamaSection.setVisible("ollama".equals(p));
            testBtn.setVisible(p != null && !"mock".equals(p));
        };
        providerCombo.addValueChangeListener(ignored -> updateSectionVisibility.run());
        updateSectionVisibility.run();

        final Runnable updateEnabledState = () -> {
            final boolean enabled = Boolean.TRUE.equals(enabledCheckbox.getValue());
            final String p = providerCombo.getValue();
            providerCombo.setEnabled(enabled);
            googleSection.setEnabled(enabled);
            openaiSection.setEnabled(enabled);
            ollamaSection.setEnabled(enabled);
            saveBtn.setEnabled(true);
            testBtn.setEnabled(enabled && p != null && !"mock".equals(p));
        };
        enabledCheckbox.addValueChangeListener(ignored -> updateEnabledState.run());
        updateEnabledState.run();

        final Supplier<Optional<List<AiConfigUpdateDto>>> buildUpdates = () -> {
            final String provider = providerCombo.getValue();
            final var updates = new ArrayList<AiConfigUpdateDto>();
            updates.add(new AiConfigUpdateDto(AiConfigKeys.AI_TUTOR_ENABLED,
                    enabledCheckbox.getValue() ? "true" : "false"));
            updates.add(new AiConfigUpdateDto(AiConfigKeys.AI_TUTOR_PROVIDER, provider != null ? provider : "mock"));
            try {
                if ("google".equals(provider)) {
                    updates.addAll(
                            List.of(new AiConfigUpdateDto(AiConfigKeys.GOOGLE_MODEL, googleModelField.getValue()),
                                    new AiConfigUpdateDto(AiConfigKeys.GOOGLE_API_BASE_URL, googleUrlField.getValue()),
                                    new AiConfigUpdateDto(AiConfigKeys.GOOGLE_TEMPERATURE,
                                            temperatureOrDefault(googleTempField)),
                                    new AiConfigUpdateDto(AiConfigKeys.GOOGLE_MAX_TOKENS,
                                            intOrDefault(googleMaxTokensField, "2000"))));
                } else if ("openai".equals(provider)) {
                    updates.addAll(List.of(
                            new AiConfigUpdateDto(AiConfigKeys.OPENAI_ORGANIZATION_ID, openaiOrgIdField.getValue()),
                            new AiConfigUpdateDto(AiConfigKeys.OPENAI_MODEL, openaiModelField.getValue()),
                            new AiConfigUpdateDto(AiConfigKeys.OPENAI_API_BASE_URL, openaiUrlField.getValue()),
                            new AiConfigUpdateDto(AiConfigKeys.OPENAI_TEMPERATURE,
                                    temperatureOrDefault(openaiTempField)),
                            new AiConfigUpdateDto(AiConfigKeys.OPENAI_MAX_TOKENS,
                                    intOrDefault(openaiMaxTokensField, "2000"))));
                } else if ("ollama".equals(provider)) {
                    updates.addAll(
                            List.of(new AiConfigUpdateDto(AiConfigKeys.OLLAMA_API_URL, ollamaApiUrlField.getValue()),
                                    new AiConfigUpdateDto(AiConfigKeys.OLLAMA_MODEL, ollamaModelField.getValue()),
                                    new AiConfigUpdateDto(AiConfigKeys.OLLAMA_TEMPERATURE,
                                            temperatureOrDefault(ollamaTempField)),
                                    new AiConfigUpdateDto(AiConfigKeys.OLLAMA_MAX_TOKENS,
                                            intOrDefault(ollamaMaxTokensField, "2000"))));
                }
            } catch (final IllegalArgumentException e) {
                NotificationUtil.showError("Validation error: " + e.getMessage());
                return Optional.empty();
            }
            return Optional.of(updates);
        };

        saveBtn.addClickListener(
                ignored -> this.onProviderSaveOrTest(false, saveBtn, testBtn, resetBtn, buildUpdates, providerCombo));
        testBtn.addClickListener(
                ignored -> this.onProviderSaveOrTest(true, saveBtn, testBtn, resetBtn, buildUpdates, providerCombo));
        resetBtn.addClickListener(ignored -> this.onProviderReset(saveBtn, testBtn, resetBtn));

        panel.add(enabledCheckbox, providerCombo, googleSection, openaiSection, ollamaSection, buttonRow);
        return panel;
    }

    private VerticalLayout buildPromptsPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var qaPrefix = this.createPromptArea("Question Answering Prefix", AiConfigKeys.PROMPT_QUESTION_PREFIX,
                "Prefix for question answering prompts");
        final var qaPostfix = this.createPromptArea("Question Answering Postfix", AiConfigKeys.PROMPT_QUESTION_POSTFIX,
                "Postfix for question answering prompts");
        final var mtPrefix = this.createPromptArea("Math Tutoring Prefix", AiConfigKeys.PROMPT_TUTORING_PREFIX,
                "Prefix for math tutoring prompts");
        final var mtPostfix = this.createPromptArea("Math Tutoring Postfix", AiConfigKeys.PROMPT_TUTORING_POSTFIX,
                "Postfix for math tutoring prompts");

        final var grid = new FormLayout();
        grid.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));
        grid.add(qaPrefix, qaPostfix, mtPrefix, mtPostfix);

        final var saveBtn = new Button("Save");
        saveBtn.addClickListener(e -> this.onPromptsSave(saveBtn, qaPrefix, qaPostfix, mtPrefix, mtPostfix));

        panel.add(grid, saveBtn);
        return panel;
    }

    // --- Field builders -----------------------------------------------------

    private PasswordField createReadOnlyApiKeyField(final String envVarName, final String docsUrl) {
        final var field = new PasswordField("API Key");
        field.setValue("••••••••");
        field.setReadOnly(true);
        field.setHelperText("API key is managed via " + envVarName + " environment variable. Get key from: " + docsUrl);
        return field;
    }

    private TextField createTextConfigField(final String label, final String configKey, final String defaultValue,
            @Nullable final String helperText) {
        final var field = new TextField(label);
        field.setValue(this.aiConfigService.getConfigValue(configKey, defaultValue));
        field.setWidthFull();
        if (helperText != null) {
            field.setHelperText(helperText);
        }
        return field;
    }

    private NumberField createTemperatureField(final String configPrefix) {
        final var field = new NumberField("Temperature");
        field.setValue(this.aiConfigService.getConfigValueAsDouble(configPrefix + ".temperature", 0.7));
        field.setMin(0.0);
        field.setMax(2.0);
        field.setStep(0.1);
        field.setHelperText(TEMPERATURE_HELPER);
        return field;
    }

    private NumberField createMaxTokensField(final String configPrefix) {
        final var field = new NumberField("Max Tokens");
        field.setValue(this.aiConfigService.getConfigValueAsInt(configPrefix + ".max-tokens", 2000).doubleValue());
        field.setMin(1);
        field.setMax(8192);
        field.setStep(1);
        field.setHelperText(MAX_TOKENS_HELPER);
        return field;
    }

    private TextArea createPromptArea(final String label, final String configKey, final String helperText) {
        final var area = new TextArea(label);
        area.setValue(this.aiConfigService.getConfigValue(configKey, ""));
        area.setWidthFull();
        area.setMinRows(18);
        area.setHelperText(helperText);
        return area;
    }

    // --- Provider button handlers -------------------------------------------

    private void onProviderSaveOrTest(final boolean withTest, final Button saveBtn, final Button testBtn,
            final Button resetBtn, final Supplier<Optional<List<AiConfigUpdateDto>>> buildUpdates,
            final ComboBox<String> providerCombo) {
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        final Long userId = this.requireUserId("save settings");
        if (userId == null) {
            return;
        }
        final var updatesOpt = buildUpdates.get();
        if (updatesOpt.isEmpty()) {
            return;
        }
        final var updates = updatesOpt.get();
        final String provider = withTest ? providerCombo.getValue() : "";
        saveBtn.setEnabled(false);
        testBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        final var _ = CompletableFuture
                .supplyAsync(() -> this.persistConfig("Provider", updates, userId), this.managedExecutor)
                .thenAccept(success -> {
                    final var _ = ui.access(() -> {
                        saveBtn.setEnabled(true);
                        testBtn.setEnabled(true);
                        resetBtn.setEnabled(true);
                        if (success) {
                            if (withTest) {
                                if ("google".equals(provider)) {
                                    this.testGoogleConnection();
                                } else if ("openai".equals(provider)) {
                                    this.testOpenAiConnection();
                                } else if ("ollama".equals(provider)) {
                                    this.testOllamaConnection();
                                }
                            } else {
                                NotificationUtil.showSuccess("Provider configuration updated successfully");
                            }
                        } else {
                            NotificationUtil.showError("Error saving configuration. Please try again later.");
                        }
                    });
                }).exceptionally(ex -> {
                    final var _ = ui.access(() -> {
                        NotificationUtil.showError("Error saving configuration. Please try again later.");
                        LOG.errorf(ex, "Error saving Provider config");
                        saveBtn.setEnabled(true);
                        testBtn.setEnabled(true);
                        resetBtn.setEnabled(true);
                    });
                    return null;
                });
    }

    private void onProviderReset(final Button saveBtn, final Button testBtn, final Button resetBtn) {
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        final Long userId = this.requireUserId("reset settings");
        if (userId == null) {
            return;
        }
        saveBtn.setEnabled(false);
        testBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        final var _ = CompletableFuture
                .runAsync(() -> this.aiConfigService.resetToDefaults(userId), this.managedExecutor).thenRun(() -> {
                    final var _ = ui.access(() -> {
                        NotificationUtil.showSuccess("All settings reset to defaults");
                        LOG.info("Reset all AI configs to defaults");
                        this.buildUi();
                    });
                }).exceptionally(ex -> {
                    final var _ = ui.access(() -> {
                        NotificationUtil.showError("Error resetting defaults. Please try again later.");
                        LOG.error("Error resetting defaults", ex);
                        saveBtn.setEnabled(true);
                        testBtn.setEnabled(true);
                        resetBtn.setEnabled(true);
                    });
                    return null;
                });
    }

    // --- Connection tests ---------------------------------------------------

    private void testConnection(final Supplier<ProviderTestResultDto> testCall, final String providerName) {
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        final var _ = CompletableFuture.supplyAsync(testCall, this.managedExecutor).thenAccept(result -> {
            final var _ = ui.access(() -> {
                final String msg = result.message != null ? result.message : "No response";
                if (result.success) {
                    NotificationUtil.showSuccess(msg);
                } else {
                    NotificationUtil.showError(msg);
                }
                LOG.infof("%s connection test: %s", providerName, result.message);
            });
        }).exceptionally(ex -> {
            final var _ = ui.access(() -> {
                NotificationUtil.showError("Connection test failed: " + ex.getMessage());
                LOG.errorf(ex, "%s connection test failed", providerName);
            });
            return null;
        });
    }

    private void testGoogleConnection() {
        this.testConnection(this.aiProviderTestService::testGoogle, "Google");
    }

    private void testOpenAiConnection() {
        this.testConnection(this.aiProviderTestService::testOpenAi, "OpenAI");
    }

    private void testOllamaConnection() {
        this.testConnection(this.aiProviderTestService::testOllama, "Ollama");
    }

    // --- Save helpers -------------------------------------------------------

    // requireUserId helper: every save method must null-check getUserId()
    // before proceeding. Do NOT move getUserId() below first use.
    @Nullable
    private Long requireUserId(final String action) {
        final Long userId = this.authService.getUserId();
        if (userId == null) {
            NotificationUtil.showError("You must be logged in to " + action);
            return null;
        }
        return userId;
    }

    private void onPromptsSave(final Button saveBtn, final TextArea questionPrefixArea,
            final TextArea questionPostfixArea, final TextArea tutoringPrefixArea, final TextArea tutoringPostfixArea) {
        final var ui = this.getUI().orElse(null);
        if (ui == null) {
            return;
        }
        final Long userId = this.requireUserId("save settings");
        if (userId == null) {
            return;
        }
        final var updates =
                List.of(new AiConfigUpdateDto(AiConfigKeys.PROMPT_QUESTION_PREFIX, questionPrefixArea.getValue()),
                        new AiConfigUpdateDto(AiConfigKeys.PROMPT_QUESTION_POSTFIX, questionPostfixArea.getValue()),
                        new AiConfigUpdateDto(AiConfigKeys.PROMPT_TUTORING_PREFIX, tutoringPrefixArea.getValue()),
                        new AiConfigUpdateDto(AiConfigKeys.PROMPT_TUTORING_POSTFIX, tutoringPostfixArea.getValue()));
        saveBtn.setEnabled(false);
        final var _ = CompletableFuture
                .supplyAsync(() -> this.persistConfig("Prompts", updates, userId), this.managedExecutor)
                .thenAccept(success -> {
                    final var _ = ui.access(() -> {
                        saveBtn.setEnabled(true);
                        if (success) {
                            NotificationUtil.showSuccess("Prompts configuration updated successfully");
                        } else {
                            NotificationUtil.showError("Error saving configuration. Please try again later.");
                        }
                    });
                }).exceptionally(ex -> {
                    final var _ = ui.access(() -> {
                        NotificationUtil.showError("Error saving configuration. Please try again later.");
                        LOG.errorf(ex, "Error saving Prompts config");
                        saveBtn.setEnabled(true);
                    });
                    return null;
                });
    }

    private boolean persistConfig(final String label, final List<AiConfigUpdateDto> updates, final Long userId) {
        try {
            this.aiConfigService.updateMultipleConfigs(updates, userId);
            LOG.infof("%s config saved", label);
            return true;
        } catch (final Exception e) {
            LOG.errorf(e, "Error saving %s config", label);
            return false;
        }
    }

    private static String temperatureOrDefault(final NumberField field) {
        final var value = field.getValue();
        return value != null ? value.toString() : "0.7";
    }

    private static String intOrDefault(final NumberField field, final String defaultValue) {
        final var value = field.getValue();
        if (value == null) {
            return defaultValue;
        }
        if (value % 1 != 0) {
            throw new IllegalArgumentException(field.getLabel() + " must be a whole number, but got: " + value);
        }
        return Integer.toString(value.intValue());
    }

}
