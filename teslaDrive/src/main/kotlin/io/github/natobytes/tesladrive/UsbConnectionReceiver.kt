package io.github.natobytes.tesladrive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

class UsbConnectionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
            val broadcastIntent = Intent(ACTION_USB_DISCONNECTED)
            context.sendBroadcast(broadcastIntent)
        }
    }

    companion object {
        const val ACTION_USB_DISCONNECTED = "io.github.natobytes.tesladrive.USB_DISCONNECTED"
    }
}
