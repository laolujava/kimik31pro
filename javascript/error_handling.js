/**
 * 错误处理示例
 *
 * 演示如何正确处理各种 API 错误情况。
 */

import fetch from 'node-fetch';

// API 配置
const API_KEY = process.env.API_KEY || 'sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG';
const API_BASE_URL = 'https://tokenra.io/v1';
const ENDPOINT = `${API_BASE_URL}/chat/completions`;

/**
 * 自定义错误类
 */
class APIError extends Error {
  constructor(message, statusCode) {
    super(message);
    this.name = 'APIError';
    this.statusCode = statusCode;
  }
}

class AuthenticationError extends APIError {
  constructor(message) {
    super(message, 401);
    this.name = 'AuthenticationError';
  }
}

class RateLimitError extends APIError {
  constructor(message) {
    super(message, 429);
    this.name = 'RateLimitError';
  }
}

class ServerError extends APIError {
  constructor(message, statusCode) {
    super(message, statusCode);
    this.name = 'ServerError';
  }
}

/**
 * 延迟函数
 */
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/**
 * 带重试机制的请求发送
 *
 * @param {Object} payload - 请求体
 * @param {number} maxRetries - 最大重试次数
 * @param {number} initialDelay - 初始延迟时间（毫秒）
 * @returns {Promise<Object|null>} 响应数据或 null
 */
async function sendRequestWithRetry(payload, maxRetries = 3, initialDelay = 1000) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${API_KEY}`
  };

  for (let attempt = 0; attempt <= maxRetries; attempt++) {
    try {
      console.log(`📤 尝试 ${attempt + 1}/${maxRetries + 1}...`);

      const response = await fetch(ENDPOINT, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(payload)
      });

      // 根据状态码处理
      if (response.ok) {
        console.log('✅ 请求成功');
        return await response.json();
      }

      const errorData = await response.json();
      const errorMessage = errorData.error?.message || '未知错误';

      if (response.status === 401) {
        // 认证错误，不应重试
        throw new AuthenticationError(`认证失败: ${errorMessage}`);
      }

      if (response.status === 429) {
        // 速率限制，应该重试
        console.log('⚠️ 速率限制，等待后重试...');
        if (attempt < maxRetries) {
          const delay = initialDelay * Math.pow(2, attempt);  // 指数退避
          await sleep(delay);
          continue;
        } else {
          throw new RateLimitError('超过速率限制');
        }
      }

      if (response.status >= 500) {
        // 服务器错误，应该重试
        console.log(`⚠️ 服务器错误 (${response.status})，等待后重试...`);
        if (attempt < maxRetries) {
          const delay = initialDelay * Math.pow(2, attempt);
          await sleep(delay);
          continue;
        } else {
          throw new ServerError(`服务器错误: ${response.status}`, response.status);
        }
      }

      // 其他错误
      throw new APIError(`API 错误 (${response.status}): ${errorMessage}`, response.status);

    } catch (error) {
      // 不可重试的错误
      if (error instanceof AuthenticationError ||
          error instanceof APIError && error.statusCode < 500) {
        throw error;
      }

      // 网络错误，可以重试
      if (error.name === 'FetchError' && attempt < maxRetries) {
        console.log(`⚠️ 网络错误: ${error.message}`);
        const delay = initialDelay * Math.pow(2, attempt);
        console.log(`等待 ${delay}ms 后重试...`);
        await sleep(delay);
        continue;
      }

      // 达到最大重试次数
      if (attempt >= maxRetries) {
        throw error;
      }
    }
  }

  return null;
}

/**
 * 测试：成功的请求
 */
async function testSuccessfulRequest() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 1: 成功的请求');
  console.log('='.repeat(50));

  const payload = {
    model: 'kimi-k3.1-pro',
    messages: [{ role: 'user', content: 'Hello' }],
    max_tokens: 50
  };

  try {
    const result = await sendRequestWithRetry(payload);
    if (result) {
      const content = result.choices[0].message.content;
      console.log(`🤖 回复: ${content}`);
    }
  } catch (error) {
    console.log(`❌ 错误: ${error.message}`);
  }
}

/**
 * 测试：无效的 API Key
 */
async function testInvalidApiKey() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 2: 无效的 API Key');
  console.log('='.repeat(50));

  // 临时使用无效的 API Key
  const originalKey = API_KEY;
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': 'Bearer invalid_key_12345'
  };

  const payload = {
    model: 'kimi-k3.1-pro',
    messages: [{ role: 'user', content: 'Hello' }],
    max_tokens: 50
  };

  try {
    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new AuthenticationError('认证失败');
    }

  } catch (error) {
    if (error instanceof AuthenticationError) {
      console.log(`✅ 正确捕获认证错误: ${error.message}`);
    } else {
      console.log(`⚠️ 其他错误: ${error.message}`);
    }
  }
}

/**
 * 测试：无效的模型名称
 */
async function testInvalidModel() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 3: 无效的模型名称');
  console.log('='.repeat(50));

  const payload = {
    model: 'non-existent-model-xyz',
    messages: [{ role: 'user', content: 'Hello' }],
    max_tokens: 50
  };

  try {
    await sendRequestWithRetry(payload, 1);
  } catch (error) {
    if (error instanceof APIError) {
      console.log(`✅ 正确捕获 API 错误: ${error.message}`);
    } else {
      console.log(`⚠️ 其他错误: ${error.message}`);
    }
  }
}

/**
 * 测试：超时处理
 */
async function testTimeoutHandling() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 4: 超时处理');
  console.log('='.repeat(50));

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${API_KEY}`
  };

  const payload = {
    model: 'kimi-k3.1-pro',
    messages: [{ role: 'user', content: 'Hello' }],
    max_tokens: 50
  };

  try {
    console.log('设置极短超时时间 (1ms)...');

    // 使用 AbortController 实现超时
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 1);

    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload),
      signal: controller.signal
    });

    clearTimeout(timeout);

  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('✅ 正确捕获超时错误');
    } else {
      console.log(`⚠️ 其他错误: ${error.message}`);
    }
  }
}

/**
 * 测试：格式错误的请求
 */
async function testMalformedRequest() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 5: 格式错误的请求');
  console.log('='.repeat(50));

  // 缺少必需字段
  const payload = {
    model: 'kimi-k3.1-pro',
    // 缺少 messages 字段
    max_tokens: 50
  };

  try {
    await sendRequestWithRetry(payload, 0);
  } catch (error) {
    if (error instanceof APIError) {
      console.log(`✅ 正确捕获 API 错误: ${error.message}`);
    } else {
      console.log(`⚠️ 其他错误: ${error.message}`);
    }
  }
}

// 执行所有测试
console.log('='.repeat(50));
console.log('错误处理示例');
console.log('='.repeat(50));

(async () => {
  await testSuccessfulRequest();
  await testInvalidApiKey();
  await testInvalidModel();
  await testTimeoutHandling();
  await testMalformedRequest();

  console.log('\n' + '='.repeat(50));
  console.log('所有测试完成');
  console.log('='.repeat(50));
})();
