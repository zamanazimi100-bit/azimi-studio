package com.azimi.guardian
 
import android.app.Activity
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
 
class MainActivity : Activity() {
 
```
private var originAuthenticationPending = false  

private val bg = 0xFF050505.toInt()  
private val white = 0xFFFFFFFF.toInt()  
private val gray = 0xFFAAAAAA.toInt()  
private val green = 0xFF50DC8C.toInt()  
private val amber = 0xFFF0BE50.toInt()  
private val blue = 0xFF4DA6FF.toInt()  
private val purple = 0xFFB56CFF.toInt()  
private val cyan = 0xFF40E0D0.toInt()  
private val red = 0xFFFF6B6B.toInt()  

override fun onCreate(savedInstanceState: Bundle?) {  
    super.onCreate(savedInstanceState)  

    GuardianDiagnosticsStartup.start(this)  

    showHome()  
}  

private fun baseLayout(): LinearLayout =  
    LinearLayout(this).apply {  
        orientation = LinearLayout.VERTICAL  
        setPadding(28, 38, 28, 30)  
        setBackgroundColor(bg)  
    }  

private fun title(text: String): TextView =  
    TextView(this).apply {  
        this.text = text  
        textSize = 28f  
        setTextColor(white)  
        setPadding(0, 0, 0, 8)  
    }  

private fun subtitle(text: String): TextView =  
    TextView(this).apply {  
        this.text = text  
        textSize = 13f  
        setTextColor(gray)  
        setPadding(0, 0, 0, 18)  
    }  

private fun section(text: String): TextView =  
    TextView(this).apply {  
        this.text = text  
        textSize = 12f  
        setTextColor(gray)  
        setPadding(0, 22, 0, 8)  
    }  

private fun info(text: String): TextView =  
    TextView(this).apply {  
        this.text = text  
        textSize = 16f  
        setTextColor(white)  
        setPadding(0, 8, 0, 12)  
    }  

private fun status(text: String, color: Int): TextView =  
    TextView(this).apply {  
        this.text = text  
        textSize = 14f  
        setTextColor(color)  
        setPadding(0, 7, 0, 7)  
    }  

private fun actionButton(  
    text: String,  
    action: () -> Unit  
): Button =  
    Button(this).apply {  
        this.text = text  
        textSize = 15f  
        isAllCaps = false  

        setTextColor(  
            when {  
                text.contains("Z CONTROL") -> blue  
                text.contains("Z VAULT") -> purple  
                text.contains("Z RECOVERY") -> amber  
                text.contains("Z SHIELD") -> cyan  
                text.contains("AZIMI AI") -> purple  
                text.contains("Z LAB") -> green  
                else -> white  
            }  
        )  

        set  
```            setOnClickListener {
                action()
            }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
            }
        }

    private fun screen(content: LinearLayout): ScrollView =
        ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(content)
        }

    private fun header(
        layout: LinearLayout,
        name: String,
        description: String
    ) {
        layout.addView(title(name))
        layout.addView(subtitle(description))
        layout.addView(status("------------------------------", gray))
    }

    private fun showHome() {
        val layout = baseLayout()

        header(
            layout,
            "AZIMI CORE",
            "AZIMI GUARDIAN · PERSONAL COMMAND CENTER"
        )

        layout.addView(
            status(
                "● GUARDIAN ONLINE",
                green
            )
        )

        layout.addView(
            info(
                "Privacy-first device control and security foundation."
            )
        )

        layout.addView(section("LIVE SYSTEM STATE"))

        layout.addView(
            status(
                "● Z VAULT     ${GuardianStorage.getVaultStatus(this)}",
                if (GuardianStorage.getVaultStatus(this) == "UNLOCKED") {
                    green
                } else {
                    amber
                }
            )
        )

        layout.addView(
            status(
                "● AI POLICY   ${GuardianStorage.getAIMemoryPolicy(this)}",
                amber
            )
        )

        layout.addView(
            status(
                "● Z SHIELD    NOT CONFIGURED",
                amber
            )
        )

        layout.addView(
            status(
                "● Z CONNECT   AUTHORIZATION REQUIRED",
                amber
            )
        )

        layout.addView(section("AZIMI SPACES"))

        layout.addView(
            actionButton("🛡  Z CONTROL") {
                showControl()
            }
        )

        layout.addView(
            actionButton("🔐  Z VAULT") {
                showVault()
            }
        )

        layout.addView(
            actionButton("🛠  Z RECOVERY") {
                showRecovery()
            }
        )

        layout.addView(
            actionButton("🛡  Z SHIELD") {
                showShield()
            }
        )

        layout.addView(
            actionButton("🤖  AZIMI AI") {
                showAI()
            }
        )

        layout.addView(
            actionButton("🧪  Z LAB") {
                showLab()
            }
        )

        layout.addView(
            actionButton("🜂  Z ORIGIN") {

                originAuthenticationPending = true

                if (VaultAuth.requestAuthentication(this)) {
                    return@actionButton
                }

                originAuthenticationPending = false

                message(
                    "Z ORIGIN",
                    "Android device authentication could not be started."
                )
            }
        )

        layout.addView(section("SECURITY PRINCIPLE"))

        layout.addView(
            info(
                "Guardian never silently accesses passwords, " +
                    "verification codes, recovery codes, API keys " +
                    "or private credentials."
            )
        )

        setContentView(screen(layout))
    }
