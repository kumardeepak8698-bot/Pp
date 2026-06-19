package com.example.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class ProvisioningActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val action = intent?.action
        if (action == "android.app.action.GET_PROVISIONING_MODE") {
            // Android 11+ and 12+ requires the MDM/DPC app to specify the provisioning mode (Device Owner, Work Profile, etc.)
            val resultIntent = Intent().apply {
                putExtra(
                    DevicePolicyManager.EXTRA_PROVISIONING_MODE,
                    DevicePolicyManager.PROVISIONING_MODE_MANAGED_PROFILE
                )
            }
            setResult(Activity.RESULT_OK, resultIntent)
            Toast.makeText(this, "Workspace mode resolved.", Toast.LENGTH_SHORT).show()
            finish()
        } else if (action == "android.app.action.ADMIN_POLICY_COMPLIANCE") {
            // Android 11+ and 12+ requires DPC to acknowledge policy compliance
            Toast.makeText(this, "Policies compliance enforced.", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            finish()
        }
    }
}
