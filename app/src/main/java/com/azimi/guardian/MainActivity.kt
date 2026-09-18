package com.azimi.guardian

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
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
    private val green = Color.rgb(80, 220, 140)
    private val amber = Color.rgb(240, 190, 80)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            typeface = Typeface.DEFAULT_BOLD
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
            typeface = Typeface.DEFAULT_BOLD
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
            setOnClickListener { action() }

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
        content: LinearLayout,
        name: String,
        description: String
    ) {
        content.addView(title(name))
        content.addView(subtitle(description))

        val line = TextView(this).apply {
            text = "────────────────────────────"
            textSize = 11f
            setTextColor(Color.DKGRAY)
        }

        content.addView(line)
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
                "● CORE ONLINE",
                green
            )
        )

        layout.addView(
            info(
                "A protected control layer for your projects, " +
                "security, AI, recovery and connected services."
            )
        )

        layout.addView(section("SYSTEM STATE"))

        layout.addView(status("● Z VAULT        LOCKED", amber))
        layout.addView(status("● Z LAB          READY", green))
        layout.addView(status("● Z RECOVERY     READY", green))
        layout.addView(status("● Z CONTROL      ACTIVE", green))
        layout.addView(status("● Z CLOUD        SEPARATED", amber))
        layout.addView(status("● Z CONNECT      AUTHORIZATION REQUIRED", amber))
        layout.addView(status("● Z SHIELD       NOT CONFIGURED", amber))
        layout.addView(status("● AZIMI AI       RESTRICTED", amber))

        layout.addView(section("AZIMI SPACES"))

        layout.addView(
            actionButton("🏠  AZIMI HOME") {
                showHomeSpace()
            }
        )

        layout.addView(
            actionButton("🔐  Z VAULT") {
                showVault()
            }
        )

        layout.addView(
            actionButton("🧪  Z LAB") {
                showLab()
            }
        )

        layout.addView(
            actionButton("🛠  Z RECOVERY") {
                showRecovery()
            }
        )

        layout.addView(
            actionButton("🛡  Z CONTROL") {
                showControl()
            }
        )

        layout.addView(
            actionButton("🛡  Z SHIELD") {
                showShield()
            }
        )

        layout.addView(
            actionButton("☁  Z CLOUD") {
                showCloud()
            }
        )

        layout.addView(
            actionButton("🔗  Z CONNECT") {
                showConnect()
            }
        )

        layout.addView(
            actionButton("🤖  AZIMI AI") {
                showAI()
            }
        )

        layout.addView(
            actionButton("🎨  AZIMI DESIGN") {
                showDesign()
            }
        )

        layout.addView(section("LANGUAGE"))

        layout.addView(
            actionButton("English / دری") {
                showLanguage()
            }
        )

        layout.addView(section("SECURITY PRINCIPLE"))

        layout.addView(
            info(
                "AZIMI never silently accesses protected accounts, " +
                "passwords, verification codes, recovery codes, API keys " +
                "or private credentials."
            )
        )

        setContentView(screen(layout))
    }

    private fun showHomeSpace() {

        val layout = baseLayout()

        header(
            layout,
            "AZIMI HOME",
            "YOUR PERSONAL TECHNOLOGY WORKSPACE"
        )

        layout.addView(
            info(
                "AZIMI HOME is designed as an original workspace layer — " +
                "not a copy of Android, Windows or iPhone."
            )
        )

        layout.addView(section("SPACES"))

        layout.addView(actionButton("NOW") {
            message(
                "NOW",
                "Your current priorities, alerts and active projects will live here."
            )
        })

        layout.addView(actionButton("BUILD") {
            message(
                "BUILD",
                "Projects, code, websites, applications and releases."
            )
        })

        layout.addView(actionButton("GUARD") {
            message(
                "GUARD",
                "Security, permissions, vault, network and recovery."
            )
        })

        layout.addView(actionButton("CREATE") {
            message(
                "CREATE",
                "Design, writing, media and AI creation."
            )
        })

        layout.addView(actionButton("LEARN") {
            message(
                "LEARN",
                "Research, education and programming practice."
            )
        })

        layout.addView(actionButton("CONNECT") {
            message(
                "CONNECT",
                "Only explicitly authorized external connections."
            )
        })

        layout.addView(actionButton("MEMORY") {
            message(
                "MEMORY",
                "Safe project knowledge and preferences only. " +
                        "Credentials are never stored here."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showVault() {

        val layout = baseLayout()

        header(
            layout,
            "Z VAULT",
            "PROTECTED PERSONAL STORAGE"
        )

        layout.addView(status("● VAULT STATUS: LOCKED", amber))

        layout.addView(
            info(
                "The vault is being designed around local protection " +
                "and explicit user control."
            )
        )

        layout.addView(section("COMPARTMENTS"))

        layout.addView(actionButton("Personal") {
            message(
                "PERSONAL",
                "Protected personal compartment foundation."
            )
        })

        layout.addView(actionButton("Documents") {
            message(
                "DOCUMENTS",
                "Protected document compartment foundation."
            )
        })

        layout.addView(actionButton("Projects") {
            message(
                "PROJECTS",
                "Protected project storage foundation."
            )
        })

        layout.addView(actionButton("Secure Notes") {
            message(
                "SECURE NOTES",
                "Protected notes foundation."
            )
        })

        layout.addView(actionButton("AI Memory") {
            message(
                "AI MEMORY",
                "Safe project context and preferences only.\n\n" +
                        "Never store passwords, verification codes, recovery codes, " +
                        "API keys or private credentials."
            )
        })

        layout.addView(actionButton("Protected Storage") {
            message(
                "PROTECTED STORAGE",
                "Local encrypted-storage implementation is the next vault layer."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showLab() {

        val layout = baseLayout()

        header(
            layout,
            "Z LAB",
            "BUILD · TEST · EXPERIMENT"
        )

        layout.addView(
            info(
                "A controlled workspace for technology projects."
            )
        )

        val modules = arrayOf(
            "Code Lab",
            "Web Lab",
            "App Lab",
            "AI Lab",
            "Design Lab"
        )

        for (module in modules) {
            layout.addView(
                actionButton(module) {
                    message(
                        module.uppercase(),
                        "Workspace foundation ready.\n\n" +
                                "The next layer will connect this workspace " +
                                "to real project operations."
                    )
                }
            )
        }

        back(layout)

        setContentView(screen(layout))
    }

    private fun showRecovery() {

        val layout = baseLayout()

        header(
            layout,
            "Z RECOVERY",
            "RECOVERY CENTER"
        )

        layout.addView(status("● RECOVERY ENGINE: READY", green))

        layout.addView(
            info(
                "Recovery tools must protect your data and must never " +
                "perform destructive operations silently."
            )
        )

        layout.addView(actionButton("Recovery Center") {
            message(
                "RECOVERY CENTER",
                "Safe recovery workflow foundation."
            )
        })

        layout.addView(actionButton("Backup Status") {
            message(
                "BACKUP STATUS",
                "No backup operation has been started."
            )
        })

        layout.addView(actionButton("Restore Planning") {
            message(
                "RESTORE PLANNING",
                "Restore planning only. No destructive operation performed."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showControl() {

        val layout = baseLayout()

        header(
            layout,
            "Z CONTROL",
            "SECURITY · PERMISSIONS · DEVICE STATE"
        )

        layout.addView(status("● GUARDIAN CORE: ACTIVE", green))

        layout.addView(
            info(
                "Android-protected operations require the appropriate " +
                "system permission, role or user authorization."
            )
        )

        layout.addView(actionButton("Security Status") {
            message(
                "SECURITY STATUS",
                "Guardian Core: ACTIVE\n" +
                        "Vault: LOCKED\n" +
                        "AI: RESTRICTED\n" +
                        "Cloud: SEPARATED\n" +
                        "Connections: AUTHORIZATION REQUIRED"
            )
        })

        layout.addView(actionButton("Permissions") {
            message(
                "PERMISSIONS",
                "No elevated permission has been requested by this screen."
            )
        })

        layout.addView(actionButton("Device Controls") {
            message(
                "DEVICE CONTROLS",
                "Only operations allowed by Android and explicitly " +
                        "authorized by you can be executed."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showShield() {

        val layout = baseLayout()

        header(
            layout,
            "Z SHIELD",
            "NETWORK PROTECTION"
        )

        layout.addView(status("● VPN STATUS: NOT CONFIGURED", amber))

        layout.addView(
            info(
                "Important: Guardian will never pretend that a VPN is " +
                "protecting your traffic when no real encrypted tunnel exists."
            )
        )

        layout.addView(section("PLANNED SHIELD COMPONENTS"))

        val modules = arrayOf(
            "Secure Tunnel",
            "DNS Protection",
            "Kill Switch",
            "Connection Information",
            "Allowed / Blocked Apps",
            "Trusted Networks",
            "Auto Connect",
            "Emergency Disconnect"
        )

        for (module in modules) {
            layout.addView(
                actionButton(module) {
                    message(
                        module.uppercase(),
                        "Z SHIELD component planned.\n\n" +
                                "Actual protection will only be shown as active " +
                                "after a real encrypted VPN tunnel is configured."
                    )
                }
            )
        }

        back(layout)

        setContentView(screen(layout))
    }

    private fun showCloud() {

        val layout = baseLayout()

        header(
            layout,
            "Z CLOUD",
            "SEPARATED CLOUD SERVICES"
        )

        layout.addView(status("● CLOUD: SEPARATED", amber))

        layout.addView(
            info(
                "Cloud services remain isolated until an explicit connection " +
                "is configured and authorized."
            )
        )

        layout.addView(actionButton("Cloud Services") {
            message(
                "CLOUD SERVICES",
                "No cloud connection is authorized from this screen."
            )
        })

        layout.addView(actionButton("Connection Status") {
            message(
                "CONNECTION STATUS",
                "Cloud: SEPARATED\nAuthorization: REQUIRED"
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showConnect() {

        val layout = baseLayout()

        header(
            layout,
            "Z CONNECT",
            "AUTHORIZED CONNECTIONS"
        )

        layout.addView(status("● EXTERNAL ACCESS: RESTRICTED", amber))

        layout.addView(
            info(
                "External services, accounts and protected resources " +
                "must be explicitly authorized."
            )
        )

        layout.addView(actionButton("Connections") {
            message(
                "CONNECTIONS",
                "No external connection has been authorized."
            )
        })

        layout.addView(actionButton("Authorization Rules") {
            message(
                "AUTHORIZATION RULES",
                "AI can prepare actions and explain them.\n\n" +
                "External execution requires your authorization."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showAI() {

        val layout = baseLayout()

        header(
            layout,
            "AZIMI AI",
            "THINK · PLAN · BUILD · LEARN"
        )

        layout.addView(status("● AI STATUS: RESTRICTED", amber))

        layout.addView(
            info(
                "AZIMI AI is designed as the intelligence layer of AZIMI — " +
                "not merely a chat box."
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
            layout.addView(
                actionButton(module) {
                    message(
                        "AZIMI AI — $module",
                        "Workspace foundation ready.\n\n" +
                                "AI execution will use authorized services " +
                                "and will not silently access protected resources."
                    )
                }
            )
        }

        back(layout)

        setContentView(screen(layout))
    }

    private fun showDesign() {

        val layout = baseLayout()

        header(
            layout,
            "AZIMI DESIGN",
            "YOUR ORIGINAL VISUAL SYSTEM"
        )

        layout.addView(
            info(
                "Themes, icons, widgets, layouts and interaction patterns " +
                "belong to the AZIMI identity."
            )
        )

        layout.addView(actionButton("Themes") {
            message(
                "THEMES",
                "AZIMI theme engine foundation."
            )
        })

        layout.addView(actionButton("Icons") {
            message(
                "ICONS",
                "AZIMI icon system foundation."
            )
        })

        layout.addView(actionButton("Widgets") {
            message(
                "WIDGETS",
                "AZIMI widget system foundation."
            )
        })

        layout.addView(actionButton("Layouts") {
            message(
                "LAYOUTS",
                "AZIMI spatial layout system foundation."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun showLanguage() {

        val layout = baseLayout()

        header(
            layout,
            "LANGUAGE / ژبه",
            "ENGLISH · DARI / دری"
        )

        layout.addView(
            info(
                "Language architecture is designed to support " +
                "English and Dari, including RTL layouts."
            )
        )

        layout.addView(actionButton("English") {
            message(
                "LANGUAGE",
                "English selected."
            )
        })

        layout.addView(actionButton("دری") {
            message(
                "زبان",
                "دری انتخاب شد."
            )
        })

        back(layout)

        setContentView(screen(layout))
    }

    private fun message(
        heading: String,
        body: String
    ) {

        val layout = baseLayout()

        layout.gravity = Gravity.CENTE
