/**
 * 流式输出示例
 *
 * 演示如何使用 Server-Sent Events (SSE) 接收流式响应。
 */

import fetch from 'node-fetch';

// API 配置
const API_KEY = process.env.API_KEY || 'sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG';
const API_BASE_URL = 'https://tokenra.io/v1';
const ENDPOINT = `${API_BASE_URL}/chat/completions`;

/**
 * 发送流式请求并实时打印输出
 */
async function sendStreamingRequest() {
  // 请求头
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${API_KEY}`
  };

  // 请求体 - 注意 stream=true
  const payload = {
    model: 'kimi-k3.1-pro',
    messages: [
      {
        role: 'user',
        content: '请用三句话介绍人工智能'
      }
    ],
    max_tokens: 1024,
    stream: true  // 启用流式输出
  };

  console.log('📤 发送流式请求...');
  console.log(`模型: ${payload.model}`);
  console.log(`消息: ${payload.messages[0].content}`);
  console.log('-'.repeat(50));
  console.log('🤖 助手回复 (流式输出):\n');

  try {
    // 发送请求
    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(`HTTP ${response.status}: ${errorData.error?.message || '未知错误'}`);
    }

    let fullContent = '';

    // 读取流式响应
    const reader = response.body;
    let buffer = '';

    for await (const chunk of reader) {
      buffer += chunk.toString('utf-8');

      // 按行分割
      const lines = buffer.split('\n');
      buffer = lines.pop() || '';  // 保留最后不完整的行

      for (const line of lines) {
        if (!line.trim()) continue;

        // SSE 格式: "data: {...}"
        if (line.startsWith('data: ')) {
          const dataStr = line.slice(6);

          // 检查结束标记
          if (dataStr.trim() === '[DONE]') {
            console.log('\n');
            console.log('-'.repeat(50));
            console.log('✅ 流式输出完成');
            break;
          }

          try {
            // 解析 JSON 数据
            const data = JSON.parse(dataStr);

            // 提取内容增量
            if (data.choices && data.choices.length > 0) {
              const delta = data.choices[0].delta || {};
              const content = delta.content || '';

              if (content) {
                process.stdout.write(content);
                fullContent += content;
              }
            }

          } catch (e) {
            // 跳过无效的 JSON
          }
        }
      }
    }

    console.log(`\n📝 完整内容长度: ${fullContent.length} 字符`);
    return fullContent;

  } catch (error) {
    console.error('❌ 请求失败:', error.message);
    return null;
  }
}

/**
 * 带上下文的流式对话示例
 */
async function sendStreamingWithContext() {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${API_KEY}`
  };

  const payload = {
    model: 'kimi-k3.1-pro',
    messages: [
      { role: 'system', content: '你是一个简洁的助手，每次回复不超过 50 字。' },
      { role: 'user', content: '什么是机器学习？' },
      { role: 'assistant', content: '机器学习是让计算机通过数据学习规律，无需显式编程即可完成任务的技术。' },
      { role: 'user', content: '它有哪些应用？' }
    ],
    max_tokens: 200,
    stream: true,
    temperature: 0.7
  };

  console.log('\n' + '='.repeat(50));
  console.log('📝 带上下文的流式对话');
  console.log('='.repeat(50));
  console.log('🤖 助手回复:\n');

  try {
    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    let buffer = '';

    for await (const chunk of response.body) {
      buffer += chunk.toString('utf-8');

      const lines = buffer.split('\n');
      buffer = lines.pop() || '';

      for (const line of lines) {
        if (!line.trim() || !line.startsWith('data: ')) continue;

        const dataStr = line.slice(6);

        if (dataStr.trim() === '[DONE]') {
          console.log('\n');
          break;
        }

        try {
          const data = JSON.parse(dataStr);
          const content = data.choices?.[0]?.delta?.content || '';
          if (content) {
            process.stdout.write(content);
          }
        } catch (e) {
          // 跳过无效 JSON
        }
      }
    }

    console.log('✅ 对话完成');

  } catch (error) {
    console.error('❌ 错误:', error.message);
  }
}

// 执行示例
console.log('='.repeat(50));
console.log('流式输出示例');
console.log('='.repeat(50));

(async () => {
  // 示例 1: 基础流式请求
  await sendStreamingRequest();

  // 示例 2: 带上下文的流式对话
  await sendStreamingWithContext();
})();
