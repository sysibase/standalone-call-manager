package com.capacitor.callmanager

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
        Permission(strings = ["android.permission.POST_NOTIFICATIONS"], alias = "notifications"),
    ]
)
/**
 * CallManagerPlugin — The Main Orchestrator
 * =============================================================================
 * Yeh plugin frontend (TypeScript) aur native Android features (CallLogs, Contacts, Overlay)
 * ke beech ka main bridge hai.
 * 
 * DESIGN PHILOSOPHY:
 *  - Non-Blocking: Bhari operations (DB sync, log fetching) 'execute' block mein hote hain.
 *  - Fault Tolerant: Har method mein range checks aur null-safety ka dhyan rakha gaya hai.
 *  - Memory Efficient: Large lists fetch karne ke liye cursor-based helpers use hote hain.
 * =============================================================================
 */
class CallManagerPlugin : Plugin() {
    companion object {
        var instance: CallManagerPlugin? = null
            private set
    }

    private var callStateReceiver: CallStateReceiver? = null
    private val pluginScope = CoroutineScope(Dispatchers.IO)

    override fun load() {
        instance = this
    }

    override fun handleOnDestroy() {
        super.handleOnDestroy()
        callStateReceiver?.let { context.unregisterReceiver(it); callStateReceiver = null }
        pluginScope.cancel()
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
            context.startActivity(intent)
            call.resolve(JSObject().apply { put("success", true) })
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
        call.resolve(JSObject().apply { put("success", true) })
    }

    @PluginMethod
    fun stopCallListener(call: PluginCall) {
        callStateReceiver?.let { context.unregisterReceiver(it); callStateReceiver = null }
        call.resolve(JSObject().apply { put("success", true) })
    }

    @PluginMethod
    override fun checkPermissions(call: PluginCall) {
        val result = JSObject()
        result.put("callLog", getPermissionState("callLog").name.lowercase())
        result.put("phoneState", getPermissionState("phoneState").name.lowercase())
        result.put("callPhone", getPermissionState("callPhone").name.lowercase())
        result.put("contacts", getPermissionState("contacts").name.lowercase())
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
        val details = CallFilterDatabase.getInstance(context).getDetails(number)
        val data = JSObject().apply { 
            put("number", number)
            put("name", name ?: details?.name ?: getContactNameByNumber(number) ?: "")
            put("entityType", details?.entityType ?: "")
            put("entityId", details?.entityId ?: "")
            put("timestamp", System.currentTimeMillis()) 
        }
        notifyListeners("callIncoming", data)
    }

    fun emitCallStarted(number: String, name: String?, startTime: Long) {
        val details = CallFilterDatabase.getInstance(context).getDetails(number)
        val contactName = name ?: details?.name ?: getContactNameByNumber(number)
        val data = JSObject().apply { 
            put("number", number)
            put("name", contactName ?: "")
            put("entityType", details?.entityType ?: "")
            put("entityId", details?.entityId ?: "")
            put("startTime", startTime) 
        }
        notifyListeners("callStarted", data)
        
        val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
        val trackingMode = prefs.getString("tracking_mode", "ALL") ?: "ALL"
        val shouldShow = trackingMode == "ALL" || details != null
        
        if (shouldShow) {
            launchCallOverlay(number, contactName ?: "", 0, CallOverlayService.MODE_DURING_CALL, details)
        } else {
            Log.d("CallManager", "Skipping overlay for $number based on tracking mode: $trackingMode")
        }
    }

    fun emitCallEnded(number: String, name: String?, startTime: Long, endTime: Long) {
        val details = CallFilterDatabase.getInstance(context).getDetails(number)
        val contactName = name ?: details?.name ?: getContactNameByNumber(number)
        val duration = ((endTime - startTime) / 1000).toInt()
        val data = JSObject().apply { 
            put("number", number)
            put("name", contactName ?: "")
            put("entityType", details?.entityType ?: "")
            put("entityId", details?.entityId ?: "")
            put("startTime", startTime)
            put("endTime", endTime)
            put("duration", duration) 
        }
        notifyListeners("callEnded", data)

        val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
        val trackingMode = prefs.getString("tracking_mode", "ALL") ?: "ALL"
        val shouldShow = trackingMode == "ALL" || details != null

        if (startTime > 0) {
            if (shouldShow) {
                launchCallOverlay(number, contactName ?: "", duration, CallOverlayService.MODE_AFTER_CALL, details)
            } else {
                CallOverlayService.stop(context)
            }
        } else {
            CallOverlayService.stop(context)
        }
    }

    fun emitOverlaySubmitted(data: JSObject) { notifyListeners("callOverlaySubmitted", data) }

    fun emitOverlayLifecycleEvent(event: String, number: String?) {
        val data = JSObject()
        if (number != null) {
            data.put("number", number)
            val details = CallFilterDatabase.getInstance(context).getDetails(number)
            if (details != null) {
                data.put("entityType", details.entityType)
                data.put("entityId", details.entityId)
            }
        }
        data.put("timestamp", System.currentTimeMillis())
        notifyListeners(event, data)
    }

