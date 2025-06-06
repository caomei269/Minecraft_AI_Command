package com.aicommand.deepseek;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class DeepSeekClient {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String API_BASE_URL = "https://api.deepseek.com";
    private static final String CHAT_ENDPOINT = "/chat/completions";
    
    private final HttpClient httpClient;
    private final Gson gson;
    private String apiKey;
    
    public DeepSeekClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.gson = new Gson();
        this.apiKey = Config.deepSeekApiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public CompletableFuture<String> generateCommand(String userRequest) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return sendChatRequest(userRequest);
            } catch (Exception e) {
                LOGGER.error("Error calling DeepSeek API", e);
                return "Error: Failed to generate command - " + e.getMessage();
            }
        });
    }
    
    private String sendChatRequest(String userRequest) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return "Error: DeepSeek API key not configured";
        }
        
        // 构建请求体 / Build request body
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "deepseek-chat");
        requestBody.addProperty("stream", false);
        requestBody.addProperty("max_tokens", 300);
        requestBody.addProperty("temperature", 0.7);
        
        JsonArray messages = new JsonArray();
        
        // 系统消息 / System message
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", 
        "你是一个智能的Minecraft指令生成器，能够根据当前游戏状态和用户请求生成精确的指令。" +
        "请根据提供的游戏状态信息（玩家位置、物品栏、世界环境等）生成最合适的Minecraft指令。" +
        "只返回指令，不附加任何解释或额外文本。如果需要多个指令，用换行符分隔。" +
        "你要发挥想象力，尽最大努力尝试达成目标" +
        "请注意用户提问时的语言，中文请求用中文回复，英文请求用英文回复。");
        messages.add(systemMessage);
        
        // 游戏数据上下文消息 / Game data context message
        String gameContext = GameDataCollector.generateGameContext();
        JsonObject contextMessage = new JsonObject();
        contextMessage.addProperty("role", "system");
        contextMessage.addProperty("content", gameContext);
        messages.add(contextMessage);
        
        // 用户消息 / User message
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userRequest);
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        
        // 创建HTTP请求 / Create HTTP request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + CHAT_ENDPOINT))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .timeout(Duration.ofSeconds(60))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();
        
        // 发送请求 / Send request
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            LOGGER.error("DeepSeek API error: {} - {}", response.statusCode(), response.body());
            return "Error: API request failed with status " + response.statusCode();
        }
        
        // 解析响应 / Parse response
        try {
            JsonObject responseJson = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray choices = responseJson.getAsJsonArray("choices");
            if (choices.size() > 0) {
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject message = firstChoice.getAsJsonObject("message");
                return message.get("content").getAsString().trim();
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing DeepSeek response", e);
            return "Error: Failed to parse API response";
        }
        
        return "Error: No response from API";
    }
}