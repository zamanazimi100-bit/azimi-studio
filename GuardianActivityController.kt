package com.azimi.guardian

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class GuardianActivityController(
    private val activity: Activity
) {

    private val bg = 0xFF050505.toInt()
    private val white = 0xFFFFFFFF.toInt()
    private val softWhite = 0xFFB8B8B8.toInt()

    fun launchDiagnosticScreen(
        buildLabel: String,
        diagnosticLabel: String
    ) {

        val screen =
            LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundColor(bg)
                setPadding(32, 32, 32, 32)
            }

        val title =
            TextView(activity).apply {
                text = "AZIMI GUARDIAN"
                textSize = 24f
                setTextColor(white)
                gravity = Gravity.CENTER
            }

        val status =
            TextView(activity).apply {
                text =
                    "\n$buildLabel\n$diagnosticLabel\n\nGUARDIAN ACTIVITY ONLINE"
                textSize = 16f
                setTextColor(softWhite)
                gravity = Gravity.CENTER
            }

        screen.addView(title)

        screen.addView(
            status,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        activity.setContentView(screen)
    }

    fun configureWindow() {

        activity.window.setNavigationBarColor(bg)
        activity.window.setStatusBarColor(bg)

        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

        if (android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.R
        ) {

            activity.window.setDecorFitsSystemWindows(
                false
            )

            activity.window.decorView.setOnApplyWindowInsetsListener {
                    view,
                    insets ->

                val bars =
                    insets.getInsets(
                        android.view.WindowInsets.Type.systemBars()
                    )

                view.setPadding(
                    0,
                    bars.top,
                    0,
                    bars.bottom
                )

                insets
            }
        }
    }
}
