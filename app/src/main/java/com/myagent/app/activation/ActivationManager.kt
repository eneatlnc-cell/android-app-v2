package com.myagent.app.activation

import android.content.Context
import android.util.Log
import com.myagent.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 激活管理器 — 激活状态持久化到外部存储（清除数据不丢失）。
 *
 * 两层策略：
 * 1. 在线模式（AuthApi.isOnline = true）→ 服务端校验激活码，返回 token
 * 2. 离线模式（AuthApi.isOnline = false）→ 硬编码激活码校验，token 为空
 *
 * Token 存储到外部存储，覆盖安装后仍有效。
 * Debug 版本自动激活，跳过激活码输入。
 */
class ActivationManager(context: Context) {
  companion object {
    private const val TAG = "ActivationManager"
    private const val ACTIVATION_DIR = "activation"
    private const val ACTIVATION_FILE = "activated.txt"
    private const val TOKEN_FILE = "auth_token.txt"

    /** 离线激活码（仅 AuthApi 不可用时生效） */
    private val OFFLINE_CODES = setOf(
      "LINGJI2024",
      "MYAGENT-V2",
      "LINGJI-PRO",
      "MEMENTO2024",
      "MEMENTO-V1",
    )
  }

  private val appContext = context.applicationContext

  /**
   * 检查是否已激活。Debug 版本始终返回 true。
   */
  fun isActivated(): Boolean {
    if (BuildConfig.DEBUG) return true
    return getActivationFile().exists()
  }

  /**
   * 尝试验证激活码。在线优先，离线兜底。
   *
   * @return true 表示激活成功
   */
  suspend fun activate(code: String): Boolean = withContext(Dispatchers.IO) {
    if (BuildConfig.DEBUG) return@withContext true
    val trimmed = code.trim()
    if (trimmed.isEmpty()) return@withContext false

    // 第 1 层：在线校验
    if (AuthApi.isOnline) {
      val result = AuthApi.activate(trimmed)
      if (result.success && result.token.isNotBlank()) {
        saveActivation(trimmed)
        saveToken(result.token)
        Log.i(TAG, "Online activation success, token expires in ${result.expiresIn}s")
        return@withContext true
      }
      Log.w(TAG, "Online activation failed: ${result.error}, falling back to offline")
    }

    // 第 2 层：离线兜底
    if (trimmed in OFFLINE_CODES) {
      saveActivation(trimmed)
      Log.i(TAG, "Offline activation success")
      return@withContext true
    }

    Log.w(TAG, "Invalid activation code: $trimmed")
    return@withContext false
  }

  /**
   * 获取鉴权 Token（用于模型下载）。
   *
   * 在线模式返回服务端签发的 token，离线模式返回 null。
   */
  fun getToken(): String? {
    if (BuildConfig.DEBUG) return null
    val file = getTokenFile()
    if (!file.exists()) return null
    return try {
      file.readText().trim().takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
      null
    }
  }

  private fun saveActivation(code: String) {
    val file = getActivationFile()
    file.parentFile?.mkdirs()
    try {
      file.writeText(code)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save activation: ${e.message}")
    }
  }

  private fun saveToken(token: String) {
    val file = getTokenFile()
    file.parentFile?.mkdirs()
    try {
      file.writeText(token)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save token: ${e.message}")
    }
  }

  private fun getActivationFile(): File {
    val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
    return File(dir, "$ACTIVATION_DIR/$ACTIVATION_FILE")
  }

  private fun getTokenFile(): File {
    val dir = appContext.getExternalFilesDir(null) ?: appContext.filesDir
    return File(dir, "$ACTIVATION_DIR/$TOKEN_FILE")
  }
}