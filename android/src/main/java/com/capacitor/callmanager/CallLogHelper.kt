package com.capacitor.callmanager

import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.CallLog
import android.util.Log
import java.util.Calendar

/**
 * =============================================================================
 * CallLogHelper.kt — Android Call Log Query Helper
 * =============================================================================
 * Android ke CallLog.Calls.CONTENT_URI se call history efficiently query karta hai.
 *
 * FEATURES:
 *  - Type filter: INCOMING / OUTGOING / MISSED / ALL
 *  - Date filter: TODAY / LAST_7_DAYS / LAST_30_DAYS / ALL
 *  - Search filter: name ya number mein partial match
 *  - Duration filter: minimum seconds se filter
 *  - Result limit: performance ke liye
 *
 * PERFORMANCE:
 *  - Background coroutine pe run hota hai (non-blocking)
 *  - Lazy cursor traversal — 10,000+ logs handle karta hai
 *  - Android-side SQL filtering jitna possible ho (Java side mein kam kaam)
 * =============================================================================
 */
object CallLogHelper {

    private const val TAG = "CallLogHelper"

    /**
     * getCallLogs() ke options
     */
    data class Options(
        val type: String = "ALL",       // "ALL" | "INCOMING" | "OUTGOING" | "MISSED"
        val search: String = "",         // Name ya number mein partial search
        val date: String = "ALL",       // "ALL" | "TODAY" | "LAST_7_DAYS" | "LAST_30_DAYS"
        val duration: Int = 0,          // Minimum duration seconds
        val limit: Int = 500,           // Max results
        val offset: Int = 0             // Pagination offset
    )

    /**
     * Ek call log entry ka model
     */
    data class CallLogEntry(
        val id: String,
        val number: String,
        val name: String?,
        val type: String,
        val date: Long,
        val duration: Long
    )

    /**
     * Call logs retrieve karo Android ke ContentProvider se.
     * READ_CALL_LOG permission required hai.
     *
     * @param context Android context
     * @param options Filtering options
     * @return List of CallLogEntry objects
     */
    fun getCallLogs(context: Context, options: Options): List<CallLogEntry> {
        val results = mutableListOf<CallLogEntry>()

        // SQL WHERE clause build karo
        val selectionParts = mutableListOf<String>()
        val selectionArgs = mutableListOf<String>()

        // Type filter
        val typeInt = callTypeToInt(options.type)
        if (typeInt != null) {
            selectionParts.add("${CallLog.Calls.TYPE} = ?")
            selectionArgs.add(typeInt.toString())
        }

        // Date filter
        val dateFrom = getDateFrom(options.date)
        if (dateFrom > 0) {
            selectionParts.add("${CallLog.Calls.DATE} >= ?")
            selectionArgs.add(dateFrom.toString())
        }

        // Duration filter (Android-side)
        if (options.duration > 0) {
            selectionParts.add("${CallLog.Calls.DURATION} >= ?")
            selectionArgs.add(options.duration.toString())
        }

        val selection = if (selectionParts.isNotEmpty()) selectionParts.joinToString(" AND ") else null
        val selectionArgsArr = if (selectionArgs.isNotEmpty()) selectionArgs.toTypedArray() else null

        // LIMIT in sortOrder can fail on some Android versions, handling manually in loop
        val sortOrder = "${CallLog.Calls.DATE} DESC"

        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION
        )

        Log.d(TAG, "Querying CallLog: selection=$selection, limit=${options.limit}, offset=${options.offset}")

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgsArr,
                sortOrder
            )

            cursor?.let { c ->
                val idIdx = c.getColumnIndexOrThrow(CallLog.Calls._ID)
                val numberIdx = c.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val nameIdx = c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
                val typeIdx = c.getColumnIndexOrThrow(CallLog.Calls.TYPE)
                val dateIdx = c.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIdx = c.getColumnIndexOrThrow(CallLog.Calls.DURATION)

                Log.d(TAG, "Cursor returned ${c.count} rows total")

                var skipped = 0

                while (c.moveToNext() && results.size < options.limit) {
                    val number = c.getString(numberIdx) ?: ""
                    val name = c.getString(nameIdx)

                    if (options.search.isNotBlank()) {
                        val searchLower = options.search.lowercase()
                        val numberMatch = number.contains(searchLower)
                        val nameMatch = name?.lowercase()?.contains(searchLower) == true
                        if (!numberMatch && !nameMatch) continue
                    }

                    if (skipped < options.offset) {
                        skipped++
                        continue
                    }

                    val entry = CallLogEntry(
                        id = c.getString(idIdx) ?: "",
                        number = number,
                        name = name?.takeIf { it.isNotBlank() },
                        type = intToCallType(c.getInt(typeIdx)),
                        date = c.getLong(dateIdx),
                        duration = c.getLong(durationIdx)
                    )
                    results.add(entry)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "READ_CALL_LOG permission denied during query", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error querying call logs", e)
        } finally {
            cursor?.close()
        }

        Log.d(TAG, "getCallLogs finished. Found ${results.size} matching entries.")
        return results
    }

    // -------------------------------------------------------------------------
    // Helper: Call type string ↔ Android int
    // -------------------------------------------------------------------------

    private fun callTypeToInt(type: String): Int? = when (type.uppercase()) {
        "INCOMING" -> CallLog.Calls.INCOMING_TYPE
        "OUTGOING" -> CallLog.Calls.OUTGOING_TYPE
        "MISSED"   -> CallLog.Calls.MISSED_TYPE
        "REJECTED" -> CallLog.Calls.REJECTED_TYPE
        "BLOCKED"  -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) CallLog.Calls.BLOCKED_TYPE else null
        else       -> null // "ALL" → no filter
    }

    private fun intToCallType(type: Int): String = when (type) {
        CallLog.Calls.INCOMING_TYPE -> "INCOMING"
        CallLog.Calls.OUTGOING_TYPE -> "OUTGOING"
        CallLog.Calls.MISSED_TYPE   -> "MISSED"
        CallLog.Calls.REJECTED_TYPE -> "REJECTED"
        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && type == CallLog.Calls.BLOCKED_TYPE) "BLOCKED"
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && type == CallLog.Calls.ANSWERED_EXTERNALLY_TYPE) "EXTERNAL"
            else "UNKNOWN"
        }
    }

    // -------------------------------------------------------------------------
    // Helper: Date range start timestamp
    // -------------------------------------------------------------------------

    private fun getDateFrom(date: String): Long {
        val cal = Calendar.getInstance()
        return when (date.uppercase()) {
            "TODAY" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            "YESTERDAY" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.timeInMillis
            }
            "WEEK", "LAST_7_DAYS" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.timeInMillis
            }
            "MONTH", "LAST_30_DAYS" -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.timeInMillis
            }
            else -> 0L // "ALL" → no date filter
        }
    }
}
