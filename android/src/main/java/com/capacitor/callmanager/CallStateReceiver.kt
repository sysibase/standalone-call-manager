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
        val action = intent.action ?: return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: ""
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
        
        Log.d("CallStateReceiver", "onReceive: action=$action, state=$state, hasNumber=${number.isNotBlank()}")
        
        val phonePerm = context.checkSelfPermission(android.Manifest.permission.READ_PHONE_STATE) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val logPerm = context.checkSelfPermission(android.Manifest.permission.READ_CALL_LOG) == android.content.pm.PackageManager.PERMISSION_GRANTED
        Log.d("CallStateReceiver", "Permissions: READ_PHONE_STATE=$phonePerm, READ_CALL_LOG=$logPerm")

        if (action != "android.intent.action.PHONE_STATE") return
        if (state.isBlank()) return
        
        val plugin = CallManagerPlugin.instance
        val prefs = context.getSharedPreferences("CallStateTemp", Context.MODE_PRIVATE)

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (number.isNotBlank()) incomingNumber = number
                isIncoming = true; callStartTime = 0L; lastState = TelephonyManager.EXTRA_STATE_RINGING
                plugin?.emitCallIncoming(incomingNumber, null)
                
                // Persist ringing state to survive process death
                prefs.edit().apply {
                    putString("last_number", incomingNumber)
                    putBoolean("was_ringing", true)
                    putBoolean("is_incoming", true)
                    apply()
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                val wasRingingStored = prefs.getBoolean("was_ringing", false)
                val storedNumber = prefs.getString("last_number", "") ?: ""
                
                if (lastState != TelephonyManager.EXTRA_STATE_RINGING && !wasRingingStored) {
                    isIncoming = false; if (number.isNotBlank()) incomingNumber = number
                } else if (incomingNumber.isBlank()) {
                    incomingNumber = storedNumber
                }

                callStartTime = System.currentTimeMillis(); lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
                plugin?.emitCallStarted(incomingNumber, null, callStartTime, isIncoming)
                
                // Persist offhook state
                prefs.edit().apply {
                    putString("last_number", incomingNumber)
                    putLong("last_start_time", callStartTime)
                    putBoolean("was_offhook", true)
                    apply()
                }

                if (plugin == null) {
                    val details = CallFilterDatabase.getInstance(context).getDetails(incomingNumber)
                    launchNativeOverlayTrigger(context, incomingNumber, 0, "DURING_CALL", isIncoming, details)
                }
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasOffhookStored = prefs.getBoolean("was_offhook", false)
                val wasRingingStored = prefs.getBoolean("was_ringing", false)
                val isIncomingStored = prefs.getBoolean("is_incoming", false)
                
                val wasOffhook = lastState == TelephonyManager.EXTRA_STATE_OFFHOOK || wasOffhookStored
                val wasRinging = lastState == TelephonyManager.EXTRA_STATE_RINGING || wasRingingStored
                val isActuallyIncoming = isIncoming || isIncomingStored
                
                if (!wasOffhook && !wasRinging) return // Ignore duplicate IDLE intents
                
                if (wasOffhook) {
                    val savedNumber = prefs.getString("last_number", "") ?: ""
                    val savedStartTime = prefs.getLong("last_start_time", 0L)
                    
                    val finalNumber = if (incomingNumber.isBlank()) savedNumber else incomingNumber
                    val finalStartTime = if (callStartTime == 0L) savedStartTime else callStartTime
                    
                    val endTime = System.currentTimeMillis()
                    val duration = if (finalStartTime > 0) ((endTime - finalStartTime) / 1000).toInt() else 0
                    
                    plugin?.emitCallEnded(finalNumber, null, finalStartTime, endTime, isActuallyIncoming)
                    
                    if (plugin == null) {
                        val details = CallFilterDatabase.getInstance(context).getDetails(finalNumber)
                        launchNativeOverlayTrigger(context, finalNumber, duration, "AFTER_CALL", isActuallyIncoming, details)
                    }
                } else if (wasRinging && isActuallyIncoming) {
                    plugin?.emitCallEnded(incomingNumber, null, 0L, System.currentTimeMillis(), true)
                } else {
                    val bgEnabled = context.getSharedPreferences("CallManagerConfig", Context.MODE_PRIVATE).getBoolean("background_enabled", true)
                    if (!bgEnabled) {
                        CallOverlayService.stop(context)
                    }
                }
                
                // Cleanup all persisted state for this call
                prefs.edit().clear().apply()
                lastState = TelephonyManager.EXTRA_STATE_IDLE; incomingNumber = ""; callStartTime = 0L; isIncoming = false
            }
        }
    }

    private fun launchNativeOverlayTrigger(context: Context, number: String, duration: Int, mode: String, isIncomingCall: Boolean, details: CallFilterDatabase.TrackedItem? = null) {
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
            putExtra("type", if (isIncomingCall) "INCOMING" else "OUTGOING")
            if (details != null) {
                putExtra("entityType", details.entityType)
                putExtra("entityId", details.entityId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d("CallManager", "Successfully requested service start for mode $mode")
        } catch (e: Exception) {
            Log.e("CallManager", "FAILED to start foreground service: ${e.message}", e)
        }
    }
}
