package com.capacitor.callmanager

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.*
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewAssetLoader.AssetsPathHandler
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.widget.*
import androidx.core.app.NotificationCompat
import com.getcapacitor.JSObject
import java.net.URLEncoder
import android.net.Uri

/**
 * CallOverlayService — Floating CRM Portal
 * =============================================================================
 * Yeh ek Foreground Service hai jo call ke waqt ya baad mein screen pe overlay dikhati hai.
 * 
 * WHY SERVICE?
 *  - Kyunki jab user kisi aur app mein ho ya call screen pe ho, tab bhi hamara UI dikhna chahiye.
 *  - Foreground service ensured hai ki Android system isse easily kill na kare.
 * 
 * WEBVIEW BRIDGE:
 *  - 'CallManagerBridge' name se ek JavaScript interface inject kiya jata hai.
 *  - Overlay (React/HTML) se 'close()', 'submitResult()', ya 'openApp()' calls handle karta hai.
 * =============================================================================
 */
class CallOverlayService : Service() {
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "call_overlay_channel"
        const val MODE_DURING_CALL = "DURING_CALL"
        const val MODE_AFTER_CALL = "AFTER_CALL"
        fun stop(context: Context) { context.stopService(Intent(context, CallOverlayService::class.java)) }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var currentMode = MODE_AFTER_CALL

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nr = intent?.getStringExtra("number") ?: ""
        val name = intent?.getStringExtra("name") ?: ""
        val dur = intent?.getIntExtra("duration", 0) ?: 0
        val entityType = intent?.getStringExtra("entityType") ?: ""
        val entityId = intent?.getStringExtra("entityId") ?: ""
        currentMode = intent?.getStringExtra("mode") ?: MODE_AFTER_CALL
        
        Log.d("CallOverlayService", "onStartCommand: num=$nr, mode=$currentMode, dur=$dur")
        
