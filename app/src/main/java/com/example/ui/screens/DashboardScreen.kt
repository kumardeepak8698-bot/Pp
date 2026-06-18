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
    val tabTitles = listOf("Launcher", "Secure Notes", "Contacts", "Security")

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
                        // Apps shortcuts launcher
                        AppsLauncherTab(
                            viewModel = viewModel,
                            profileApps = profileApps,
                            installedApps = installedApps,
                            onModifyShortcuts = onNavigateToAppSelection,
                            onLaunchApp = { profileApp ->
                                if (profileApp.isLocked && masterPin != null) {
                                    appToLaunchAfterValidation = profileApp
                                    appLockInputText = ""
                                    appLockError = ""
                                    showAppLockVerifyDialog = true
                                } else {
                                    launchApplicationIntent(context, profileApp.packageName, profileApp.appName)
                                }
                            }
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

// --- LAUNCHER SHORTCUTS ---

@Composable
fun AppsLauncherTab(
    viewModel: ProfileViewModel,
    profileApps: List<ProfileApp>,
    installedApps: List<AppModel>,
    onModifyShortcuts: () -> Unit,
    onLaunchApp: (ProfileApp) -> Unit
) {
    val context = LocalContext.current
    val appIconMap = remember(installedApps) {
        installedApps.associate { it.packageName to it.icon }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sandboxed launcher note banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "OS Restriction Notice: True sandbox app duplication requires device-level profiles. MultiProfile separates secure files/notes/contacts storage, and provides quick secure lockable launcher shorts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Shortcut Workspace Launcher",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            TextButton(
                onClick = onModifyShortcuts,
                modifier = Modifier.testTag("edit_apps_button")
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Customize")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (profileApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.AddLink,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No shortcuts created yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onModifyShortcuts) {
                        Text("Add apps to profile workspace")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 100.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(profileApps, key = { it.packageName }) { app ->
                    val iconBitmap = appIconMap[app.packageName]
                    
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLaunchApp(app) }
                            .testTag("shortcut_card_${app.packageName}")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Lock state indicator top right
                            IconButton(
                                onClick = { viewModel.toggleAppLockState(app.packageName, !app.isLocked) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(32.dp)
                                    .padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (app.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock overlay",
                                    tint = if (app.isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp, horizontal = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap.asImageBitmap(),
                                            contentDescription = app.appName,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Android,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
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
