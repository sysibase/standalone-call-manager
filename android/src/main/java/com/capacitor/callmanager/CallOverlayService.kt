package com.capacitor.callmanager

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
import android.widget.*
import androidx.core.app.NotificationCompat
import com.getcapacitor.JSObject

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
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent ?: return START_NOT_STICKY
        val nr = intent.getStringExtra("number") ?: ""
        val name = intent.getStringExtra("name") ?: ""
        val dur = intent.getIntExtra("duration", 0)
        currentMode = intent.getStringExtra("mode") ?: MODE_AFTER_CALL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) return START_NOT_STICKY
        removeOverlay()
        showOverlay(nr, name, dur)
        return START_NOT_STICKY
    }

    private fun showOverlay(number: String, name: String, duration: Int) {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        
        val inflater = LayoutInflater.from(this)
        val resId = resources.getIdentifier(if (currentMode == MODE_DURING_CALL) "overlay_during_call" else "overlay_after_call", "layout", packageName)
        
        // Fallback to programmatic view if XML fails to load (standalone plugin resilience)
        if (resId == 0) {
            overlayView = if (currentMode == MODE_DURING_CALL) createDuringCallViewFallback(number, name) else createAfterCallViewFallback(number, name, duration)
        } else {
            overlayView = inflater.inflate(resId, null)
            if (currentMode == MODE_DURING_CALL) setupDuringCallView(overlayView!!, number, name)
            else setupAfterCallView(overlayView!!, number, name, duration)
        }

        val params = WindowManager.LayoutParams(
            if (currentMode == MODE_DURING_CALL) WindowManager.LayoutParams.WRAP_CONTENT else WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (currentMode == MODE_DURING_CALL) Gravity.TOP or Gravity.START else Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            if (currentMode == MODE_DURING_CALL) { x = 40; y = 200 } else { y = 120 }
        }

        if (currentMode == MODE_DURING_CALL) {
            makeDraggable(overlayView!!, params)
        }

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
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
        btnClose?.setOnClickListener { stopSelf() }

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
            CallManagerPlugin.instance?.emitOverlaySubmitted(JSObject().apply { 
                put("number", number)
                put("name", name)
                put("status", selectedStatus)
                put("notes", etNotes?.text?.toString() ?: "")
                put("duration", duration)
                put("timestamp", System.currentTimeMillis()) 
            })
            stopSelf()
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

    private fun createAfterCallViewFallback(number: String, name: String, duration: Int): View {
        val container = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 40, 48, 32); background = android.graphics.drawable.GradientDrawable().apply { setColor(Color.parseColor("#1E1E3F")); cornerRadius = 48f } }
        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(LinearLayout(this@CallOverlayService).apply {
                orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                addView(TextView(this@CallOverlayService).apply { text = "Call Session Result"; textSize = 12f; setTextColor(Color.parseColor("#7B7BCC")) })
                addView(TextView(this@CallOverlayService).apply { text = if (name.isNotBlank()) name else "Contact"; textSize = 20f; setTextColor(-1); setTypeface(null, 1) })
                addView(TextView(this@CallOverlayService).apply { text = "$number • ${duration/60}m ${duration%60}s"; textSize = 13f; setTextColor(Color.parseColor("#A0A0C0")) })
            })
            addView(TextView(this@CallOverlayService).apply { text = "✕"; textSize = 20f; setTextColor(Color.parseColor("#A0A0C0")); setPadding(20,20,20,20); setOnClickListener { stopSelf() } })
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
                CallManagerPlugin.instance?.emitOverlaySubmitted(JSObject().apply { put("number", number); put("name", name); put("status", selectedStatus); put("notes", notes.text.toString()); put("duration", duration); put("timestamp", System.currentTimeMillis()) })
                stopSelf()
            }
        })
        return container
    }

    private fun removeOverlay() { try { overlayView?.let { windowManager?.removeView(it) }; overlayView = null } catch (e: Exception) {} }
    private fun createNotificationChannel() { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL_ID, "Overlay", NotificationManager.IMPORTANCE_LOW)) }
    private fun buildNotification(): Notification = NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("CRM Overlay Active").setSmallIcon(android.R.drawable.ic_menu_call).setOngoing(true).build()
    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { super.onDestroy(); removeOverlay() }
}
