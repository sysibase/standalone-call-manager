package com.ibase.plugins.callmanager

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log

/**
 * =============================================================================
 * SMSHelper.kt — Android SMS Query & Send Helper
 * =============================================================================
 * Android ke Telephony ContentProvider se SMS efficiently query aur send karta hai.
 *
 * FEATURES:
 *  - SMS Thread (Conversation) indexing
 *  - Message history for specific threads
 *  - Background SMS sending using SmsManager
 * =============================================================================
 */
object SMSHelper {

    private const val TAG = "SMSHelper"

    data class SMSThread(
        val id: String,
        val snippet: String,
        val date: Long,
        val msgCount: Int,
        val address: String
    )

    data class SMSMessage(
        val id: String,
        val address: String,
        val body: String,
        val date: Long,
        val type: String // "RECEIVED" | "SENT"
    )

    fun getSMSThreads(context: Context): List<SMSThread> {
        val threads = mutableListOf<SMSThread>()
        val contentResolver = context.contentResolver

        val uri = Uri.parse("content://sms/conversations")
        val projection = arrayOf("thread_id", "msg_count", "snippet")

        val cursor = contentResolver.query(uri, projection, null, null, "date DESC")

        cursor?.use { c ->
            val idIdx = c.getColumnIndex("thread_id")
            val countIdx = c.getColumnIndex("msg_count")
            val snippetIdx = c.getColumnIndex("snippet")

            while (c.moveToNext()) {
                val threadId = c.getString(idIdx)
                val count = c.getInt(countIdx)
                val snippet = c.getString(snippetIdx) ?: ""

                // Address nikaalne ke liye thread ka last message query karte hain
                val address = getAddressForThread(context, threadId)

                // Date nikaalne ke liye bhi last message query karte hain (simplified logic)
                val date = getDateForThread(context, threadId)

                threads.add(SMSThread(
                    id = threadId,
                    snippet = snippet,
                    date = date,
                    msgCount = count,
                    address = address
                ))
            }
        }

        Log.d(TAG, "getSMSThreads returned ${threads.size} threads")
        return threads
    }

    fun getMessagesForThread(context: Context, threadId: String): List<SMSMessage> {
        val messages = mutableListOf<SMSMessage>()
        val contentResolver = context.contentResolver

        val uri = Telephony.Sms.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        val selection = "${Telephony.Sms.THREAD_ID} = ?"
        val selectionArgs = arrayOf(threadId)

        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, "date ASC")

        cursor?.use { c ->
            val idIdx = c.getColumnIndex(Telephony.Sms._ID)
            val addressIdx = c.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndex(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndex(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndex(Telephony.Sms.TYPE)

            while (c.moveToNext()) {
                val typeInt = c.getInt(typeIdx)
                val type = if (typeInt == Telephony.Sms.MESSAGE_TYPE_INBOX) "INCOMING" else "OUTGOING"

                messages.add(SMSMessage(
                    id = c.getString(idIdx),
                    address = c.getString(addressIdx) ?: "",
                    body = c.getString(bodyIdx) ?: "",
                    date = c.getLong(dateIdx),
                    type = type
                ))
            }
        }

        return messages
    }

    fun sendSMS(number: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, message, null, null)
            Log.d(TAG, "SMS sent to $number")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Missing SEND_SMS permission", e)
            throw Exception("PERMISSION_DENIED: Missing SEND_SMS permission")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS to $number", e)
            throw e
        }
    }

    private fun getAddressForThread(context: Context, threadId: String): String {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId),
            "date DESC LIMIT 1"
        )
        cursor?.use {
            if (it.moveToFirst()) return it.getString(0) ?: "Unknown"
        }
        return "Unknown"
    }

    private fun getDateForThread(context: Context, threadId: String): Long {
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.DATE),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId),
            "date DESC LIMIT 1"
        )
        cursor?.use {
            if (it.moveToFirst()) return it.getLong(0)
        }
        return 0L
    }
}
