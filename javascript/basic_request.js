/**
 * 基础 API 请求示例
 *
 * 演示如何发送简单的聊天完成请求到 Tokenra API。
 */

import fetch from 'node-fetch';

// API 配置
const API_KEY = process.env.API_KEY || 'sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG';
const API_BASE_URL = 'https://tokenra.io/v1';
const ENDPOINT = `${API_BASE_URL}/chat/completions`;

/**
 * 发送基础的聊天完成请求
 */
async function sendBasicRequest() {
  // 请求头
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${API_KEY}`
  };

  // 请求体
  const payload = {
    model: 'kimi-k3.1-pro',
    messages: [
      {
        role: 'user',
        content: 'what model'
      }
    ],
    max_tokens: 1024,
    stream: false
  };

  console.log('📤 发送请求...');
  console.log(`端点: ${ENDPOINT}`);
  console.log(`模型: ${payload.model}`);
  console.log(`消息: ${payload.messages[0].content}`);
  console.log('-'.repeat(50));

  try {
    // 发送 POST 请求
    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    // 检查响应状态
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(`HTTP ${response.status}: ${errorData.error?.message || '未知错误'}`);
    }

    // 解析响应
    const result = await response.json();

    // 打印响应信息
    console.log('✅ 请求成功!');
    console.log(`响应 ID: ${result.id || 'N/A'}`);
    console.log(`模型: ${result.model || 'N/A'}`);
    console.log('-'.repeat(50));

    // 提取助手回复
    if (result.choices && result.choices.length > 0) {
      const assistantMessage = result.choices[0].message.content;
      console.log(`🤖 助手回复:\n${assistantMessage}`);
    }

    // Token 使用统计
    if (result.usage) {
      const usage = result.usage;
      console.log('-'.repeat(50));
      console.log('📊 Token 使用:');
      console.log(`  输入 tokens: ${usage.prompt_tokens || 0}`);
      console.log(`  输出 tokens: ${usage.completion_tokens || 0}`);
      console.log(`  总计 tokens: ${usage.total_tokens || 0}`);
    }

    return result;

  } catch (error) {
    if (error.name === 'FetchError') {
      console.error('❌ 网络错误:', error.message);
    } else if (error.message.startsWith('HTTP')) {
      console.error('❌ API 错误:', error.message);
    } else {
      console.error('❌ 请求失败:', error.message);
    }
    return null;
  }
}

// 执行示例
console.log('='.repeat(50));
console.log('基础 API 请求示例');
console.log('='.repeat(50));

sendBasicRequest();
