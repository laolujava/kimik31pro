/**
 * Token 使用和计费示例
 *
 * 演示如何统计 Token 使用量并估算成本。
 */

import fetch from 'node-fetch';

// API 配置
const API_KEY = process.env.API_KEY || 'sk-RTvHnOzHYiuYnVUOkuK7r6YTsQDkm86dT3K5lgZ21yMYHDwG';
const API_BASE_URL = 'https://tokenra.io/v1';
const ENDPOINT = `${API_BASE_URL}/chat/completions`;

// 价格配置 (示例价格，请根据实际情况调整)
// 单位: USD per 1M tokens
const PRICING = {
  'kimi-k3.1-pro': {
    input: 0.5,   // $0.5 per 1M input tokens
    output: 1.5   // $1.5 per 1M output tokens
  }
};

/**
 * 计算请求成本
 *
 * @param {string} model - 模型名称
 * @param {number} promptTokens - 输入 token 数
 * @param {number} completionTokens - 输出 token 数
 * @returns {number} 成本（美元）
 */
function calculateCost(model, promptTokens, completionTokens) {
  if (!(model in PRICING)) {
    console.log(`⚠️ 未知模型价格: ${model}`);
    return 0.0;
  }

  const inputCost = (promptTokens / 1_000_000) * PRICING[model].input;
  const outputCost = (completionTokens / 1_000_000) * PRICING[model].output;

  return inputCost + outputCost;
}

/**
 * 发送请求并跟踪 Token 使用
 */
