package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 错误处理示例
 *
 * 演示如何正确处理各种 API 错误情况。
 */
public class ErrorHandling {

    private static String API_KEY = System.getenv().getOrDefault(
        "API_KEY",
        "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG"
    );
    private static final String API_BASE_URL = "https://tokenra.io/v1";
    private static final String ENDPOINT = API_BASE_URL + "/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;

    public ErrorHandling() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    /**
     * 自定义异常类
     */
    static class APIException extends Exception {
        private final int statusCode;

        public APIException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    static class AuthenticationException extends APIException {
        public AuthenticationException(String message) {
            super(message, 401);
        }
    }

    static class RateLimitException extends APIException {
        public RateLimitException(String message) {
            super(message, 429);
        }
    }

    static class ServerException extends APIException {
        public ServerException(String message, int statusCode) {
            super(message, statusCode);
        }
    }

    /**
     * 带重试机制的请求发送
     */
    public JsonObject sendRequestWithRetry(JsonObject payload, int maxRetries, int initialDelay)
            throws APIException {

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            System.out.println("📤 尝试 " + (attempt + 1) + "/" + (maxRetries + 1) + "...");

            try {
                RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
                Request request = new Request.Builder()
                        .url(ENDPOINT)
                        .addHeader("Authorization", "Bearer " + API_KEY)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    // 根据状态码处理
                    if (response.isSuccessful()) {
                        System.out.println("✅ 请求成功");
                        return gson.fromJson(response.body().string(), JsonObject.class);
                    }

                    String errorBody = response.body() != null ? response.body().string() : "{}";
                    JsonObject errorData = gson.fromJson(errorBody, JsonObject.class);
                    String errorMessage = "未知错误";

                    if (errorData.has("error") && errorData.getAsJsonObject("error").has("message")) {
                        errorMessage = errorData.getAsJsonObject("error").get("message").getAsString();
                    }

                    if (response.code() == 401) {
                        // 认证错误，不应重试
                        throw new AuthenticationException("认证失败: " + errorMessage);
                    }

                    if (response.code() == 429) {
                        // 速率限制，应该重试
                        System.out.println("⚠️ 速率限制，等待后重试...");
                        if (attempt < maxRetries) {
                            int delay = initialDelay * (int) Math.pow(2, attempt);  // 指数退避
                            Thread.sleep(delay);
                            continue;
                        } else {
                            throw new RateLimitException("超过速率限制");
                        }
                    }

                    if (response.code() >= 500) {
                        // 服务器错误，应该重试
                        System.out.println("⚠️ 服务器错误 (" + response.code() + ")，等待后重试...");
                        if (attempt < maxRetries) {
                            int delay = initialDelay * (int) Math.pow(2, attempt);
                            Thread.sleep(delay);
                            continue;
                        } else {
                            throw new ServerException("服务器错误: " + response.code(), response.code());
                        }
                    }

                    // 其他错误
                    throw new APIException("API 错误 (" + response.code() + "): " + errorMessage, response.code());
                }

            } catch (AuthenticationException | APIException e) {
                // 不可重试的错误
                throw e;
            } catch (IOException e) {
                // 网络错误，可以重试
                System.out.println("⚠️ 网络错误: " + e.getMessage());
                if (attempt < maxRetries) {
                    int delay = initialDelay * (int) Math.pow(2, attempt);
                    System.out.println("等待 " + delay + "ms 后重试...");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new APIException("请求被中断", 0);
                    }
                } else {
                    throw new APIException("请求超时，已达最大重试次数: " + e.getMessage(), 0);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new APIException("请求被中断", 0);
            }
        }

        return null;
    }

    /**
     * 测试：成功的请求
     */
    public void testSuccessfulRequest() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 1: 成功的请求");
        System.out.println("=".repeat(50));

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 50);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Hello");
        messages.add(message);
        payload.add("messages", messages);

        try {
            JsonObject result = sendRequestWithRetry(payload, 3, 1000);
            if (result != null && result.has("choices")) {
                String content = result.getAsJsonArray("choices")
                        .get(0).getAsJsonObject()
                        .getAsJsonObject("message")
                        .get("content").getAsString();
                System.out.println("🤖 回复: " + content);
            }
        } catch (APIException e) {
            System.out.println("❌ 错误: " + e.getMessage());
        }
    }

    /**
     * 测试：无效的 API Key
     */
    public void testInvalidApiKey() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 2: 无效的 API Key");
        System.out.println("=".repeat(50));

        // 临时使用无效的 API Key
        String originalKey = API_KEY;
        API_KEY = "invalid_key_12345";

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 50);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Hello");
        messages.add(message);
        payload.add("messages", messages);

        try {
            sendRequestWithRetry(payload, 0, 1000);
        } catch (AuthenticationException e) {
            System.out.println("✅ 正确捕获认证错误: " + e.getMessage());
        } catch (APIException e) {
            System.out.println("⚠️ 其他错误: " + e.getMessage());
        } finally {
            API_KEY = originalKey;
        }
    }

    /**
     * 测试：无效的模型名称
     */
    public void testInvalidModel() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 3: 无效的模型名称");
        System.out.println("=".repeat(50));

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "non-existent-model-xyz");
        payload.addProperty("max_tokens", 50);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Hello");
        messages.add(message);
        payload.add("messages", messages);

        try {
            sendRequestWithRetry(payload, 1, 1000);
        } catch (APIException e) {
            System.out.println("✅ 正确捕获 API 错误: " + e.getMessage());
        }
    }

    /**
     * 测试：超时处理
     */
    public void testTimeoutHandling() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 4: 超时处理");
        System.out.println("=".repeat(50));

        System.out.println("设置极短超时时间 (1ms)...");

        OkHttpClient shortTimeoutClient = new OkHttpClient.Builder()
                .connectTimeout(1, TimeUnit.MILLISECONDS)
                .readTimeout(1, TimeUnit.MILLISECONDS)
                .build();

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 50);

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "Hello");
        messages.add(message);
        payload.add("messages", messages);

        try {
            RequestBody body = RequestBody.create(gson.toJson(payload), JSON);
            Request request = new Request.Builder()
                    .url(ENDPOINT)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .post(body)
                    .build();

            shortTimeoutClient.newCall(request).execute();
        } catch (IOException e) {
            System.out.println("✅ 正确捕获超时错误");
        }
    }

    /**
     * 测试：格式错误的请求
     */
    public void testMalformedRequest() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 5: 格式错误的请求");
        System.out.println("=".repeat(50));

        // 缺少必需字段
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.addProperty("max_tokens", 50);
        // 缺少 messages 字段

        try {
            sendRequestWithRetry(payload, 0, 1000);
        } catch (APIException e) {
            System.out.println("✅ 正确捕获 API 错误: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("错误处理示例");
        System.out.println("=".repeat(50));

        ErrorHandling example = new ErrorHandling();

        example.testSuccessfulRequest();
        example.testInvalidApiKey();
        example.testInvalidModel();
        example.testTimeoutHandling();
        example.testMalformedRequest();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("所有测试完成");
        System.out.println("=".repeat(50));
    }
}
