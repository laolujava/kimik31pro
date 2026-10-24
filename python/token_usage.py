"""
Token 使用和计费示例

演示如何统计 Token 使用量并估算成本。
"""

import requests
import json
import os

# API 配置
API_KEY = os.getenv("API_KEY", "sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG")
API_BASE_URL = "https://tokenra.io/v1"
ENDPOINT = f"{API_BASE_URL}/chat/completions"

# 价格配置 (示例价格，请根据实际情况调整)
# 单位: USD per 1M tokens
PRICING = {
    "kimi-k3.1-pro": {
        "input": 0.5,   # $0.5 per 1M input tokens
        "output": 1.5   # $1.5 per 1M output tokens
    }
}


def calculate_cost(model, prompt_tokens, completion_tokens):
    """
    计算请求成本

    Args:
        model: 模型名称
        prompt_tokens: 输入 token 数
        completion_tokens: 输出 token 数

    Returns:
        成本（美元）
    """
    if model not in PRICING:
        print(f"⚠️ 未知模型价格: {model}")
        return 0.0

    input_cost = (prompt_tokens / 1_000_000) * PRICING[model]["input"]
    output_cost = (completion_tokens / 1_000_000) * PRICING[model]["output"]

    return input_cost + output_cost


def send_request_and_track_usage(messages, max_tokens=1024):
    """发送请求并跟踪 Token 使用"""

    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {API_KEY}"
    }

    payload = {
        "model": "kimi-k3.1-pro",
        "messages": messages,
        "max_tokens": max_tokens,
        "stream": False
    }

    try:
        response = requests.post(
            ENDPOINT,
            headers=headers,
            json=payload,
            timeout=30
        )

        response.raise_for_status()
        result = response.json()

        # 提取使用统计
        usage = result.get('usage', {})
        prompt_tokens = usage.get('prompt_tokens', 0)
        completion_tokens = usage.get('completion_tokens', 0)
        total_tokens = usage.get('total_tokens', 0)

        # 计算成本
        cost = calculate_cost(payload['model'], prompt_tokens, completion_tokens)

        # 返回结果和统计
        return {
            'content': result['choices'][0]['message']['content'],
            'usage': {
                'prompt_tokens': prompt_tokens,
                'completion_tokens': completion_tokens,
                'total_tokens': total_tokens,
                'cost_usd': cost
            }
        }

    except Exception as e:
        print(f"❌ 请求失败: {e}")
        return None


def test_single_request():
    """测试：单次请求的 Token 使用"""
    print("=" * 50)
    print("测试 1: 单次请求 Token 使用")
    print("=" * 50)

    messages = [
        {"role": "user", "content": "什么是人工智能？请用一句话回答。"}
    ]

    result = send_request_and_track_usage(messages, max_tokens=100)

    if result:
        print(f"🤖 回复: {result['content']}\n")
        print("📊 Token 使用统计:")
        print(f"  输入 tokens: {result['usage']['prompt_tokens']}")
        print(f"  输出 tokens: {result['usage']['completion_tokens']}")
        print(f"  总计 tokens: {result['usage']['total_tokens']}")
        print(f"  💰 估算成本: ${result['usage']['cost_usd']:.6f}")


def test_conversation_tracking():
    """测试：多轮对话的 Token 累计"""
    print("\n" + "=" * 50)
    print("测试 2: 多轮对话 Token 累计")
    print("=" * 50)

    conversation = [
        {"role": "user", "content": "什么是机器学习？"}
    ]

    total_cost = 0.0
    total_tokens_used = 0

    # 第一轮
    print("\n📝 第 1 轮对话:")
    result = send_request_and_track_usage(conversation, max_tokens=200)

    if result:
        print(f"🤖 {result['content']}")
        total_cost += result['usage']['cost_usd']
        total_tokens_used += result['usage']['total_tokens']

        # 添加助手回复到对话历史
        conversation.append({
            "role": "assistant",
            "content": result['content']
        })

        # 第二轮
        conversation.append({
            "role": "user",
            "content": "它有哪些应用场景？"
        })

        print("\n📝 第 2 轮对话:")
        result = send_request_and_track_usage(conversation, max_tokens=200)

        if result:
            print(f"🤖 {result['content']}")
            total_cost += result['usage']['cost_usd']
            total_tokens_used += result['usage']['total_tokens']

    print("\n" + "-" * 50)
    print("📊 累计统计:")
    print(f"  总 tokens: {total_tokens_used}")
    print(f"  💰 总成本: ${total_cost:.6f}")


