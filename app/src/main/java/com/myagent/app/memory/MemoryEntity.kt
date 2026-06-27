package com.myagent.app.memory

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记忆实体 — 双层存储架构。
 *
 * - SHORT_TERM：逐字对话，最多 50 条，滚动窗口（清晰）
 * - LONG_TERM：压缩摘要，永久存储不删除（模糊）
 *
 * 压缩策略：短期溢出时，取最旧的 20 条 → 关键词提取 → 合并为一条 LONG_TERM 摘要。
 */
@Entity(tableName = "memories")
data class MemoryEntity(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,

  @ColumnInfo(name = "role")
  val role: String, // "user" / "assistant" / "system"

  @ColumnInfo(name = "content")
  val content: String,

  @ColumnInfo(name = "session_id")
  val sessionId: String = "default",

  @ColumnInfo(name = "created_at_ms")
  val createdAtMs: Long = System.currentTimeMillis(),

  /**
   * 记忆类型。
   * - "short" — 短期逐字记忆（最多 50 条）
   * - "long"  — 长期模糊摘要（永久存储）
   */
  @ColumnInfo(name = "memory_type")
  val memoryType: String = TYPE_SHORT_TERM,
) {
  companion object {
    const val TYPE_SHORT_TERM = "short"
    const val TYPE_LONG_TERM = "long"
  }
}