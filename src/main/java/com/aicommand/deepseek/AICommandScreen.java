package com.aicommand.deepseek;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AICommandScreen extends Screen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int PADDING = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int EDITBOX_HEIGHT = 20;
    
    private EditBox inputBox;
    private EditBox apiKeyBox;
    private Button generateButton;
    private Button executeButton;
    private Button configButton;
    private CycleButton<Config.AIProvider> providerButton;
    private List<String> generatedCommands;
    private List<String> outputLines;
    private boolean showConfig = false;
    private DeepSeekClient deepSeekClient;
    private OpenAIClient openAIClient;
    private Config.AIProvider currentProvider;
    private String savedInputText = ""; // 保存用户输入的文本 / Save user input text
    private boolean hasGeneratedCommands = false; // 是否已生成命令 / Whether commands have been generated
    
    public AICommandScreen() {
        super(Component.translatable("screen.aicommand.title"));
        this.generatedCommands = new ArrayList<>();
        this.outputLines = new ArrayList<>();
        this.deepSeekClient = new DeepSeekClient();
        this.openAIClient = new OpenAIClient();
        this.currentProvider = Config.aiProvider;
    }
    
    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        // 在窗口大小改变前保存当前输入框的内容 / Save current input box content before window resize
        if (this.inputBox != null && !this.inputBox.getValue().trim().isEmpty()) {
            String currentInput = this.inputBox.getValue().trim();
            // 只有当输入不是默认示例文本时才保存 / Only save if input is not default example text
            if (!currentInput.equals(Component.translatable("gui.aicommand.example_input").getString())) {
                this.savedInputText = currentInput;
            }
        }
        super.resize(minecraft, width, height);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 40;
        
        // 配置按钮 / Config button
        this.configButton = Button.builder(
            Component.translatable(showConfig ? "gui.aicommand.hide_config" : "gui.aicommand.show_config"),
            button -> {
                // 在重新初始化前保存当前输入框的内容 / Save current input box content before re-initialization
                if (this.inputBox != null && !this.inputBox.getValue().trim().isEmpty()) {
                    String currentInput = this.inputBox.getValue().trim();
                    // 只有当输入不是默认示例文本时才保存 / Only save if input is not default example text
                    if (!currentInput.equals(Component.translatable("gui.aicommand.example_input").getString())) {
                        this.savedInputText = currentInput;
                    }
                }
                showConfig = !showConfig;
                button.setMessage(Component.translatable(showConfig ? "gui.aicommand.hide_config" : "gui.aicommand.show_config"));
                this.clearWidgets();
                this.init();
            }
        ).bounds(this.width - 100, 10, 90, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.configButton);
        
        if (showConfig) {
            // AI服务提供商选择器 / AI service provider selector
            this.providerButton = CycleButton.<Config.AIProvider>builder(provider -> {
                switch (provider) {
                    case DEEPSEEK:
                        return Component.literal("DeepSeek");
                    case OPENAI:
                        return Component.literal("OpenAI");
                    default:
                        return Component.literal("Unknown");
                }
            })
            .withValues(Config.AIProvider.DEEPSEEK, Config.AIProvider.OPENAI)
            .withInitialValue(this.currentProvider)
            .create(PADDING, startY, 210, BUTTON_HEIGHT, Component.translatable("gui.aicommand.ai_provider"), (button, provider) -> {
                this.currentProvider = provider;
                // 保存AI服务提供商选择到配置文件 / Save AI service provider selection to config file
                Config.aiProvider = provider;
                try {
                    java.lang.reflect.Field field = Config.class.getDeclaredField("AI_PROVIDER");
                    field.setAccessible(true);
                    net.minecraftforge.common.ForgeConfigSpec.EnumValue<Config.AIProvider> configValue = 
                        (net.minecraftforge.common.ForgeConfigSpec.EnumValue<Config.AIProvider>) field.get(null);
                    configValue.set(provider);
                } catch (Exception e) {
                    LOGGER.error("Failed to save AI provider to config", e);
                }
                // 更新API密钥输入框的值 / Update API key input box value
                if (this.apiKeyBox != null) {
                    String currentKey = provider == Config.AIProvider.DEEPSEEK ? Config.deepSeekApiKey : Config.openAIApiKey;
                    this.apiKeyBox.setValue(currentKey != null ? currentKey : "");
                }
            });
            this.addRenderableWidget(this.providerButton);
            
            startY += 30;
            
            // API Key输入框 / API Key input box
            this.addRenderableWidget(Button.builder(
                Component.translatable("gui.aicommand.api_key"),
                button -> {}
            ).bounds(PADDING, startY, 80, BUTTON_HEIGHT).build());
            
            String currentKey = this.currentProvider == Config.AIProvider.DEEPSEEK ? Config.deepSeekApiKey : Config.openAIApiKey;
            this.apiKeyBox = new EditBox(this.font, PADDING + 90, startY, this.width - PADDING * 2 - 90, EDITBOX_HEIGHT, Component.translatable("gui.aicommand.api_key"));
            this.apiKeyBox.setValue(currentKey != null ? currentKey : "");
            this.apiKeyBox.setMaxLength(200);
            this.addRenderableWidget(this.apiKeyBox);
            
            startY += 30;
            
            // 保存API Key按钮 / Save API Key button
            Button saveButton = Button.builder(
                Component.translatable("gui.aicommand.save_api_key"),
                button -> {
                    String newApiKey = this.apiKeyBox.getValue();
                    if (this.currentProvider == Config.AIProvider.DEEPSEEK) {
                        this.deepSeekClient.setApiKey(newApiKey);
                        // 保存到配置文件 / Save to config file / Save to config file
                        Config.deepSeekApiKey = newApiKey;
                        // 更新配置规格值 / Update config spec value / Update config spec value
                        try {
                            java.lang.reflect.Field field = Config.class.getDeclaredField("DEEPSEEK_API_KEY");
                            field.setAccessible(true);
                            net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> configValue = 
                                (net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String>) field.get(null);
                            configValue.set(newApiKey);
                        } catch (Exception e) {
                            LOGGER.error("Failed to save DeepSeek API key to config", e);
                        }
                    } else {
                        this.openAIClient.setApiKey(newApiKey);
                        // 保存到配置文件 / Save to config file / Save to config file
                        Config.openAIApiKey = newApiKey;
                        // 更新配置规格值 / Update config spec value / Update config spec value
                        try {
                            java.lang.reflect.Field field = Config.class.getDeclaredField("OPENAI_API_KEY");
                            field.setAccessible(true);
                            net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String> configValue = 
                                (net.minecraftforge.common.ForgeConfigSpec.ConfigValue<String>) field.get(null);
                            configValue.set(newApiKey);
                        } catch (Exception e) {
                            LOGGER.error("Failed to save OpenAI API key to config", e);
                        }
                    }
                    this.outputLines.add(Component.translatable("gui.aicommand.api_key_updated").getString());
                }
            ).bounds(PADDING, startY, 100, BUTTON_HEIGHT).build();
            this.addRenderableWidget(saveButton);
            
            startY += 40;
        }
        
        // 用户输入框标签 / User input box label
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.aicommand.describe_input"),
            button -> {}
        ).bounds(PADDING, startY, 150, BUTTON_HEIGHT).build());
        
        startY += 25;
        
        // 用户输入框 / User input box
        this.inputBox = new EditBox(this.font, PADDING, startY, this.width - PADDING * 2, EDITBOX_HEIGHT, Component.translatable("gui.aicommand.input"));
        this.inputBox.setMaxLength(500);
        // 恢复之前保存的输入文本，如果没有则使用默认示例 / Restore previously saved input text, or use default example if none
        if (savedInputText.isEmpty()) {
            this.inputBox.setValue(Component.translatable("gui.aicommand.example_input").getString());
        } else {
            this.inputBox.setValue(savedInputText);
        }
        this.addRenderableWidget(this.inputBox);
        
        startY += 30;
        
        // 生成命令按钮 / Generate command button
        this.generateButton = Button.builder(
            Component.translatable("gui.aicommand.generate_command"),
            button -> generateCommand()
        ).bounds(PADDING, startY, 120, BUTTON_HEIGHT).build();
        this.addRenderableWidget(this.generateButton);
        
        // 执行命令按钮 / Execute command button
        this.executeButton = Button.builder(
            Component.translatable("gui.aicommand.execute_commands"),
            button -> executeCommands()
        ).bounds(PADDING + 130, startY, 120, BUTTON_HEIGHT).build();
        // 根据是否有生成的命令来设置按钮状态 / Set button state based on whether commands have been generated
        this.executeButton.active = hasGeneratedCommands && !this.generatedCommands.isEmpty();
        this.addRenderableWidget(this.executeButton);
        
        // 清除按钮 / Clear button
        Button clearButton = Button.builder(
            Component.translatable("gui.aicommand.clear"),
            button -> {
                this.generatedCommands.clear();
                this.outputLines.clear();
                this.hasGeneratedCommands = false;
                this.savedInputText = "";
                this.executeButton.active = false;
                // 重置输入框为默认示例文本 / Reset input box to default example text
                if (this.inputBox != null) {
                    this.inputBox.setValue(Component.translatable("gui.aicommand.example_input").getString());
                }
            }
        ).bounds(PADDING + 260, startY, 60, BUTTON_HEIGHT).build();
        this.addRenderableWidget(clearButton);
        
        // 关闭按钮 / Close button
        Button closeButton = Button.builder(
            Component.translatable("gui.aicommand.close"),
            button -> this.onClose()
        ).bounds(this.width - PADDING - 60, startY, 60, BUTTON_HEIGHT).build();
        this.addRenderableWidget(closeButton);
    }
    
    private void generateCommand() {
        String userInput = this.inputBox.getValue().trim();
        if (userInput.isEmpty()) {
            this.outputLines.add(Component.translatable("gui.aicommand.enter_description").getString());
            return;
        }
        
        // 检查API key是否配置 / Check if API key is configured
        String currentApiKey = this.currentProvider == Config.AIProvider.DEEPSEEK 
            ? Config.deepSeekApiKey 
            : Config.openAIApiKey;
            
        if (currentApiKey == null || currentApiKey.trim().isEmpty()) {
            String providerName = this.currentProvider == Config.AIProvider.DEEPSEEK ? "DeepSeek" : "OpenAI";
            this.outputLines.add("❌ " + Component.translatable("gui.aicommand.api_key_required", providerName).getString());
            this.outputLines.add(Component.translatable("gui.aicommand.please_configure_api_key").getString());
            return;
        }
        
        // 保存用户输入的文本 / Save user input text
        this.savedInputText = userInput;
        
        this.generateButton.active = false;
        this.outputLines.add(Component.translatable("gui.aicommand.generating_for", userInput).getString());
        this.outputLines.add(Component.translatable("gui.aicommand.please_wait").getString());
        
        // 根据选择的AI服务提供商异步调用API / Asynchronously call API based on selected AI service provider
        var clientFuture = this.currentProvider == Config.AIProvider.DEEPSEEK 
            ? this.deepSeekClient.generateCommand(userInput)
            : this.openAIClient.generateCommand(userInput);
            
        clientFuture.thenAccept(result -> {
            Minecraft.getInstance().execute(() -> {
                this.generateButton.active = true;
                
                if (result.startsWith("Error:")) {
                    this.outputLines.add("❌ " + result);
                } else {
                    this.outputLines.add(Component.translatable("gui.aicommand.generated_commands").getString());
                    
                    // 分割多个命令 / Split multiple commands
                    String[] commands = result.split("\n");
                    this.generatedCommands.clear();
                    
                    for (String command : commands) {
                        command = command.trim();
                        if (!command.isEmpty()) {
                            // 确保命令以/开头 / Ensure command starts with /
                            if (!command.startsWith("/")) {
                                command = "/" + command;
                            }
                            this.generatedCommands.add(command);
                            this.outputLines.add("  " + command);
                        }
                    }
                    
                    // 更新命令生成状态 / Update command generation status
                    this.hasGeneratedCommands = !this.generatedCommands.isEmpty();
                    this.executeButton.active = this.hasGeneratedCommands;
                }
            });
        }).exceptionally(throwable -> {
            Minecraft.getInstance().execute(() -> {
                this.generateButton.active = true;
                this.outputLines.add("❌ " + Component.translatable("gui.aicommand.error", throwable.getMessage()).getString());
                LOGGER.error("Error generating command", throwable);
            });
            return null;
        });
    }
    
    private void executeCommands() {
        if (this.generatedCommands.isEmpty()) {
            this.outputLines.add(Component.translatable("gui.aicommand.no_commands").getString());
            return;
        }
        
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            this.outputLines.add("❌ " + Component.translatable("gui.aicommand.no_player").getString());
            return;
        }
        
        this.outputLines.add(Component.translatable("gui.aicommand.executing_commands", this.generatedCommands.size()).getString());
        
        for (String command : this.generatedCommands) {
            try {
                // 执行命令 / Execute command
                minecraft.player.connection.sendUnsignedCommand(command.startsWith("/") ? command.substring(1) : command);
                this.outputLines.add("✅ " + Component.translatable("gui.aicommand.executed", command).getString());
                LOGGER.info("Executed command: {}", command);
            } catch (Exception e) {
                this.outputLines.add("❌ " + Component.translatable("gui.aicommand.failed_execute", command).getString());
                LOGGER.error("Failed to execute command: {}", command, e);
            }
        }
        
        this.executeButton.active = false;
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 渲染背景 / Render background
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        // 渲染标题 / Render title
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        
        // 渲染输出区域 / Render output area
        int outputStartY = showConfig ? 200 : 140;
        int outputHeight = this.height - outputStartY - 20;
        
        // 输出区域背景 / Output area background
        guiGraphics.fill(PADDING, outputStartY, this.width - PADDING, this.height - 20, 0x88000000);
        
        // 渲染输出文本 / Render output text
        int lineHeight = this.font.lineHeight + 2;
        int maxLines = outputHeight / lineHeight;
        int startLine = Math.max(0, this.outputLines.size() - maxLines);
        
        for (int i = startLine; i < this.outputLines.size(); i++) {
            String line = this.outputLines.get(i);
            int y = outputStartY + 5 + (i - startLine) * lineHeight;
            
            // 根据内容选择颜色 / Choose color based on content
            int color = 0xFFFFFF;
            if (line.startsWith("❌")) {
                color = 0xFF5555;
            } else if (line.startsWith("✅")) {
                color = 0x55FF55;
            } else if (line.startsWith("  /")) {
                color = 0xFFFF55;
            }
            
            guiGraphics.drawString(this.font, line, PADDING + 5, y, color);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}