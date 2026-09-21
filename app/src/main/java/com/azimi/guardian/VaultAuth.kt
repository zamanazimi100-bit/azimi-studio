package com.azimi.guardian

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent

object VaultAuth {

    const val REQUEST_CODE = 7401

    fun isDeviceSecure(context: Context): Boolean {
        val keyguard =
            context.getSystemService(Context.KEYGUARD_SERVICE)
                    as KeyguardManager

        return keyguard.isKeyguardSecure
    }

    fun requestAuthentication(activity: Activity): Boolean {
        val keyguard =
            activity.getSystemService(Context.KEYGUARD_SERVICE)
                    as KeyguardManager

        if (!keyguard.isKeyguardSecure) {
            return false
        }

        val intent: Intent =
            keyguard.createConfirmDeviceCredentialIntent(
                "Unlock AZIMI Vault",
                "Authenticate to access your protected Vault."
            )

        if (intent != null) {
            activity.startActivityForResult(
                intent,
                REQUEST_CODE
            )

            return true
        }

        return false
    }
}
