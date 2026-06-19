package com.example.data

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast

class ProfileDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "Work Profile Admin Enabled", Toast.LENGTH_SHORT).show()
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, ProfileDeviceAdminReceiver::class.java)
        
        try {
            dpm.setProfileName(adminComponent, "Work Profile")
            dpm.setProfileEnabled(adminComponent)
            Toast.makeText(context, "Work Profile Created and Activated Successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Activation Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
