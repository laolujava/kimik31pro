package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * 流式输出示例
 *
 * 演示如何使用 Server-Sent Events (SSE) 接收流式响应。
 */
public class StreamingRequest {

    private static final String API_KEY = System.getenv().getOrDefault(
        "API_KEY",
        "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG"
    );
    private static final String API_BASE_URL = "https://tokenra.io/v1";
    private static final String ENDPOINT = API_BASE_URL + "/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;

    public StreamingRequest() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * 发送流式请求并实时打印输出
     */
    public String sendStreamingRequest() throws IOException {
        // 构造请求体
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 1024);
        payload.addProperty("stream", true);  // 启用流式输出

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "请用三句话介绍人工智能");
        messages.add(message);
        payload.add("messages", messages);

        System.out.println("📤 发送流式请求...");
        System.out.println("模型: " + payload.get("model").getAsString());
        System.out.println("消息: " + message.get("content").getAsString());
        System.out.println("-".repeat(50));
        System.out.println("🤖 助手回复 (流式输出):\n");

        // 构造请求
        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }

            StringBuilder fullContent = new StringBuilder();

            // 读取流式响应
            InputStream inputStream = response.body().byteStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // SSE 格式: "data: {...}"
                if (line.startsWith("data: ")) {
                    String dataStr = line.substring(6);

                    // 检查结束标记
                    if (dataStr.trim().equals("[DONE]")) {
                        System.out.println("\n");
                        System.out.println("-".repeat(50));
                        System.out.println("✅ 流式输出完成");
                        break;
                    }

                    try {
                        // 解析 JSON 数据
                        JsonObject chunk = gson.fromJson(dataStr, JsonObject.class);

                        // 提取内容增量
                        if (chunk.has("choices")) {
                            JsonArray choices = chunk.getAsJsonArray("choices");
                            if (choices.size() > 0) {
                                JsonObject choice = choices.get(0).getAsJsonObject();
                                if (choice.has("delta")) {
                                    JsonObject delta = choice.getAsJsonObject("delta");
                                    if (delta.has("content")) {
                                        String content = delta.get("content").getAsString();
                                        System.out.print(content);
                                        fullContent.append(content);
                                    }
                                }
                            }
                        }

                    } catch (Exception e) {
                        // 跳过无效的 JSON
                    }
                }
            }

            System.out.println("\n📝 完整内容长度: " + fullContent.length() + " 字符");
            return fullContent.toString();

        } catch (IOException e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 带上下文的流式对话示例
     */
    public void sendStreamingWithContext() throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 200);
        payload.addProperty("stream", true);
        payload.addProperty("temperature", 0.7);

        JsonArray messages = new JsonArray();

        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "你是一个简洁的助手，每次回复不超过 50 字。");
        messages.add(sysMsg);

        JsonObject userMsg1 = new JsonObject();
        userMsg1.addProperty("role", "user");
        userMsg1.addProperty("content", "什么是机器学习？");
        messages.add(userMsg1);

        JsonObject assistantMsg = new JsonObject();
        assistantMsg.addProperty("role", "assistant");
        assistantMsg.addProperty("content", "机器学习是让计算机通过数据学习规律，无需显式编程即可完成任务的技术。");
        messages.add(assistantMsg);

        JsonObject userMsg2 = new JsonObject();
        userMsg2.addProperty("role", "user");
        userMsg2.addProperty("content", "它有哪些应用？");
        messages.add(userMsg2);

        payload.add("messages", messages);

        System.out.println("\n" + "=".repeat(50));
        System.out.println("📝 带上下文的流式对话");
        System.out.println("=".repeat(50));
        System.out.println("🤖 助手回复:\n");

        RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }

            InputStream inputStream = response.body().byteStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String dataStr = line.substring(6);

                    if (dataStr.trim().equals("[DONE]")) {
                        System.out.println("\n");
                        break;
                    }

                    try {
                        JsonObject chunk = gson.fromJson(dataStr, JsonObject.class);
                        if (chunk.has("choices")) {
                            JsonArray choices = chunk.getAsJsonArray("choices");
                            if (choices.size() > 0) {
                                JsonObject delta = choices.get(0).getAsJsonObject()
                                        .getAsJsonObject("delta");
                                if (delta != null && delta.has("content")) {
                                    System.out.print(delta.get("content").getAsString());
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 跳过无效 JSON
                    }
                }
            }

            System.out.println("✅ 对话完成");

        }
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("流式输出示例");
        System.out.println("=".repeat(50));

        StreamingRequest example = new StreamingRequest();

        try {
            // 示例 1: 基础流式请求
            example.sendStreamingRequest();

            // 示例 2: 带上下文的流式对话
            example.sendStreamingWithContext();

        } catch (IOException e) {
            System.err.println("发生错误: " + e.getMessage());
            System.exit(1);
        }
    }
}
