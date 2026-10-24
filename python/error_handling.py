"""
错误处理示例

演示如何正确处理各种 API 错误情况。
"""

import requests
import json
import time
import os

# API 配置
API_KEY = os.getenv("API_KEY", "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG")
API_BASE_URL = "https://tokenra.io/v1"
ENDPOINT = f"{API_BASE_URL}/chat/completions"


class APIError(Exception):
    """API 错误基类"""
    pass


class AuthenticationError(APIError):
    """认证错误 (401)"""
    pass


class RateLimitError(APIError):
    """速率限制错误 (429)"""
    pass


class ServerError(APIError):
    """服务器错误 (5xx)"""
    pass


def send_request_with_retry(payload, max_retries=3, initial_delay=1):
    """
    带重试机制的请求发送

    Args:
        payload: 请求体
        max_retries: 最大重试次数
        initial_delay: 初始延迟时间（秒）

    Returns:
        响应数据或 None
    """

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }

    for attempt in range(max_retries + 1):
        try:
            print(f"📤 尝试 {attempt + 1}/{max_retries + 1}...")

            response = requests.post(
                ENDPOINT,
                headers=headers,
                json=payload,
                timeout=30
            )

            # 根据状态码处理
            if response.status_code == 200:
                print("✅ 请求成功")
                return response.json()

            elif response.status_code == 401:
                # 认证错误，不应重试
                error_data = response.json()
                raise AuthenticationError(f"认证失败: {error_data.get('error', {}).get('message', '无效的 API Key')}")

            elif response.status_code == 429:
                # 速率限制，应该重试
                print("⚠️ 速率限制，等待后重试...")
                if attempt < max_retries:
                    delay = initial_delay * (2 ** attempt)  # 指数退避
                    time.sleep(delay)
                    continue
                else:
                    raise RateLimitError("超过速率限制")

            elif 500 <= response.status_code < 600:
                # 服务器错误，应该重试
                print(f"⚠️ 服务器错误 ({response.status_code})，等待后重试...")
                if attempt < max_retries:
                    delay = initial_delay * (2 ** attempt)
                    time.sleep(delay)
                    continue
                else:
                    raise ServerError(f"服务器错误: {response.status_code}")

            else:
                # 其他错误
                error_data = response.json()
                error_msg = error_data.get('error', {}).get('message', '未知错误')
                raise APIError(f"API 错误 ({response.status_code}): {error_msg}")

        except requests.exceptions.Timeout:
            print("⚠️ 请求超时")
            if attempt < max_retries:
                delay = initial_delay * (2 ** attempt)
                print(f"等待 {delay} 秒后重试...")
                time.sleep(delay)
                continue
            else:
                raise APIError("请求超时，已达最大重试次数")

        except requests.exceptions.ConnectionError as e:
            print(f"⚠️ 连接错误: {e}")
            if attempt < max_retries:
                delay = initial_delay * (2 ** attempt)
                print(f"等待 {delay} 秒后重试...")
                time.sleep(delay)
                continue
            else:
                raise APIError("连接失败，已达最大重试次数")

        except (AuthenticationError, json.JSONDecodeError) as e:
            # 这些错误不应重试
            raise

    return None


def test_successful_request():
    """测试：成功的请求"""
    print("\n" + "=" * 50)
    print("测试 1: 成功的请求")
    print("=" * 50)

    payload = {
        "model": "kimi-k3.1-pro",
        "messages": [{"role": "user", "content": "Hello"}],
        "max_tokens": 50
    }

    try:
        result = send_request_with_retry(payload)
        if result:
            content = result['choices'][0]['message']['content']
            print(f"🤖 回复: {content}")
    except Exception as e:
        print(f"❌ 错误: {e}")


def test_invalid_api_key():
    """测试：无效的 API Key"""
    print("\n" + "=" * 50)
    print("测试 2: 无效的 API Key")
    print("=" * 50)

    # 临时使用无效的 API Key
    global API_KEY
    original_key = API_KEY
    API_KEY = "invalid_key_12345"

    payload = {
        "model": "kimi-k3.1-pro",
        "messages": [{"role": "user", "content": "Hello"}],
        "max_tokens": 50
    }

    try:
        result = send_request_with_retry(payload, max_retries=0)
    except AuthenticationError as e:
        print(f"✅ 正确捕获认证错误: {e}")
    except Exception as e:
        print(f"⚠️ 其他错误: {e}")
    finally:
        API_KEY = original_key


def test_invalid_model():
    """测试：无效的模型名称"""
    print("\n" + "=" * 50)
    print("测试 3: 无效的模型名称")
    print("=" * 50)

    payload = {
        "model": "non-existent-model-xyz",
        "messages": [{"role": "user", "content": "Hello"}],
        "max_tokens": 50
    }

    try:
        result = send_request_with_retry(payload, max_retries=1)
    except APIError as e:
        print(f"✅ 正确捕获 API 错误: {e}")
    except Exception as e:
        print(f"⚠️ 其他错误: {e}")


def test_timeout_handling():
    """测试：超时处理"""
    print("\n" + "=" * 50)
    print("测试 4: 超时处理")
    print("=" * 50)

    payload = {
        "model": "kimi-k3.1-pro",
        "messages": [{"role": "user", "content": "Hello"}],
        "max_tokens": 50
    }

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }

    try:
        print("设置极短超时时间 (0.001 秒)...")
        response = requests.post(
            ENDPOINT,
            headers=headers,
            json=payload,
            timeout=0.001  # 极短超时，必定失败
        )
    except requests.exceptions.Timeout:
        print("✅ 正确捕获超时错误")
    except Exception as e:
        print(f"⚠️ 其他错误: {e}")


def test_malformed_request():
    """测试：格式错误的请求"""
    print("\n" + "=" * 50)
    print("测试 5: 格式错误的请求")
    print("=" * 50)

    # 缺少必需字段
    payload = {
        "model": "kimi-k3.1-pro",
        # 缺少 messages 字段
        "max_tokens": 50
    }

    try:
        result = send_request_with_retry(payload, max_retries=0)
    except APIError as e:
        print(f"✅ 正确捕获 API 错误: {e}")
    except Exception as e:
        print(f"⚠️ 其他错误: {e}")


if __name__ == "__main__":
    print("=" * 50)
    print("错误处理示例")
    print("=" * 50)

    # 运行所有测试
    test_successful_request()
    test_invalid_api_key()
    test_invalid_model()
    test_timeout_handling()
    test_malformed_request()

    print("\n" + "=" * 50)
    print("所有测试完成")
    print("=" * 50)
