package com.myagent.app.ui.chat

import com.myagent.app.MainViewModel
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 聊天页面 — 编排层。
 *
 * 职责：
 * - 收集 StateFlow（messages / streamingText / isLoading / error）
 * - 组合子组件：消息列表 + 输入栏
 * - 不持有 UI 渲染细节，全部委托给子组件
 */
@Composable
fun ChatScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier,
) {
  val messages by viewModel.chatMessages.collectAsState()
  val streamingText by viewModel.chatStreamingText.collectAsState()
  val isLoading by viewModel.chatLoading.collectAsState()
  val error by viewModel.chatError.collectAsState()

  val listState = rememberLazyListState()
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // 自动滚动到最新消息
  LaunchedEffect(messages.size, streamingText) {
    if (messages.isNotEmpty()) {
      listState.animateScrollToItem(messages.size - 1)
    }
  }

  Column(
    modifier = modifier
      .clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
      ) {
        focusManager.clearFocus()
        keyboardController?.hide()
      }
  ) {
    // ── 消息列表 ──
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (messages.isEmpty() && !isLoading) {
        item {
          EmptyChatHint()
        }
      }

      items(messages, key = { it.id }) { message ->
        MessageBubble(
          message = message,
          onPlayTts = { text ->
            scope.launch {
              playTts(viewModel, context, message.id, text)
            }
          },
        )
      }

      // 流式文字
      if (!streamingText.isNullOrEmpty()) {
        item {
          StreamingTextBubble(text = streamingText!!)
        }
      }

      // 错误提示
      if (error != null) {
        item {
          Text(
            text = error!!,
            color = MaterialTheme.colorScheme.error,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 4.dp),
          )
        }
      }
    }

    // ── 输入栏（多模态） ──
    ChatInputBar(
      isLoading = isLoading,
      onSendText = { text -> viewModel.sendChat(text) },
      onSendImage = { uri -> viewModel.sendImage(uri) },
      onSendVoice = { uri -> viewModel.sendVoice(uri) },
      onAbort = { viewModel.abortChat() },
    )
  }
}

@Composable
private fun EmptyChatHint() {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(32.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = "开始和 Memento 聊天吧！\n支持文字、图片、语音",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 16.sp,
    )
  }
}

@Composable
private fun StreamingTextBubble(text: String) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    contentAlignment = Alignment.CenterStart,
  ) {
    Text(
      text = text,
      modifier = Modifier
        .clip(RoundedCornerShape(12.dp))
        .background(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(12.dp),
        )
        .padding(12.dp)
        .widthIn(max = 300.dp),
      fontSize = 15.sp,
    )
  }
}

private suspend fun playTts(
  viewModel: MainViewModel,
  context: android.content.Context,
  messageId: String,
  text: String,
) {
  try {
    val wav = withContext(Dispatchers.Default) {
      viewModel.synthesizeSpeech(text)
    }
    val tmp = File(context.cacheDir, "tts_${messageId}.wav")
    FileOutputStream(tmp).use { it.write(wav) }
    withContext(Dispatchers.Main) {
      val mp = MediaPlayer()
      mp.setDataSource(tmp.absolutePath)
      mp.prepare()
      mp.start()
      mp.setOnCompletionListener { it.release() }
    }
  } catch (_: Exception) {
    // TTS 播放失败，静默忽略
  }
}