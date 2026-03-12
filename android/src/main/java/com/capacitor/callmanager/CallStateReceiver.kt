package com.capacitor.callmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.os.Build

/**
 * CallStateReceiver — The Early Warning System
 * =============================================================================
 * Yeh BroadcastReceiver system ke phone state changes (Ringing, Offhook, Idle) ko listen karta hai.
 * 
 * CORE LOGIC:
 *  - Ringing: Incoming number detect karta hai.
 *  - Offhook: Call start hone ka timestamp record karta hai.
 *  - Idle: Call end hone par duration calculate karta hai aur overlay trigger karta hai.
 * 
 * PERSISTENCE:
 *  - Agar app killed (plugin == null) hai, tab bhi yeh receiver database se lead status check karke
 *    native overlay service ko start kar deta hai.
 * =============================================================================
 */
class CallStateReceiver : BroadcastReceiver() {
    companion object {
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var incomingNumber = ""
        private var callStartTime = 0L
        private var isIncoming = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        val plugin = CallManagerPlugin.instance

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (number.isNotBlank()) incomingNumber = number
                isIncoming = true; callStartTime = 0L; lastState = TelephonyManager.EXTRA_STATE_RINGING
                plugin?.emitCallIncoming(incomingNumber, null)
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState != TelephonyManager.EXTRA_STATE_RINGING) {
                    isIncoming = false; if (number.isNotBlank()) incomingNumber = number
                }
                callStartTime = System.currentTimeMillis(); lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
                plugin?.emitCallStarted(incomingNumber, null, callStartTime)
                
                // Persist state to handle process death during a long call
                context.getSharedPreferences("CallStateTemp", Context.MODE_PRIVATE).edit().apply {
                    putString("last_number", incomingNumber)
                    putLong("last_start_time", callStartTime)
                    apply()
                }

                if (plugin == null) {
                    val details = CallFilterDatabase.getInstance(context).getDetails(incomingNumber)
                    launchNativeOverlayTrigger(context, incomingNumber, 0, "DURING_CALL", details)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasOffhook = lastState == TelephonyManager.EXTRA_STATE_OFFHOOK
                val wasRinging = lastState == TelephonyManager.EXTRA_STATE_RINGING
                
                if (wasOffhook) {
                    val prefs = context.getSharedPreferences("CallStateTemp", Context.MODE_PRIVATE)
                    val savedNumber = prefs.getString("last_number", "") ?: ""
                    val savedStartTime = prefs.getLong("last_start_time", 0L)
                    
                    val finalNumber = if (incomingNumber.isBlank()) savedNumber else incomingNumber
                    val finalStartTime = if (callStartTime == 0L) savedStartTime else callStartTime
                    
                    val endTime = System.currentTimeMillis()
                    val duration = if (finalStartTime > 0) ((endTime - finalStartTime) / 1000).toInt() else 0
                    
                    plugin?.emitCallEnded(finalNumber, null, finalStartTime, endTime)
                    
                    if (plugin == null) {
                        val details = CallFilterDatabase.getInstance(context).getDetails(finalNumber)
                        launchNativeOverlayTrigger(context, finalNumber, duration, "AFTER_CALL", details)
                    }
                    
                    // Cleanup persisted state
                    prefs.edit().clear().apply()
                } else if (wasRinging && isIncoming) {
                    plugin?.emitCallEnded(incomingNumber, null, 0L, System.currentTimeMillis())
                } else {
                    CallOverlayService.stop(context)
                }
                
                lastState = TelephonyManager.EXTRA_STATE_IDLE; incomingNumber = ""; callStartTime = 0L; isIncoming = false
            }
        }
    }

    private fun launchNativeOverlayTrigger(context: Context, number: String, duration: Int, mode: String, details: CallFilterDatabase.TrackedItem? = null) {
        val prefs = context.getSharedPreferences("CallManagerConfig", Context.MODE_PRIVATE)
        val backgroundEnabled = prefs.getBoolean("background_enabled", true)
        
        if (!backgroundEnabled) {
            Log.d("CallManager", "Background service disabled, skipping native overlay.")
            return
        }

        val trackingMode = prefs.getString("tracking_mode", "ALL") ?: "ALL"
        if (trackingMode == "SELECTED") {
            if (details == null) {
                Log.d("CallManager", "Number $number is not in tracking list, skipping overlay.")
                return
            }
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(context)) return
        
        // Native fallback contact fetching since it's unlinked from Cap runtime context
        var contactName = details?.name ?: ""
        try {
            if (contactName.isBlank() && number.isNotBlank()) {
                val uri = android.net.Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(number))
                val cursor = context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
                cursor?.use { if (it.moveToFirst()) contactName = it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)) }
            }
        } catch (e: Exception) {
            Log.e("CallManager", "Failed to fetch contact name in background", e)
        }

        val intent = Intent(context, CallOverlayService::class.java).apply {
            putExtra("number", number)
            putExtra("name", contactName)
            putExtra("duration", duration)
            putExtra("mode", mode)
            if (details != null) {
                putExtra("entityType", details.entityType)
                putExtra("entityId", details.entityId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
}
