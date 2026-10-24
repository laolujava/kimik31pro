# Python API 示例

完整的 Python 示例代码，演示如何使用 Tokenra API。

## 安装依赖

```bash
pip install -r requirements.txt
```

## 配置 API Key

### 方式 1: 环境变量（推荐）

```bash
export API_KEY="your_api_key_here"
```

### 方式 2: 修改代码

直接在代码中修改 `API_KEY` 变量。

## 示例文件说明

### 1. basic_request.py - 基础请求

演示最基本的 API 调用：

```bash
python basic_request.py
```

**功能**:
- 发送简单的聊天请求
- 正确设置请求头和请求体
- 解析响应和 Token 使用统计

### 2. streaming_request.py - 流式输出

演示如何处理 Server-Sent Events (SSE) 流式响应：

```bash
python streaming_request.py
```

**功能**:
- 启用流式输出 (`stream=True`)
- 逐块接收和显示响应
- 处理 SSE 格式数据
- 检测流结束标记 `[DONE]`

### 3. error_handling.py - 错误处理

演示完整的错误处理和重试机制：

```bash
python error_handling.py
```

**功能**:
- HTTP 状态码检查
- 自定义异常类
- 指数退避重试策略
- 区分可重试和不可重试错误
- 超时处理

### 4. token_usage.py - Token 计费

演示如何跟踪 Token 使用量并估算成本：

```bash
python token_usage.py
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

```python
headers = {
    "Content-Type": "application/json",
    "Authorization": f"Bearer {API_KEY}"
}
```

### 请求体

```python
payload = {
    "model": "kimi-k3.1-pro",
    "messages": [
        {"role": "user", "content": "你的问题"}
    ],
    "max_tokens": 1024,
    "stream": False
}
```

### 发送请求

```python
response = requests.post(
    ENDPOINT,
    headers=headers,
    json=payload,
    timeout=30
)

response.raise_for_status()
result = response.json()
```

## 最佳实践

### 1. 使用超时

始终设置超时参数，避免请求挂起：

```python
response = requests.post(..., timeout=30)
```

### 2. 错误处理

使用 `raise_for_status()` 自动检查错误：

```python
try:
    response.raise_for_status()
except requests.exceptions.HTTPError as e:
    print(f"HTTP 错误: {e}")
```

### 3. 流式输出

使用 `stream=True` 接收大块响应：

```python
response = requests.post(..., stream=True)

for line in response.iter_lines():
    # 处理每一行
```

### 4. 重试机制

对暂时性错误实现指数退避重试：

```python
for attempt in range(max_retries):
    try:
        response = requests.post(...)
        break
    except requests.exceptions.Timeout:
        delay = initial_delay * (2 ** attempt)
        time.sleep(delay)
```

## 从 OpenAI SDK 迁移

如果你正在使用 OpenAI 的官方 Python SDK，迁移非常简单：

```python
from openai import OpenAI

# 只需修改这两行
client = OpenAI(
    api_key="your_tokenra_key",
    base_url="https://tokenra.io/v1"
)

# 其余代码完全相同
response = client.chat.completions.create(
    model="kimi-k3.1-pro",
    messages=[{"role": "user", "content": "Hello"}]
)
```

## 常见问题

### Q: 如何处理中文字符？

A: requests 库自动处理 UTF-8 编码，无需额外配置。

### Q: 如何设置代理？

A: 传递 `proxies` 参数：

```python
proxies = {
    "http": "http://proxy.example.com:8080",
    "https": "http://proxy.example.com:8080"
}

response = requests.post(..., proxies=proxies)
```

### Q: 如何保存对话历史？

A: 维护一个 `messages` 列表，每次请求都传递完整历史：

```python
messages = []
messages.append({"role": "user", "content": "问题 1"})

# 第一次请求
response = send_request(messages)
messages.append({"role": "assistant", "content": response['content']})

# 第二次请求
messages.append({"role": "user", "content": "问题 2"})
response = send_request(messages)
```

## 性能优化

### 1. 复用 Session

使用 `requests.Session()` 复用连接：

```python
session = requests.Session()
session.headers.update(headers)

response = session.post(ENDPOINT, json=payload)
```

### 2. 并发请求

使用 `concurrent.futures` 并发处理：

```python
from concurrent.futures import ThreadPoolExecutor

with ThreadPoolExecutor(max_workers=10) as executor:
    futures = [executor.submit(send_request, msg) for msg in messages]
    results = [f.result() for f in futures]
```

### 3. 缓存响应

对相同请求缓存结果：

```python
import functools

@functools.lru_cache(maxsize=128)
def cached_request(message):
    # 你的请求逻辑
    pass
```

## 依赖说明

- **requests**: HTTP 请求库
- **python-dotenv**: 环境变量管理（可选）

## 参考资料

- [Requests 文档](https://requests.readthedocs.io/)
- [OpenAI Python SDK](https://github.com/openai/openai-python)
