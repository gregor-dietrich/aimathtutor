package de.vptr.aimathtutor.view.admin;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import de.vptr.aimathtutor.dto.AiProviderTestResultDto;
import de.vptr.aimathtutor.service.AiConfigService;
import de.vptr.aimathtutor.service.AiProviderTestService;
import de.vptr.aimathtutor.service.ai.AiConfigKeys;
import de.vptr.aimathtutor.util.NotificationUtil;
import jakarta.inject.Inject;

/**
 * Admin view for managing AI tutor configuration at runtime.
 * Allows admins to change AI provider, model, temperature, prompts, and other
 * settings
 * without restarting the application.
 */
@Route(value = "admin/config", layout = AdminMainLayout.class)
@PageTitle("AI Configuration - AI Math Tutor")
public class AdminConfigView extends AbstractAdminView {

    private static final Logger LOG = Logger.getLogger(AdminConfigView.class);
    private static final String TEMPERATURE_HELPER = "Temperature (0.0-2.0): Lower = more focused, Higher = more creative";
    private static final String MAX_TOKENS_HELPER = "Maximum tokens in response (1-8192)";

    @Inject
    private transient AiConfigService aiConfigService;

    @Inject
    private transient AiProviderTestService aiProviderTestService;

    @Inject
    private transient ManagedExecutor managedExecutor;

