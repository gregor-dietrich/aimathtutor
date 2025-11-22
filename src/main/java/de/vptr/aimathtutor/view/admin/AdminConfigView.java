package de.vptr.aimathtutor.view.admin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.vptr.aimathtutor.dto.AiConfigUpdateDto;
import de.vptr.aimathtutor.service.AiConfigService;
import de.vptr.aimathtutor.service.AuthService;
import de.vptr.aimathtutor.service.UserRankService;
import de.vptr.aimathtutor.util.NotificationUtil;
import de.vptr.aimathtutor.view.LoginView;
import jakarta.inject.Inject;

/**
 * Admin view for managing AI tutor configuration at runtime.
 * Allows admins to change AI provider, model, temperature, prompts, and other
 * settings
 * without restarting the application.
 */
@Route(value = "admin/config", layout = AdminMainLayout.class)
@PageTitle("AI Configuration - AI Math Tutor")
public class AdminConfigView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger LOG = LoggerFactory.getLogger(AdminConfigView.class);

    @Inject
    private transient AuthService authService;

    @Inject
    private transient AiConfigService aiConfigService;

    @Inject
    private transient UserRankService userRankService;

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
        if (!this.authService.isAuthenticated()) {
            event.forwardTo(LoginView.class);
            return;
        }

        // Configuration view requires exercise or lesson management permissions
        final var userRank = this.userRankService.getCurrentUserRank();
        if (userRank == null) {
            NotificationUtil.showError("You do not have permission to access this page");
            event.forwardTo("/");
            return;
        }

        final var hasPermission = userRank.hasAnyExercisePermission() || userRank.hasAnyLessonPermission();
        if (!hasPermission) {
            NotificationUtil.showError("You do not have permission to access this page");
            event.forwardTo("/");
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

        // OpenAI tab
        final var openaiTab = new Tab("OpenAI");
        final var openaiPanel = this.buildOpenAiPanel();

        // Ollama tab
        final var ollamaTab = new Tab("Ollama");
        final var ollamaPanel = this.buildOllamaPanel();

        // Prompts tab
        final var promptsTab = new Tab("Prompts");
        final var promptsPanel = this.buildPromptsPanel();

        tabs.add(generalTab, geminiTab, openaiTab, ollamaTab, promptsTab);

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
        final var enabledCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Enable AI Tutor");
        final String enabledValue = this.aiConfigService.getConfigValue("ai.tutor.enabled", "true");
        enabledCheckbox.setValue("true".equalsIgnoreCase(enabledValue));
        enabledCheckbox.setLabel("Enable AI Tutor");

        // AI Provider selection
        final var providerCombo = new com.vaadin.flow.component.combobox.ComboBox<String>("AI Provider");
        providerCombo.setItems("mock", "gemini", "openai", "ollama");
        providerCombo.setValue(this.aiConfigService.getConfigValue("ai.tutor.provider", "mock"));
        providerCombo.setWidthFull();

        // Save button
        final var saveBtn = new Button("Save", e -> this.saveGeneralConfig(enabledCheckbox, providerCombo));
        saveBtn.addClickListener(e -> LOG.info("General config save clicked"));

        panel.add(enabledCheckbox, providerCombo, saveBtn);
        return panel;
    }

    private VerticalLayout buildGeminiPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        // API Key field (read-only, informational)
        final var apiKeyField = new PasswordField("API Key");
        apiKeyField.setValue("••••••••");
        apiKeyField.setReadOnly(true);
        apiKeyField.setHelperText(
                "API key is managed via GEMINI_API_KEY environment variable. Get key from: https://aistudio.google.com/app/apikey");

        // Model field
        final var modelField = new TextField("Model");
        modelField.setValue(this.aiConfigService.getConfigValue("gemini.model", "gemini-2.5-flash-lite"));
        modelField.setWidthFull();
        modelField.setHelperText("Gemini model name (e.g., gemini-2.5-flash-lite)");

        // API Base URL
        final var urlField = new TextField("API Base URL");
        urlField.setValue(this.aiConfigService.getConfigValue("gemini.api.base-url",
                "https://generativelanguage.googleapis.com"));
        urlField.setWidthFull();

        // Temperature
        final var tempField = new NumberField("Temperature");
        tempField.setValue(this.aiConfigService.getConfigValueAsDouble("gemini.temperature", 0.7));
        tempField.setMin(0.0);
        tempField.setMax(2.0);
        tempField.setStep(0.1);
        tempField.setHelperText("Temperature (0.0-2.0): Lower = more focused, Higher = more creative");

        // Max Tokens
        final var maxTokensField = new NumberField("Max Tokens");
        maxTokensField.setValue(this.aiConfigService.getConfigValueAsInt("gemini.max-tokens", 1000).doubleValue());
        maxTokensField.setMin(1);
        maxTokensField.setMax(8192);
        maxTokensField.setStep(1);
        maxTokensField.setHelperText("Maximum tokens in response (1-8192)");

        // Save button
        final var saveBtn = new Button("Save",
                e -> this.saveGeminiConfig(modelField, urlField, tempField, maxTokensField));

        panel.add(apiKeyField, modelField, urlField, tempField, maxTokensField, saveBtn);
        return panel;
    }

    private VerticalLayout buildOpenAiPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        // API Key field (read-only, informational)
        final var apiKeyField = new PasswordField("API Key");
        apiKeyField.setValue("••••••••");
        apiKeyField.setReadOnly(true);
        apiKeyField.setHelperText(
                "API key is managed via OPENAI_API_KEY environment variable. Get key from: https://platform.openai.com/api-keys");

        // Organization ID
        final var orgIdField = new TextField("Organization ID (Optional)");
        orgIdField.setValue(this.aiConfigService.getConfigValue("openai.organization-id", ""));
        orgIdField.setWidthFull();

        // Model field
        final var modelField = new TextField("Model");
        modelField.setValue(this.aiConfigService.getConfigValue("openai.model", "gpt-4o-mini"));
        modelField.setWidthFull();
        modelField.setHelperText("OpenAI model name (e.g., gpt-4o-mini)");

        // API Base URL
        final var urlField = new TextField("API Base URL");
        urlField.setValue(this.aiConfigService.getConfigValue("openai.api.base-url", "https://api.openai.com/v1"));
        urlField.setWidthFull();

        // Temperature
        final var tempField = new NumberField("Temperature");
        tempField.setValue(this.aiConfigService.getConfigValueAsDouble("openai.temperature", 0.7));
        tempField.setMin(0.0);
        tempField.setMax(2.0);
        tempField.setStep(0.1);
        tempField.setHelperText("Temperature (0.0-2.0): Lower = more focused, Higher = more creative");

        // Max Tokens
        final var maxTokensField = new NumberField("Max Tokens");
        maxTokensField.setValue(this.aiConfigService.getConfigValueAsInt("openai.max-tokens", 1000).doubleValue());
        maxTokensField.setMin(1);
        maxTokensField.setMax(8192);
        maxTokensField.setStep(1);
        maxTokensField.setHelperText("Maximum tokens in response (1-8192)");

        // Save button
        final var saveBtn = new Button("Save",
                e -> this.saveOpenAiConfig(orgIdField, modelField, urlField, tempField, maxTokensField));

        panel.add(apiKeyField, orgIdField, modelField, urlField, tempField, maxTokensField, saveBtn);
        return panel;
    }

    private VerticalLayout buildOllamaPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        // API URL
        final var apiUrlField = new TextField("API URL");
        apiUrlField.setValue(this.aiConfigService.getConfigValue("ollama.api.url", "http://localhost:11434"));
        apiUrlField.setWidthFull();
        apiUrlField.setHelperText("Ollama API URL (e.g., http://localhost:11434)");

        // Model field
        final var modelField = new TextField("Model");
        modelField.setValue(this.aiConfigService.getConfigValue("ollama.model", "llama3.1:8b"));
        modelField.setWidthFull();
        modelField.setHelperText("Ollama model name (e.g., llama3.1:8b)");

        // Temperature
        final var tempField = new NumberField("Temperature");
        tempField.setValue(this.aiConfigService.getConfigValueAsDouble("ollama.temperature", 0.7));
        tempField.setMin(0.0);
        tempField.setMax(2.0);
        tempField.setStep(0.1);
        tempField.setHelperText("Temperature (0.0-2.0): Lower = more focused, Higher = more creative");

        // Max Tokens
        final var maxTokensField = new NumberField("Max Tokens");
        maxTokensField.setValue(this.aiConfigService.getConfigValueAsInt("ollama.max-tokens", 1000).doubleValue());
        maxTokensField.setMin(1);
        maxTokensField.setMax(8192);
        maxTokensField.setStep(1);
        maxTokensField.setHelperText("Maximum tokens in response (1-8192)");

        // Timeout
        final var timeoutField = new NumberField("Timeout (seconds)");
        timeoutField.setValue(this.aiConfigService.getConfigValueAsInt("ollama.timeout-seconds", 30).doubleValue());
        timeoutField.setMin(1);
        timeoutField.setMax(300);
        timeoutField.setStep(1);
        timeoutField.setHelperText("API timeout in seconds (1-300)");

        // Save button
        final var saveBtn = new Button("Save",
                e -> this.saveOllamaConfig(apiUrlField, modelField, tempField, maxTokensField, timeoutField));

        panel.add(apiUrlField, modelField, tempField, maxTokensField, timeoutField, saveBtn);
        return panel;
    }

    private VerticalLayout buildPromptsPanel() {
        final var panel = new VerticalLayout();
        panel.setSpacing(true);
        panel.setPadding(true);

        // Question Answering Prefix
        final var qaPrefix = new TextArea("Question Answering Prefix");
        qaPrefix.setValue(this.aiConfigService.getConfigValue("ai.prompt.question.answering.prefix", ""));
        qaPrefix.setWidthFull();
        qaPrefix.setMinHeight("150px");
        qaPrefix.setHelperText("Prefix for question answering prompts");

        // Question Answering Postfix
        final var qaPostfix = new TextArea("Question Answering Postfix");
        qaPostfix.setValue(this.aiConfigService.getConfigValue("ai.prompt.question.answering.postfix", ""));
        qaPostfix.setWidthFull();
        qaPostfix.setMinHeight("150px");
        qaPostfix.setHelperText("Postfix for question answering prompts");

        // Math Tutoring Prefix
        final var mtPrefix = new TextArea("Math Tutoring Prefix");
        mtPrefix.setValue(this.aiConfigService.getConfigValue("ai.prompt.math.tutoring.prefix", ""));
        mtPrefix.setWidthFull();
        mtPrefix.setMinHeight("150px");
        mtPrefix.setHelperText("Prefix for math tutoring prompts");

        // Math Tutoring Postfix
        final var mtPostfix = new TextArea("Math Tutoring Postfix");
        mtPostfix.setValue(this.aiConfigService.getConfigValue("ai.prompt.math.tutoring.postfix", ""));
        mtPostfix.setWidthFull();
        mtPostfix.setMinHeight("150px");
        mtPostfix.setHelperText("Postfix for math tutoring prompts");

        // Save button
        final var saveBtn = new Button("Save",
                e -> this.savePromptsConfig(qaPrefix, qaPostfix, mtPrefix, mtPostfix));

        panel.add(qaPrefix, qaPostfix, mtPrefix, mtPostfix, saveBtn);
        return panel;
    }

    private void saveGeneralConfig(final com.vaadin.flow.component.checkbox.Checkbox enabledCheckbox,
            final com.vaadin.flow.component.combobox.ComboBox<String> providerCombo) {
        try {
            final var updates = List.of(
                    new AiConfigUpdateDto("ai.tutor.enabled", enabledCheckbox.getValue() ? "true" : "false"),
                    new AiConfigUpdateDto("ai.tutor.provider", providerCombo.getValue()));

            final Long userId = this.authService.getUserId();
            this.aiConfigService.updateMultipleConfigs(updates, userId);

            NotificationUtil.showSuccess("AI configuration updated successfully");
            LOG.info("General config saved");
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.error("Validation error saving config", e);
        } catch (final Exception e) {
            NotificationUtil.showError("Error saving configuration: " + e.getMessage());
            LOG.error("Error saving general config", e);
        }
    }

    private void saveGeminiConfig(final TextField modelField, final TextField urlField,
            final NumberField tempField, final NumberField maxTokensField) {
        try {
            final var tempValue = tempField.getValue();
            final var maxTokensValue = maxTokensField.getValue();

            final var updates = List.of(
                    new AiConfigUpdateDto("gemini.model", modelField.getValue()),
                    new AiConfigUpdateDto("gemini.api.base-url", urlField.getValue()),
                    new AiConfigUpdateDto("gemini.temperature", tempValue != null ? tempValue.toString() : "0.7"),
                    new AiConfigUpdateDto("gemini.max-tokens",
                            maxTokensValue != null ? maxTokensValue.intValue() + "" : "1000"));

            final Long userId = this.authService.getUserId();
            this.aiConfigService.updateMultipleConfigs(updates, userId);

            NotificationUtil.showSuccess("Gemini configuration updated successfully");
            LOG.info("Gemini config saved");
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.error("Validation error saving Gemini config", e);
        } catch (final Exception e) {
            NotificationUtil.showError("Error saving configuration: " + e.getMessage());
            LOG.error("Error saving Gemini config", e);
        }
    }

    private void saveOpenAiConfig(final TextField orgIdField, final TextField modelField, final TextField urlField,
            final NumberField tempField, final NumberField maxTokensField) {
        try {
            final var tempValue = tempField.getValue();
            final var maxTokensValue = maxTokensField.getValue();

            final var updates = List.of(
                    new AiConfigUpdateDto("openai.organization-id", orgIdField.getValue()),
                    new AiConfigUpdateDto("openai.model", modelField.getValue()),
                    new AiConfigUpdateDto("openai.api.base-url", urlField.getValue()),
                    new AiConfigUpdateDto("openai.temperature", tempValue != null ? tempValue.toString() : "0.7"),
                    new AiConfigUpdateDto("openai.max-tokens",
                            maxTokensValue != null ? maxTokensValue.intValue() + "" : "1000"));

            final Long userId = this.authService.getUserId();
            this.aiConfigService.updateMultipleConfigs(updates, userId);

            NotificationUtil.showSuccess("OpenAI configuration updated successfully");
            LOG.info("OpenAI config saved");
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.error("Validation error saving OpenAI config", e);
        } catch (final Exception e) {
            NotificationUtil.showError("Error saving configuration: " + e.getMessage());
            LOG.error("Error saving OpenAI config", e);
        }
    }

    private void saveOllamaConfig(final TextField apiUrlField, final TextField modelField,
            final NumberField tempField, final NumberField maxTokensField, final NumberField timeoutField) {
        try {
            final var tempValue = tempField.getValue();
            final var maxTokensValue = maxTokensField.getValue();
            final var timeoutValue = timeoutField.getValue();

            final var updates = List.of(
                    new AiConfigUpdateDto("ollama.api.url", apiUrlField.getValue()),
                    new AiConfigUpdateDto("ollama.model", modelField.getValue()),
                    new AiConfigUpdateDto("ollama.temperature", tempValue != null ? tempValue.toString() : "0.7"),
                    new AiConfigUpdateDto("ollama.max-tokens",
                            maxTokensValue != null ? maxTokensValue.intValue() + "" : "1000"),
                    new AiConfigUpdateDto("ollama.timeout-seconds",
                            timeoutValue != null ? timeoutValue.intValue() + "" : "30"));

            final Long userId = this.authService.getUserId();
            this.aiConfigService.updateMultipleConfigs(updates, userId);

            NotificationUtil.showSuccess("Ollama configuration updated successfully");
            LOG.info("Ollama config saved");
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.error("Validation error saving Ollama config", e);
        } catch (final Exception e) {
            NotificationUtil.showError("Error saving configuration: " + e.getMessage());
            LOG.error("Error saving Ollama config", e);
        }
    }

    private void savePromptsConfig(final TextArea questionPrefixArea, final TextArea questionPostfixArea,
            final TextArea tutoringPrefixArea, final TextArea tutoringPostfixArea) {
        try {
            final var updates = List.of(
                    new AiConfigUpdateDto("ai.prompt.question.answering.prefix",
                            questionPrefixArea.getValue()),
                    new AiConfigUpdateDto("ai.prompt.question.answering.postfix",
                            questionPostfixArea.getValue()),
                    new AiConfigUpdateDto("ai.prompt.math.tutoring.prefix", tutoringPrefixArea.getValue()),
                    new AiConfigUpdateDto("ai.prompt.math.tutoring.postfix", tutoringPostfixArea.getValue()));

            final Long userId = this.authService.getUserId();
            this.aiConfigService.updateMultipleConfigs(updates, userId);

            NotificationUtil.showSuccess("Prompts updated successfully");
            LOG.info("Prompts saved");
        } catch (final IllegalArgumentException e) {
            NotificationUtil.showError("Validation error: " + e.getMessage());
            LOG.error("Validation error saving prompts", e);
        } catch (final Exception e) {
            NotificationUtil.showError("Error saving configuration: " + e.getMessage());
            LOG.error("Error saving prompts", e);
        }
    }
}
