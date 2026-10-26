package de.vptr.aimathtutor.view.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.dto.Permission;
import de.vptr.aimathtutor.dto.ProviderTestResultDto;
import de.vptr.aimathtutor.service.ai.AiConfigKeys;
import de.vptr.aimathtutor.service.ai.AiConfigService;
import de.vptr.aimathtutor.service.ai.ProviderTestService;
import de.vptr.aimathtutor.util.AsyncDataLoader;
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

    /** Identifies the newest settings load; a result for an older one, or one finishing after detach, is dropped. */
    private int loadRequestId;

    @Override
    protected boolean isAuthorized() {
        // Intentional: viewing the AI configuration is allowed for admins and for any teacher with
        // exercise or lesson permissions, so they can see which provider/model/prompts are active.
        // This grants read access only — saving still requires the dedicated aiConfigEdit permission
        // (enforced in AiConfigService), and API keys are never shown (masked, read-only fields).
        final var userRank = this.callerRank;
        return userRank != null && (userRank.has(Permission.ADMIN_VIEW) || userRank.hasAny(Permission.Group.EXERCISE)
                || userRank.hasAny(Permission.Group.LESSON));
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
     * Shows the title and a loading placeholder, and starts loading the settings once the view is attached.
     */
    @Override
    protected void buildUi() {
        this.showLoading();
        if (this.isAttached()) {
            this.loadConfig();
        }
    }

    @Override
    protected void onAttach(final AttachEvent event) {
        super.onAttach(event);
        this.loadConfig();
    }

    @Override
    protected void onDetach(final DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        this.loadRequestId++;
    }

    private void loadConfig() {
        final int requestId = ++this.loadRequestId;
        AsyncDataLoader.load(this.aiConfigService::getEffectiveConfig, this, config -> {
            if (requestId == this.loadRequestId) {
                this.showConfig(config);
            }
        }, () -> {
            if (requestId == this.loadRequestId) {
                this.showLoadFailure();
            }
        }, "Failed to load AI settings. Please try again.");
    }

    /** Replaces everything below the title with the given content. */
    private void showBelowTitle(final Component... content) {
        this.removeAll();
        this.add(new H2("AI Configuration"));
        this.add(content);
    }

    private void showLoading() {
        this.showBelowTitle(new Span("Loading AI settings..."));
    }

    private void showLoadFailure() {
        final var retryBtn = new Button("Retry", ignored -> {
            this.showLoading();
            this.loadConfig();
        });
        this.showBelowTitle(new Span("The AI settings could not be loaded."), retryBtn);
    }

    private void showConfig(final Map<String, String> config) {
        final var tabs = new Tabs();
        tabs.setWidthFull();

        final var providerTab = new Tab("Provider");
        final var providerPanel = this.buildProviderPanel(config);

        final var promptsTab = new Tab("Prompts");
        final var promptsPanel = this.buildPromptsPanel(config);

        tabs.add(providerTab, promptsTab);

        final var contentContainer = new VerticalLayout();
        contentContainer.setSpacing(true);
        contentContainer.setPadding(false);
        contentContainer.setWidthFull();
        contentContainer.add(providerPanel);
        this.showBelowTitle(tabs, contentContainer);

        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll();
            if (event.getSelectedTab().equals(promptsTab)) {
                contentContainer.add(promptsPanel);
            } else {
                contentContainer.add(providerPanel);
            }
        });
    }

    private VerticalLayout buildProviderPanel(final Map<String, String> config) {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var enabledCheckbox = new Checkbox("Enable AI Tutor");
        enabledCheckbox.setValue("true".equalsIgnoreCase(configValue(config, AiConfigKeys.AI_TUTOR_ENABLED)));

        final var providerCombo = new ComboBox<String>("AI Provider");
        providerCombo.setItems("mock", "google", "ollama", "openai");
        providerCombo.setValue(configValue(config, AiConfigKeys.AI_TUTOR_PROVIDER));
        providerCombo.setWidthFull();

        // Google provider fields
        final var googleApiKeyField =
                this.createReadOnlyApiKeyField("app.google.api.key", "https://aistudio.google.com/app/apikey");
        final var googleModelField = this.createTextConfigField("Model", configValue(config, AiConfigKeys.GOOGLE_MODEL),
                "Google AI model name (e.g., gemini-2.5-flash-lite, gemma-4-31b-it, gemini-3.1-pro, ...)");
        final var googleUrlField =
                this.createTextConfigField("API Base URL", configValue(config, AiConfigKeys.GOOGLE_API_BASE_URL), null);
        final var googleTempField = this.createTemperatureField(config, AiConfigKeys.GOOGLE_PREFIX);
        final var googleMaxTokensField = this.createMaxTokensField(config, AiConfigKeys.GOOGLE_PREFIX);
        final var googleSection = new VerticalLayout(googleApiKeyField, googleModelField, googleUrlField,
                googleTempField, googleMaxTokensField);
        googleSection.setSpacing(true);
        googleSection.setPadding(false);

        // OpenAI provider fields
        final var openaiApiKeyField =
                this.createReadOnlyApiKeyField("app.openai.api.key", "https://platform.openai.com/api-keys");
        final var openaiOrgIdField = this.createTextConfigField("Organization ID (Optional)",
                configValue(config, AiConfigKeys.OPENAI_ORGANIZATION_ID), null);
        final var openaiModelField = this.createTextConfigField("Model", configValue(config, AiConfigKeys.OPENAI_MODEL),
                "OpenAI model name (e.g., gpt-5-nano, gpt-5.4-mini, gpt-5.5, ...)");
        final var openaiUrlField =
                this.createTextConfigField("API Base URL", configValue(config, AiConfigKeys.OPENAI_API_BASE_URL), null);
        final var openaiTempField = this.createTemperatureField(config, AiConfigKeys.OPENAI_PREFIX);
        final var openaiMaxTokensField = this.createMaxTokensField(config, AiConfigKeys.OPENAI_PREFIX);
        final var openaiSection = new VerticalLayout(openaiApiKeyField, openaiOrgIdField, openaiModelField,
                openaiUrlField, openaiTempField, openaiMaxTokensField);
        openaiSection.setSpacing(true);
        openaiSection.setPadding(false);

        // Ollama provider fields
        final var ollamaApiUrlField = this.createTextConfigField("API URL",
                configValue(config, AiConfigKeys.OLLAMA_API_URL), "Ollama API URL (e.g., http://localhost:11434)");
        final var ollamaModelField = this.createTextConfigField("Model", configValue(config, AiConfigKeys.OLLAMA_MODEL),
                "Ollama model name (e.g., gemma3:1b, llama3.2:3b, qwen3:8b, phi4:14b, ...)");
        final var ollamaTempField = this.createTemperatureField(config, AiConfigKeys.OLLAMA_PREFIX);
        final var ollamaMaxTokensField = this.createMaxTokensField(config, AiConfigKeys.OLLAMA_PREFIX);
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
        providerCombo.addValueChangeListener(ignored -> updateEnabledState.run());
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

        saveBtn.addClickListener(ignored -> this.onProviderSaveOrTest(false, saveBtn, testBtn, resetBtn, buildUpdates,
                providerCombo, updateEnabledState));
        testBtn.addClickListener(ignored -> this.onProviderSaveOrTest(true, saveBtn, testBtn, resetBtn, buildUpdates,
                providerCombo, updateEnabledState));
        resetBtn.addClickListener(ignored -> this.onProviderReset(saveBtn, testBtn, resetBtn, updateEnabledState));

        panel.add(enabledCheckbox, providerCombo, googleSection, openaiSection, ollamaSection, buttonRow);
        return panel;
    }

    private VerticalLayout buildPromptsPanel(final Map<String, String> config) {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        final var qaPrefix = this.createPromptArea("Question Answering Prefix",
                configValue(config, AiConfigKeys.PROMPT_QUESTION_PREFIX), "Prefix for question answering prompts");
        final var qaPostfix = this.createPromptArea("Question Answering Postfix",
                configValue(config, AiConfigKeys.PROMPT_QUESTION_POSTFIX), "Postfix for question answering prompts");
        final var mtPrefix = this.createPromptArea("Math Tutoring Prefix",
                configValue(config, AiConfigKeys.PROMPT_TUTORING_PREFIX), "Prefix for math tutoring prompts");
        final var mtPostfix = this.createPromptArea("Math Tutoring Postfix",
                configValue(config, AiConfigKeys.PROMPT_TUTORING_POSTFIX), "Postfix for math tutoring prompts");

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
        field.setValue("•••••••••••••••••");
        field.setWidthFull();
        field.setReadOnly(true);
        field.setHelperText("API key is managed via the " + envVarName + " property. Get key from: " + docsUrl);
        return field;
    }

    private TextField createTextConfigField(final String label, final String value, @Nullable final String helperText) {
        final var field = new TextField(label);
        field.setValue(value);
        field.setWidthFull();
        if (helperText != null) {
            field.setHelperText(helperText);
        }
        return field;
    }

    private NumberField createTemperatureField(final Map<String, String> config, final String configPrefix) {
        final var field = new NumberField("Temperature");
        field.setValue(Double.parseDouble(configValue(config, configPrefix + AiConfigKeys.SUFFIX_TEMPERATURE)));
        field.setMin(0.0);
        field.setMax(2.0);
        field.setStep(0.1);
        field.setHelperText(TEMPERATURE_HELPER);
        return field;
    }

    private NumberField createMaxTokensField(final Map<String, String> config, final String configPrefix) {
        final var field = new NumberField("Max Tokens");
        field.setValue(Double.parseDouble(configValue(config, configPrefix + AiConfigKeys.SUFFIX_MAX_TOKENS)));
        field.setMin(1);
        field.setMax(8192);
        field.setStep(1);
        field.setHelperText(MAX_TOKENS_HELPER);
        return field;
    }

    private TextArea createPromptArea(final String label, final String value, final String helperText) {
        final var area = new TextArea(label);
        area.setValue(value);
        area.setWidthFull();
        area.setMinRows(18);
        area.setHelperText(helperText);
        return area;
    }

    // --- Provider button handlers -------------------------------------------

    private void onProviderSaveOrTest(final boolean withTest, final Button saveBtn, final Button testBtn,
            final Button resetBtn, final Supplier<Optional<List<AiConfigUpdateDto>>> buildUpdates,
            final ComboBox<String> providerCombo, final Runnable updateEnabledState) {
        // Captured here on the UI thread: the supplier runs off it, and the session may end meanwhile.
        final var credentials = this.authService.currentSessionCredentials();
        final var updatesOpt = buildUpdates.get();
        if (updatesOpt.isEmpty()) {
            return;
        }
        final var updates = updatesOpt.get();
        final String provider = withTest ? providerCombo.getValue() : "";
        saveBtn.setEnabled(false);
        testBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        // updateEnabledState re-enables the save and test buttons
        final Runnable reenable = () -> {
            resetBtn.setEnabled(true);
            updateEnabledState.run();
        };
        AsyncDataLoader.load(() -> {
            this.aiConfigService.updateMultipleConfigs(updates, credentials);
            return Boolean.TRUE;
        }, this, ignored -> {
            // With a test, the buttons stay disabled until it completes.
            if ("google".equals(provider)) {
                this.testConnection(() -> this.aiProviderTestService.testGoogle(credentials), "Google", reenable);
            } else if ("openai".equals(provider)) {
                this.testConnection(() -> this.aiProviderTestService.testOpenAi(credentials), "OpenAI", reenable);
            } else if ("ollama".equals(provider)) {
                this.testConnection(() -> this.aiProviderTestService.testOllama(credentials), "Ollama", reenable);
            } else {
                reenable.run();
                if (!withTest) {
                    NotificationUtil.showSuccess("Provider configuration updated successfully");
                }
            }
        }, reenable, "Error saving configuration. Please try again later.");
    }

    private void onProviderReset(final Button saveBtn, final Button testBtn, final Button resetBtn,
            final Runnable updateEnabledState) {
        // Captured here on the UI thread: the supplier runs off it, and the session may end meanwhile.
        final var credentials = this.authService.currentSessionCredentials();
        saveBtn.setEnabled(false);
        testBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        AsyncDataLoader.load(() -> {
            this.aiConfigService.resetToDefaults(credentials);
            return Boolean.TRUE;
        }, this, ignored -> {
            NotificationUtil.showSuccess("All settings reset to defaults");
            this.showLoading();
            this.loadConfig();
        }, () -> {
            resetBtn.setEnabled(true);
            updateEnabledState.run();
        }, "Error resetting defaults. Please try again later.");
    }

    // --- Connection tests ---------------------------------------------------

    private void testConnection(final Supplier<ProviderTestResultDto> testCall, final String providerName,
            final Runnable onDone) {
        AsyncDataLoader.load(testCall, this, result -> {
            onDone.run();
            final String msg = result.message != null ? result.message : "No response";
            if (result.success) {
                NotificationUtil.showSuccess(msg);
            } else {
                NotificationUtil.showError(msg);
            }
            LOG.infof("%s connection test: %s", providerName, result.message);
        }, onDone, "Connection test failed. Please try again later.");
    }

    // --- Save helpers -------------------------------------------------------

    private void onPromptsSave(final Button saveBtn, final TextArea questionPrefixArea,
            final TextArea questionPostfixArea, final TextArea tutoringPrefixArea, final TextArea tutoringPostfixArea) {
        // Captured here on the UI thread: the supplier runs off it, and the session may end meanwhile.
        final var credentials = this.authService.currentSessionCredentials();
        final var updates =
                List.of(new AiConfigUpdateDto(AiConfigKeys.PROMPT_QUESTION_PREFIX, questionPrefixArea.getValue()),
                        new AiConfigUpdateDto(AiConfigKeys.PROMPT_QUESTION_POSTFIX, questionPostfixArea.getValue()),
                        new AiConfigUpdateDto(AiConfigKeys.PROMPT_TUTORING_PREFIX, tutoringPrefixArea.getValue()),
                        new AiConfigUpdateDto(AiConfigKeys.PROMPT_TUTORING_POSTFIX, tutoringPostfixArea.getValue()));
        saveBtn.setEnabled(false);
        AsyncDataLoader.load(() -> {
            this.aiConfigService.updateMultipleConfigs(updates, credentials);
            return Boolean.TRUE;
        }, this, ignored -> {
            saveBtn.setEnabled(true);
            NotificationUtil.showSuccess("Prompts configuration updated successfully");
        }, () -> saveBtn.setEnabled(true), "Error saving configuration. Please try again later.");
    }

    /** The snapshot holds every key (see {@code AiConfigService.getEffectiveConfig()}); a missing one is a bug. */
    private static String configValue(final Map<String, String> config, final String key) {
        return Objects.requireNonNull(config.get(key), key);
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