    @Override
    protected boolean isAuthorized() {
        final var userRank = this.userRankService.getCurrentUserRank();
        return userRank != null && (userRank.canAdminView()
                || userRank.hasAnyExercisePermission()
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
     * Called before the view is shown. Ensures authentication and proper
     * permissions.
     * Configuration can only be managed by users with exercise or lesson
     * permissions.
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

        // Title
        final var title = new H2("AI Configuration");
        this.add(title);

        // Create tabs for different configuration categories
        final var tabs = new Tabs();
        tabs.setWidthFull();

        // General tab
        final var generalTab = new Tab("General");
        final var generalPanel = this.buildGeneralPanel();

        // Gemini tab
        final var geminiTab = new Tab("Gemini");
        final var geminiPanel = this.buildGeminiPanel();

        // Ollama tab
        final var ollamaTab = new Tab("Ollama");
        final var ollamaPanel = this.buildOllamaPanel();

        // OpenAI tab
        final var openaiTab = new Tab("OpenAI");
        final var openaiPanel = this.buildOpenAiPanel();

        // Prompts tab
        final var promptsTab = new Tab("Prompts");
        final var promptsPanel = this.buildPromptsPanel();

        tabs.add(generalTab, geminiTab, ollamaTab, openaiTab, promptsTab);

        this.add(tabs);

        // Container for tab content
        final var contentContainer = new VerticalLayout();
        contentContainer.setSpacing(true);
        contentContainer.setPadding(false);
        contentContainer.setWidthFull();
        contentContainer.add(generalPanel);

        this.add(contentContainer);

        // Handle tab selection
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            final Tab selected = event.getSelectedTab();
            if (selected == generalTab) {
                contentContainer.add(generalPanel);
            } else if (selected == geminiTab) {
                contentContainer.add(geminiPanel);
            } else if (selected == openaiTab) {
                contentContainer.add(openaiPanel);
            } else if (selected == ollamaTab) {
                contentContainer.add(ollamaPanel);
            } else if (selected == promptsTab) {
                contentContainer.add(promptsPanel);
            }
        });
    }

    private VerticalLayout buildGeneralPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        // AI Enabled
        final var enabledCheckbox = new Checkbox("Enable AI Tutor");
        final String enabledValue = this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_ENABLED, "true");
        enabledCheckbox.setValue("true".equalsIgnoreCase(enabledValue));
        enabledCheckbox.setLabel("Enable AI Tutor");

        // AI Provider selection
        final var providerCombo = new ComboBox<String>("AI Provider");
        providerCombo.setItems("mock", "gemini", "ollama", "openai");
        providerCombo.setValue(this.aiConfigService.getConfigValue(AiConfigKeys.AI_TUTOR_PROVIDER, "mock"));
        providerCombo.setWidthFull();

        // Save button
        final var saveBtn = new Button("Save", ignored -> this.saveGeneralConfig(enabledCheckbox, providerCombo));

        // Reset to defaults button
        final var resetBtn = new Button("Reset to Defaults", ignored -> this.resetAllToDefaults());
        resetBtn.getStyle().set("margin-left", "auto");

        final var buttonRow = new HorizontalLayout(saveBtn, resetBtn);
        buttonRow.setWidthFull();
        buttonRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        panel.add(enabledCheckbox, providerCombo, buttonRow);
        return panel;
    }

    private VerticalLayout buildGeminiPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var apiKeyField = this.createReadOnlyApiKeyField("GEMINI_API_KEY",
                "https://aistudio.google.com/app/apikey");

        final var modelField = this.createTextConfigField("Model", AiConfigKeys.GEMINI_MODEL, "gemma-3-27b-it",
                "Gemini model name (e.g., gemma-3-27b-it)");

        final var urlField = this.createTextConfigField("API Base URL", AiConfigKeys.GEMINI_API_BASE_URL,
                "https://generativelanguage.googleapis.com", null);

        final var tempField = this.createTemperatureField(AiConfigKeys.GEMINI_PREFIX);
        final var maxTokensField = this.createMaxTokensField(AiConfigKeys.GEMINI_PREFIX);

        final var saveBtn = new Button("Save",
                ignored -> this.saveGeminiConfig(modelField, urlField, tempField, maxTokensField));
        final var testBtn = new Button("Test Connection", ignored -> {
            this.saveGeminiConfig(modelField, urlField, tempField, maxTokensField);
            this.testGeminiConnection();
        });

        panel.add(apiKeyField, modelField, urlField, tempField, maxTokensField,
                this.buildSaveTestRow(saveBtn, testBtn));
        return panel;
    }

    private VerticalLayout buildOpenAiPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var apiKeyField = this.createReadOnlyApiKeyField("OPENAI_API_KEY",
                "https://platform.openai.com/api-keys");

        final var orgIdField = this.createTextConfigField("Organization ID (Optional)",
                AiConfigKeys.OPENAI_ORGANIZATION_ID, "", null);

        final var modelField = this.createTextConfigField("Model", AiConfigKeys.OPENAI_MODEL, "gpt-5-nano",
                "OpenAI model name (e.g., gpt-5-nano)");

        final var urlField = this.createTextConfigField("API Base URL", AiConfigKeys.OPENAI_API_BASE_URL,
                "https://api.openai.com/v1", null);

        final var tempField = this.createTemperatureField(AiConfigKeys.OPENAI_PREFIX);
        final var maxTokensField = this.createMaxTokensField(AiConfigKeys.OPENAI_PREFIX);

        final var saveBtn = new Button("Save",
                ignored -> this.saveOpenAiConfig(orgIdField, modelField, urlField, tempField, maxTokensField));
        final var testBtn = new Button("Test Connection", ignored -> {
            this.saveOpenAiConfig(orgIdField, modelField, urlField, tempField, maxTokensField);
            this.testOpenAiConnection();
        });

        panel.add(apiKeyField, orgIdField, modelField, urlField, tempField, maxTokensField,
                this.buildSaveTestRow(saveBtn, testBtn));
        return panel;
    }

    private VerticalLayout buildOllamaPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var apiUrlField = this.createTextConfigField("API URL", AiConfigKeys.OLLAMA_API_URL,
                "http://ollama:11434", "Ollama API URL (e.g., http://localhost:11434)");

        final var modelField = this.createTextConfigField("Model", AiConfigKeys.OLLAMA_MODEL, "llama3.2:3b",
                "Ollama model name (e.g., llama3.2:3b)");

        final var tempField = this.createTemperatureField(AiConfigKeys.OLLAMA_PREFIX);

        final var maxTokensField = this.createMaxTokensField(AiConfigKeys.OLLAMA_PREFIX);
        maxTokensField.setHelperText("Maximum tokens in response (1-8192). Use 2000+ to prevent truncated responses.");

        final var timeoutField = new NumberField("Timeout (seconds)");
        timeoutField.setValue(this.aiConfigService.getConfigValueAsInt(AiConfigKeys.OLLAMA_TIMEOUT_SECONDS, 30)
                .doubleValue());
        timeoutField.setMin(1);
        timeoutField.setMax(300);
        timeoutField.setStep(1);
        timeoutField.setHelperText("API timeout in seconds (1-300)");

        final var saveBtn = new Button("Save",
                ignored -> this.saveOllamaConfig(apiUrlField, modelField, tempField, maxTokensField, timeoutField));
        final var testBtn = new Button("Test Connection", ignored -> {
            this.saveOllamaConfig(apiUrlField, modelField, tempField, maxTokensField, timeoutField);
            this.testOllamaConnection();
        });

        panel.add(apiUrlField, modelField, tempField, maxTokensField, timeoutField,
                this.buildSaveTestRow(saveBtn, testBtn));
        return panel;
    }

    private VerticalLayout buildPromptsPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var qaPrefix = this.createPromptArea("Question Answering Prefix",
                AiConfigKeys.PROMPT_QUESTION_PREFIX, "Prefix for question answering prompts");
        final var qaPostfix = this.createPromptArea("Question Answering Postfix",
                AiConfigKeys.PROMPT_QUESTION_POSTFIX, "Postfix for question answering prompts");
        final var mtPrefix = this.createPromptArea("Math Tutoring Prefix",
                AiConfigKeys.PROMPT_TUTORING_PREFIX, "Prefix for math tutoring prompts");
        final var mtPostfix = this.createPromptArea("Math Tutoring Postfix",
                AiConfigKeys.PROMPT_TUTORING_POSTFIX, "Postfix for math tutoring prompts");

        // Save button
        final var saveBtn = new Button("Save",
                ignored -> this.savePromptsConfig(qaPrefix, qaPostfix, mtPrefix, mtPostfix));

        panel.add(qaPrefix, qaPostfix, mtPrefix, mtPostfix, saveBtn);
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
            final String helperText) {
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
        area.setMinHeight("150px");
        area.setHelperText(helperText);
        return area;
    }

    private HorizontalLayout buildSaveTestRow(final Button saveBtn, final Button testBtn) {
        final var row = new HorizontalLayout(saveBtn, testBtn);
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return row;
    }

    // --- Connection tests ---------------------------------------------------

    private void testConnection(final Supplier<AiProviderTestResultDto> testCall,
            final String providerName) {
        final var ui = getUI().orElse(null);
        if (ui == null) {
            return;
        }
        CompletableFuture.supplyAsync(testCall::get, this.managedExecutor).thenAccept(result -> {
            ui.access(() -> {
                if (result.success) {
                    NotificationUtil.showSuccess(result.message);
                } else {
                    NotificationUtil.showError(result.message);
                }
                LOG.infof("%s connection test: %s",  providerName,  result.message);
            });
        }).exceptionally(ex -> {
            ui.access(() -> {
                NotificationUtil.showError("Connection test failed: " + ex.getMessage());
                LOG.errorf(ex, "%s connection test failed",  providerName);
            });
            return null;
        });
    }

    private void testGeminiConnection() {
        this.testConnection(this.aiProviderTestService::testGemini, "Gemini");
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
    private Long requireUserId(final String action) {
        final Long userId = this.authService.getUserId();
        if (userId == null) {
            NotificationUtil.showError("You must be logged in to " + action);
            return null;
        }
        return userId;
    }

    /**
     * Persist a list of config updates with standard error handling and
     * a "{label} configuration updated successfully" notification on success.
     */
    private void saveProviderConfig(final String label, final List<AiConfigUpdateDto> updates) {
        try {
            final Long userId = this.requireUserId("save settings");
            if (userId == null) {
                return;
            }

            this.aiConfigService.updateMultipleConfigs(updates, userId);

            NotificationUtil.showSuccess(label + " configuration updated successfully");
            LOG.infof("%s config saved",  label);
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.errorf(e, "Validation error saving %s config",  label);
        } catch (final Exception e) {
            NotificationUtil.showError("Error saving configuration. Please try again later.");
            LOG.errorf(e, "Error saving %s config",  label);
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
        if (value.doubleValue() != value.intValue()) {
            throw new IllegalArgumentException(
                    field.getLabel() + " must be a whole number, but got: " + value);
        }
        return Integer.toString(value.intValue());
    }

    private void resetAllToDefaults() {
        try {
            final Long userId = this.requireUserId("reset settings");
            if (userId == null) {
                return;
            }
            this.aiConfigService.resetToDefaults(userId);
            NotificationUtil.showSuccess("All settings reset to defaults");
            LOG.info("Reset all AI configs to defaults");
            this.buildUi();
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.error("Validation error resetting defaults", e);
        } catch (final Exception e) {
            NotificationUtil.showError("Error resetting defaults. Please try again later.");
            LOG.error("Error resetting defaults", e);
        }
    }

    private void saveGeneralConfig(final Checkbox enabledCheckbox,
            final ComboBox<String> providerCombo) {
        this.saveProviderConfig("AI", List.of(
                new AiConfigUpdateDto(AiConfigKeys.AI_TUTOR_ENABLED, enabledCheckbox.getValue() ? "true" : "false"),
                new AiConfigUpdateDto(AiConfigKeys.AI_TUTOR_PROVIDER, providerCombo.getValue())));
    }

    private void saveGeminiConfig(final TextField modelField, final TextField urlField,
            final NumberField tempField, final NumberField maxTokensField) {
        this.saveProviderConfig("Gemini", List.of(
                new AiConfigUpdateDto(AiConfigKeys.GEMINI_MODEL, modelField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.GEMINI_API_BASE_URL, urlField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.GEMINI_TEMPERATURE, temperatureOrDefault(tempField)),
                new AiConfigUpdateDto(AiConfigKeys.GEMINI_MAX_TOKENS, intOrDefault(maxTokensField, "2000"))));
    }

    private void saveOpenAiConfig(final TextField orgIdField, final TextField modelField, final TextField urlField,
            final NumberField tempField, final NumberField maxTokensField) {
        this.saveProviderConfig("OpenAI", List.of(
                new AiConfigUpdateDto(AiConfigKeys.OPENAI_ORGANIZATION_ID, orgIdField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.OPENAI_MODEL, modelField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.OPENAI_API_BASE_URL, urlField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.OPENAI_TEMPERATURE, temperatureOrDefault(tempField)),
                new AiConfigUpdateDto(AiConfigKeys.OPENAI_MAX_TOKENS, intOrDefault(maxTokensField, "2000"))));
    }

    private void saveOllamaConfig(final TextField apiUrlField, final TextField modelField,
            final NumberField tempField, final NumberField maxTokensField, final NumberField timeoutField) {
        this.saveProviderConfig("Ollama", List.of(
                new AiConfigUpdateDto(AiConfigKeys.OLLAMA_API_URL, apiUrlField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.OLLAMA_MODEL, modelField.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.OLLAMA_TEMPERATURE, temperatureOrDefault(tempField)),
                new AiConfigUpdateDto(AiConfigKeys.OLLAMA_MAX_TOKENS, intOrDefault(maxTokensField, "2000")),
                new AiConfigUpdateDto(AiConfigKeys.OLLAMA_TIMEOUT_SECONDS, intOrDefault(timeoutField, "30"))));
    }

    private void savePromptsConfig(final TextArea questionPrefixArea, final TextArea questionPostfixArea,
            final TextArea tutoringPrefixArea, final TextArea tutoringPostfixArea) {
        this.saveProviderConfig("Prompts", List.of(
                new AiConfigUpdateDto(AiConfigKeys.PROMPT_QUESTION_PREFIX, questionPrefixArea.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.PROMPT_QUESTION_POSTFIX, questionPostfixArea.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.PROMPT_TUTORING_PREFIX, tutoringPrefixArea.getValue()),
                new AiConfigUpdateDto(AiConfigKeys.PROMPT_TUTORING_POSTFIX, tutoringPostfixArea.getValue())));
    }
}
