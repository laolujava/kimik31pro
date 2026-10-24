# API 教程示例集合

这是一个真实可运行的 API 教程仓库，提供 Python、JavaScript 和 Java 的完整示例代码。

## 🚀 快速开始

本教程使用 Tokenra API (与 OpenAI API 完全兼容) 作为示例端点。

**API 端点**: `https://tokenra.io/v1/chat/completions`

### 环境要求

- **Python**: 3.7+
- **Node.js**: 14+
- **Java**: 11+

## 📚 目录结构

```
.
├── python/          # Python 示例
├── javascript/      # JavaScript/Node.js 示例
├── java/            # Java 示例
└── README.md        # 本文件
```

## 🔑 鉴权说明

所有示例使用 Bearer Token 认证方式：

```
Authorization: Bearer YOUR_API_KEY
```

API Key 通过环境变量 `API_KEY` 传递，或在代码中直接配置。

## 📖 功能覆盖

每种语言的示例都包含以下功能：

### 1. 基础请求
- 简单的聊天完成请求
- 正确的请求头设置
- JSON 请求体构造

### 2. 流式输出 (Streaming)
- Server-Sent Events (SSE) 处理
- 逐块接收响应
- 实时输出展示

### 3. 错误处理
- HTTP 状态码检查
- 网络超时处理
- API 错误响应解析
- 重试机制

### 4. Token 计费
- 使用量统计 (prompt_tokens, completion_tokens, total_tokens)
- 成本估算示例
- 请求参数优化建议

### 5. 迁移指南
- 从 OpenAI API 迁移步骤
- 配置差异说明
- 兼容性注意事项

## 🐍 Python 示例

### 安装依赖

```bash
cd python
pip install -r requirements.txt
```

### 运行示例

```bash
# 基础请求
python basic_request.py

# 流式输出
python streaming_request.py

# 错误处理
python error_handling.py

# Token 计费
python token_usage.py
```

详见 [python/README.md](python/README.md)

## 🟨 JavaScript 示例

### 安装依赖

```bash
cd javascript
npm install
```

### 运行示例

```bash
# 基础请求
node basic_request.js

# 流式输出
node streaming_request.js

# 错误处理
node error_handling.js

# Token 计费
node token_usage.js
```

详见 [javascript/README.md](javascript/README.md)

## ☕ Java 示例

### 构建项目

```bash
cd java
mvn clean install
```

### 运行示例

```bash
# 基础请求
mvn exec:java -Dexec.mainClass="com.example.BasicRequest"

# 流式输出
mvn exec:java -Dexec.mainClass="com.example.StreamingRequest"

# 错误处理
mvn exec:java -Dexec.mainClass="com.example.ErrorHandling"

# Token 计费
mvn exec:java -Dexec.mainClass="com.example.TokenUsage"
```

详见 [java/README.md](java/README.md)

## 🔧 配置说明

### 环境变量

推荐使用环境变量管理 API Key：

```bash
# Linux/Mac
export API_KEY="your_api_key_here"

# Windows PowerShell
$env:API_KEY="your_api_key_here"

# Windows CMD
set API_KEY=your_api_key_here
```

### 配置文件

也可以创建 `.env` 文件：

```
API_KEY=your_api_key_here
API_BASE_URL=https://tokenra.io/v1
```

## 📊 API 参数说明

### 请求参数

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| model | string | 是 | 模型名称，如 `kimi-k3.1-pro` |
| messages | array | 是 | 对话消息数组 |
| max_tokens | integer | 否 | 最大生成 token 数 |
| temperature | float | 否 | 采样温度 (0-2) |
| stream | boolean | 否 | 是否流式输出 |
| top_p | float | 否 | 核采样参数 |

### 响应格式

**非流式响应**:

```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "kimi-k3.1-pro",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "回复内容"
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 9,
    "completion_tokens": 12,
    "total_tokens": 21
  }
}
```

**流式响应** (SSE):

```
data: {"id":"chatcmpl-123","choices":[{"delta":{"content":"你"}}]}

data: {"id":"chatcmpl-123","choices":[{"delta":{"content":"好"}}]}

data: [DONE]
```

## 🔄 从 OpenAI API 迁移

### 仅需修改两处

1. **修改 Base URL**:
   ```
   https://api.openai.com/v1 → https://tokenra.io/v1
   ```

2. **替换 API Key**:
   ```
   sk-...OpenAI Key → 你的 Tokenra Key
   ```

### 代码示例

**Python (使用 OpenAI SDK)**:

```python
from openai import OpenAI

client = OpenAI(
    api_key="your_tokenra_key",
    base_url="https://tokenra.io/v1"  # 仅此一行不同
)

response = client.chat.completions.create(
    model="kimi-k3.1-pro",
    messages=[{"role": "user", "content": "Hello"}]
)
```

**JavaScript**:

```javascript
import OpenAI from 'openai';

const client = new OpenAI({
  apiKey: 'your_tokenra_key',
  baseURL: 'https://tokenra.io/v1'  // 仅此一行不同
});

const response = await client.chat.completions.create({
  model: 'kimi-k3.1-pro',
  messages: [{ role: 'user', content: 'Hello' }]
});
```

## 💡 最佳实践

### 1. 错误重试策略

- 使用指数退避算法
- 最大重试次数: 3-5 次
- 仅对可重试错误进行重试 (5xx, 网络超时)

### 2. 超时设置

- 连接超时: 10 秒
- 读取超时: 60 秒 (非流式), 120 秒 (流式)

### 3. Token 优化

- 使用 `max_tokens` 控制成本
- 精简 system prompt
- 对长对话进行截断或摘要

### 4. 并发控制

- 根据限流配置控制并发数
- 使用连接池复用连接
- 实现速率限制器

## 🐛 常见问题

### 401 Unauthorized

- 检查 API Key 是否正确
- 确认 Authorization 头格式: `Bearer YOUR_KEY`

### 429 Too Many Requests

- 超过速率限制，需要降低请求频率
- 实现重试机制和退避策略

### 超时错误

- 增加超时时间
- 检查网络连接
- 使用流式输出获取更快响应

### 流式输出断开

- 确保正确处理 SSE 格式
- 检查 `stream: true` 参数
- 处理 `[DONE]` 结束标记

## 📝 许可证

MIT License

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题，请提交 GitHub Issue。
