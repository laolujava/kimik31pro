"""
流式输出示例

演示如何使用 Server-Sent Events (SSE) 接收流式响应。
"""

import requests
import json
import os

# API 配置
API_KEY = os.getenv("API_KEY", "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG")
API_BASE_URL = "https://tokenra.io/v1"
ENDPOINT = f"{API_BASE_URL}/chat/completions"


def send_streaming_request():
    """发送流式请求并实时打印输出"""

    # 请求头
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }

    # 请求体 - 注意 stream=True
    payload = {
        "model": "kimi-k3.1-pro",
        "messages": [
            {
                "role": "user",
                "content": "请用三句话介绍人工智能"
            }
        ],
        "max_tokens": 1024,
        "stream": True  # 启用流式输出
    }

    print("📤 发送流式请求...")
    print(f"模型: {payload['model']}")
    print(f"消息: {payload['messages'][0]['content']}")
    print("-" * 50)
    print("🤖 助手回复 (流式输出):\n")

    try:
        # 发送请求，使用 stream=True 接收流式响应
        response = requests.post(
            ENDPOINT,
            headers=headers,
            json=payload,
            stream=True,
            timeout=60
        )

        response.raise_for_status()

        # 逐行读取 SSE 流
        full_content = ""

        for line in response.iter_lines():
            if not line:
                continue

            line = line.decode('utf-8')

            # SSE 格式: "data: {...}"
            if line.startswith('data: '):
                data_str = line[6:]  # 移除 "data: " 前缀

                # 检查结束标记
                if data_str.strip() == '[DONE]':
                    print("\n")
                    print("-" * 50)
                    print("✅ 流式输出完成")
                    break

                try:
                    # 解析 JSON 数据
                    chunk = json.loads(data_str)

                    # 提取内容增量
                    if chunk.get('choices'):
                        delta = chunk['choices'][0].get('delta', {})
                        content = delta.get('content', '')

                        if content:
                            print(content, end='', flush=True)
                            full_content += content

                except json.JSONDecodeError as e:
                    print(f"\n⚠️ JSON 解析错误: {e}")
                    continue

        print(f"\n📝 完整内容长度: {len(full_content)} 字符")
        return full_content

    except requests.exceptions.HTTPError as e:
        print(f"❌ HTTP 错误: {e}")
        print(f"响应内容: {e.response.text}")
    except requests.exceptions.Timeout:
        print("❌ 请求超时")
    except requests.exceptions.RequestException as e:
        print(f"❌ 请求失败: {e}")

    return None


def send_streaming_with_context():
    """带上下文的流式对话示例"""

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }

    payload = {
        "model": "kimi-k3.1-pro",
        "messages": [
            {"role": "system", "content": "你是一个简洁的助手，每次回复不超过 50 字。"},
            {"role": "user", "content": "什么是机器学习？"},
            {"role": "assistant", "content": "机器学习是让计算机通过数据学习规律，无需显式编程即可完成任务的技术。"},
            {"role": "user", "content": "它有哪些应用？"}
        ],
        "max_tokens": 200,
        "stream": True,
        "temperature": 0.7
    }

    print("\n" + "=" * 50)
    print("📝 带上下文的流式对话")
    print("=" * 50)
    print("🤖 助手回复:\n")

    try:
        response = requests.post(
            ENDPOINT,
            headers=headers,
            json=payload,
            stream=True,
            timeout=60
        )

        response.raise_for_status()

        for line in response.iter_lines():
            if not line:
                continue

            line = line.decode('utf-8')

            if line.startswith('data: '):
                data_str = line[6:]

                if data_str.strip() == '[DONE]':
                    print("\n")
                    break

                try:
                    chunk = json.loads(data_str)
                    if chunk.get('choices'):
                        content = chunk['choices'][0].get('delta', {}).get('content', '')
                        if content:
                            print(content, end='', flush=True)
                except json.JSONDecodeError:
                    continue

        print("✅ 对话完成")

    except Exception as e:
        print(f"❌ 错误: {e}")


if __name__ == "__main__":
    print("=" * 50)
    print("流式输出示例")
    print("=" * 50)

    # 示例 1: 基础流式请求
    send_streaming_request()

    # 示例 2: 带上下文的流式对话
    send_streaming_with_context()
