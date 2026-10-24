# Java API 示例

完整的 Java 示例代码，演示如何使用 Tokenra API。

## 环境要求

- Java 11 或更高版本
- Maven 3.6 或更高版本

## 构建项目

```bash
cd java
mvn clean install
```

## 配置 API Key

### 方式 1: 环境变量（推荐）

```bash
export API_KEY="your_api_key_here"
```

### 方式 2: 修改代码

直接在代码中修改 `API_KEY` 常量。

## 示例文件说明

### 1. BasicRequest.java - 基础请求

演示最基本的 API 调用：

```bash
mvn exec:java -Dexec.mainClass="com.example.BasicRequest"
```

**功能**:
- 发送简单的聊天请求
- 正确设置请求头和请求体
- 解析响应和 Token 使用统计

### 2. StreamingRequest.java - 流式输出

演示如何处理 Server-Sent Events (SSE) 流式响应：

```bash
mvn exec:java -Dexec.mainClass="com.example.StreamingRequest"
```

**功能**:
- 启用流式输出 (`stream: true`)
- 逐块接收和显示响应
- 处理 SSE 格式数据
- 检测流结束标记 `[DONE]`

### 3. ErrorHandling.java - 错误处理

演示完整的错误处理和重试机制：

```bash
mvn exec:java -Dexec.mainClass="com.example.ErrorHandling"
```

**功能**:
- HTTP 状态码检查
- 自定义异常类
- 指数退避重试策略
- 区分可重试和不可重试错误
- 超时处理

### 4. TokenUsage.java - Token 计费

演示如何跟踪 Token 使用量并估算成本：

```bash
mvn exec:java -Dexec.mainClass="com.example.TokenUsage"
```

**功能**:
- 提取使用统计 (`usage` 字段)
- 成本计算
- 多轮对话 Token 累计
- `max_tokens` 参数影响分析
- 提示词优化建议
- 月度成本估算

## 代码结构

### 请求头

```java
Request request = new Request.Builder()
    .url(ENDPOINT)
    .addHeader("Authorization", "Bearer " + API_KEY)
    .post(body)
    .build();
```

### 请求体

```java
JsonObject payload = new JsonObject();
payload.addProperty("model", "kimi-k3.1-pro");
payload.addProperty("max_tokens", 1024);
payload.addProperty("stream", false);

JsonArray messages = new JsonArray();
JsonObject message = new JsonObject();
message.addProperty("role", "user");
message.addProperty("content", "你的问题");
messages.add(message);
payload.add("messages", messages);
```

### 发送请求

```java
OkHttpClient client = new OkHttpClient();
Gson gson = new Gson();

RequestBody body = RequestBody.create(
    gson.toJson(payload),
    MediaType.get("application/json; charset=utf-8")
);

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
    // 处理结果
}
```

## 最佳实践

### 1. 使用 try-with-resources

自动关闭资源，避免内存泄漏：

```java
try (Response response = client.newCall(request).execute()) {
    // 处理响应
}
```

### 2. 错误处理

检查响应状态并抛出异常：

```java
if (!response.isSuccessful()) {
    String errorBody = response.body().string();
    throw new IOException("HTTP " + response.code() + ": " + errorBody);
}
```

### 3. 流式输出

使用 BufferedReader 读取 SSE 流：

```java
InputStream inputStream = response.body().byteStream();
BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

String line;
while ((line = reader.readLine()) != null) {
    if (line.startsWith("data: ")) {
        String dataStr = line.substring(6);
        // 处理数据
    }
}
```

### 4. 超时设置

配置连接和读取超时：

```java
OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build();
```

### 5. 重试机制

实现指数退避重试：

```java
for (int attempt = 0; attempt <= maxRetries; attempt++) {
    try {
        // 发送请求
        return response;
    } catch (IOException e) {
        if (attempt < maxRetries) {
            int delay = initialDelay * (int) Math.pow(2, attempt);
            Thread.sleep(delay);
        } else {
            throw e;
        }
    }
}
```

## 依赖说明

### OkHttp

高效的 HTTP 客户端：

- 连接池和复用
- GZIP 压缩
- 响应缓存
- 超时配置

```xml
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>okhttp</artifactId>
    <version>4.12.0</version>
</dependency>
```

### Gson

Google 的 JSON 库：

