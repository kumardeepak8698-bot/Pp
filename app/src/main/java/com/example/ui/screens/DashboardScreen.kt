package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.AppModel
import com.example.data.IsolatedContact
import com.example.data.IsolatedNote
import com.example.data.Profile
import com.example.data.ProfileApp
import com.example.ui.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ProfileViewModel,
    onNavigateToAppSelection: () -> Unit,
    onNavigateBackToProfiles: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentProfile by viewModel.currentProfile.collectAsState()
    val profileApps by viewModel.profileApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    
    val notes by viewModel.isolatedNotes.collectAsState()
    val contacts by viewModel.isolatedContacts.collectAsState()
    
    val masterPin by viewModel.masterPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Enterprise Workspace", "Secure Notes", "Contacts", "Security")

    // Active screen locks or overlays
    var appToLaunchAfterValidation by remember { mutableStateOf<ProfileApp?>(null) }
    var showAppLockVerifyDialog by remember { mutableStateOf(false) }
    var appLockInputText by remember { mutableStateOf("") }
    var appLockError by remember { mutableStateOf("") }

    if (currentProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val activeProfile = currentProfile!!
    val profileColor = Color(activeProfile.colorHex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(profileColor.copy(alpha = 0.2f))
                                .border(1.5.dp, profileColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getProfileIcon(activeProfile.iconName),
                                contentDescription = null,
                                tint = profileColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text(
                                activeProfile.name,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Profile Workspace Enabled",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBackToProfiles) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Workspace list")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.lockApp()
                        onNavigateBackToProfiles()
                    }) {
                        Icon(Icons.Default.PowerSettingsNew, contentDescription = "Lock & Lockout Session")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Elegant scrolling custom Material 3 tab rows to isolate scopes
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = profileColor
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("dashboard_tab_$title")
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Enterprise Managed Profile system workspace tab
                        EnterpriseWorkspaceTab(
                            viewModel = viewModel
                        )
                    }
                    1 -> {
                        // Isolated Room Notes
                        NotesVaultTab(
                            notes = notes,
                            profileColor = profileColor,
                            onAddNote = { t, c -> viewModel.addNote(t, c) },
                            onDeleteNote = { id -> viewModel.deleteNote(id) },
                            onUpdateNote = { id, t, c -> viewModel.updateNote(id, t, c) }
                        )
                    }
                    2 -> {
                        // Isolated Room Contacts
                        ContactsTab(
                            contacts = contacts,
                            profileColor = profileColor,
                            onAddContact = { n, p, e -> viewModel.addContact(n, p, e) },
                            onDeleteContact = { id -> viewModel.deleteContact(id) }
                        )
                    }
                    3 -> {
                        // Security Settings Locker manager
                        SecuritySettingsTab(
                            viewModel = viewModel,
                            masterPin = masterPin,
                            profileColor = profileColor,
                            isBiometricEnabled = isBiometricEnabled
                        )
                    }
                }
            }
        }
    }

    // Modal dialog for app lock verification
    if (showAppLockVerifyDialog && appToLaunchAfterValidation != null) {
        val targetApp = appToLaunchAfterValidation!!
        
        AlertDialog(
            onDismissRequest = {
                showAppLockVerifyDialog = false
                appToLaunchAfterValidation = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Secure Space Lock")
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Launching '${targetApp.appName}' requires identity validation in profile workspace '${activeProfile.name}'.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    OutlinedTextField(
                        value = appLockInputText,
                        onValueChange = {
                            if (it.length <= 4) {
                                appLockInputText = it
                                appLockError = ""
                            }
                        },
                        label = { Text("Enter Master PIN") },
                        singleLine = true,
                        isError = appLockError.isNotEmpty(),
                        supportingText = {
                            if (appLockError.isNotEmpty()) {
                                Text(appLockError, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("app_lock_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (appLockInputText == masterPin) {
                            showAppLockVerifyDialog = false
                            appToLaunchAfterValidation = null
                            launchApplicationIntent(context, targetApp.packageName, targetApp.appName)
                        } else {
                            appLockError = "Incorrect passcode. Security block active."
                            appLockInputText = ""
                        }
                    },
                    modifier = Modifier.testTag("app_lock_verify_confirm")
                ) {
                    Text("Unlock & Launch")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAppLockVerifyDialog = false
                    appToLaunchAfterValidation = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- ENTERPRISE WORKSPACE TAB (TRUE WORK PROFILE) ---

@Composable
fun EnterpriseWorkspaceTab(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val systemProfiles by viewModel.systemProfiles.collectAsState()
    val systemNotifications by viewModel.systemNotifications.collectAsState()
    val isCurrentlyWorkProfile by viewModel.isCurrentlyWorkProfile.collectAsState()
    val diagnosticsInfo by viewModel.diagnosticsInfo.collectAsState()

    val isVpnAlwaysOn by viewModel.isVpnAlwaysOn.collectAsState()
    val isIdMaskingEnabled by viewModel.isIdMaskingEnabled.collectAsState()
    val isAntiTrackingEnabled by viewModel.isAntiTrackingEnabled.collectAsState()
    val profileProxyHost by viewModel.profileProxyHost.collectAsState()
    val profileProxyPort by viewModel.profileProxyPort.collectAsState()

    var showSetupGuide by remember { mutableStateOf(false) }
    var selectedAppProfileId by remember { mutableStateOf("personal") }
    var mockNotifTitle by remember { mutableStateOf("") }
    var mockNotifBody by remember { mutableStateOf("") }
    var mockNotifType by remember { mutableStateOf("Work") }
    var showNotifSimulationDialog by remember { mutableStateOf(false) }

    var isEditingProxy by remember { mutableStateOf(false) }
    var proxyHostInput by remember { mutableStateOf(profileProxyHost) }
    var proxyPortInput by remember { mutableStateOf(profileProxyPort) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. True Android Profile Status Banner ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentlyWorkProfile) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("profile_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isCurrentlyWorkProfile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyWorkProfile) Icons.Default.BusinessCenter else Icons.Default.Person,
                            contentDescription = "Active Container Icon",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isCurrentlyWorkProfile) "Logged into WORK CONTAINER" else "Logged into PERSONAL CONTAINER",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentlyWorkProfile) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (isCurrentlyWorkProfile) "True isolation verified. Storage and Accounts are separated from Personal." else "Running under main profile. Work container available for isolated workspace setup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isCurrentlyWorkProfile) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // --- 2. Setup Wizard / Configuration Card ---
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("setup_wizard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SettingsSuggest,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Profile Configuration Wizard",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { showSetupGuide = !showSetupGuide }) {
                            Icon(
                                imageVector = if (showSetupGuide) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle Guide Details"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Automatically provision a secure Work Profile container. All documents, apps, accounts, contacts, and settings in this container will be strictly offline-isolated and encrypted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.startWorkProfileProvisioning(context) },
                            modifier = Modifier.weight(1f).testTag("trigger_provisioning_button")
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Provision Container")
                        }
                        
                        OutlinedButton(
                            onClick = { showSetupGuide = !showSetupGuide },
                            modifier = Modifier.weight(1f).testTag("rom_troubleshooter_toggle")
                        ) {
                            Text(if (showSetupGuide) "Hide Assist" else "🛠️ ROM Setup Help")
                        }
                    }

                    if (showSetupGuide) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "⚠️ Realme, Xiaomi & Android ROM Compatibility Fixes",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Text(
                            "Custom OS interfaces (like realme UI, ColorOS, MIUI/HyperOS) modify Android's default Device Admin features. If you are seeing 'Work profile isn't available' or 'Contact IT Admin', please perform the following fixes:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        var activeBrandHelp by remember { mutableStateOf("realme") }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = activeBrandHelp == "realme",
                                onClick = { activeBrandHelp = "realme" },
                                label = { Text("Realme / Oppo", style = MaterialTheme.typography.bodySmall) }
                            )
                            FilterChip(
                                selected = activeBrandHelp == "xiaomi",
                                onClick = { activeBrandHelp = "xiaomi" },
                                label = { Text("Xiaomi / Redmi", style = MaterialTheme.typography.bodySmall) }
                            )
                            FilterChip(
                                selected = activeBrandHelp == "general",
                                onClick = { activeBrandHelp = "general" },
                                label = { Text("Admin Block", style = MaterialTheme.typography.bodySmall) }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                when (activeBrandHelp) {
                                    "realme" -> {
                                        Text(
                                            "🔧 Realme UI / ColorOS (Realme 8, etc.) Fix:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Text(
                                            "1. Disable App Cloner: Go to Settings > Apps > App Cloner and turn off all dual/clone apps. Realme's clone accounts occupy the profile handles and block Managed Profiles.\n\n" +
                                            "2. Turn off Kid/Guest Space: Deactivate Settings > Special features > Kid Space.\n\n" +
                                            "3. Verify Multiple Users: Ensure guest users under Settings > Users & Accounts aren't holding system administrative lock handles.",
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    "xiaomi" -> {
                                        Text(
                                            "🔧 Xiaomi MIUI / HyperOS Fix:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Text(
                                            "1. Delete Second Space: Go to Settings > Special features > Second Space and delete any created secondary workspace.\n\n" +
                                            "2. Clear Dual Apps Accounts: Go to Settings > Apps > Dual Apps > click Settings wheel -> choose 'Delete dual apps accounts'. All dual apps must be removed first.\n\n" +
                                            "3. Enable MIUI Optimization: Ensure developer values allow custom DPC provisioning triggers.",
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 16.sp
                                        )
                                    }
                                    "general" -> {
                                        Text(
                                            "🔧 'Contact IT Admin' Error Fix:",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                        Text(
                                            "1. Remove Existing Work Profile: Go to Settings > Users & accounts. Under Work Profile section, select 'Uninstall/Remove'.\n\n" +
                                            "2. Disable Other Admins: Go to Settings > Security > Device Admin Apps or Passwords & Security > System Security > Device Admin Apps, and turn off other managed apps (e.g. Find My Device, Outlook Device Management).\n\n" +
                                            "3. Corporate Google Account Conflicts: If you have a school/work Google Account signed in on your main profile, the OS policy locks further manual work profile provisioning.",
                                            style = MaterialTheme.typography.bodySmall,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "🛠️ Developer Command Line Alternative:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "If your custom UI blocks on-device wizards, enable USB debugging, plug your phone into a PC, and force setup using ADB:\n" +
                            "adb shell dpm set-profile-owner --user 0 com.example/com.example.data.ProfileDeviceAdminReceiver",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            lineHeight = 14.sp
                        )


                    }
                }
            }
        }

        // --- 3. Profile Health & Storage diagnostics screen ---
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("diagnostics_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Isolation Diagnostics & Storage Health",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    diagnosticsInfo.forEach { (title, status) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    status,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3.5 Network & Identity Privacy Policies Guard Card ---
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("privacy_guard_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Network & ID Privacy Policies",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Configure Managed DPC container safety options. Isolate network routing, proxy tunnels, and mask platform-identifiers from tracking SDKs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    // 1. Hardware ID Masking (SSAID Protection)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Profile Hardware ID Masking",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Enforces modern Android SSAID sandboxing. Apps get a virtualized device ID, separating them from the main phone hardware identifiers (IMEI/Serial).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isIdMaskingEnabled,
                            onCheckedChange = { viewModel.toggleIdMasking(it) }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    // 2. Always-On VPN Secure Routing Simulation
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Isolated Profile Always-On VPN",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Enforces network tunnel routing ONLY within this container. Personal space remains fully active on Wi-Fi/Mobile without VPN overhead.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isVpnAlwaysOn,
                            onCheckedChange = { viewModel.toggleVpnAlwaysOn(it) }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    // 3. Anti-Tracking Privacy Shield (Shielding VPN Status)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Anti-Tracking Shield (VPN/Proxy Cloak)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "DPC level safety policy restricts apps inside this space from identifying active VPN/proxy bindings, preventing third-party blocking.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isAntiTrackingEnabled,
                            onCheckedChange = { viewModel.toggleAntiTracking(it) }
                        )
                    }

                    Divider(modifier = Modifier.padding(vertical = 6.dp))

                    // 4. Custom Proxy Settings for Container
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Profile-Specific HTTP Proxy",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Route all workspace container web traffic through a secure server. Current: $profileProxyHost:$profileProxyPort",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 14.sp
                                )
                            }
                            TextButton(
                                onClick = {
                                    if (isEditingProxy) {
                                        viewModel.updateProxyConfiguration(proxyHostInput, proxyPortInput)
                                    } else {
                                        proxyHostInput = profileProxyHost
                                        proxyPortInput = profileProxyPort
                                    }
                                    isEditingProxy = !isEditingProxy
                                }
                            ) {
                                Text(if (isEditingProxy) "Save" else "Configure")
                            }
                        }

                        if (isEditingProxy) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = proxyHostInput,
                                    onValueChange = { proxyHostInput = it },
                                    label = { Text("Proxy Host IP") },
                                    modifier = Modifier.weight(3f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                                OutlinedTextField(
                                    value = proxyPortInput,
                                    onValueChange = { proxyPortInput = it },
                                    label = { Text("Port") },
                                    modifier = Modifier.weight(1.5f),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. Profile Specific Notifications ---
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("notifications_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Profile Isolation Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { showNotifSimulationDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Simulate")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (systemNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No notification logs inside containers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            systemNotifications.forEach { notif ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (notif.profileType == "Work") MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (notif.profileType == "Work") Icons.Default.BusinessCenter else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (notif.profileType == "Work") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(notif.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            Text(notif.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Text(notif.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { viewModel.clearSystemNotification(notif.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear Alert", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. Profile-Specific App Lists ---
        item {
            ElevatedCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().testTag("app_lists_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            tint = MaterialTheme.colorScheme.primary,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Containerized Interactive App Lists",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ScrollableTabRow(
                        selectedTabIndex = if (selectedAppProfileId == "personal") 0 else 1,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 0.dp
                    ) {
                        Tab(
                            selected = selectedAppProfileId == "personal",
                            onClick = { selectedAppProfileId = "personal" },
                            text = { Text("Personal Container") }
                        )
                        Tab(
                            selected = selectedAppProfileId == "work",
                            onClick = { selectedAppProfileId = "work" },
                            text = { Text("Secure Work") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeSystemProfile = systemProfiles.find { it.id == selectedAppProfileId }
                    if (activeSystemProfile == null || activeSystemProfile.apps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    if (selectedAppProfileId == "work") "Work Profile is not provisioned or configured yet. Complete the Profile Configuration Wizard to enable." else "No apps detected in this container",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            activeSystemProfile.apps.forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .clickable {
                                            viewModel.launchApplicationInProfile(app.packageName, app.activityName, activeSystemProfile.isWorkProfile)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Launch,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(app.appName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        Text(app.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.launchApplicationInProfile(app.packageName, app.activityName, activeSystemProfile.isWorkProfile)
                                        }
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch App", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNotifSimulationDialog) {
        AlertDialog(
            onDismissRequest = { showNotifSimulationDialog = false },
            title = { Text("Simulate Container Notification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter custom mock alert values and route it to your isolated profile containers dynamically.", style = MaterialTheme.typography.bodySmall)
                    
                    OutlinedTextField(
                        value = mockNotifTitle,
                        onValueChange = { mockNotifTitle = it },
                        label = { Text("Notification Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = mockNotifBody,
                        onValueChange = { mockNotifBody = it },
                        label = { Text("Notification Body") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Target Profile:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mockNotifType == "Work", onClick = { mockNotifType = "Work" })
                            Text("Work", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mockNotifType == "Personal", onClick = { mockNotifType = "Personal" })
                            Text("Personal", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalTitle = mockNotifTitle.ifEmpty { "Secure System Update" }
                        val finalBody = mockNotifBody.ifEmpty { "Offline-isolated configuration synced." }
                        viewModel.simulateIncomingNotification(mockNotifType, finalTitle, finalBody)
                        showNotifSimulationDialog = false
                        mockNotifTitle = ""
                        mockNotifBody = ""
                    }
                ) {
                    Text("Trigger Simulation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotifSimulationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


}

fun launchApplicationIntent(context: Context, packageName: String, appName: String) {
    val pm = context.packageManager
    val intent = pm.getLaunchIntentForPackage(packageName)
    if (intent != null) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Redirect failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Nice simulator alert in sandbox envs
        Toast.makeText(context, "LAUNCH SIMULATION: Opening '$appName' ($packageName)", Toast.LENGTH_LONG).show()
    }
}

// --- ISOLATED ROOM SECURE NOTES ---

@Composable
fun NotesVaultTab(
    notes: List<IsolatedNote>,
    profileColor: Color,
    onAddNote: (title: String, content: String) -> Unit,
    onDeleteNote: (noteId: Int) -> Unit,
    onUpdateNote: (noteId: Int, title: String, content: String) -> Unit
) {
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var activeEditingNote by remember { mutableStateOf<IsolatedNote?>(null) }
    
    val tintColor = profileColor

    Box(modifier = Modifier.fillMaxSize()) {
        if (notes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.NoteAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Note sandbox is completely empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Record secure lists or codes. Notes created in this profile space are kept isolated and are inaccessible to other profiles.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notes, key = { it.id }) { note ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeEditingNote = note }
                            .testTag("note_item_${note.title}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    note.title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = { onDeleteNote(note.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete secret note",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // FAB to spawn note writer
        FloatingActionButton(
            onClick = { showAddNoteDialog = true },
            containerColor = tintColor,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_note_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Private Note")
        }
    }

    if (showAddNoteDialog) {
        NoteInputDialog(
            onDismiss = { showAddNoteDialog = false },
            onConfirm = { t, c ->
                onAddNote(t, c)
                showAddNoteDialog = false
            }
        )
    }

    if (activeEditingNote != null) {
        NoteInputDialog(
            note = activeEditingNote,
            onDismiss = { activeEditingNote = null },
            onConfirm = { t, c ->
                onUpdateNote(activeEditingNote!!.id, t, c)
                activeEditingNote = null
            }
        )
    }
}

@Composable
fun NoteInputDialog(
    note: IsolatedNote? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (note == null) "Create Isolated Note" else "Edit Isolated Note") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = false },
                    label = { Text("Note Title") },
                    isError = titleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Note Body") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onConfirm(title, content)
                    }
                },
                modifier = Modifier.testTag("note_dialog_confirm")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- ISOLATED PRIVATE CONTACTS ---

@Composable
fun ContactsTab(
    contacts: List<IsolatedContact>,
    profileColor: Color,
    onAddContact: (name: String, phone: String, email: String) -> Unit,
    onDeleteContact: (contactId: Int) -> Unit
) {
    var showAddContactDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (contacts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContactPhone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Contact sandbox is completely empty",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Store secret work or private context lines. Files are safely sandboxed on Room local tables aligned with your profile id.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(contacts, key = { it.id }) { contact ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("contact_item_${contact.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(profileColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = profileColor
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    contact.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    contact.phoneNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (contact.email.isNotEmpty()) {
                                    Text(
                                        contact.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { onDeleteContact(contact.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete contact",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddContactDialog = true },
            containerColor = profileColor,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_contact_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "New Private Contact")
        }
    }

    if (showAddContactDialog) {
        ContactAddDialog(
            onDismiss = { showAddContactDialog = false },
            onConfirm = { n, p, e ->
                onAddContact(n, p, e)
                showAddContactDialog = false
            }
        )
    }
}

@Composable
fun ContactAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, email: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Isolated Contact") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    label = { Text("Name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contact_email_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, phone, email)
                    }
                },
                modifier = Modifier.testTag("contact_dialog_confirm")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// --- MASTER SECURITY CONFIG ---

@Composable
fun SecuritySettingsTab(
    viewModel: ProfileViewModel,
    masterPin: String?,
    profileColor: Color,
    isBiometricEnabled: Boolean
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText0 by remember { mutableStateOf("") }
    var pinErrorText by remember { mutableStateOf("") }

    val hasPinSetup = masterPin != null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Locker Security Configurations",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Customize global app credentials or fingerprint settings to isolate workspace launchers and databases.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Master Locker PIN",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                if (hasPinSetup) "Active: App lock is armed." else "Disabled: No lock passcode registered.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasPinSetup) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                        }
                        Switch(
                            checked = hasPinSetup,
                            onCheckedChange = { active ->
                                if (active) {
                                    showPinDialog = true
                                } else {
                                    viewModel.disableMasterPIN()
                                }
                            },
                            thumbContent = {
                                Icon(
                                    imageVector = if (hasPinSetup) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier.testTag("pin0_lock_switch")
                        )
                    }

                    if (hasPinSetup) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { showPinDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = profileColor
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("change_pin0_button")
                        ) {
                            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Change PIN Passcode")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Biometric Unlock Integration",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Allows logging in or launching secured profiles and application shortcuts instantly using fingerprint biometrics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { viewModel.toggleBiometric(it) },
                        thumbContent = {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("biometric_switch")
                    )
                }
            }
        }

        item {
            Divider(modifier = Modifier.padding(vertical = 12.dp))
            Text(
                "How Is Isolation Achieved?",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "1. Database Segregation: Notes, bookmarks, and private contacts are registered with high integrity against individual profile key indices in Room tables.\n" +
                "2. Dynamic Launch Filters: Whenever shortcut launchers are triggered, system-level filters analyze individual profiles to overlay security PIN pads so apps are private.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set Master Security PIN") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Type a new 4-digit master locker passcode below:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = pinText0,
                        onValueChange = {
                            if (it.length <= 4) {
                                pinText0 = it
                                pinErrorText = ""
                            }
                        },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        isError = pinErrorText.isNotEmpty(),
                        supportingText = {
                            if (pinErrorText.isNotEmpty()) {
                                Text(pinErrorText, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("new_master_pin_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinText0.length == 4 && pinText0.all { it.isDigit() }) {
                            viewModel.setupMasterPIN(pinText0)
                            showPinDialog = false
                            pinText0 = ""
                        } else {
                            pinErrorText = "Requires exactly 4 numeric characters."
                        }
                    },
                    modifier = Modifier.testTag("submit_new_pin_button")
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinDialog = false
                    pinText0 = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}
