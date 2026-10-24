package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;

/**
 * 基础 API 请求示例
 *
 * 演示如何发送简单的聊天完成请求到 Tokenra API。
 */
public class BasicRequest {

    // API 配置
    private static final String API_KEY = System.getenv().getOrDefault(
        "API_KEY",
        "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG"
    );
    private static final String API_BASE_URL = "https://tokenra.io/v1";
    private static final String ENDPOINT = API_BASE_URL + "/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;

    public BasicRequest() {
        this.client = new OkHttpClient();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * 发送基础的聊天完成请求
     */
    public JsonObject sendBasicRequest() throws IOException {
        // 构造请求体
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 1024);
        payload.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "what model");
        messages.add(message);
        payload.add("messages", messages);

        System.out.println("📤 发送请求...");
        System.out.println("端点: " + ENDPOINT);
        System.out.println("模型: " + payload.get("model").getAsString());
        System.out.println("消息: " + message.get("content").getAsString());
        System.out.println("-".repeat(50));

        // 构造请求
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 检查响应状态
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("HTTP " + response.code() + ": " + errorBody);
            }

            // 解析响应
            String responseBody = response.body().string();
            JsonObject result = gson.fromJson(responseBody, JsonObject.class);

            // 打印响应信息
            System.out.println("✅ 请求成功!");
            System.out.println("响应 ID: " + result.get("id").getAsString());
            System.out.println("模型: " + result.get("model").getAsString());
            System.out.println("-".repeat(50));

            // 提取助手回复
            if (result.has("choices")) {
                JsonArray choices = result.getAsJsonArray("choices");
                if (choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    String assistantMessage = choice.getAsJsonObject("message")
                            .get("content").getAsString();
                    System.out.println("🤖 助手回复:\n" + assistantMessage);
                }
            }

            // Token 使用统计
            if (result.has("usage")) {
                JsonObject usage = result.getAsJsonObject("usage");
                System.out.println("-".repeat(50));
                System.out.println("📊 Token 使用:");
                System.out.println("  输入 tokens: " + usage.get("prompt_tokens").getAsInt());
                System.out.println("  输出 tokens: " + usage.get("completion_tokens").getAsInt());
                System.out.println("  总计 tokens: " + usage.get("total_tokens").getAsInt());
            }

            return result;

        } catch (IOException e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
            throw e;
        }
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("基础 API 请求示例");
        System.out.println("=".repeat(50));

        BasicRequest example = new BasicRequest();
        try {
            example.sendBasicRequest();
        } catch (IOException e) {
            System.err.println("发生错误: " + e.getMessage());
            System.exit(1);
        }
    }
}
