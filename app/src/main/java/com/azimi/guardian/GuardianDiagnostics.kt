package com.azimi.guardian

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import android.os.BatteryManager

object GuardianDiagnostics {

    data class Report(
        val device: String,
        val androidVersion: String,
        val cpu: String,
        val ramTotalMb: Long,
        val storageFreeGb: Double,
        val batteryPercent: Int,
        val deviceSecure: Boolean,
        val keystoreWorking: Boolean,
        val vaultInitialized: Boolean,
        val vaultStatus: String,
        val lastError: String
    )

    fun collect(
        context: Context
    ): Report {
        val keyguard = context.getSystemService(
            Context.KEYGUARD_SERVICE
        ) as? KeyguardManager

        val statFs = StatFs(
            context.filesDir.absolutePath
        )

        val freeBytes = statFs.availableBytes

        val freeGb = freeBytes.toDouble() /
            (1024.0 * 1024.0 * 1024.0)

        val batteryManager = context.getSystemService(
            Context.BATTERY_SERVICE
        ) as? BatteryManager

        val battery = batteryManager?.getIntProperty(
            BatteryManager.BATTERY_PROPERTY_CAPACITY
        ) ?: -1

        val keystoreWorking = VaultCrypto.testEncryption(
            context
        )

        return Report(
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = Build.VERSION.RELEASE,
            cpu = Build.HARDWARE,
            ramTotalMb = getTotalRamMb(context),
            storageFreeGb = freeGb,
            batteryPercent = battery,
            deviceSecure = keyguard?.isKeyguardSecure == true,
            keystoreWorking = keystoreWorking,
            vaultInitialized = GuardianStorage.isVaultInitialized(
                context
            ),
            vaultStatus = GuardianStorage.getVaultStatus(
                context
            ),
            lastError = GuardianStorage.getLastError(
                context
            )
        )
    }

    private fun getTotalRamMb(
        context: Context
    ): Long {
        val activityManager =
            context.getSystemService(
                Context.ACTIVITY_SERVICE
            ) as android.app.ActivityManager

        val memoryInfo =
            android.app.ActivityManager.MemoryInfo()

        activityManager.getMemoryInfo(memoryInfo)

        return memoryInfo.totalMem /
            (1024 * 1024)
    }

    fun toText(
        report: Report
    ): String {
        return """
            AZIMI GUARDIAN DIAGNOSTIC REPORT

            Device: ${report.device}
            Android: ${report.androidVersion}
            CPU: ${report.cpu}
            RAM: ${report.ramTotalMb} MB
            Free Storage: %.2f GB
            Battery: ${report.batteryPercent}%
            Device Security: ${if (report.deviceSecure) "AVAILABLE" else "NOT AVAILABLE"}
            Keystore Test: ${if (report.keystoreWorking) "PASS" else "FAIL"}
            Vault Initialized: ${report.vaultInitialized}
            Vault Status: ${report.vaultStatus}
            Last Error: ${report.lastError}
        """.trimIndent().format(
            report.storageFreeGb
        )
    }
}