        // Re-declare foreground to stay alive (Android 14)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
             Log.w("CallOverlayService", "Overlay permission missing, showing notification only")
             return START_STICKY 
        }

        if (nr.isNotBlank()) {
            removeOverlay()
            showOverlay(nr, name, dur, entityType, entityId)
        }
        
        return START_STICKY
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showOverlay(number: String, name: String, duration: Int, entityType: String = "", entityId: String = "") {
        val prefs = getSharedPreferences("CallManagerConfig", MODE_PRIVATE)
        val targetPath = prefs.getString("overlay_url", null)
        val autoOpen = prefs.getBoolean("auto_open_app", false)

        if (autoOpen && number != "" && currentMode == MODE_AFTER_CALL) {
            Log.d("CallOverlayService", "Auto-Open App Enabled: Triggering via Deep Link")
            val encodedName = URLEncoder.encode(name, "UTF-8")
            val deepLinkUrl = "ibase://overlay/after-call?source=app_link&number=$number&name=$encodedName&duration=$duration"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) // Pull to front
                setPackage(packageName)
            }
            try {
                startActivity(intent)
                removeOverlay()
                return
            } catch (e: Exception) {
                Log.e("CallOverlayService", "Deep link failed", e)
            }
        }
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        
        if (!targetPath.isNullOrBlank()) {
            val root = FrameLayout(this)
            
            // 1. WebView
            val webView = WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.allowContentAccess = true
                settings.databaseEnabled = true
                setBackgroundColor(0) // Transparent
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
                
                val assetLoader = WebViewAssetLoader.Builder()
                    .setDomain("appassets.androidplatform.net")
                    .addPathHandler("/assets/", AssetsPathHandler(this@CallOverlayService))
                    .build()

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                        return assetLoader.shouldInterceptRequest(request.url)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("CallOverlayService", "WebView loaded: $url")
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        Log.e("CallOverlayService", "WebView error: ${error.description} for ${request.url}")
                        if (request.isForMainFrame) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(this@CallOverlayService, "Overlay Error: ${error.description}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                
                addJavascriptInterface(OverlayBridge(this@CallOverlayService), "CallManagerBridge")
                
                val encodedName = URLEncoder.encode(name, "UTF-8")
                val queryParams = "source=overlay&number=$number&name=$encodedName&duration=$duration&mode=$currentMode"
                
                // Select correct React route
                val path = if (currentMode == MODE_DURING_CALL) "/overlay/during-call" else "/overlay/after-call"
                val fullUrl = "https://appassets.androidplatform.net/assets/public/index.html?path=$path&$queryParams"
                
                Log.d("CallOverlayService", "Loading URL: $fullUrl")
                loadUrl(fullUrl)
            }
            
            root.apply {
                setBackgroundColor(0) // Ensure FrameLayout is also transparent
            }
            root.addView(webView, FrameLayout.LayoutParams(-1, -1))
            
            overlayView = root
        } else {
            val inflater = LayoutInflater.from(this)
            val resId = resources.getIdentifier(if (currentMode == MODE_DURING_CALL) "overlay_during_call" else "overlay_after_call", "layout", packageName)
            
            if (resId == 0) {
                overlayView = if (currentMode == MODE_DURING_CALL) createDuringCallViewFallback(number, name) else createAfterCallViewFallback(number, name, duration, entityType, entityId)
            } else {
                overlayView = inflater.inflate(resId, null)
                if (currentMode == MODE_DURING_CALL) setupDuringCallView(overlayView!!, number, name)
                else setupAfterCallView(overlayView!!, number, name, duration)
            }
        }

        val width = prefs.getInt("overlay_width", if (currentMode == MODE_DURING_CALL) WindowManager.LayoutParams.WRAP_CONTENT else WindowManager.LayoutParams.MATCH_PARENT)
        val height = prefs.getInt("overlay_height", WindowManager.LayoutParams.WRAP_CONTENT)

        val params = WindowManager.LayoutParams(
            width,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (currentMode == MODE_DURING_CALL) Gravity.TOP or Gravity.START else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            if (currentMode == MODE_DURING_CALL) { x = 40; y = 200 } else { y = 120 }
        }

        if (currentMode == MODE_DURING_CALL || !targetPath.isNullOrBlank()) {
            makeDraggable(overlayView!!, params)
        }

        try {
            if (overlayView?.parent == null) {
                windowManager?.addView(overlayView, params)
                Log.d("CallOverlayService", "Successfully added overlay view for $number")
            } else {
                Log.d("CallOverlayService", "Overlay view already added, skipping addView")
            }
            CallManagerPlugin.instance?.emitOverlayLifecycleEvent("callOverlayOpened", number)
        } catch (e: Exception) {
            Log.e("CallOverlayService", "Failed to add overlay view", e)
        }
    }

    private inner class OverlayBridge(val context: Context) {
        @JavascriptInterface
        fun submitResult(dataJson: String) {
            Handler(Looper.getMainLooper()).post {
                try {
                    val data = JSObject()
                    try {
                        val obj = org.json.JSONObject(dataJson)
                        val keys = obj.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            data.put(key, obj.get(key))
                        }
                    } catch (e: Exception) {
                        Log.e("CallOverlayService", "JSON parse error", e)
                    }
                    val plugin = CallManagerPlugin.instance
                    if (plugin != null) {
                        plugin.emitOverlaySubmitted(data)
                    } else {
                        savePendingSubmission(context, dataJson)
                    }
                    
                    val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
                    if (bgEnabled) removeOverlay() else stopSelf()
                } catch (e: Exception) {
                    Log.e("CallOverlayService", "Failed to submit overlay result via bridge", e)
                }
            }
        }

        @JavascriptInterface
        fun close() {
            val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
            Handler(Looper.getMainLooper()).post { 
                if (bgEnabled) removeOverlay() else stopSelf() 
            }
        }

        @JavascriptInterface
        fun openApp(url: String?) {
            Handler(Looper.getMainLooper()).post {
                try {
                    val intent = if (!url.isNullOrBlank()) {
                        val uri = android.net.Uri.parse(url)
                        if (uri.scheme == null) {
                            Log.e("CallOverlayService", "Invalid URL scheme in openApp: $url")
                            return@post
                        }
                        Intent(Intent.ACTION_VIEW, uri)
                    } else {
                        context.packageManager.getLaunchIntentForPackage(context.packageName)
                    }
                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    
                    val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
                    if (bgEnabled) removeOverlay() else stopSelf()
                } catch (e: Exception) {
                    Log.e("CallOverlayService", "Failed to open app via bridge", e)
                }
            }
        }
    }

    private fun setupDuringCallView(view: View, number: String, name: String) {
        val tvCallActive = view.findViewById<TextView>(resources.getIdentifier("tvCallActive", "id", packageName))
        tvCallActive?.text = "Call Active: ${if (name.isNotBlank()) name else number}"
    }

    private fun setupAfterCallView(view: View, number: String, name: String, duration: Int) {
        val contactNameTv = view.findViewById<TextView>(resources.getIdentifier("tvContactName", "id", packageName))
        val callDetailsTv = view.findViewById<TextView>(resources.getIdentifier("tvCallDetails", "id", packageName))
        val btnClose = view.findViewById<TextView>(resources.getIdentifier("btnClose", "id", packageName))
        val btnInterested = view.findViewById<Button>(resources.getIdentifier("btnInterested", "id", packageName))
        val btnFollowUp = view.findViewById<Button>(resources.getIdentifier("btnFollowUp", "id", packageName))
        val btnNotInterested = view.findViewById<Button>(resources.getIdentifier("btnNotInterested", "id", packageName))
        val etNotes = view.findViewById<EditText>(resources.getIdentifier("etNotes", "id", packageName))
        val btnSave = view.findViewById<Button>(resources.getIdentifier("btnSave", "id", packageName))

        contactNameTv?.text = if (name.isNotBlank()) name else "Contact"
        callDetailsTv?.text = "$number • ${duration/60}m ${duration%60}s"
        btnClose?.setOnClickListener { 
            val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
            if (bgEnabled) removeOverlay() else stopSelf() 
        }

        var selectedStatus = "FOLLOW_UP"
        val statusButtons = listOf(btnInterested, btnFollowUp, btnNotInterested)

        val selectStatus = { btn: Button?, status: String ->
            btn?.let {
                selectedStatus = status
                statusButtons.forEach { b -> b?.alpha = 0.5f; b?.setTextColor(Color.parseColor("#A0A0C0")) }
                it.alpha = 1.0f
                when (status) {
                    "INTERESTED" -> it.setTextColor(Color.parseColor("#10B981"))
                    "FOLLOW_UP" -> it.setTextColor(Color.parseColor("#F59E0B"))
                    "NOT_INTERESTED" -> it.setTextColor(Color.parseColor("#EF4444"))
                }
            }
        }

        btnInterested?.setOnClickListener { selectStatus(btnInterested, "INTERESTED") }
        btnFollowUp?.setOnClickListener { selectStatus(btnFollowUp, "FOLLOW_UP") }
        btnNotInterested?.setOnClickListener { selectStatus(btnNotInterested, "NOT_INTERESTED") }
        
        // Initial state
        selectStatus(btnFollowUp, "FOLLOW_UP")

        btnSave?.setOnClickListener {
            val submitData = JSObject().apply { 
                put("number", number)
                put("name", name)
                put("status", selectedStatus)
                put("notes", etNotes?.text?.toString() ?: "")
                put("duration", duration)
                put("timestamp", System.currentTimeMillis()) 
            }
            
            val plugin = CallManagerPlugin.instance
            if (plugin != null) {
                plugin.emitOverlaySubmitted(submitData)
            } else {
                savePendingSubmission(this@CallOverlayService, submitData.toString())
            }
            val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
            if (bgEnabled) {
                removeOverlay()
            } else {
                stopSelf()
            }
        }
    }

    private fun makeDraggable(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                else -> false
            }
        }
    }

    // --- FALLBACKS IF XML RESOURCES FAIL TO LOAD ---
    private fun createDuringCallViewFallback(number: String, name: String): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(32, 24, 32, 24)
            background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#CC1E1E3F")); cornerRadius = 100f; setStroke(2, Color.parseColor("#4B4BCC")) }
            addView(View(this@CallOverlayService).apply { layoutParams = LinearLayout.LayoutParams(24, 24).apply { marginEnd = 16 }; background = android.graphics.drawable.GradientDrawable().apply { shape = android.graphics.drawable.GradientDrawable.OVAL; setColor(Color.parseColor("#10B981")) } })
            addView(TextView(this@CallOverlayService).apply { text = "Call Active: ${if (name.isNotBlank()) name else number}"; textSize = 12f; setTextColor(-1) })
        }
    }

    private fun createAfterCallViewFallback(number: String, name: String, duration: Int, entityType: String = "", entityId: String = ""): View {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 40, 48, 32); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#1E1E3F")); cornerRadius = 48f } }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@CallOverlayService).apply {
                orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addView(TextView(this@CallOverlayService).apply { text = "Call Session Result"; textSize = 12f; setTextColor(Color.parseColor("#7B7BCC")) })
                addView(TextView(this@CallOverlayService).apply { text = if (name.isNotBlank()) name else "Contact"; textSize = 20f; setTextColor(-1); setTypeface(null, 1) })
                val entityTag = if (entityType.isNotBlank() || entityId.isNotBlank()) " [$entityType: $entityId]" else ""
                addView(TextView(this@CallOverlayService).apply { text = "$number • ${duration/60}m ${duration%60}s$entityTag"; textSize = 13f; setTextColor(Color.parseColor("#A0A0C0")) })
            })
            addView(TextView(this@CallOverlayService).apply { text = "✕"; textSize = 20f; setTextColor(Color.parseColor("#A0A0C0")); setPadding(20,20,20,20); setOnClickListener { 
                val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
                if (bgEnabled) removeOverlay() else stopSelf() 
            } })
        }
        container.addView(top)
        var selectedStatus = "FOLLOW_UP"; val sButtons = mutableListOf<Button>()
        val sRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; weightSum = 3f }
        listOf("INTERESTED" to "#10B981", "FOLLOW_UP" to "#F59E0B", "NOT_INTERESTED" to "#EF4444").forEach { (s, c) ->
            val b = Button(this).apply {
                text = s.replace("_", " "); textSize = 10f; setTextColor(-1)
                background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#2A2A4A")); cornerRadius = 16f }
                layoutParams = LinearLayout.LayoutParams(0, 100, 1f).apply { marginEnd = 8 }
                setOnClickListener { selectedStatus = s; sButtons.forEach { it.alpha = 0.5f; it.setTextColor(Color.GRAY) }; alpha = 1f; setTextColor(Color.parseColor(c)) }
            }
            sButtons.add(b); sRow.addView(b)
        }
        sButtons[1].apply { alpha = 1f; setTextColor(Color.parseColor("#F59E0B")) }; container.addView(sRow)
        val notes = EditText(this).apply { hint = "Notes..."; setHintTextColor(Color.parseColor("#555580")); setTextColor(-1); setBackgroundColor(Color.parseColor("#2A2A4A")); setPadding(24,24,24,24); minLines = 3; gravity = Gravity.TOP or Gravity.START; layoutParams = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 24, 0, 24) } }
        container.addView(notes)
        container.addView(Button(this).apply {
            text = "SAVE DETAILS"; setTextColor(-1); setTypeface(null, 1); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#4B4BCC")); cornerRadius = 24f }
            layoutParams = LinearLayout.LayoutParams(-1, 140); setOnClickListener {
                val submitData = JSObject().apply { put("number", number); put("name", name); put("status", selectedStatus); put("notes", notes.text.toString()); put("duration", duration); put("timestamp", System.currentTimeMillis()) }
                
                val plugin = CallManagerPlugin.instance
                if (plugin != null) {
                    plugin.emitOverlaySubmitted(submitData)
                } else {
                    savePendingSubmission(this@CallOverlayService, submitData.toString())
                }
                val bgEnabled = getSharedPreferences("CallManagerConfig", MODE_PRIVATE).getBoolean("background_enabled", true)
                if (bgEnabled) removeOverlay() else stopSelf()
            }
        })
        return container
    }

    private fun removeOverlay() { 
        try { 
            overlayView?.let { 
                Log.d("CallOverlayService", "Removing overlay view")
                windowManager?.removeView(it)
                if (it is WebView) {
                    it.stopLoading()
                    it.destroy()
                }
            }
            overlayView = null 
            CallManagerPlugin.instance?.emitOverlayLifecycleEvent("callOverlayClosed", null)
        } catch (e: Exception) {
            Log.e("CallOverlayService", "Error removing overlay", e)
        } 
    }
    private fun createNotificationChannel() { 
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "Ibase Call Service", NotificationManager.IMPORTANCE_HIGH)
            channel.description = "Persistent background service for incoming calls"
            manager.createNotificationChannel(channel)
        }
    }
    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Ibase Overlay Active")
        .setContentText("Listening for calls...")
        .setSmallIcon(android.R.drawable.ic_menu_call)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(Notification.CATEGORY_SERVICE)
        .build()
    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { super.onDestroy(); removeOverlay() }

    private fun savePendingSubmission(context: Context, dataJson: String) {
        val prefs = context.getSharedPreferences("CallManagerPending", Context.MODE_PRIVATE)
        val existing = prefs.getStringSet("pending_submissions", mutableSetOf()) ?: mutableSetOf()
        val newSet = mutableSetOf<String>().apply { addAll(existing); add(dataJson) }
        prefs.edit().putStringSet("pending_submissions", newSet).apply()
    }
}
