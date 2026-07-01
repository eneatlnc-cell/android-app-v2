package com.myagent.app.proactive

import com.myagent.app.model.PersonaType
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * 主动搭话引擎 — 根据时间、场景自动发起 AI 搭话。
 *
 * 触发规则：
 * - 早间问候（6:00-10:00）
 * - 晚间问候（20:00-23:00）
 * - 空闲搭话（用户 10 分钟无操作）
 * - 启动搭话（App 冷启动时，30% 概率）
 */
class ProactiveTrigger {

  /**
   * 检查是否应该主动搭话。
   *
   * @param lastInteractionMs 上次用户交互时间戳（毫秒），0 表示从未交互
   * @param isAppLaunch 是否为 App 冷启动
   * @return 需要搭话时返回 true
   */
  fun shouldTrigger(
    lastInteractionMs: Long,
    isAppLaunch: Boolean,
  ): Boolean {
    val now = System.currentTimeMillis()

    // 规则 1：时间触发（早晚问候）
    if (isTimeTrigger()) {
      return true
    }

    // 规则 2：空闲触发（10 分钟无交互）
    if (lastInteractionMs > 0 && (now - lastInteractionMs) > IDLE_THRESHOLD_MS) {
      return true
    }

    // 规则 3：冷启动触发（30% 概率，且不是首次）
    if (isAppLaunch && lastInteractionMs > 0 && Math.random() < 0.3) {
      return true
    }

    return false
  }

  /**
   * 获取主动搭话内容。根据人格和当前时间返回合适的文案。
   */
  fun getProactiveMessage(persona: PersonaType = PersonaType.FUNNY): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val morning = messagesByTime(hour, persona)
    // 随机选一条，避免重复
    return morning[Math.floor(Math.random() * morning.size).toInt()]
  }

  // ── 内部 ──

  private val IDLE_THRESHOLD_MS = 10 * 60 * 1000L // 10 分钟

  private fun isTimeTrigger(): Boolean {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return hour in 6..9 || hour in 20..22
  }

  private fun messagesByTime(hour: Int, persona: PersonaType): List<String> {
    val isMorning = hour in 6..9
    return when (persona) {
      PersonaType.FUNNY -> if (isMorning) {
        listOf(
          "早啊！今天打算怎么摸鱼？🐟",
          "太阳晒屁股啦！起床搞事情！☀️",
          "早上好！我昨晚梦见你会来找我聊天，果然！",
          "新的一天，新的摆烂！准备好了吗？😎",
        )
      } else {
        listOf(
          "晚上好！今天过得怎么样？",
          "夜深了，该和 AI 聊人生了 🌙",
          "一天的忙碌结束了，来放松一下？",
          "嘿！我刚学到个新梗，要不要听？😏",
        )
      }

      PersonaType.WARM -> if (isMorning) {
        listOf(
          "早安，今天也要好好照顾自己哦 🌸",
          "新的一天开始了，希望能和你一起度过美好的一天",
          "早上好呀，昨晚睡得好吗？",
          "今天的阳光很好，记得出门走走 ☀️",
        )
      } else {
        listOf(
          "晚安前的最后一段时光，想和你一起度过 🌙",
          "今天辛苦了，好好休息哦",
          "夜深了，有什么心事都可以和我说",
          "希望今天的你能睡个好觉 💤",
        )
      }

      PersonaType.SHARP -> if (isMorning) {
        listOf(
          "早。咖啡续命时间到了。☕",
          "新的一天，新的 KPI。",
          "起床了，别浪费时间。",
          "早上好。今天效率如何？",
        )
      } else {
        listOf(
          "还在加班？效率堪忧啊。",
          "晚上好。反思一下今天做了什么？",
          "夜深了，高效人士都睡了。",
          "今天浪费了多少时间？明天改进。",
        )
      }

      PersonaType.SCHOLAR -> if (isMorning) {
        listOf(
          "早安！今天想学点什么？📚",
          "一日之计在于晨，准备好吸收新知识了吗？",
          "早上好！研究表明清晨是大脑最活跃的时段",
          "新的一天，新的知识库等你探索",
        )
      } else {
        listOf(
          "晚上好！睡前复习一下今天学到的知识吧 📖",
          "夜间是巩固记忆的好时机，来聊聊今天的收获？",
          "知识不会自己进脑子，但我们可以一起学习",
          "今天读了什么有趣的东西吗？",
        )
      }
    }
  }
}