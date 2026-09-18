package com.azimi.guardian

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val bg = Color.rgb(5, 5, 5)
    private val panel = Color.rgb(18, 18, 18)
    private val white = Color.WHITE
    private val gray = Color.rgb(170, 170, 170)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun baseLayout(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(bg)
        }
    }

    private fun title(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 28f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 20)
        }
    }

    private fun section(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 13f
            setTextColor(gray)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 22, 0, 10)
        }
    }

    private fun info(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(white)
            setPadding(0, 10, 0, 10)
        }
    }

    private fun actionButton(text: String, action: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            textSize = 15f
            setOnClickListener { action() }

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
        }
    }

    private fun scroll(content: LinearLayout): ScrollView {
        return ScrollView(this).apply {
            setBackgroundColor(bg)
            addView(content)
        }
    }

    private fun header(content: LinearLayout, name: String) {
        content.addView(title(name))

        val line = TextView(this).apply {
            text = "────────────────────────"
            textSize = 12f
            setTextColor(Color.DKGRAY)
        }

        content.addView(line)
    }

    // ------------------------------------------------------------
    // HOME
    // ------------------------------------------------------------

    private fun showHome() {
        val layout = baseLayout()
        header(layout, "AZIMI GUARDIAN")

        layout.addView(info("SECURITY COMMAND CENTER"))
        layout.addView(
            info(
                "\nSYSTEM STATUS\n\n" +
                "● Z VAULT       LOCKED\n" +
                "● Z LAB         ISOLATED\n" +
                "● Z RECOVERY    READY\n" +
                "● Z CONTROL     PROTECTED\n" +
                "● Z CLOUD       SEPARATED\n" +
                "● Z CONNECT     AUTHORIZED ONLY\n" +
                "● AZIMI AI      RESTRICTED"
            )
        )

        layout.addView(section("GUARDIAN MODULES"))

        layout.addView(actionButton("🔐  Z VAULT") {
            showVault()
        })

        layout.addView(actionButton("🧪  Z LAB") {
            showLab()
        })

        layout.addView(actionButton("🛠  Z RECOVERY") {
            showRecovery()
        })

        layout.addView(actionButton("🛡  Z CONTROL") {
            showControl()
        })

        layout.addView(actionButton("☁  Z CLOUD") {
            showCloud()
        })

        layout.addView(actionButton("🔗  Z CONNECT") {
            showConnect()
        })

        layout.addView(actionButton("🤖  AZIMI AI") {
            showAI()
        })

        layout.addView(section("LANGUAGE"))

        layout.addView(actionButton("English / دری") {
            showLanguage()
        })

        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // Z VAULT
    // ------------------------------------------------------------

    private fun showVault() {
        val layout = baseLayout()
        header(layout, "Z VAULT")

        layout.addView(
            info(
                "🔒 VAULT STATUS\n\n" +
                "LOCKED\n\n" +
                "Protected local storage foundation."
            )
        )

        layout.addView(section("VAULT COMPARTMENTS"))

        layout.addView(actionButton("Personal") {
            showMessage("PERSONAL\n\nProtected compartment foundation.")
        })

        layout.addView(actionButton("Documents") {
            showMessage("DOCUMENTS\n\nProtected document compartment foundation.")
        })

        layout.addView(actionButton("Projects") {
            showMessage("PROJECTS\n\nProtected project compartment foundation.")
        })

        layout.addView(actionButton("Secure Notes") {
            showMessage("SECURE NOTES\n\nProtected notes compartment foundation.")
        })

        layout.addView(actionButton("AI Memory") {
            showMessage(
                "AI MEMORY\n\n" +
                "Only safe project context and preferences should be stored.\n\n" +
                "Passwords, verification codes, recovery codes, API keys and private credentials must never be stored here."
            )
        })

        layout.addView(actionButton("Protected Storage") {
            showMessage("PROTECTED STORAGE\n\nSecure-storage foundation ready for the next implementation phase.")
        })

        addBack(layout)

        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // Z LAB
    // ------------------------------------------------------------

    private fun showLab() {
        val layout = baseLayout()
        header(layout, "Z LAB")

        layout.addView(
            info(
                "🧪 EXPERIMENTATION SPACE\n\n" +
                "Build, test and organize technology projects."
            )
        )

        layout.addView(section("LABS"))

        layout.addView(actionButton("Code Lab") {
            showMessage("CODE LAB\n\nCoding workspace foundation.")
        })

        layout.addView(actionButton("Web Lab") {
            showMessage("WEB LAB\n\nWebsite development workspace foundation.")
        })

        layout.addView(actionButton("App Lab") {
            showMessage("APP LAB\n\nApplication development workspace foundation.")
        })

        layout.addView(actionButton("AI Lab") {
            showMessage("AI LAB\n\nAI experimentation workspace foundation.")
        })

        layout.addView(actionButton("Design Lab") {
            showMessage("DESIGN LAB\n\nDesign and creative workspace foundation.")
        })

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // RECOVERY
    // ------------------------------------------------------------

    private fun showRecovery() {
        val layout = baseLayout()
        header(layout, "Z RECOVERY")

        layout.addView(
            info(
                "🛠 RECOVERY CENTER\n\n" +
                "STATUS: READY\n\n" +
                "Recovery planning and backup awareness."
            )
        )

        layout.addView(actionButton("Recovery Center") {
            showMessage("RECOVERY CENTER\n\nSafe recovery workflow foundation.")
        })

        layout.addView(actionButton("Backup Status") {
            showMessage("BACKUP STATUS\n\nNo backup operation has been started.")
        })

        layout.addView(actionButton("Restore Planning") {
            showMessage("RESTORE PLANNING\n\nRestore planning foundation. No destructive action is performed.")
        })

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // CONTROL
    // ------------------------------------------------------------

    private fun showControl() {
        val layout = baseLayout()
        header(layout, "Z CONTROL")

        layout.addView(
            info(
                "🛡 SECURITY CONTROL\n\n" +
                "Guardian monitors available security information.\n\n" +
                "Protected Android operations require explicit system permissions."
            )
        )

        layout.addView(actionButton("Security Status") {
            showMessage(
                "SECURITY STATUS\n\n" +
                "Guardian Core: ACTIVE\n" +
                "Vault: LOCKED\n" +
                "AI: RESTRICTED\n" +
                "Connections: AUTHORIZATION REQUIRED"
            )
        })

        layout.addView(actionButton("Permissions") {
            showMessage(
                "PERMISSIONS\n\n" +
                "No elevated permission has been requested by this screen."
            )
        })

        layout.addView(actionButton("Device Controls") {
            showMessage(
                "DEVICE CONTROLS\n\n" +
                "Android system controls require the appropriate official permissions or roles."
            )
        })

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // CLOUD
    // ------------------------------------------------------------

    private fun showCloud() {
        val layout = baseLayout()
        header(layout, "Z CLOUD")

        layout.addView(
            info(
                "☁ CLOUD SERVICES\n\n" +
                "STATUS: SEPARATED\n\n" +
                "Cloud services remain isolated until explicitly connected."
            )
        )

        layout.addView(actionButton("Cloud Services") {
            showMessage(
                "CLOUD SERVICES\n\n" +
                "No cloud connection has been authorized from this screen."
            )
        })

        layout.addView(actionButton("Connection Status") {
            showMessage(
                "CONNECTION STATUS\n\n" +
                "Cloud: SEPARATED\n" +
                "Authorization: REQUIRED"
            )
        })

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // CONNECT
    // ------------------------------------------------------------

    private fun showConnect() {
        val layout = baseLayout()
        header(layout, "Z CONNECT")

        layout.addView(
            info(
                "🔗 AUTHORIZED CONNECTIONS\n\n" +
                "External services must be explicitly authorized."
            )
        )

        layout.addView(actionButton("Connections") {
            showMessage(
                "CONNECTIONS\n\n" +
                "No external connection has been authorized."
            )
        })

        layout.addView(actionButton("Authorization Rules") {
            showMessage(
                "AUTHORIZATION RULES\n\n" +
                "Guardian should never silently access accounts, files or external services."
            )
        })

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // AZIMI AI
    // ------------------------------------------------------------

    private fun showAI() {
        val layout = baseLayout()
        header(layout, "AZIMI AI")

        layout.addView(
            info(
                "🤖 AZIMI AI CORE\n\n" +
                "STATUS: RESTRICTED\n\n" +
                "AI capabilities can be connected through authorized services."
            )
        )

        layout.addView(section("AI WORKSPACES"))

        val modules = arrayOf(
            "Coding",
            "Website Builder",
            "App Builder",
            "AI Builder",
            "Marketing",
            "Education",
            "Design",
            "Video Production",
            "Security Architecture",
            "Project Management",
            "Research & Learning"
        )

        for (module in modules) {
            layout.addView(actionButton(module) {
                showMessage(
                    "AZIMI AI — $module\n\n" +
                    "Workspace foundation ready.\n\n" +
                    "Advanced AI execution will require an authorized AI service connection."
                )
            })
        }

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // LANGUAGE
    // ------------------------------------------------------------

    private fun showLanguage() {
        val layout = baseLayout()
        header(layout, "LANGUAGE / ژبه")

        layout.addView(
            info(
                "CURRENT LANGUAGE\n\n" +
                "English\n\n" +
                "Dari / دری support foundation"
            )
        )

        layout.addView(actionButton("English") {
            showMessage("LANGUAGE\n\nEnglish selected.")
        })

        layout.addView(actionButton("دری") {
            showMessage("زبان\n\nدری انتخاب شد.")
        })

        addBack(layout)
        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // MESSAGE SCREEN
    // ------------------------------------------------------------

    private fun showMessage(message: String) {
        val layout = baseLayout()
        layout.gravity = Gravity.CENTER_HORIZONTAL

        layout.addView(title("AZIMI GUARDIAN"))

        layout.addView(
            info("\n$message")
        )

        layout.addView(actionButton("← BACK") {
            showHome()
        })

        setContentView(scroll(layout))
    }

    // ------------------------------------------------------------
    // BACK BUTTON
    // ------------------------------------------------------------

    private fun addBack(layout: LinearLayout) {
        layout.addView(section("NAVIGATION"))

        layout.addView(actionButton("← BACK TO GUARDIAN") {
            showHome()
        })
    }

    override fun onBackPressed() {
        showHome()
    }
}
