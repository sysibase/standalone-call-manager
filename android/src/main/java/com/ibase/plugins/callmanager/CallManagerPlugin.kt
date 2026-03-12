package com.ibase.plugins.callmanager

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import com.getcapacitor.annotation.ActivityCallback
import androidx.activity.result.ActivityResult
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.cancel
import java.io.File

@CapacitorPlugin(
    name = "CallManager",
    permissions = [
        Permission(strings = [Manifest.permission.READ_CALL_LOG], alias = "callLog"),
        Permission(strings = [Manifest.permission.READ_PHONE_STATE], alias = "phoneState"),
        Permission(strings = [Manifest.permission.CALL_PHONE], alias = "callPhone"),
        Permission(strings = [Manifest.permission.READ_CONTACTS], alias = "contacts"),
        Permission(strings = [Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS], alias = "sms"),
        Permission(strings = [Manifest.permission.RECORD_AUDIO], alias = "microphone"),
    ]
)
class CallManagerPlugin : Plugin() {
    companion object {
        var instance: CallManagerPlugin? = null
            private set
    }

    private var callStateReceiver: CallStateReceiver? = null
    private val pluginScope = CoroutineScope(Dispatchers.IO)
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null

    override fun load() {
        instance = this
    }

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        callStateReceiver?.let { context.unregisterReceiver(it); callStateReceiver = null }
        pluginScope.cancel()
        recorder?.release()
    }

    @PluginMethod
    fun getCallLogs(call: PluginCall) {
        if (getPermissionState("callLog") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing callLog permission")
        }
        pluginScope.launch {
            try {
                val options = CallLogHelper.Options(
                    type = call.getString("type", "ALL") ?: "ALL",
                    search = call.getString("search", "") ?: "",
                    date = call.getString("date", "ALL") ?: "ALL",
                    limit = call.getInt("limit", 500) ?: 500,
                    offset = call.getInt("offset", 0) ?: 0
                )
                val logs = CallLogHelper.getCallLogs(context, options)
                withContext(Dispatchers.Main) {
                    val result = JSObject()
                    val logsArray = com.getcapacitor.JSArray()
                    logs.forEach { log ->
                        val obj = JSObject()
                        obj.put("id", log.id); obj.put("number", log.number); obj.put("name", log.name ?: getContactNameByNumber(log.number) ?: ""); obj.put("type", log.type); obj.put("date", log.date); obj.put("duration", log.duration)
                        logsArray.put(obj)
                    }
                    result.put("logs", logsArray); result.put("total", logs.size)
                    call.resolve(result)
                }
            } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
        }
    }

    @PluginMethod
    fun initCalling(call: PluginCall) {
        val number = call.getString("number") ?: return call.reject("INVALID_ARGUMENT", "Number required")
        if (getPermissionState("callPhone") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing callPhone permission")
        }
        try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent); call.resolve()
        } catch (e: Exception) { call.reject("FEATURE_NOT_SUPPORTED", e.message, e) }
    }

    @PluginMethod
    fun startCallListener(call: PluginCall) {
        if (getPermissionState("phoneState") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing phoneState permission")
        }
        if (callStateReceiver == null) {
            callStateReceiver = CallStateReceiver()
            val filter = IntentFilter("android.intent.action.PHONE_STATE")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(callStateReceiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(callStateReceiver, filter)
            }
        }
        call.resolve()
    }

    @PluginMethod
    fun stopCallListener(call: PluginCall) {
        callStateReceiver?.let { context.unregisterReceiver(it); callStateReceiver = null }
        call.resolve()
    }

    @PluginMethod
    override fun checkPermissions(call: PluginCall) {
        val result = JSObject()
        result.put("callLog", getPermissionState("callLog").name.lowercase())
        result.put("phoneState", getPermissionState("phoneState").name.lowercase())
        result.put("callPhone", getPermissionState("callPhone").name.lowercase())
        result.put("contacts", getPermissionState("contacts").name.lowercase())
        result.put("sms", getPermissionState("sms").name.lowercase())
        result.put("microphone", getPermissionState("microphone").name.lowercase())
        result.put("overlay", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) "granted" else "denied")
        call.resolve(result)
    }

    @PluginMethod
    override fun requestPermissions(call: PluginCall) {
        if (hasRequiredPermissions()) {
            checkPermissions(call)
        } else {
            requestAllPermissions(call, "permissionsCallback")
        }
    }

    @PermissionCallback
    private fun permissionsCallback(call: PluginCall) {
        checkPermissions(call)
    }

    @PluginMethod
    fun requestOverlayPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            startActivityForResult(call, intent, "overlayPermissionCallback")
        } else {
            call.resolve(JSObject().apply { put("overlay", "granted") })
        }
    }

    @ActivityCallback
    private fun overlayPermissionCallback(call: PluginCall, result: ActivityResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(context)) {
            call.resolve(JSObject().apply { put("overlay", "granted") })
        } else {
            call.resolve(JSObject().apply { put("overlay", "denied") })
        }
    }

    fun emitCallIncoming(number: String, name: String?) {
        val data = JSObject().apply { put("number", number); put("name", name ?: getContactNameByNumber(number) ?: ""); put("timestamp", System.currentTimeMillis()) }
        notifyListeners("callIncoming", data)
    }

    fun emitCallStarted(number: String, name: String?, startTime: Long) {
        val contactName = name ?: getContactNameByNumber(number)
        val data = JSObject().apply { put("number", number); put("name", contactName ?: ""); put("startTime", startTime) }
        notifyListeners("callStarted", data)
        launchCallOverlay(number, contactName ?: "", 0, CallOverlayService.MODE_DURING_CALL)
    }

    fun emitCallEnded(number: String, name: String?, startTime: Long, endTime: Long) {
        val contactName = name ?: getContactNameByNumber(number)
        val duration = ((endTime - startTime) / 1000).toInt()
        val data = JSObject().apply { put("number", number); put("name", contactName ?: ""); put("startTime", startTime); put("endTime", endTime); put("duration", duration) }
        notifyListeners("callEnded", data)
        if (startTime > 0) launchCallOverlay(number, contactName ?: "", duration, CallOverlayService.MODE_AFTER_CALL) else CallOverlayService.stop(context)
    }

    fun emitOverlaySubmitted(data: JSObject) { notifyListeners("callOverlaySubmitted", data) }

    private fun launchCallOverlay(number: String, name: String, duration: Int, mode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return
        val intent = Intent(context, CallOverlayService::class.java).apply { putExtra("number", number); putExtra("name", name); putExtra("duration", duration); putExtra("mode", mode); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
    }

    @PluginMethod
    fun getContacts(call: PluginCall) {
        if (getPermissionState("contacts") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing contacts permission")
        }
        pluginScope.launch {
            try {
                val search = call.getString("search")
                val limit = call.getInt("limit", 500) ?: 500
                val offset = call.getInt("offset", 0) ?: 0
                val contacts = ContactsHelper.getContacts(context, search, limit, offset)
                withContext(Dispatchers.Main) {
                    val array = com.getcapacitor.JSArray()
                    contacts.forEach { c ->
                        val obj = JSObject().apply { put("id", c.id); put("name", c.name); val ns = com.getcapacitor.JSArray(); c.numbers.forEach { ns.put(it) }; put("numbers", ns); put("photoUri", c.photoUri ?: "") }
                        array.put(obj)
                    }; val res = JSObject().apply { put("contacts", array); put("total", contacts.size) }; call.resolve(res)
                }
            } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
        }
    }

    @PluginMethod
    fun getSMSThreads(call: PluginCall) {
        if (getPermissionState("sms") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing SMS permission")
        }
        pluginScope.launch {
            try {
                val threads = SMSHelper.getSMSThreads(context)
                withContext(Dispatchers.Main) {
                    val array = com.getcapacitor.JSArray()
                    threads.forEach { t ->
                        val obj = JSObject().apply { put("id", t.id); put("snippet", t.snippet); put("date", t.date); put("msgCount", t.msgCount); put("address", t.address); put("contactName", getContactNameByNumber(t.address) ?: "") }
                        array.put(obj)
                    }; val res = JSObject().apply { put("threads", array) }; call.resolve(res)
                }
            } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
        }
    }

    @PluginMethod
    fun getSMSMessages(call: PluginCall) {
        if (getPermissionState("sms") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing SMS permission")
        }
        val threadId = call.getString("threadId") ?: return call.reject("INVALID_ARGUMENT", "threadId required")
        pluginScope.launch {
            try {
                val messages = SMSHelper.getMessagesForThread(context, threadId)
                withContext(Dispatchers.Main) {
                    val array = com.getcapacitor.JSArray()
                    messages.forEach { m ->
                        val obj = JSObject().apply { put("id", m.id); put("address", m.address); put("body", m.body); put("date", m.date); put("type", m.type) }
                        array.put(obj)
                    }; val res = JSObject().apply { put("messages", array) }; call.resolve(res)
                }
            } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
        }
    }

    @PluginMethod
    fun sendSMS(call: PluginCall) {
        if (getPermissionState("sms") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing SMS permission")
        }
        val nr = call.getString("number"); val msg = call.getString("message")
        if (nr == null || msg == null) return call.reject("INVALID_ARGUMENT", "Params missing")
        try { SMSHelper.sendSMS(nr, msg); call.resolve() } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
    }

    @PluginMethod
    fun startRecording(call: PluginCall) {
        if (getPermissionState("microphone") != com.getcapacitor.PermissionState.GRANTED) {
            return call.reject("PERMISSION_DENIED", "Missing microphone permission")
        }
        if (recorder != null) return call.reject("UNAVAILABLE", "Already recording")
        try {
            val externalDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
            recordingFile = File(externalDir ?: context.cacheDir, "rec_${System.currentTimeMillis()}.mp3")
            recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()).apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(recordingFile?.absolutePath); prepare(); start() }
            call.resolve()
        } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
    }

    @PluginMethod
    fun pauseRecording(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.pause()
                call.resolve()
            } catch(e: Exception) {
                call.reject("UNAVAILABLE", e.message, e)
            }
        } else {
            call.reject("FEATURE_NOT_SUPPORTED", "Pause requires Android N+")
        }
    }

    @PluginMethod
    fun resumeRecording(call: PluginCall) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                recorder?.resume()
                call.resolve()
            } catch(e: Exception) {
                call.reject("UNAVAILABLE", e.message, e)
            }
        } else {
            call.reject("FEATURE_NOT_SUPPORTED", "Resume requires Android N+")
        }
    }

    @PluginMethod
    fun stopRecording(call: PluginCall) {
        if (recorder == null) return call.reject("UNAVAILABLE", "Not recording")
        try {
            recorder?.apply { stop(); release() }; recorder = null
            val res = JSObject().apply { put("filePath", recordingFile?.absolutePath ?: "") }; call.resolve(res)
        } catch (e: Exception) { call.reject("UNAVAILABLE", e.message, e) }
    }

    private fun getContactNameByNumber(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
        return cursor?.use { if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)) else null }
    }
}
