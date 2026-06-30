package com.myagent.app.ui.chat

import android.media.MediaRecorder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import java.io.File

/**
 * 多模态输入栏 — 语音 / 图片 / 文本 / 发送。
 *
 * 状态管理完全内聚，通过回调向上传递用户意图。
 */
@Composable
fun ChatInputBar(
  isLoading: Boolean,
  onSendText: (String) -> Unit,
  onSendImage: (Uri) -> Unit,
  onSendVoice: (Uri) -> Unit,
  onAbort: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var inputText by remember { mutableStateOf("") }
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val context = LocalContext.current

  // --- 语音录制状态 ---
  var isRecording by remember { mutableStateOf(false) }
  var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
  var audioFile by remember { mutableStateOf<File?>(null) }

  // --- 图片选择器 ---
  val imagePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    uri?.let { onSendImage(it) }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // 语音入口 — 长按录音，松开发送；加载中禁用
    IconButton(
      onClick = {
        if (isLoading) return@IconButton
        if (isRecording) {
          stopRecording(recorder, audioFile) { file -> onSendVoice(Uri.fromFile(file)) }
          recorder = null
          audioFile = null
          isRecording = false
        } else {
          startRecording(context) { mr, file ->
            recorder = mr
            audioFile = file
            isRecording = true
          }
        }
      },
      modifier = Modifier.size(44.dp),
      enabled = !isLoading,
    ) {
      Icon(
        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
        contentDescription = if (isRecording) "停止录音" else "语音输入",
        tint = if (isRecording) MaterialTheme.colorScheme.error
        else if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // 图片入口 — 加载中禁用，防止多模态输入与推理冲突导致闪退
    IconButton(
      onClick = { if (!isLoading) imagePicker.launch("image/*") },
      modifier = Modifier.size(44.dp),
      enabled = !isLoading,
    ) {
      Icon(
        imageVector = Icons.Default.Image,
        contentDescription = "图片输入",
        tint = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // 文本输入
    OutlinedTextField(
      value = inputText,
      onValueChange = { inputText = it },
      placeholder = { Text("和 Memento 说点什么...") },
      modifier = Modifier.weight(1f),
      maxLines = 4,
      shape = RoundedCornerShape(24.dp),
    )

    Spacer(modifier = Modifier.width(4.dp))

    if (isLoading) {
      IconButton(onClick = onAbort) {
        Icon(
          imageVector = Icons.Default.Stop,
          contentDescription = "停止生成",
          tint = MaterialTheme.colorScheme.error,
        )
      }
    } else {
      IconButton(
        onClick = {
          val text = inputText.trim()
          if (text.isNotEmpty()) {
            onSendText(text)
            inputText = ""
            focusManager.clearFocus()
            keyboardController?.hide()
          }
        },
      ) {
        Icon(
          imageVector = Icons.Default.Send,
          contentDescription = "发送",
          tint = MaterialTheme.colorScheme.primary,
        )
      }
    }
  }
}

private fun startRecording(
  context: android.content.Context,
  onReady: (MediaRecorder, File) -> Unit,
) {
  try {
    val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
    val mr = MediaRecorder(context).apply {
      setAudioSource(MediaRecorder.AudioSource.MIC)
      setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      setAudioSamplingRate(16000)
      setAudioEncodingBitRate(32000)
      setOutputFile(file.absolutePath)
      prepare()
      start()
    }
    onReady(mr, file)
  } catch (_: Exception) {
    // 录音启动失败，静默忽略
  }
}

private fun stopRecording(
  recorder: MediaRecorder?,
  audioFile: File?,
  onComplete: (File) -> Unit,
) {
  try {
    recorder?.stop()
    recorder?.release()
  } catch (_: Exception) {}
  audioFile?.let { onComplete(it) }
}