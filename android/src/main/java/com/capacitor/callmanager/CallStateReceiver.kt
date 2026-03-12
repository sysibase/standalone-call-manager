package com.capacitor.callmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

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
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                val wasOffhook = lastState == TelephonyManager.EXTRA_STATE_OFFHOOK
                val wasRinging = lastState == TelephonyManager.EXTRA_STATE_RINGING
                if (wasOffhook) plugin?.emitCallEnded(incomingNumber, null, callStartTime, System.currentTimeMillis())
                else if (wasRinging && isIncoming) plugin?.emitCallEnded(incomingNumber, null, 0L, System.currentTimeMillis())
                lastState = TelephonyManager.EXTRA_STATE_IDLE; incomingNumber = ""; callStartTime = 0L; isIncoming = false
            }
        }
    }
}