def test_max_tokens_impact():
    """测试：max_tokens 参数对成本的影响"""
    print("\n" + "=" * 50)
    print("测试 3: max_tokens 对成本的影响")
    print("=" * 50)

    messages = [
        {"role": "user", "content": "介绍一下深度学习"}
    ]

    max_tokens_options = [50, 100, 500]

    for max_tokens in max_tokens_options:
        print(f"\n设置 max_tokens={max_tokens}:")

        result = send_request_and_track_usage(messages, max_tokens=max_tokens)

        if result:
            print(f"  实际输出 tokens: {result['usage']['completion_tokens']}")
            print(f"  总 tokens: {result['usage']['total_tokens']}")
            print(f"  💰 成本: ${result['usage']['cost_usd']:.6f}")


def test_prompt_optimization():
    """测试：提示词优化对成本的影响"""
    print("\n" + "=" * 50)
    print("测试 4: 提示词优化")
    print("=" * 50)

    # 冗长的提示词
    verbose_messages = [
        {
            "role": "system",
            "content": "你是一个非常有帮助的助手，你总是提供详细、准确、有见地的回答。你会考虑问题的各个方面，并给出全面的解释。"
        },
        {
            "role": "user",
            "content": "请你告诉我，人工智能这个概念到底是什么意思？"
        }
    ]

    # 简洁的提示词
    concise_messages = [
        {
            "role": "user",
            "content": "什么是人工智能？"
        }
    ]

    print("\n📝 冗长提示词:")
    result1 = send_request_and_track_usage(verbose_messages, max_tokens=100)
    if result1:
        print(f"  输入 tokens: {result1['usage']['prompt_tokens']}")
        print(f"  💰 成本: ${result1['usage']['cost_usd']:.6f}")

    print("\n📝 简洁提示词:")
    result2 = send_request_and_track_usage(concise_messages, max_tokens=100)
    if result2:
        print(f"  输入 tokens: {result2['usage']['prompt_tokens']}")
        print(f"  💰 成本: ${result2['usage']['cost_usd']:.6f}")

    if result1 and result2:
        savings = result1['usage']['prompt_tokens'] - result2['usage']['prompt_tokens']
        print(f"\n💡 节省了 {savings} 个输入 tokens")


def estimate_monthly_cost():
    """估算月度成本"""
    print("\n" + "=" * 50)
    print("测试 5: 月度成本估算")
    print("=" * 50)

    # 假设场景
    daily_requests = 1000
    avg_prompt_tokens = 50
    avg_completion_tokens = 150

    # 单次请求成本
    cost_per_request = calculate_cost(
        "kimi-k3.1-pro",
        avg_prompt_tokens,
        avg_completion_tokens
    )

    # 月度成本
    monthly_cost = cost_per_request * daily_requests * 30

    print(f"📊 假设场景:")
    print(f"  每日请求数: {daily_requests:,}")
    print(f"  平均输入 tokens: {avg_prompt_tokens}")
    print(f"  平均输出 tokens: {avg_completion_tokens}")
    print(f"\n💰 成本估算:")
    print(f"  单次请求: ${cost_per_request:.6f}")
    print(f"  每日: ${cost_per_request * daily_requests:.2f}")
    print(f"  每月: ${monthly_cost:.2f}")


if __name__ == "__main__":
    print("=" * 50)
    print("Token 使用和计费示例")
    print("=" * 50)

    test_single_request()
    test_conversation_tracking()
    test_max_tokens_impact()
    test_prompt_optimization()
    estimate_monthly_cost()

    print("\n" + "=" * 50)
    print("所有测试完成")
    print("=" * 50)
