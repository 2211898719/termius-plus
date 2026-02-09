import {client} from "@shared/api-client";

/**
 * AI 命令生成
 * @param {string} message - 用户输入的消息
 * @returns {Promise} 返回 AI 生成的命令
 */
export function chat2Command(message) {
  return client.get("/api-admin/ai/chat2Command", {params: {message}})


}