async function sendRequestAndTrackUsage(messages, maxTokens = 1024) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${API_KEY}`
  };

  const payload = {
    model: 'kimi-k3.1-pro',
    messages: messages,
    max_tokens: maxTokens,
    stream: false
  };

  try {
    const response = await fetch(ENDPOINT, {
      method: 'POST',
      headers: headers,
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }

    const result = await response.json();

    // 提取使用统计
    const usage = result.usage || {};
    const promptTokens = usage.prompt_tokens || 0;
    const completionTokens = usage.completion_tokens || 0;
    const totalTokens = usage.total_tokens || 0;

    // 计算成本
    const cost = calculateCost(payload.model, promptTokens, completionTokens);

    // 返回结果和统计
    return {
      content: result.choices[0].message.content,
      usage: {
        promptTokens,
        completionTokens,
        totalTokens,
        costUsd: cost
      }
    };

  } catch (error) {
    console.error(`❌ 请求失败: ${error.message}`);
    return null;
  }
}

/**
 * 测试：单次请求的 Token 使用
 */
async function testSingleRequest() {
  console.log('='.repeat(50));
  console.log('测试 1: 单次请求 Token 使用');
  console.log('='.repeat(50));

  const messages = [
    { role: 'user', content: '什么是人工智能？请用一句话回答。' }
  ];

  const result = await sendRequestAndTrackUsage(messages, 100);

  if (result) {
    console.log(`🤖 回复: ${result.content}\n`);
    console.log('📊 Token 使用统计:');
    console.log(`  输入 tokens: ${result.usage.promptTokens}`);
    console.log(`  输出 tokens: ${result.usage.completionTokens}`);
    console.log(`  总计 tokens: ${result.usage.totalTokens}`);
    console.log(`  💰 估算成本: $${result.usage.costUsd.toFixed(6)}`);
  }
}

/**
 * 测试：多轮对话的 Token 累计
 */
async function testConversationTracking() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 2: 多轮对话 Token 累计');
  console.log('='.repeat(50));

  const conversation = [
    { role: 'user', content: '什么是机器学习？' }
  ];

  let totalCost = 0.0;
  let totalTokensUsed = 0;

  // 第一轮
  console.log('\n📝 第 1 轮对话:');
  let result = await sendRequestAndTrackUsage(conversation, 200);

  if (result) {
    console.log(`🤖 ${result.content}`);
    totalCost += result.usage.costUsd;
    totalTokensUsed += result.usage.totalTokens;

    // 添加助手回复到对话历史
    conversation.push({
      role: 'assistant',
      content: result.content
    });

    // 第二轮
    conversation.push({
      role: 'user',
      content: '它有哪些应用场景？'
    });

    console.log('\n📝 第 2 轮对话:');
    result = await sendRequestAndTrackUsage(conversation, 200);

    if (result) {
      console.log(`🤖 ${result.content}`);
      totalCost += result.usage.costUsd;
      totalTokensUsed += result.usage.totalTokens;
    }
  }

  console.log('\n' + '-'.repeat(50));
  console.log('📊 累计统计:');
  console.log(`  总 tokens: ${totalTokensUsed}`);
  console.log(`  💰 总成本: $${totalCost.toFixed(6)}`);
}

/**
 * 测试：max_tokens 参数对成本的影响
 */
async function testMaxTokensImpact() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 3: max_tokens 对成本的影响');
  console.log('='.repeat(50));

  const messages = [
    { role: 'user', content: '介绍一下深度学习' }
  ];

  const maxTokensOptions = [50, 100, 500];

  for (const maxTokens of maxTokensOptions) {
    console.log(`\n设置 max_tokens=${maxTokens}:`);

    const result = await sendRequestAndTrackUsage(messages, maxTokens);

    if (result) {
      console.log(`  实际输出 tokens: ${result.usage.completionTokens}`);
      console.log(`  总 tokens: ${result.usage.totalTokens}`);
      console.log(`  💰 成本: $${result.usage.costUsd.toFixed(6)}`);
    }
  }
}

/**
 * 测试：提示词优化对成本的影响
 */
async function testPromptOptimization() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 4: 提示词优化');
  console.log('='.repeat(50));

  // 冗长的提示词
  const verboseMessages = [
    {
      role: 'system',
      content: '你是一个非常有帮助的助手，你总是提供详细、准确、有见地的回答。你会考虑问题的各个方面，并给出全面的解释。'
    },
    {
      role: 'user',
      content: '请你告诉我，人工智能这个概念到底是什么意思？'
    }
  ];

  // 简洁的提示词
  const conciseMessages = [
    {
      role: 'user',
      content: '什么是人工智能？'
    }
  ];

  console.log('\n📝 冗长提示词:');
  const result1 = await sendRequestAndTrackUsage(verboseMessages, 100);
  if (result1) {
    console.log(`  输入 tokens: ${result1.usage.promptTokens}`);
    console.log(`  💰 成本: $${result1.usage.costUsd.toFixed(6)}`);
  }

  console.log('\n📝 简洁提示词:');
  const result2 = await sendRequestAndTrackUsage(conciseMessages, 100);
  if (result2) {
    console.log(`  输入 tokens: ${result2.usage.promptTokens}`);
    console.log(`  💰 成本: $${result2.usage.costUsd.toFixed(6)}`);
  }

  if (result1 && result2) {
    const savings = result1.usage.promptTokens - result2.usage.promptTokens;
    console.log(`\n💡 节省了 ${savings} 个输入 tokens`);
  }
}

/**
 * 估算月度成本
 */
function estimateMonthlyCost() {
  console.log('\n' + '='.repeat(50));
  console.log('测试 5: 月度成本估算');
  console.log('='.repeat(50));

  // 假设场景
  const dailyRequests = 1000;
  const avgPromptTokens = 50;
  const avgCompletionTokens = 150;

  // 单次请求成本
  const costPerRequest = calculateCost(
    'kimi-k3.1-pro',
    avgPromptTokens,
    avgCompletionTokens
  );

  // 月度成本
  const monthlyCost = costPerRequest * dailyRequests * 30;

  console.log('📊 假设场景:');
  console.log(`  每日请求数: ${dailyRequests.toLocaleString()}`);
  console.log(`  平均输入 tokens: ${avgPromptTokens}`);
  console.log(`  平均输出 tokens: ${avgCompletionTokens}`);
  console.log('\n💰 成本估算:');
  console.log(`  单次请求: $${costPerRequest.toFixed(6)}`);
  console.log(`  每日: $${(costPerRequest * dailyRequests).toFixed(2)}`);
  console.log(`  每月: $${monthlyCost.toFixed(2)}`);
}

// 执行所有测试
console.log('='.repeat(50));
console.log('Token 使用和计费示例');
console.log('='.repeat(50));

(async () => {
  await testSingleRequest();
  await testConversationTracking();
  await testMaxTokensImpact();
  await testPromptOptimization();
  estimateMonthlyCost();

  console.log('\n' + '='.repeat(50));
  console.log('所有测试完成');
  console.log('='.repeat(50));
})();
