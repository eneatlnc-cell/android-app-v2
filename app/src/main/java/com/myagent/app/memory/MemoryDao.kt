package com.myagent.app.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MemoryDao {

  // ── 通用 ──

  @Insert
  suspend fun insert(memory: MemoryEntity)

  @Insert
  suspend fun insertAll(memories: List<MemoryEntity>)

  @Query("SELECT COUNT(*) FROM memories")
  suspend fun getCount(): Int

  @Query("DELETE FROM memories")
  suspend fun deleteAll()

  // ── 短期记忆（逐字，滚动窗口） ──

  /** 获取最近 N 条短期记忆（正序，用于注入 Prompt） */
  @Query("""
    SELECT * FROM memories 
    WHERE memory_type = 'short' 
    ORDER BY created_at_ms DESC 
    LIMIT :limit
  """)
  suspend fun getRecentShortTerm(limit: Int): List<MemoryEntity>

  /** 短期记忆总数 */
  @Query("SELECT COUNT(*) FROM memories WHERE memory_type = 'short'")
  suspend fun getShortTermCount(): Int

  /** 获取最旧的 N 条短期记忆（用于压缩） */
  @Query("""
    SELECT * FROM memories 
    WHERE memory_type = 'short' 
    ORDER BY created_at_ms ASC 
    LIMIT :limit
  """)
  suspend fun getOldestShortTerm(limit: Int): List<MemoryEntity>

  /** 删除指定 ID 的短期记忆（压缩后清除） */
  @Query("DELETE FROM memories WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<Long>)

  // ── 长期记忆（摘要，永久存储） ──

  /** 获取最近 N 条长期摘要（正序，用于注入 Prompt） */
  @Query("""
    SELECT * FROM memories 
    WHERE memory_type = 'long' 
    ORDER BY created_at_ms DESC 
    LIMIT :limit
  """)
  suspend fun getRecentLongTerm(limit: Int): List<MemoryEntity>

  /** 长期记忆总数 */
  @Query("SELECT COUNT(*) FROM memories WHERE memory_type = 'long'")
  suspend fun getLongTermCount(): Int

  /** 关键词搜索所有记忆 */
  @Query("""
    SELECT * FROM memories 
    WHERE content LIKE '%' || :keyword || '%' 
    ORDER BY created_at_ms DESC 
    LIMIT :limit
  """)
  suspend fun searchByKeyword(keyword: String, limit: Int = 10): List<MemoryEntity>
}