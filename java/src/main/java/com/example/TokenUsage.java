package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Token 使用和计费示例
 *
 * 演示如何统计 Token 使用量并估算成本。
 */
public class TokenUsage {

    private static final String API_KEY = System.getenv().getOrDefault(
        "API_KEY",
        "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG"
    );
    private static final String API_BASE_URL = "https://tokenra.io/v1";
    private static final String ENDPOINT = API_BASE_URL + "/chat/completions";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    // 价格配置 (示例价格，请根据实际情况调整)
    // 单位: USD per 1M tokens
    private static final Map<String, Map<String, Double>> PRICING = new HashMap<>();

    static {
        Map<String, Double> kimiPricing = new HashMap<>();
        kimiPricing.put("input", 0.5);   // $0.5 per 1M input tokens
        kimiPricing.put("output", 1.5);  // $1.5 per 1M output tokens
        PRICING.put("kimi-k3.1-pro", kimiPricing);
    }

    private final OkHttpClient client;
    private final Gson gson;

    public TokenUsage() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
    }

    /**
     * 使用统计结果类
     */
    static class UsageResult {
        String content;
        int promptTokens;
        int completionTokens;
        int totalTokens;
        double costUsd;

        public UsageResult(String content, int promptTokens, int completionTokens,
                          int totalTokens, double costUsd) {
            this.content = content;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.totalTokens = totalTokens;
            this.costUsd = costUsd;
        }
    }

    /**
     * 计算请求成本
     */
    public static double calculateCost(String model, int promptTokens, int completionTokens) {
        if (!PRICING.containsKey(model)) {
            System.out.println("⚠️ 未知模型价格: " + model);
            return 0.0;
        }

        Map<String, Double> modelPricing = PRICING.get(model);
        double inputCost = (promptTokens / 1_000_000.0) * modelPricing.get("input");
        double outputCost = (completionTokens / 1_000_000.0) * modelPricing.get("output");

        return inputCost + outputCost;
    }

    /**
     * 发送请求并跟踪 Token 使用
     */
    public UsageResult sendRequestAndTrackUsage(JsonArray messages, int maxTokens) throws IOException {
        JsonObject payload = new JsonObject();
        payload.addProperty("model", "kimi-k3.1-pro");
        payload.add("messages", messages);
        payload.addProperty("max_tokens", maxTokens);
        payload.addProperty("stream", false);

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

            JsonObject result = gson.fromJson(response.body().string(), JsonObject.class);

            // 提取使用统计
            JsonObject usage = result.has("usage") ? result.getAsJsonObject("usage") : new JsonObject();
            int promptTokens = usage.has("prompt_tokens") ? usage.get("prompt_tokens").getAsInt() : 0;
            int completionTokens = usage.has("completion_tokens") ? usage.get("completion_tokens").getAsInt() : 0;
            int totalTokens = usage.has("total_tokens") ? usage.get("total_tokens").getAsInt() : 0;

            // 计算成本
            double cost = calculateCost("kimi-k3.1-pro", promptTokens, completionTokens);

            // 提取内容
            String content = result.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            return new UsageResult(content, promptTokens, completionTokens, totalTokens, cost);
        }
    }

    /**
     * 测试：单次请求的 Token 使用
     */
    public void testSingleRequest() {
        System.out.println("=".repeat(50));
        System.out.println("测试 1: 单次请求 Token 使用");
        System.out.println("=".repeat(50));

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "什么是人工智能？请用一句话回答。");
        messages.add(message);

        try {
            UsageResult result = sendRequestAndTrackUsage(messages, 100);

            System.out.println("🤖 回复: " + result.content + "\n");
            System.out.println("📊 Token 使用统计:");
            System.out.println("  输入 tokens: " + result.promptTokens);
            System.out.println("  输出 tokens: " + result.completionTokens);
            System.out.println("  总计 tokens: " + result.totalTokens);
            System.out.println(String.format("  💰 估算成本: $%.6f", result.costUsd));

        } catch (IOException e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
        }
    }

    /**
     * 测试：多轮对话的 Token 累计
     */
    public void testConversationTracking() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 2: 多轮对话 Token 累计");
        System.out.println("=".repeat(50));

        JsonArray conversation = new JsonArray();
        JsonObject userMsg1 = new JsonObject();
        userMsg1.addProperty("role", "user");
        userMsg1.addProperty("content", "什么是机器学习？");
        conversation.add(userMsg1);

        double totalCost = 0.0;
        int totalTokensUsed = 0;

        try {
            // 第一轮
            System.out.println("\n📝 第 1 轮对话:");
            UsageResult result = sendRequestAndTrackUsage(conversation, 200);

            System.out.println("🤖 " + result.content);
            totalCost += result.costUsd;
            totalTokensUsed += result.totalTokens;

            // 添加助手回复到对话历史
            JsonObject assistantMsg = new JsonObject();
            assistantMsg.addProperty("role", "assistant");
            assistantMsg.addProperty("content", result.content);
            conversation.add(assistantMsg);

            // 第二轮
            JsonObject userMsg2 = new JsonObject();
            userMsg2.addProperty("role", "user");
            userMsg2.addProperty("content", "它有哪些应用场景？");
            conversation.add(userMsg2);

            System.out.println("\n📝 第 2 轮对话:");
            result = sendRequestAndTrackUsage(conversation, 200);

            System.out.println("🤖 " + result.content);
            totalCost += result.costUsd;
            totalTokensUsed += result.totalTokens;

            System.out.println("\n" + "-".repeat(50));
            System.out.println("📊 累计统计:");
            System.out.println("  总 tokens: " + totalTokensUsed);
            System.out.println(String.format("  💰 总成本: $%.6f", totalCost));

        } catch (IOException e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
        }
    }

    /**
     * 测试：max_tokens 参数对成本的影响
     */
    public void testMaxTokensImpact() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 3: max_tokens 对成本的影响");
        System.out.println("=".repeat(50));

        JsonArray messages = new JsonArray();
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", "介绍一下深度学习");
        messages.add(message);

        int[] maxTokensOptions = {50, 100, 500};

        for (int maxTokens : maxTokensOptions) {
            System.out.println("\n设置 max_tokens=" + maxTokens + ":");

            try {
                UsageResult result = sendRequestAndTrackUsage(messages, maxTokens);

                System.out.println("  实际输出 tokens: " + result.completionTokens);
                System.out.println("  总 tokens: " + result.totalTokens);
                System.out.println(String.format("  💰 成本: $%.6f", result.costUsd));

            } catch (IOException e) {
                System.err.println("  ❌ 请求失败: " + e.getMessage());
            }
        }
    }

    /**
     * 测试：提示词优化对成本的影响
     */
    public void testPromptOptimization() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 4: 提示词优化");
        System.out.println("=".repeat(50));

        // 冗长的提示词
        JsonArray verboseMessages = new JsonArray();
        JsonObject sysMsg = new JsonObject();
        sysMsg.addProperty("role", "system");
        sysMsg.addProperty("content", "你是一个非常有帮助的助手，你总是提供详细、准确、有见地的回答。你会考虑问题的各个方面，并给出全面的解释。");
        verboseMessages.add(sysMsg);
        JsonObject userMsg1 = new JsonObject();
        userMsg1.addProperty("role", "user");
        userMsg1.addProperty("content", "请你告诉我，人工智能这个概念到底是什么意思？");
        verboseMessages.add(userMsg1);

        // 简洁的提示词
        JsonArray conciseMessages = new JsonArray();
        JsonObject userMsg2 = new JsonObject();
        userMsg2.addProperty("role", "user");
        userMsg2.addProperty("content", "什么是人工智能？");
        conciseMessages.add(userMsg2);

        try {
            System.out.println("\n📝 冗长提示词:");
            UsageResult result1 = sendRequestAndTrackUsage(verboseMessages, 100);
            System.out.println("  输入 tokens: " + result1.promptTokens);
            System.out.println(String.format("  💰 成本: $%.6f", result1.costUsd));

            System.out.println("\n📝 简洁提示词:");
            UsageResult result2 = sendRequestAndTrackUsage(conciseMessages, 100);
            System.out.println("  输入 tokens: " + result2.promptTokens);
            System.out.println(String.format("  💰 成本: $%.6f", result2.costUsd));

            int savings = result1.promptTokens - result2.promptTokens;
            System.out.println("\n💡 节省了 " + savings + " 个输入 tokens");

        } catch (IOException e) {
            System.err.println("❌ 请求失败: " + e.getMessage());
        }
    }

    /**
     * 估算月度成本
     */
    public void estimateMonthlyCost() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("测试 5: 月度成本估算");
        System.out.println("=".repeat(50));

        // 假设场景
        int dailyRequests = 1000;
        int avgPromptTokens = 50;
        int avgCompletionTokens = 150;

        // 单次请求成本
        double costPerRequest = calculateCost("kimi-k3.1-pro", avgPromptTokens, avgCompletionTokens);

        // 月度成本
        double monthlyCost = costPerRequest * dailyRequests * 30;

        System.out.println("📊 假设场景:");
        System.out.println("  每日请求数: " + String.format("%,d", dailyRequests));
        System.out.println("  平均输入 tokens: " + avgPromptTokens);
        System.out.println("  平均输出 tokens: " + avgCompletionTokens);
        System.out.println("\n💰 成本估算:");
        System.out.println(String.format("  单次请求: $%.6f", costPerRequest));
        System.out.println(String.format("  每日: $%.2f", costPerRequest * dailyRequests));
        System.out.println(String.format("  每月: $%.2f", monthlyCost));
    }

    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("Token 使用和计费示例");
        System.out.println("=".repeat(50));

        TokenUsage example = new TokenUsage();

        example.testSingleRequest();
        example.testConversationTracking();
        example.testMaxTokensImpact();
        example.testPromptOptimization();
        example.estimateMonthlyCost();

        System.out.println("\n" + "=".repeat(50));
        System.out.println("所有测试完成");
        System.out.println("=".repeat(50));
    }
}
