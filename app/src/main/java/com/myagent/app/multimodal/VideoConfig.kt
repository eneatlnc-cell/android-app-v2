package com.myagent.app.multimodal

/**
 * 视频渲染配置 — 用户可选画质预设。
 *
 * 默认最低画质（省电 + 快速渲染），用户可在设置中切换。
 */
data class VideoConfig(
  val width: Int = 854,
  val height: Int = 480,
  val fps: Int = 24,
  val maxDuration: Int = 5,
) {
  companion object {
    val LOW = VideoConfig(854, 480, 24, 5)
    val MEDIUM = VideoConfig(1280, 720, 30, 10)
    val HIGH = VideoConfig(1920, 1080, 30, 15)

    val PRESETS = listOf(LOW, MEDIUM, HIGH)
    val PRESET_LABELS = listOf("低画质 (480p · 省电)", "标准画质 (720p)", "高画质 (1080p)")

    fun fromPresetIndex(index: Int): VideoConfig =
      PRESETS.getOrElse(index) { LOW }

    /** 从 SharedPreferences 字符串反序列化，格式："width,height,fps,maxDuration" */
    fun fromString(raw: String?): VideoConfig {
      if (raw.isNullOrBlank()) return LOW
      val parts = raw.split(",").map { it.trim() }
      return try {
        VideoConfig(
          width = parts.getOrElse(0) { "854" }.toInt(),
          height = parts.getOrElse(1) { "480" }.toInt(),
          fps = parts.getOrElse(2) { "24" }.toInt(),
          maxDuration = parts.getOrElse(3) { "5" }.toInt(),
        )
      } catch (_: Exception) {
        LOW
      }
    }

    /** 序列化为 SharedPreferences 字符串 */
    fun toString(config: VideoConfig): String =
      "${config.width},${config.height},${config.fps},${config.maxDuration}"
  }
}