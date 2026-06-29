package com.myagent.app.chat

import android.graphics.Bitmap
import com.myagent.app.memory.MemoryManager
import com.myagent.app.model.LocalModelLoader
import com.myagent.app.model.PersonaManager
import com.myagent.app.multimodal.MultiModalDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 聊天控制器 — 协调 LocalModelLoader、MemoryManager、PersonaManager、MultiModalDispatcher。
 *
 * v2.0 多模态路由：
 * LLM 回复中携带 [GEN_IMAGE:主题] 或 [GEN_VIDEO:主题] 标记时，
 * 自动调用 MultiModalDispatcher 生成图片/视频，追加到消息列表。
 */
class ChatController(
  private val scope: CoroutineScope,
  private val modelLoader: LocalModelLoader,
  private val memoryManager: MemoryManager,
  private val personaManager: PersonaManager,
  private val cacheDir: File,
) {
  private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
  val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

  private val _streamingText = MutableStateFlow<String?>(null)
  val streamingText: StateFlow<String?> = _streamingText.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _errorText = MutableStateFlow<String?>(null)
  val errorText: StateFlow<String?> = _errorText.asStateFlow()

  private var currentStreamJob: Job? = null

  // ── 多模态标记解析 ──

  private val imageTag = Regex("""^\[GEN_IMAGE:(.+?)]\s*""")
  private val videoTag = Regex("""^\[GEN_VIDEO:(.+?)]\s*""")

  private data class GenAction(val type: String, val prompt: String)

  private fun parseMultimodalTag(text: String): Pair<String, GenAction?> {
    imageTag.find(text)?.let { match ->
      val prompt = match.groupValues[1].trim()
      val clean = text.removeRange(match.range).trimStart()
      return clean to GenAction("image", prompt)
    }
    videoTag.find(text)?.let { match ->
      val prompt = match.groupValues[1].trim()
      val clean = text.removeRange(match.range).trimStart()
      return clean to GenAction("video", prompt)
    }
    return text to null
  }

  // ── 发送消息 ──

  fun sendMessage(message: String, attachments: List<OutgoingAttachment> = emptyList()) {
    val trimmed = message.trim()
    if (trimmed.isEmpty() && attachments.isEmpty()) return

    currentStreamJob?.cancel()

    val userMessage = ChatMessage(
      id = UUID.randomUUID().toString(),
      role = "user",
      content = trimmed,
    )
    _messages.value = _messages.value + userMessage

    memoryManager.saveMemory(role = "user", content = trimmed.ifEmpty { "[图片]" })

    _errorText.value = null
    _isLoading.value = true
    _streamingText.value = ""

    currentStreamJob = scope.launch {
      try {
        val systemPrompt = personaManager.getSystemPrompt()
        val memoryContext = memoryManager.getFullContext()
        val promptText = trimmed.ifEmpty { "请描述这张图片" }

        val fullPrompt = buildString {
          append(systemPrompt)
          if (memoryContext.isNotEmpty()) {
            append("\n\n")
            append(memoryContext)
            append("\n--- 当前对话 ---\n")
          }
          append("用户: $promptText\n")
          append("Memento: ")
        }

        // 流式推理
        val assistantId = UUID.randomUUID().toString()
        val assistantMessage = ChatMessage(
          id = assistantId,
          role = "assistant",
          content = "",
        )
        _messages.value = _messages.value + assistantMessage

        val fullResponse = StringBuilder()
        modelLoader.generate(fullPrompt).collect { chunk ->
          fullResponse.append(chunk)
          _streamingText.value = fullResponse.toString()
        }

        val rawContent = fullResponse.toString()

        // 解析多模态意图标记
        val (cleanContent, genAction) = parseMultimodalTag(rawContent)

        // 更新文字消息（去掉标记）
        _messages.value = _messages.value.map {
          if (it.id == assistantId) it.copy(content = cleanContent) else it
        }

        // 保存助手回复到记忆
        val cleaned = cleanContent.trim()
        if (cleaned.isNotEmpty() && !isLoopOutput(cleaned)) {
          memoryManager.saveMemory(role = "assistant", content = cleaned)
        }

        _streamingText.value = null
        _isLoading.value = false

        // 多模态生成
        if (genAction != null) {
          dispatchGeneration(genAction)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        _errorText.value = e.message ?: "发送失败，请重试"
        _streamingText.value = null
        _isLoading.value = false
      }
    }
  }

  /**
   * 调度多模态生成 — 图片/视频。
   */
  private suspend fun dispatchGeneration(action: GenAction) {
    when (action.type) {
      "image" -> {
        try {
          val bitmap = MultiModalDispatcher.generateImage(action.prompt)
          val file = File(cacheDir, "gen_${UUID.randomUUID()}.png")
          FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
          val imageMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "assistant",
            content = action.prompt,
            type = "image",
            attachmentUri = file.toURI().toString(),
          )
          _messages.value = _messages.value + imageMsg
        } catch (e: Exception) {
          _errorText.value = "图片生成失败: ${e.message}"
        }
      }
      "video" -> {
        val progressId = UUID.randomUUID().toString()
        val progressMsg = ChatMessage(
          id = progressId,
          role = "assistant",
          content = "正在渲染视频「${action.prompt}」，请稍候...",
        )
        _messages.value = _messages.value + progressMsg
        try {
          val videoFile = MultiModalDispatcher.renderVideo(action.prompt)
          val videoMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "assistant",
            content = action.prompt,
            type = "video",
            attachmentUri = videoFile.toURI().toString(),
          )
          _messages.value = _messages.value.map {
            if (it.id == progressId) videoMsg else it
          }
        } catch (e: Exception) {
          _messages.value = _messages.value.map {
            if (it.id == progressId) it.copy(content = "视频生成失败: ${e.message}") else it
          }
        }
      }
    }
  }

  fun sendImage(imageUri: String, caption: String = "") {
    val message = ChatMessage(
      id = UUID.randomUUID().toString(),
      role = "user",
      content = caption.ifEmpty { "图片" },
      type = "image",
      attachmentUri = imageUri,
    )
    _messages.value = _messages.value + message
    sendMessage(caption.ifEmpty { "请描述这张图片" })
  }

  fun sendVoice(audioUri: String, transcript: String = "") {
    val message = ChatMessage(
      id = UUID.randomUUID().toString(),
      role = "user",
      content = transcript.ifEmpty { "语音消息" },
      type = "voice",
      attachmentUri = audioUri,
    )
    _messages.value = _messages.value + message
    if (transcript.isNotEmpty()) {
      sendMessage(transcript)
    }
  }

  fun abort() {
    currentStreamJob?.cancel()
    currentStreamJob = null
    _streamingText.value = null
    _isLoading.value = false
  }

  fun clearMessages() {
    currentStreamJob?.cancel()
    currentStreamJob = null
    _messages.value = emptyList()
    _streamingText.value = null
    _isLoading.value = false
    _errorText.value = null
  }

  private fun isLoopOutput(text: String): Boolean {
    if (text.length < 3) return false
    val tokens = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.size < 3) return false
    val maxCount = tokens.groupingBy { it }.eachCount().values.maxOrNull() ?: 0
    return maxCount > tokens.size / 2
  }
}