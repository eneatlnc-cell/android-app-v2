package com.myagent.app

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 皮肤系统 — 完整的视觉主题包，包含颜色、气泡形状、背景纹理。
 *
 * 每套皮肤定义暗色/亮色两套调色板，自动跟随系统或手动切换。
 */
enum class SkinMode(
  val rawValue: String,
  val displayName: String,
  val description: String,
  val emoji: String,
) {
  NYX(
    rawValue = "nyx",
    displayName = "午夜紫金",
    description = "暗紫底色 + 金色点缀，高级神秘",
    emoji = "🌙",
  ),
  DUSK(
    rawValue = "dusk",
    displayName = "日暮暖橙",
    description = "暖橙渐变 + 奶油色，温暖活力",
    emoji = "🌅",
  ),
  OCEAN(
    rawValue = "ocean",
    displayName = "深海蓝",
    description = "深海蓝 + 白色，冷静专业",
    emoji = "🌊",
  ),
  FOREST(
    rawValue = "forest",
    displayName = "翡翠绿",
    description = "墨绿 + 薄荷色，清新自然",
    emoji = "🌿",
  ),
  SAKURA(
    rawValue = "sakura",
    displayName = "樱花粉",
    description = "粉色渐变 + 白色，柔和治愈",
    emoji = "🌸",
  );

  // ── 暗色调色板 ──

  fun darkColors(): SkinColors = when (this) {
    NYX -> SkinColors(
      canvas = Color(0xFF0A0A12),
      surface = Color(0xFF16162A),
      surfaceRaised = Color(0xFF1E1E3A),
      surfacePressed = Color(0xFF282850),
      border = Color(0xFF2A2A48),
      borderStrong = Color(0xFF3D3D68),
      text = Color(0xFFE8E0F0),
      textMuted = Color(0xFF9B90B8),
      textSubtle = Color(0xFF6B6088),
      primary = Color(0xFF7C5CE7),
      primaryText = Color(0xFFFFFFFF),
      accent = Color(0xFFE8B83A),
      success = Color(0xFF00D4AA),
      successSoft = Color(0xFF0F2B24),
      warning = Color(0xFFFDCB6E),
      warningSoft = Color(0xFF2B2412),
      danger = Color(0xFFFF7675),
      dangerSoft = Color(0xFF2C1414),
      userBubble = Color(0xFF7C5CE7),
      assistantBubble = Color(0xFF1E1E3A),
      bubbleRadius = BubbleRadius(16.dp, 16.dp, 6.dp, 16.dp),
      backgroundPattern = BackgroundPattern.DOTS,
    )

    DUSK -> SkinColors(
      canvas = Color(0xFF1A1210),
      surface = Color(0xFF2A1E18),
      surfaceRaised = Color(0xFF352820),
      surfacePressed = Color(0xFF423028),
      border = Color(0xFF3A2A20),
      borderStrong = Color(0xFF503A2E),
      text = Color(0xFFF5EDE6),
      textMuted = Color(0xFFB8A898),
      textSubtle = Color(0xFF887868),
      primary = Color(0xFFFF6B35),
      primaryText = Color(0xFFFFFFFF),
      accent = Color(0xFFFFB84D),
      success = Color(0xFF4ECDC4),
      successSoft = Color(0xFF0F2B26),
      warning = Color(0xFFFDCB6E),
      warningSoft = Color(0xFF2B2412),
      danger = Color(0xFFFF7675),
      dangerSoft = Color(0xFF2C1414),
      userBubble = Color(0xFFFF6B35),
      assistantBubble = Color(0xFF352820),
      bubbleRadius = BubbleRadius(18.dp, 18.dp, 4.dp, 18.dp),
      backgroundPattern = BackgroundPattern.GRADIENT,
    )

    OCEAN -> SkinColors(
      canvas = Color(0xFFF0F4F8),
      surface = Color(0xFFFFFFFF),
      surfaceRaised = Color(0xFFF8FAFC),
      surfacePressed = Color(0xFFE8EEF4),
      border = Color(0xFFD8E0E8),
      borderStrong = Color(0xFFBCC8D4),
      text = Color(0xFF141820),
      textMuted = Color(0xFF5A6470),
      textSubtle = Color(0xFF8E98A8),
      primary = Color(0xFF0A84FF),
      primaryText = Color(0xFFFFFFFF),
      accent = Color(0xFF34C759),
      success = Color(0xFF00B894),
      successSoft = Color(0xFFE8F8F3),
      warning = Color(0xFFE8A844),
      warningSoft = Color(0xFFFFF8E8),
      danger = Color(0xFFE87070),
      dangerSoft = Color(0xFFFFECEC),
      userBubble = Color(0xFF0A84FF),
      assistantBubble = Color(0xFFF0F4F8),
      bubbleRadius = BubbleRadius(8.dp, 8.dp, 2.dp, 8.dp),
      backgroundPattern = BackgroundPattern.SOLID,
    )

    FOREST -> SkinColors(
      canvas = Color(0xFF0E1A14),
      surface = Color(0xFF1A2E22),
      surfaceRaised = Color(0xFF243A2E),
      surfacePressed = Color(0xFF2E4638),
      border = Color(0xFF2A3E30),
      borderStrong = Color(0xFF3E5646),
      text = Color(0xFFE8F0EA),
      textMuted = Color(0xFF98B8A0),
      textSubtle = Color(0xFF688870),
      primary = Color(0xFF00D4AA),
      primaryText = Color(0xFF0E1A14),
      accent = Color(0xFFB8E840),
      success = Color(0xFF00D4AA),
      successSoft = Color(0xFF0F2B24),
      warning = Color(0xFFFDCB6E),
      warningSoft = Color(0xFF2B2412),
      danger = Color(0xFFFF7675),
      dangerSoft = Color(0xFF2C1414),
      userBubble = Color(0xFF00D4AA),
      assistantBubble = Color(0xFF243A2E),
      bubbleRadius = BubbleRadius(14.dp, 14.dp, 4.dp, 14.dp),
      backgroundPattern = BackgroundPattern.DOTS,
    )

    SAKURA -> SkinColors(
      canvas = Color(0xFFFFF5F7),
      surface = Color(0xFFFFFFFF),
      surfaceRaised = Color(0xFFFFF0F3),
      surfacePressed = Color(0xFFF8E0E8),
      border = Color(0xFFF0D8E0),
      borderStrong = Color(0xFFE0B8C8),
      text = Color(0xFF2A1A20),
      textMuted = Color(0xFF8A6A70),
      textSubtle = Color(0xFFB898A0),
      primary = Color(0xFFFF6B9D),
      primaryText = Color(0xFFFFFFFF),
      accent = Color(0xFFFFB8D0),
      success = Color(0xFF4ECDC4),
      successSoft = Color(0xFFE8F8F3),
      warning = Color(0xFFE8A844),
      warningSoft = Color(0xFFFFF8E8),
      danger = Color(0xFFE87070),
      dangerSoft = Color(0xFFFFECEC),
      userBubble = Color(0xFFFF6B9D),
      assistantBubble = Color(0xFFFFF0F3),
      bubbleRadius = BubbleRadius(20.dp, 20.dp, 8.dp, 20.dp),
      backgroundPattern = BackgroundPattern.GRADIENT,
    )
  }

  // ── 亮色调色板 ──

  fun lightColors(): SkinColors = when (this) {
    NYX -> SkinColors(
      canvas = Color(0xFFFAFBFC),
      surface = Color(0xFFFFFFFF),
      surfaceRaised = Color(0xFFF5F0FF),
      surfacePressed = Color(0xFFEDE5FF),
      border = Color(0xFFE0D8F0),
      borderStrong = Color(0xFFCCC0E0),
      text = Color(0xFF1A1828),
      textMuted = Color(0xFF5A5470),
      textSubtle = Color(0xFF8E88A8),
      primary = Color(0xFF7C5CE7),
      primaryText = Color(0xFFFFFFFF),
      accent = Color(0xFFD4A020),
      success = Color(0xFF00B894),
      successSoft = Color(0xFFE8F8F3),
      warning = Color(0xFFE8A844),
      warningSoft = Color(0xFFFFF8E8),
      danger = Color(0xFFE87070),
      dangerSoft = Color(0xFFFFECEC),
      userBubble = Color(0xFF7C5CE7),
      assistantBubble = Color(0xFFF5F0FF),
      bubbleRadius = BubbleRadius(16.dp, 16.dp, 6.dp, 16.dp),
      backgroundPattern = BackgroundPattern.SOLID,
    )

    DUSK -> SkinColors(
      canvas = Color(0xFFFFFAF5),
      surface = Color(0xFFFFFFFF),
      surfaceRaised = Color(0xFFFFF0E8),
      surfacePressed = Color(0xFFF8E8D8),
      border = Color(0xFFF0D8C0),
      borderStrong = Color(0xFFE0C0A0),
      text = Color(0xFF2A1A10),
      textMuted = Color(0xFF8A6A50),
      textSubtle = Color(0xFFB89880),
      primary = Color(0xFFFF6B35),
      primaryText = Color(0xFFFFFFFF),
      accent = Color(0xFFFFB84D),
      success = Color(0xFF4ECDC4),
      successSoft = Color(0xFFE8F8F3),
      warning = Color(0xFFE8A844),
      warningSoft = Color(0xFFFFF8E8),
      danger = Color(0xFFE87070),
      dangerSoft = Color(0xFFFFECEC),
      userBubble = Color(0xFFFF6B35),
      assistantBubble = Color(0xFFFFF0E8),
      bubbleRadius = BubbleRadius(18.dp, 18.dp, 4.dp, 18.dp),
      backgroundPattern = BackgroundPattern.SOLID,
    )

    OCEAN -> darkColors() // 深海蓝本身就是亮色基调
    FOREST -> lightColors().copy(primary = Color(0xFF00B894), userBubble = Color(0xFF00B894))
    SAKURA -> darkColors() // 樱花粉本身就是亮色基调
  }

  companion object {
    fun fromRawValue(value: String?): SkinMode =
      entries.firstOrNull { it.rawValue == value?.trim()?.lowercase() } ?: NYX
  }
}

// ── 皮肤颜色令牌 ──

data class SkinColors(
  val canvas: Color,
  val surface: Color,
  val surfaceRaised: Color,
  val surfacePressed: Color,
  val border: Color,
  val borderStrong: Color,
  val text: Color,
  val textMuted: Color,
  val textSubtle: Color,
  val primary: Color,
  val primaryText: Color,
  val accent: Color,
  val success: Color,
  val successSoft: Color,
  val warning: Color,
  val warningSoft: Color,
  val danger: Color,
  val dangerSoft: Color,
  val userBubble: Color,
  val assistantBubble: Color,
  val bubbleRadius: BubbleRadius,
  val backgroundPattern: BackgroundPattern,
)

// ── 气泡圆角 ──

data class BubbleRadius(
  val topStart: Dp = 12.dp,
  val topEnd: Dp = 12.dp,
  val bottomStart: Dp = 4.dp,
  val bottomEnd: Dp = 12.dp,
)

// ── 聊天背景纹理 ──

enum class BackgroundPattern {
  /** 纯色 */
  SOLID,
  /** 渐变 */
  GRADIENT,
  /** 点阵 */
  DOTS,
}