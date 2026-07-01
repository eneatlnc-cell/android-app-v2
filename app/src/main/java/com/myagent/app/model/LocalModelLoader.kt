package com.myagent.app.model

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 本地模型加载器 — 使用 LiteRT-LM 真实推理，模型必须下载完成后才能使用。
 *
 * v2.0：LiteRT-LM 替代 llama.cpp，纯 Kotlin 实现，无需 JNI/NDK。
 * v2.1：移除 Mock 降级，模型未就绪时拒绝推理。
 */
class LocalModelLoader(
  private val context: Context,
  private var modelPath: String?,
) {
  companion object {
    private const val TAG = "LocalModelLoader"
    private const val INFERENCE_TIMEOUT_MS = 120_000L
  }

  private val engine = LiteRtEngine(context)
  @Volatile private var initialized = false

  /**
   * 初始化引擎。modelPath 为 null 时跳过，等待下载完成后 reload。
   */
  fun init() {
    if (modelPath == null) {
      Log.i(TAG, "Model not yet downloaded, waiting for download")
      return
    }
    doInitialize(modelPath!!)
  }

  /**
   * 下载完成后重新加载模型。
   */
  fun reload(newModelPath: String) {
    modelPath = newModelPath
    doInitialize(newModelPath)
  }

  private fun doInitialize(path: String) {
    try {
      if (!engine.init(path)) {
        Log.e(TAG, "Engine init failed")
        return
      }
      initialized = true
      Log.i(TAG, "LiteRT-LM engine ready: $path")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Engine init failed: ${e.message}")
      initialized = false
    }
  }

  /**
   * 尝试自动恢复：如果模型文件存在但引擎未初始化，尝试重新初始化。
   */
  private fun tryAutoRecover(): Boolean {
    if (initialized) return true
    if (modelPath == null) return false
    Log.i(TAG, "Auto-recovering: re-initializing engine from $modelPath")
    doInitialize(modelPath!!)
    return initialized
  }

  /**
   * 流式生成回复。模型未就绪时尝试自动恢复，仍失败则返回提示。
   */
  fun generate(prompt: String): Flow<String> {
    if (!tryAutoRecover()) {
      Log.w(TAG, "Model not ready, cannot generate")
      return callbackFlow {
        trySend("模型尚未下载完成，请等待下载结束后再试。")
        close()
      }
    }
    return callbackFlow {
      val inferenceScope = CoroutineScope(Dispatchers.IO)

      val inferenceJob = inferenceScope.launch {
        try {
          engine.generate(prompt).collect { chunk ->
            trySend(chunk)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Inference error: ${e.message}")
        }
        close()
      }

      val finished = withTimeoutOrNull(INFERENCE_TIMEOUT_MS) {
        inferenceJob.join()
        true
      }

      if (finished != true) {
        inferenceScope.cancel()
        Log.e(TAG, "Inference timed out after ${INFERENCE_TIMEOUT_MS}ms")
        trySend("抱歉，模型推理超时了。可能是手机内存不足，请尝试重启 App")
        // 超时后必须关闭引擎释放内存，否则模型权重持续占用
        engine.close()
        initialized = false
        close()
      }

      awaitClose { inferenceScope.cancel() }
    }
  }

  /**
   * 多模态流式生成（文本 + 图片）。
   * 图片路径传给 LiteRT-LM Conversation，Gemma 4 原生视觉编码器解析。
   */
  fun generateWithImages(prompt: String, imagePaths: List<String>): Flow<String> {
    if (!tryAutoRecover()) {
      Log.w(TAG, "Model not ready, cannot generate")
      return callbackFlow {
        trySend("模型尚未下载完成，请等待下载结束后再试。")
        close()
      }
    }
    return callbackFlow {
      val inferenceScope = CoroutineScope(Dispatchers.IO)

      val inferenceJob = inferenceScope.launch {
        try {
          engine.generateWithImages(prompt, imagePaths).collect { chunk ->
            trySend(chunk)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          Log.e(TAG, "Inference with images error: ${e.message}")
        }
        close()
      }

      val finished = withTimeoutOrNull(INFERENCE_TIMEOUT_MS * 2) { // 图片推理给更多时间
        inferenceJob.join()
        true
      }

      if (finished != true) {
        inferenceScope.cancel()
        Log.e(TAG, "Inference with images timed out")
        trySend("抱歉，模型推理超时了。可能是手机内存不足，请尝试重启 App")
        // 超时后必须关闭引擎释放内存，否则模型权重持续占用
        engine.close()
        initialized = false
        close()
      }

      awaitClose { inferenceScope.cancel() }
    }
  }

  fun isRealModelAvailable(): Boolean = initialized && modelPath != null

  fun unload() {
    if (initialized) {
      engine.close()
      initialized = false
    }
  }
}