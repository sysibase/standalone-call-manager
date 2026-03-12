package com.capacitor.callmanager

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class CallFilterDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "CallFilter.db"
        private const val DATABASE_VERSION = 2 // Upgraded version
        private const val TABLE_TRACKED = "tracked_numbers"
        private const val COL_NUMBER = "number"
        private const val COL_NAME = "name"
        private const val COL_ENTITY_TYPE = "entity_type"
        private const val COL_ENTITY_ID = "entity_id"

        @Volatile
        private var instance: CallFilterDatabase? = null

        fun getInstance(context: Context): CallFilterDatabase {
            val appContext = context.applicationContext
            // Use device-protected storage if available (for Direct Boot support)
            val protectedContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appContext.createDeviceProtectedStorageContext()
            } else {
                appContext
            }

            return instance ?: synchronized(this) {
                instance ?: CallFilterDatabase(protectedContext).also { 
                    instance = it
                    // Proactively ensure the database directory exists to avoid "CantOpenDatabase" errors
                    try {
                        protectedContext.getDatabasePath(DATABASE_NAME).parentFile?.mkdirs()
                    } catch (e: Exception) {
                        Log.e("CallFilterDB", "Failed to ensure DB directory exists", e)
                    }
                }
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_TRACKED (
                $COL_NUMBER TEXT PRIMARY KEY,
                $COL_NAME TEXT,
                $COL_ENTITY_TYPE TEXT,
                $COL_ENTITY_ID TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Simple migration: drop and recreate or add columns
            // For a plugin, simple recreation might be cleaner if data loss is acceptable, 
            // but let's try to add columns for professional approach
            try {
                db.execSQL("ALTER TABLE $TABLE_TRACKED ADD COLUMN $COL_NAME TEXT")
                db.execSQL("ALTER TABLE $TABLE_TRACKED ADD COLUMN $COL_ENTITY_TYPE TEXT")
                db.execSQL("ALTER TABLE $TABLE_TRACKED ADD COLUMN $COL_ENTITY_ID TEXT")
            } catch (e: Exception) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_TRACKED")
                onCreate(db)
            }
        }
    }

    fun addTrackedItems(items: List<TrackedItem>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            items.forEach { item ->
                val values = ContentValues().apply {
                    put(COL_NUMBER, normalize(item.number))
                    put(COL_NAME, item.name)
                    put(COL_ENTITY_TYPE, item.entityType)
                    put(COL_ENTITY_ID, item.entityId)
                }
                db.insertWithOnConflict(TABLE_TRACKED, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun removeNumbers(numbers: List<String>): Int {
        val db = writableDatabase
        var deletedCount = 0
        db.beginTransaction()
        try {
            numbers.forEach { number ->
                deletedCount += db.delete(TABLE_TRACKED, "$COL_NUMBER = ?", arrayOf(normalize(number)))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return deletedCount
    }

    fun removeAll(): Boolean {
        return writableDatabase.delete(TABLE_TRACKED, null, null) >= 0
    }

    fun removeByEntity(entityType: String): Int {
        return writableDatabase.delete(TABLE_TRACKED, "$COL_ENTITY_TYPE = ?", arrayOf(entityType))
    }

    fun removeByEntityId(entityId: String): Int {
        return writableDatabase.delete(TABLE_TRACKED, "$COL_ENTITY_ID = ?", arrayOf(entityId))
    }

    fun getAll(): List<TrackedItem> {
        return queryItems(null, null)
    }

    fun getByEntity(entityType: String): List<TrackedItem> {
        return queryItems("$COL_ENTITY_TYPE = ?", arrayOf(entityType))
    }

    private fun queryItems(selection: String?, selectionArgs: Array<String>?): List<TrackedItem> {
        val list = mutableListOf<TrackedItem>()
        val cursor = readableDatabase.query(TABLE_TRACKED, null, selection, selectionArgs, null, null, null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(TrackedItem(
                    number = it.getString(it.getColumnIndexOrThrow(COL_NUMBER)),
                    name = it.getString(it.getColumnIndexOrThrow(COL_NAME)),
                    entityType = it.getString(it.getColumnIndexOrThrow(COL_ENTITY_TYPE)),
                    entityId = it.getString(it.getColumnIndexOrThrow(COL_ENTITY_ID))
                ))
            }
        }
        return list
    }

    fun getDetails(number: String): TrackedItem? {
        val norm = normalize(number)
        val cursor = readableDatabase.query(TABLE_TRACKED, null, "$COL_NUMBER = ?", arrayOf(norm), null, null, null)
        return cursor.use {
            if (it.moveToFirst()) {
                TrackedItem(
                    number = it.getString(it.getColumnIndexOrThrow(COL_NUMBER)),
                    name = it.getString(it.getColumnIndexOrThrow(COL_NAME)),
                    entityType = it.getString(it.getColumnIndexOrThrow(COL_ENTITY_TYPE)),
                    entityId = it.getString(it.getColumnIndexOrThrow(COL_ENTITY_ID))
                )
            } else null
        }
    }

    fun isNumberTracked(number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        val norm = normalize(number)
        val cursor = readableDatabase.query(TABLE_TRACKED, arrayOf(COL_NUMBER), "$COL_NUMBER = ?", arrayOf(norm), null, null, null)
        val exists = cursor.use { it.count > 0 }
        return exists
    }

    private fun normalize(number: String): String {
        // Strip everything except digits to ensure consistent matching (+123 -> 123)
        return number.replace(Regex("[^0-9]"), "")
    }

    data class TrackedItem(
        val number: String,
        val name: String? = null,
        val entityType: String? = null,
        val entityId: String? = null
    )
}