    private fun launchCallOverlay(number: String, name: String, duration: Int, mode: String, details: CallFilterDatabase.TrackedItem? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) return
        val intent = Intent(context, CallOverlayService::class.java).apply { 
            putExtra("number", number)
            putExtra("name", name)
            putExtra("duration", duration)
            putExtra("mode", mode)
            if (details != null) {
                putExtra("entityType", details.entityType)
                putExtra("entityId", details.entityId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
        }
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
    fun showOverlay(call: PluginCall) {
        val number = call.getString("number", "") ?: ""
        val name = call.getString("name", "") ?: ""
        val duration = call.getInt("duration", 0) ?: 0
        val mode = call.getString("mode", "AFTER_CALL") ?: "AFTER_CALL"
        
        launchCallOverlay(number, name, duration, mode)
        call.resolve(JSObject().apply { put("success", true) })
    }

    @PluginMethod
    fun hideOverlay(call: PluginCall) {
        CallOverlayService.stop(context)
        call.resolve(JSObject().apply { put("success", true) })
    }

    @PluginMethod
    fun setOverlayConfig(call: PluginCall) {
        val url = call.getString("url")
        val height = call.getInt("height", -2) // WRAP_CONTENT
        val width = call.getInt("width", -1)   // MATCH_PARENT
        
        execute {
            try {
                val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putString("overlay_url", url)
                    putInt("overlay_height", height ?: -2)
                    putInt("overlay_width", width ?: -1)
                    apply()
                }
                call.resolve(JSObject().apply { put("success", true) })
            } catch (e: Exception) {
                call.reject("Failed to set overlay config", e)
            }
        }
    }

    @PluginMethod
    fun submitOverlayResult(call: PluginCall) {
        val data = call.data
        emitOverlaySubmitted(data)
        CallOverlayService.stop(context)
        call.resolve(JSObject().apply { put("success", true) })
    }

    @PluginMethod
    fun getPendingSubmissions(call: PluginCall) {
        execute {
            try {
                val prefs = context.getSharedPreferences("CallManagerPending", android.content.Context.MODE_PRIVATE)
                val pending = prefs.getStringSet("pending_submissions", mutableSetOf()) ?: mutableSetOf()
                
                val result = JSObject()
                val array = com.getcapacitor.JSArray()
                pending.forEach { 
                    try {
                        array.put(JSObject(it))
                    } catch (e: Exception) {
                        Log.e("CallManager", "Failed to parse pending submission: $it", e)
                    }
                }
                result.put("success", true)
                result.put("submissions", array)
                call.resolve(result)
            } catch (e: Exception) {
                call.reject("Failed to get pending submissions", e)
            }
        }
    }

    @PluginMethod
    fun clearPendingSubmissions(call: PluginCall) {
        execute {
            try {
                val prefs = context.getSharedPreferences("CallManagerPending", android.content.Context.MODE_PRIVATE)
                prefs.edit().remove("pending_submissions").apply()
                call.resolve(JSObject().apply { put("success", true) })
            } catch (e: Exception) {
                call.reject("Failed to clear pending submissions", e)
            }
        }
    }

    @PluginMethod
    fun setBackgroundServiceEnabled(call: PluginCall) {
        val enabled = call.getBoolean("enabled", true) ?: true
        execute {
            try {
                val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("background_enabled", enabled).apply()
                call.resolve(JSObject().apply { put("success", true) })
            } catch (e: Exception) {
                call.reject("Failed to set background service status", e)
            }
        }
    }

    @PluginMethod
    fun isBackgroundServiceEnabled(call: PluginCall) {
        execute {
            val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
            val enabled = prefs.getBoolean("background_enabled", true)
            val res = JSObject()
            res.put("success", true)
            res.put("enabled", enabled)
            call.resolve(res)
        }
    }

    @PluginMethod
    fun setTrackingMode(call: PluginCall) {
        val mode = call.getString("mode", "ALL") ?: "ALL"
        execute {
            try {
                val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("tracking_mode", mode).apply()
                call.resolve(JSObject().apply { put("success", true) })
            } catch (e: Exception) {
                call.reject("Failed to set tracking mode", e)
            }
        }
    }

    @PluginMethod
    fun getTrackingMode(call: PluginCall) {
        execute {
            val prefs = context.getSharedPreferences("CallManagerConfig", android.content.Context.MODE_PRIVATE)
            val mode = prefs.getString("tracking_mode", "ALL") ?: "ALL"
            call.resolve(JSObject().apply { 
                put("success", true)
                put("mode", mode) 
            })
        }
    }

    @PluginMethod
    fun addTrackedNumbers(call: PluginCall) {
        val itemsArray = call.getArray("items") ?: com.getcapacitor.JSArray()
        val items = mutableListOf<CallFilterDatabase.TrackedItem>()
        
        // Loop through the input array and map to our internal model.
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.getJSONObject(i)
            items.add(CallFilterDatabase.TrackedItem(
                number = obj.getString("number") ?: "",
                name = obj.optString("name"),
                entityType = obj.optString("entityType"),
                entityId = obj.optString("entityId")
            ))
        }
        
        // Execute in background to prevent UI lag on large batches
        execute {
            try {
                CallFilterDatabase.getInstance(context).addTrackedItems(items)
                val result = JSObject().apply {
                    put("success", true)
                    put("count", items.size)
                }
                call.resolve(result)
            } catch (e: Exception) {
                call.reject("Failed to add tracked items", e)
            }
        }
    }

