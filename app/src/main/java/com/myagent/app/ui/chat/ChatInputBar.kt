package com.myagent.app.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * 多模态输入栏 — 加号折叠 + 按住说话（微信风格）。
 *
 * 布局：
 *   [+] [ 输入框 / 按住说话 ] [发送/停止]
 *
 * 点击加号 → 底部浮层：图片 / 语音 / 视频
 * 点击"语音" → 输入框切换为"按住说话"按钮
 * 按住说话 → 上滑取消 → 松手发送
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputBar(
  isLoading: Boolean,
  onSendText: (String) -> Unit,
  onSendImage: (Uri) -> Unit,
  onSendVideo: (Uri) -> Unit,
  onSendVoice: (Uri) -> Unit,
  onAbort: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var inputText by remember { mutableStateOf("") }
  val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  // --- 加号浮层 ---
  var showSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  // --- 语音模式 ---
  var isVoiceMode by remember { mutableStateOf(false) }
  var isRecording by remember { mutableStateOf(false) }
  var isCancelling by remember { mutableStateOf(false) }
  val recorderRef = remember { AtomicReference<MediaRecorder?>(null) }
  val audioFileRef = remember { AtomicReference<File?>(null) }
  var hasMicPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    )
  }
  val micPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasMicPermission = granted
    if (granted) {
      // 权限授予后自动进入语音模式（解决"二次确认"问题）
      isVoiceMode = true
    }
  }

  // 录音动画
  val recordingScale by animateFloatAsState(
    targetValue = if (isRecording) 1.3f else 1f,
    animationSpec = tween(200),
  )

  // --- 图片选择器 ---
  val imagePicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) onSendImage(uri)
    // 用户取消选择时不弹 Toast（是主动行为，不需要反馈）
  }

  // --- 视频选择器 ---
  val videoPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) onSendVideo(uri)
  }

  /** 等待浮层关闭后启动文件选择器 */
  fun launchAfterSheetClose(block: () -> Unit) {
    showSheet = false
    keyboardController?.hide()
    scope.launch {
      // 等待 ModalBottomSheet 完全关闭（而非硬编码 300ms）
      sheetState.hide()
      // 确认浮层已隐藏
      delay(100)
      block()
    }
  }

  // ── 加号浮层 ──
  if (showSheet) {
    ModalBottomSheet(
      onDismissRequest = { showSheet = false },
      sheetState = sheetState,
      shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 32.dp),
      ) {
        // 图片
        SheetOption(
          icon = Icons.Default.Image,
          label = "图片",
          description = "从相册选择图片",
          onClick = {
            launchAfterSheetClose { imagePicker.launch("image/*") }
          },
        )
        // 语音
        SheetOption(
          icon = Icons.Default.Mic,
          label = "语音",
          description = "按住说话，松手发送，上滑取消",
          onClick = {
            showSheet = false
            if (!hasMicPermission) {
              micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
              // 权限回调中自动进入语音模式，此处不需要额外处理
            } else {
              isVoiceMode = true
            }
          },
        )
        // 视频
        SheetOption(
          icon = Icons.Default.Videocam,
          label = "视频",
          description = "从相册选择视频",
          onClick = {
            launchAfterSheetClose { videoPicker.launch("video/*") }
          },
        )
      }
    }
  }

  // ── 主输入栏 ──
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // 加号按钮
    IconButton(
      onClick = { showSheet = true },
      modifier = Modifier.size(44.dp),
      enabled = !isLoading,
    ) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = "更多",
        tint = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    if (isVoiceMode) {
      // ── 语音模式：按住说话（微信风格）──
      Box(
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(
            when {
              isCancelling -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
              isRecording -> MaterialTheme.colorScheme.errorContainer
              else -> MaterialTheme.colorScheme.surfaceVariant
            },
          )
          // pointerInput key 绑定 hasMicPermission，确保权限变化后闭包捕获最新值
          .pointerInput(hasMicPermission) {
            awaitEachGesture {
              val down = awaitFirstDown(requireUnconsumed = false)
              down.consume()

              // 权限检查（使用最新值，因为 key 已绑定 hasMicPermission）
              if (!hasMicPermission) {
                Toast.makeText(context, "请先授予麦克风权限", Toast.LENGTH_SHORT).show()
                // 等待手势结束
                while (true) {
                  val e = awaitPointerEvent()
                  e.changes.forEach { it.consume() }
                  if (e.changes.all { !it.pressed }) break
                }
                return@awaitEachGesture
              }

              // 按下 → 开始录音
              isRecording = true
              isCancelling = false
              var cancelled = false
              var mr: MediaRecorder? = null
              var file: File? = null

              try {
                file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
                mr = createRecorder(context, file)
                // prepare/start 在 IO 线程执行，避免主线程 ANR
                withContext(Dispatchers.IO) {
                  mr!!.prepare()
                  mr!!.start()
                }
                recorderRef.set(mr)
                audioFileRef.set(file)
              } catch (e: Exception) {
                Log.e("ChatInputBar", "Recording start failed: ${e.message}", e)
                isRecording = false
                isVoiceMode = false
                Toast.makeText(context, "录音启动失败，请重试", Toast.LENGTH_SHORT).show()
                try { mr?.release() } catch (_: Exception) {}
                // 耗尽剩余手势事件
                while (true) {
                  val e = awaitPointerEvent()
                  e.changes.forEach { it.consume() }
                  if (e.changes.all { !it.pressed }) break
                }
                return@awaitEachGesture
              }

              // 跟踪拖动：上滑超过阈值 → 取消
              val cancelThreshold = 120f
              var dragOffset = 0f
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull() ?: break
                dragOffset = change.position.y - down.position.y
                cancelled = dragOffset < -cancelThreshold
                isCancelling = cancelled
                change.consume()
                if (!change.pressed) break
              }

              // 松手 → 停止录音
              stopRecording(
                recorder = recorderRef.getAndSet(null),
                audioFile = audioFileRef.getAndSet(null),
                cancelled = cancelled,
              ) { f ->
                onSendVoice(Uri.fromFile(f))
              }
              isRecording = false
              isCancelling = false
              if (!cancelled) {
                isVoiceMode = false
              }
            }
          },
        contentAlignment = Alignment.Center,
      ) {
        if (isCancelling) {
          Text(
            text = "松开取消",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
          )
        } else {
          Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "按住说话",
            tint = if (isRecording) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .size(24.dp)
              .scale(recordingScale),
          )
        }
      }
      // 返回文本按钮
      IconButton(
        onClick = { isVoiceMode = false },
        modifier = Modifier.size(44.dp),
      ) {
        Icon(
          imageVector = Icons.Default.Stop,
          contentDescription = "返回键盘",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      // ── 文本模式 ──
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
}

// ── 浮层选项 ──

@Composable
private fun SheetOption(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  label: String,
  description: String,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = label,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(24.dp),
      )
    }
    Spacer(modifier = Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    // 点击区域
    IconButton(onClick = onClick) {
      Icon(
        imageVector = Icons.Default.Add,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
  }
}

// ── 录音工具函数 ──

/** 创建并配置 MediaRecorder，不调用 prepare/start（由调用方在 IO 线程执行） */
private fun createRecorder(
  context: android.content.Context,
  outputFile: File,
): MediaRecorder {
  return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    MediaRecorder(context)
  } else {
    @Suppress("DEPRECATION")
    MediaRecorder()
  }).apply {
    setAudioSource(MediaRecorder.AudioSource.MIC)
    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
    setAudioSamplingRate(16000)
    setAudioEncodingBitRate(32000)
    setOutputFile(outputFile.absolutePath)
  }
}

/**
 * 停止录音。
 * @param cancelled 是否用户主动取消（上滑取消），取消时丢弃录音文件
 */
private fun stopRecording(
  recorder: MediaRecorder?,
  audioFile: File?,
  cancelled: Boolean,
  onComplete: (File) -> Unit,
) {
  try {
    recorder?.stop()
  } catch (e: Exception) {
    Log.e("ChatInputBar", "Recorder stop failed: ${e.message}", e)
  }
  try {
    recorder?.release()
  } catch (e: Exception) {
    Log.e("ChatInputBar", "Recorder release failed: ${e.message}", e)
  }

  if (cancelled) {
    // 上滑取消：删除录音文件
    audioFile?.delete()
    return
  }

  // 仅在文件有效时发送
  if (audioFile != null && audioFile.exists() && audioFile.length() > 0) {
    onComplete(audioFile)
  } else {
    audioFile?.delete()
    Log.w("ChatInputBar", "Recording file invalid, discarded")
  }
}