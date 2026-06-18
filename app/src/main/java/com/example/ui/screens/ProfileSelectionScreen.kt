package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Profile
import com.example.ui.ProfileViewModel

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileSelectionScreen(
    viewModel: ProfileViewModel,
    onProfileSelected: (Profile) -> Unit,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.allProfiles.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "MultiProfile Manager",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_profile_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add New Profile")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (profiles.isEmpty()) {
                // Empty State Pattern
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "No Profiles Drawer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "No Profiles Found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Workspace profiles encapsulate isolated local storage, app listings, private notes, and custom lockers. Add a new profile below to begin.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "User Profiles Workspaces",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    
                    Text(
                        text = "Select a profile workspace to load its isolated data vault. Tap and hold a profile card to customize or delete.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(profiles, key = { it.id }) { profile ->
                            ProfileCard(
                                profile = profile,
                                onClick = { onProfileSelected(profile) },
                                onLongClick = { editingProfile = profile },
                                modifier = Modifier.testTag("profile_card_${profile.name}")
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialogs for Profile Add/Edit
    if (showAddDialog) {
        ProfileEditDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, icon, color ->
                viewModel.createProfile(name, icon, color)
                showAddDialog = false
            }
        )
    }

    if (editingProfile != null) {
        ProfileEditDialog(
            profile = editingProfile,
            onDismiss = { editingProfile = null },
            onConfirm = { name, icon, color ->
                viewModel.updateProfileNameAndColor(editingProfile!!, name, icon, color)
                editingProfile = null
            },
            onDelete = {
                viewModel.deleteProfile(editingProfile!!.id)
                editingProfile = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileCard(
    profile: Profile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = Color(profile.colorHex)
    
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.large,
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Visual badge
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.15f))
                    .border(2.dp, themeColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getProfileIcon(profile.iconName),
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    "Isolated Vault",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    profile: Profile? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, icon: String, color: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(profile?.iconName ?: "person") }
    var selectedColor by remember { mutableStateOf(profile?.colorHex ?: 0xFF9C27B0) }
    var nameError by remember { mutableStateOf(false) }

    val colors = listOf(
        0xFF9C27B0, // Purple
        0xFF009688, // Teal
        0xFFFF5722, // Orange
        0xFF2196F3, // Blue
        0xFF4CAF50, // Green
        0xFFFFEB3B, // Yellow
        0xFFE91E63  // Pink
    )

    val icons = listOf(
        Pair("person", Icons.Default.Person),
        Pair("work", Icons.Default.Work),
        Pair("computer", Icons.Default.Computer),
        Pair("school", Icons.Default.School),
        Pair("security", Icons.Default.AdminPanelSettings),
        Pair("favorite", Icons.Default.Favorite)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (profile == null) "Create Profile Workspace" else "Modify Profile")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Profile Name") },
                    isError = nameError,
                    singleLine = true,
                    supportingText = {
                        if (nameError) {
                            Text("Profile name cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input")
                )

                // Select Icon Row
                Column {
                    Text("Select Identity Tag", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        icons.forEach { (iconKey, vector) ->
                            val isSelected = selectedIcon == iconKey
                            IconButton(
                                onClick = { selectedIcon = iconKey },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = iconKey,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Select Color Palette Grid
                Column {
                    Text("Select Workspace Theme", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.forEach { hexColor ->
                            val isSelected = selectedColor == hexColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(hexColor))
                                    .border(
                                        2.dp,
                                        if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedColor = hexColor }
                            )
                        }
                    }
                }
                
                if (profile != null && onDelete != null) {
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_delete_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Profile & Clear Storage")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onConfirm(name, selectedIcon, selectedColor)
                    }
                },
                modifier = Modifier.testTag("profile_dialog_confirm")
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

fun getProfileIcon(iconName: String): ImageVector {
    return when (iconName) {
        "person" -> Icons.Default.Person
        "work" -> Icons.Default.Work
        "computer" -> Icons.Default.Computer
        "school" -> Icons.Default.School
        "security" -> Icons.Default.AdminPanelSettings
        "favorite" -> Icons.Default.Favorite
        else -> Icons.Default.Person
    }
}
