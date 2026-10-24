"""
基础 API 请求示例

演示如何发送简单的聊天完成请求到 Tokenra API。
"""

import requests
import json
import os

# API 配置
API_KEY = os.getenv("API_KEY", "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG")
API_BASE_URL = "https://tokenra.io/v1"
ENDPOINT = f"{API_BASE_URL}/chat/completions"


def send_basic_request():
    """发送基础的聊天完成请求"""

    # 请求头
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }

    # 请求体
    payload = {
        "model": "kimi-k3.1-pro",
        "messages": [
            {
                "role": "user",
                "content": "what model"
            }
        ],
        "max_tokens": 1024,
        "stream": False
    }

    print("📤 发送请求...")
    print(f"端点: {ENDPOINT}")
    print(f"模型: {payload['model']}")
    print(f"消息: {payload['messages'][0]['content']}")
    print("-" * 50)

    try:
        # 发送 POST 请求
        response = requests.post(
            ENDPOINT,
            headers=headers,
            json=payload,
            timeout=30
        )

        # 检查响应状态
        response.raise_for_status()

        # 解析响应
        result = response.json()

        # 打印响应信息
        print("✅ 请求成功!")
        print(f"响应 ID: {result.get('id', 'N/A')}")
        print(f"模型: {result.get('model', 'N/A')}")
        print("-" * 50)

        # 提取助手回复
        if result.get('choices'):
            assistant_message = result['choices'][0]['message']['content']
            print(f"🤖 助手回复:\n{assistant_message}")

        # Token 使用统计
        if result.get('usage'):
            usage = result['usage']
            print("-" * 50)
            print("📊 Token 使用:")
            print(f"  输入 tokens: {usage.get('prompt_tokens', 0)}")
            print(f"  输出 tokens: {usage.get('completion_tokens', 0)}")
            print(f"  总计 tokens: {usage.get('total_tokens', 0)}")

        return result

    except requests.exceptions.HTTPError as e:
        print(f"❌ HTTP 错误: {e}")
        print(f"响应内容: {e.response.text}")
    except requests.exceptions.Timeout:
        print("❌ 请求超时")
    except requests.exceptions.RequestException as e:
        print(f"❌ 请求失败: {e}")
    except json.JSONDecodeError:
        print("❌ 响应解析失败")

    return None


if __name__ == "__main__":
    print("=" * 50)
    print("基础 API 请求示例")
    print("=" * 50)
    send_basic_request()