    @PluginMethod
    fun removeTrackedNumbers(call: PluginCall) {
        val numbers = call.getArray("numbers")?.toList<String>() ?: emptyList()
        execute {
            try {
                val count = CallFilterDatabase.getInstance(context).removeNumbers(numbers)
                val result = JSObject().apply {
                    put("success", true)
                    put("count", count)
                }
                call.resolve(result)
            } catch (e: Exception) {
                call.reject("Failed to remove numbers", e)
            }
        }
    }

    @PluginMethod
    fun removeAllTrackedNumbers(call: PluginCall) {
        execute {
            try {
                CallFilterDatabase.getInstance(context).removeAll()
                call.resolve(JSObject().apply { put("success", true) })
            } catch (e: Exception) {
                call.reject("Failed to clear tracked items", e)
            }
        }
    }

    @PluginMethod
    fun removeTrackedNumbersByEntity(call: PluginCall) {
        val entityType = call.getString("entityType") ?: return call.reject("entityType is required")
        execute {
            try {
                val count = CallFilterDatabase.getInstance(context).removeByEntity(entityType)
                val result = JSObject().apply {
                    put("success", true)
                    put("count", count)
                }
                call.resolve(result)
            } catch (e: Exception) {
                call.reject("Failed to remove items by entity", e)
            }
        }
    }

    @PluginMethod
    fun removeTrackedNumbersByEntityId(call: PluginCall) {
        val entityId = call.getString("entityId") ?: return call.reject("entityId is required")
        execute {
            try {
                val count = CallFilterDatabase.getInstance(context).removeByEntityId(entityId)
                val result = JSObject().apply {
                    put("success", true)
                    put("count", count)
                }
                call.resolve(result)
            } catch (e: Exception) {
                call.reject("Failed to remove items by entity ID", e)
            }
        }
    }

    @PluginMethod
    fun getAllTrackedNumbers(call: PluginCall) {
        execute {
            try {
                val items = CallFilterDatabase.getInstance(context).getAll()
                val array = com.getcapacitor.JSArray()
                items.forEach { item ->
                    val obj = JSObject().apply {
                        put("number", item.number)
                        put("name", item.name)
                        put("entityType", item.entityType)
                        put("entityId", item.entityId)
                    }
                    array.put(obj)
                }
                call.resolve(JSObject().apply { 
                    put("success", true)
                    put("items", array) 
                })
            } catch (e: Exception) {
                call.reject("Failed to fetch all tracked items", e)
            }
        }
    }

    @PluginMethod
    fun getTrackedNumbersByEntity(call: PluginCall) {
        val entityType = call.getString("entityType") ?: return call.reject("entityType is required")
        execute {
            try {
                val items = CallFilterDatabase.getInstance(context).getByEntity(entityType)
                val array = com.getcapacitor.JSArray()
                items.forEach { item ->
                    val obj = JSObject().apply {
                        put("number", item.number)
                        put("name", item.name)
                        put("entityType", item.entityType)
                        put("entityId", item.entityId)
                    }
                    array.put(obj)
                }
                call.resolve(JSObject().apply { 
                    put("success", true)
                    put("items", array) 
                })
            } catch (e: Exception) {
                call.reject("Failed to fetch items by entity", e)
            }
        }
    }

    @PluginMethod
    fun getTrackedNumbersByEntityId(call: PluginCall) {
        val entityId = call.getString("entityId") ?: return call.reject("entityId is required")
        execute {
            try {
                val items = CallFilterDatabase.getInstance(context).getByEntityId(entityId)
                val array = com.getcapacitor.JSArray()
                items.forEach { item ->
                    val obj = JSObject().apply {
                        put("number", item.number)
                        put("name", item.name)
                        put("entityType", item.entityType)
                        put("entityId", item.entityId)
                    }
                    array.put(obj)
                }
                call.resolve(JSObject().apply { 
                    put("success", true)
                    put("items", array) 
                })
            } catch (e: Exception) {
                call.reject("Failed to fetch items by entity ID", e)
            }
        }
    }

    private fun getContactNameByNumber(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        val uri = Uri.withAppendedPath(android.provider.ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val cursor = context.contentResolver.query(uri, arrayOf(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
        return cursor?.use { if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(android.provider.ContactsContract.PhoneLookup.DISPLAY_NAME)) else null }
    }
}
