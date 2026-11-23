package com.example.yallaquicksend

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.*
import android.graphics.Color
import android.content.Context

class ModOverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: View? = null
    private var running = false
    private var index = 0
    private var alphabet = false
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showPanel()
        return START_STICKY
    }

    private fun params(w: Int, h: Int) = WindowManager.LayoutParams(
        w, h,
        if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    )

    private fun showPanel() {
        if (panel != null) return
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(10,10,10,10)
            setBackgroundColor(Color.rgb(30,27,52))
        }
        val head = TextView(this).apply {
            text = "YALLA QUICK SEND  ✕"
            setTextColor(Color.WHITE); textSize = 16f
            setOnClickListener { stopSelf() }
        }
        root.addView(head)
        val modes = LinearLayout(this)
        val number = Button(this).apply { text = "123"; setOnClickListener { alphabet=false; index=0 } }
        val abc = Button(this).apply { text = "ABC"; setOnClickListener { alphabet=true; index=0 } }
        modes.addView(number, LinearLayout.LayoutParams(0,48,1f)); modes.addView(abc, LinearLayout.LayoutParams(0,48,1f))
        root.addView(modes)
        val grid = GridLayout(this).apply { columnCount=5 }
        val chars = listOf("1","2","4","5","6","A","B","C","D","E","F","G","H","I","J")
        chars.forEach { c ->
            val b=Button(this).apply { text=c; setOnClickListener { KeyboardBridge.insert(this@ModOverlayService,c) } }
            grid.addView(b, GridLayout.LayoutParams().apply { width=0; height=48; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f) })
        }
        root.addView(grid)
        val send=Button(this).apply {
            text="➤ SEND"
            setOnClickListener { YallaAccessibilityService.instance?.clickVisibleSend()
                ?: Toast.makeText(this@ModOverlayService,"Enable Accessibility service first",Toast.LENGTH_SHORT).show() }
        }
        root.addView(send)
        val auto=LinearLayout(this)
        val start=Button(this).apply { text="AUTO START"; setOnClickListener { running=true; index=0; handler.post(autoTask) } }
        val stop=Button(this).apply { text="STOP"; setOnClickListener { running=false; handler.removeCallbacks(autoTask) } }
        auto.addView(start,LinearLayout.LayoutParams(0,48,1f)); auto.addView(stop,LinearLayout.LayoutParams(0,48,1f))
        root.addView(auto)
        panel=root
        wm.addView(root,params(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity=Gravity.TOP or Gravity.END; x=8; y=120
        })
    }

    private val autoTask=object:Runnable {
        override fun run() {
            if(!running) return
            val p=getSharedPreferences("settings",Context.MODE_PRIVATE)
            val seq=if(alphabet) p.getString("letters","ABCDEFGHIJKLMNOPQRSTUVWXYZ") ?: "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                     else p.getString("numbers","12456") ?: "12456"
            if(seq.isNotEmpty()) {
                KeyboardBridge.insert(this@ModOverlayService,seq[index++ % seq.length].toString())
                YallaAccessibilityService.instance?.clickVisibleSend()
            }
            handler.postDelayed(this,p.getInt("delay",500).toLong().coerceIn(300,5000))
        }
    }

    override fun onDestroy() {
        running=false; handler.removeCallbacks(autoTask)
        if(panel!=null) { wm.removeView(panel); panel=null }
        super.onDestroy()
    }
}
