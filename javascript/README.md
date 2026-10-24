# JavaScript API 示例

完整的 JavaScript/Node.js 示例代码，演示如何使用 Tokenra API。

## 安装依赖

```bash
npm install
```

## 配置 API Key

### 方式 1: 环境变量（推荐）

```bash
export API_KEY="your_api_key_here"
```

### 方式 2: 修改代码

直接在代码中修改 `API_KEY` 常量。

## 示例文件说明

### 1. basic_request.js - 基础请求

演示最基本的 API 调用：

```bash
npm run basic
# 或
node basic_request.js
```

**功能**:
- 发送简单的聊天请求
- 正确设置请求头和请求体
- 解析响应和 Token 使用统计

### 2. streaming_request.js - 流式输出

演示如何处理 Server-Sent Events (SSE) 流式响应：

```bash
npm run streaming
# 或
node streaming_request.js
```

**功能**:
- 启用流式输出 (`stream: true`)
- 逐块接收和显示响应
- 处理 SSE 格式数据
- 检测流结束标记 `[DONE]`

### 3. error_handling.js - 错误处理

演示完整的错误处理和重试机制：

```bash
npm run error
# 或
node error_handling.js
```

**功能**:
- HTTP 状态码检查
- 自定义异常类
- 指数退避重试策略
- 区分可重试和不可重试错误
- 超时处理（使用 AbortController）

### 4. token_usage.js - Token 计费

演示如何跟踪 Token 使用量并估算成本：

```bash
npm run token
# 或
node token_usage.js
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

```javascript
const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${API_KEY}`
};
```

### 请求体

```javascript
const payload = {
  model: 'kimi-k3.1-pro',
  messages: [
    { role: 'user', content: '你的问题' }
  ],
  max_tokens: 1024,
  stream: false
};
```

### 发送请求

```javascript
const response = await fetch(ENDPOINT, {
  method: 'POST',
  headers: headers,
  body: JSON.stringify(payload)
});

if (!response.ok) {
  throw new Error(`HTTP ${response.status}`);
}

const result = await response.json();
```

## 最佳实践

### 1. 使用 async/await

现代 JavaScript 推荐使用 async/await 语法：

```javascript
async function sendRequest() {
  try {
    const response = await fetch(ENDPOINT, { /* ... */ });
    const data = await response.json();
    return data;
  } catch (error) {
    console.error('请求失败:', error);
  }
}
```

### 2. 错误处理

检查响应状态并处理错误：

```javascript
if (!response.ok) {
  const errorData = await response.json();
  throw new Error(errorData.error?.message || '未知错误');
}
```

### 3. 流式输出

使用异步迭代器处理流：

```javascript
for await (const chunk of response.body) {
  const text = chunk.toString('utf-8');
  // 处理每个数据块
}
```

### 4. 超时控制

使用 AbortController 实现超时：

```javascript
const controller = new AbortController();
const timeout = setTimeout(() => controller.abort(), 30000);

try {
  const response = await fetch(ENDPOINT, {
    signal: controller.signal,
    // ...
  });
} finally {
  clearTimeout(timeout);
}
```

### 5. 重试机制

对暂时性错误实现指数退避重试：

```javascript
async function sendWithRetry(payload, maxRetries = 3) {
  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      return await fetch(ENDPOINT, { /* ... */ });
    } catch (error) {
      if (attempt < maxRetries) {
        const delay = 1000 * Math.pow(2, attempt);
        await new Promise(resolve => setTimeout(resolve, delay));
      } else {
        throw error;
      }
    }
  }
}
```

## 从 OpenAI SDK 迁移

如果你正在使用 OpenAI 的官方 JavaScript SDK，迁移非常简单：

```javascript
import OpenAI from 'openai';

// 只需修改这两行
const client = new OpenAI({
  apiKey: 'your_tokenra_key',
  baseURL: 'https://tokenra.io/v1'
});

// 其余代码完全相同
const response = await client.chat.completions.create({
  model: 'kimi-k3.1-pro',
  messages: [{ role: 'user', content: 'Hello' }]
});
```

## ES Modules vs CommonJS

本示例使用 ES Modules (`import`/`export`)。如果你的项目使用 CommonJS，需要：

1. 移除 `package.json` 中的 `"type": "module"`
2. 替换 `import` 为 `require`:

```javascript
// ES Modules
import fetch from 'node-fetch';

// CommonJS
const fetch = require('node-fetch');
```

## 浏览器环境

在浏览器中使用时：

1. 移除 `import fetch from 'node-fetch'`（浏览器原生支持 fetch）
2. 注意 CORS 限制
3. 不要在前端直接暴露 API Key

推荐做法是在后端代理 API 请求：

```javascript
// 前端
const response = await fetch('/api/chat', {
  method: 'POST',
  body: JSON.stringify({ messages: [...] })
});

// 后端
app.post('/api/chat', async (req, res) => {
  const response = await fetch('https://tokenra.io/v1/chat/completions', {
    headers: { 'Authorization': `Bearer ${process.env.API_KEY}` },
    body: JSON.stringify(req.body)
  });
  res.json(await response.json());
});
```

## 常见问题

### Q: node-fetch 报错？

A: 确保安装了 v3 版本（支持 ES Modules）：

```bash
npm install node-fetch@3
```

### Q: 如何设置代理？

A: 使用 `https-proxy-agent`:

```bash
npm install https-proxy-agent
```

```javascript
import { HttpsProxyAgent } from 'https-proxy-agent';

const agent = new HttpsProxyAgent('http://proxy.example.com:8080');

const response = await fetch(ENDPOINT, {
  agent: agent,
  // ...
});
```

### Q: 如何处理并发请求？

A: 使用 `Promise.all`:

```javascript
const requests = messages.map(msg =>
  sendRequest([{ role: 'user', content: msg }])
);

const results = await Promise.all(requests);
```

### Q: 如何限制并发数？

A: 使用 `p-limit` 库：

```bash
npm install p-limit
```

```javascript
import pLimit from 'p-limit';

const limit = pLimit(10);  // 最多 10 个并发

const results = await Promise.all(
  messages.map(msg => limit(() => sendRequest(msg)))
);
```

## 性能优化

### 1. 连接复用

`node-fetch` 会自动复用 HTTP 连接。

### 2. 并发控制

限制并发数以避免速率限制：

```javascript
import pLimit from 'p-limit';

const limit = pLimit(10);
```

### 3. 缓存响应

对相同请求缓存结果：

```javascript
const cache = new Map();

async function cachedRequest(key, payload) {
  if (cache.has(key)) {
    return cache.get(key);
  }

  const result = await sendRequest(payload);
  cache.set(key, result);
  return result;
}
```

## 依赖说明

- **node-fetch**: HTTP 客户端（Node.js < 18 需要，Node.js 18+ 可选）
- **dotenv**: 环境变量管理（可选）

## TypeScript 支持

添加类型定义：

```bash
npm install --save-dev @types/node
```

```typescript
interface Message {
  role: 'system' | 'user' | 'assistant';
  content: string;
}

interface ChatRequest {
  model: string;
  messages: Message[];
  max_tokens?: number;
  stream?: boolean;
}

interface Usage {
  prompt_tokens: number;
  completion_tokens: number;
  total_tokens: number;
}

interface ChatResponse {
  id: string;
  choices: Array<{
    message: Message;
    finish_reason: string;
  }>;
  usage: Usage;
}
```

## 参考资料

- [node-fetch 文档](https://github.com/node-fetch/node-fetch)
- [Fetch API 标准](https://fetch.spec.whatwg.org/)
- [OpenAI Node.js SDK](https://github.com/openai/openai-node)
