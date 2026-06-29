package com.myagent.app.ui.chat

import com.myagent.app.chat.ChatMessage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 多模态消息气泡 — 支持文字、图片、语音、视频。
 *
 * 根据消息角色和类型自动切换布局与配色。
 */
@Composable
fun MessageBubble(
  message: ChatMessage,
  onPlayTts: (String) -> Unit = {},
) {
  val isUser = message.role == "user"

  Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 300.dp)
        .clip(
          RoundedCornerShape(
            topStart = 12.dp,
            topEnd = 12.dp,
            bottomStart = if (isUser) 12.dp else 4.dp,
            bottomEnd = if (isUser) 4.dp else 12.dp,
          ),
        )
        .background(
          if (isUser) MaterialTheme.colorScheme.primary
          else MaterialTheme.colorScheme.surfaceVariant,
        )
        .padding(12.dp),
    ) {
      MessageContent(message = message, isUser = isUser)
      // TTS 播放按钮 — 仅 AI 消息
      if (!isUser && message.content.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        IconButton(
          onClick = { onPlayTts(message.content) },
          modifier = Modifier
            .size(28.dp)
            .align(Alignment.End),
        ) {
          Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "播放语音",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun MessageContent(
  message: ChatMessage,
  isUser: Boolean,
) {
  val contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary
  else MaterialTheme.colorScheme.onSurface

  when (message.type) {
    "image" -> ImageBubble(message = message, isUser = isUser, contentColor = contentColor)
    "voice" -> VoiceBubble(message = message, contentColor = contentColor)
    "video" -> VideoBubble(message = message, isUser = isUser, contentColor = contentColor)
    else -> TextBubble(message = message, contentColor = contentColor)
  }
}

@Composable
private fun ImageBubble(
  message: ChatMessage,
  isUser: Boolean,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  if (message.attachmentUri != null) {
    AsyncImage(
      model = ImageRequest.Builder(LocalContext.current)
        .data(message.attachmentUri)
        .crossfade(true)
        .build(),
      contentDescription = "图片",
      modifier = Modifier
        .width(200.dp)
        .height(150.dp)
        .clip(RoundedCornerShape(8.dp)),
      contentScale = ContentScale.Crop,
    )
  } else {
    Box(
      modifier = Modifier
        .width(200.dp)
        .height(150.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(
          if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.Image,
        contentDescription = "图片",
        tint = contentColor.copy(alpha = if (isUser) 0.5f else 0.4f),
        modifier = Modifier.size(40.dp),
      )
    }
  }
  if (message.content.isNotEmpty()) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = message.content, color = contentColor, fontSize = 13.sp)
  }
}

@Composable
private fun VoiceBubble(
  message: ChatMessage,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(
      imageVector = Icons.Default.PlayArrow,
      contentDescription = "播放",
      tint = contentColor,
      modifier = Modifier.size(20.dp),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = if (message.content.isNotEmpty()) message.content else "语音消息",
      color = contentColor,
      fontSize = 14.sp,
    )
  }
}

@Composable
private fun VideoBubble(
  message: ChatMessage,
  isUser: Boolean,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  Box(
    modifier = Modifier
      .width(200.dp)
      .height(120.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(
        if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
      ),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = Icons.Default.PlayArrow,
      contentDescription = "播放视频",
      tint = contentColor.copy(alpha = if (isUser) 0.6f else 0.5f),
      modifier = Modifier.size(44.dp),
    )
  }
  if (message.content.isNotEmpty()) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(text = message.content, color = contentColor, fontSize = 13.sp)
  }
}

@Composable
private fun TextBubble(
  message: ChatMessage,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  if (message.content.isNotEmpty()) {
    Text(text = message.content, color = contentColor, fontSize = 15.sp)
  }
  if (message.attachmentUri != null) {
    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = "[附件]",
      color = contentColor.copy(alpha = 0.6f),
      fontSize = 12.sp,
    )
  }
}