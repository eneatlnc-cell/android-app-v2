package com.myagent.app.chat

/**
 * 聊天消息数据模型。
 *
 * 支持多模态内容：
 * - type: "text" | "image" | "video" | "mixed"
 * - attachmentUri: 附件本地路径（图片/语音文件）
 * - attachmentMimeType: 附件 MIME 类型
 */
data class ChatMessage(
  val id: String,
  val role: String, // "user" 或 "assistant"
  val content: String,
  val timestampMs: Long = System.currentTimeMillis(),
  val type: String = "text",
  val attachmentUri: String? = null,
  val attachmentMimeType: String? = null,
  val localPath: String? = null, // 本地文件绝对路径，用于保存到相册/打开系统播放器
)

/**
 * 发送中的消息附件
 */
data class OutgoingAttachment(
  val type: String,
  val mimeType: String,
  val fileName: String,
  val base64: String,
)