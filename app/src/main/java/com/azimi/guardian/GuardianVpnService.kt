package com.azimi.guardian

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor

class GuardianVpnService : VpnService() {

    private var tunnelInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        /*
         * Z SHIELD SAFETY RULE
         *
         * This service intentionally does NOT claim that
         * the device is protected.
         *
         * A real VPN requires an actual encrypted tunnel
         * and a configured backend/endpoint.
         */

        tunnelInterface?.close()
        tunnelInterface = null

        stopSelf()

        return START_NOT_STICKY
    }

    override fun onDestroy() {

        tunnelInterface?.close()
        tunnelInterface = null

        super.onDestroy()
    }
}