- 简洁的 API
- 类型安全
- 对象序列化/反序列化

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.10.1</version>
</dependency>
```

## 从 OpenAI Java SDK 迁移

如果你正在使用类似 OpenAI 的 Java SDK，迁移步骤：

1. **更换 Base URL**:

```java
// 原来
OpenAiService service = new OpenAiService("your_openai_key");

// 现在 - 使用 OkHttp 直接调用
OkHttpClient client = new OkHttpClient();
String endpoint = "https://tokenra.io/v1/chat/completions";
```

2. **调整请求构造**:

```java
// 使用相同的请求格式
JsonObject payload = new JsonObject();
payload.addProperty("model", "kimi-k3.1-pro");
// ... 其余参数相同
```

## 常见问题

### Q: 如何处理 JSON 解析异常？

A: 使用 try-catch 捕获 JsonSyntaxException：

```java
try {
    JsonObject result = gson.fromJson(responseBody, JsonObject.class);
} catch (JsonSyntaxException e) {
    System.err.println("JSON 解析失败: " + e.getMessage());
}
```

### Q: 如何设置代理？

A: 配置 OkHttpClient 的代理：

```java
Proxy proxy = new Proxy(
    Proxy.Type.HTTP,
    new InetSocketAddress("proxy.example.com", 8080)
);

OkHttpClient client = new OkHttpClient.Builder()
    .proxy(proxy)
    .build();
```

### Q: 如何处理中文乱码？

A: 确保使用 UTF-8 编码：

```java
MediaType JSON = MediaType.get("application/json; charset=utf-8");
RequestBody body = RequestBody.create(json, JSON);

// 读取响应时也要指定编码
BufferedReader reader = new BufferedReader(
    new InputStreamReader(inputStream, StandardCharsets.UTF_8)
);
```

### Q: 如何实现并发请求？

A: 使用 ExecutorService：

```java
ExecutorService executor = Executors.newFixedThreadPool(10);

List<Future<JsonObject>> futures = new ArrayList<>();
for (String message : messages) {
    Future<JsonObject> future = executor.submit(() -> sendRequest(message));
    futures.add(future);
}

// 获取结果
for (Future<JsonObject> future : futures) {
    JsonObject result = future.get();
}

executor.shutdown();
```

## 性能优化

### 1. 连接池

OkHttp 默认使用连接池，无需额外配置。

### 2. 请求压缩

OkHttp 自动处理 GZIP 压缩。

### 3. 缓存 OkHttpClient

创建一个全局的 OkHttpClient 实例，避免重复创建：

```java
public class ApiClient {
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build();

    public static OkHttpClient getClient() {
        return CLIENT;
    }
}
```

### 4. 异步请求

使用 OkHttp 的异步 API：

```java
client.newCall(request).enqueue(new Callback() {
    @Override
    public void onResponse(Call call, Response response) {
        // 处理响应
    }

    @Override
    public void onFailure(Call call, IOException e) {
        // 处理错误
    }
});
```

## 类型安全的替代方案

### 使用 POJO 类

定义请求和响应的 Java 类：

```java
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private int max_tokens;
    private boolean stream;
    
    // getters and setters
}

public class Message {
    private String role;
    private String content;
    
    // getters and setters
}

public class ChatResponse {
    private String id;
    private List<Choice> choices;
    private Usage usage;
    
    // getters and setters
}

// 使用
ChatRequest request = new ChatRequest();
request.setModel("kimi-k3.1-pro");
// ...

String json = gson.toJson(request);
ChatResponse response = gson.fromJson(responseBody, ChatResponse.class);
```

## 打包和部署

### 创建可执行 JAR

```bash
mvn package
java -jar target/api-tutorial-java-1.0.0.jar
```

### 添加依赖到 JAR

使用 Maven Shade Plugin：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-shade-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>shade</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 单元测试

添加 JUnit 依赖：

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.0</version>
    <scope>test</scope>
</dependency>
```

编写测试：

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BasicRequestTest {
    @Test
    void testSendRequest() throws IOException {
        BasicRequest client = new BasicRequest();
        JsonObject result = client.sendBasicRequest();
        
        assertNotNull(result);
        assertTrue(result.has("choices"));
    }
}
```

## 参考资料

- [OkHttp 文档](https://square.github.io/okhttp/)
- [Gson 文档](https://github.com/google/gson)
- [Maven 文档](https://maven.apache.org/)
