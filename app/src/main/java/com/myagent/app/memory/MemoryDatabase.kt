package com.myagent.app.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room 数据库 — 双层记忆存储。
 *
 * v1: 单表 memories（id, role, content, session_id, created_at_ms）
 * v2: 新增 memory_type 列（"short"/"long"），默认 "short"
 */
@Database(
  entities = [MemoryEntity::class],
  version = 2,
  exportSchema = false,
)
abstract class MemoryDatabase : RoomDatabase() {
  abstract fun memoryDao(): MemoryDao

  companion object {
    @Volatile
    private var INSTANCE: MemoryDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          ALTER TABLE memories ADD COLUMN memory_type TEXT NOT NULL DEFAULT 'short'
        """.trimIndent())
      }
    }

    fun getInstance(context: Context): MemoryDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room
          .databaseBuilder(context.applicationContext, MemoryDatabase::class.java, "lingji_memory.db")
          .addMigrations(MIGRATION_1_2)
          .fallbackToDestructiveMigration()
          .build()
          .also { INSTANCE = it }
      }
    }
  }
}