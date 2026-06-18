package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ProfileManagerApp
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val application: Application,
    private val repository: ProfileRepository
) : ViewModel() {

    // --- State Management ---
    val allProfiles: StateFlow<List<Profile>> = repository.allProfilesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppModel>>(emptyList())
    val installedApps: StateFlow<List<AppModel>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    // Apps associated with the currently selected profile
    private val _profileApps = MutableStateFlow<List<ProfileApp>>(emptyList())
    val profileApps: StateFlow<List<ProfileApp>> = _profileApps.asStateFlow()

    // Isolated Data for Selected Profile
    private val _isolatedNotes = MutableStateFlow<List<IsolatedNote>>(emptyList())
    val isolatedNotes: StateFlow<List<IsolatedNote>> = _isolatedNotes.asStateFlow()

    private val _isolatedContacts = MutableStateFlow<List<IsolatedContact>>(emptyList())
    val isolatedContacts: StateFlow<List<IsolatedContact>> = _isolatedContacts.asStateFlow()

    // Global App Security Lock Settings
    private val _masterPin = MutableStateFlow<String?>(null)
    val masterPin: StateFlow<String?> = _masterPin.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(false)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    // Screen states
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    init {
        loadSecuritySettings()
        scanInstalledApps()
        
        // Listen to profiles list; auto-select first profile or initialize default settings if empty
        viewModelScope.launch {
            allProfiles.collect { profiles ->
                if (profiles.isNotEmpty() && _currentProfile.value == null) {
                    selectProfile(profiles.first())
                } else if (profiles.isEmpty() && repository.hasBeenInitialized().not()) {
                    // Create default Profile 1, Profile 2
                    createProfile("Profile 1", "face", 0xFF6200EE)
                    createProfile("Profile 2", "work", 0xFF03DAC6)
                    repository.markInitialized()
                }
            }
        }
    }

    private fun loadSecuritySettings() {
        viewModelScope.launch {
            _masterPin.value = repository.getMasterPIN()
            _isBiometricEnabled.value = repository.isBiometricEnabled()
            if (_masterPin.value == null) {
                _isAuthenticated.value = true // No pin setup implies auto authenticated
            }
        }
    }

    fun scanInstalledApps() {
        viewModelScope.launch(Dispatchers.Default) {
            _isLoadingApps.value = true
            val apps = repository.getInstalledAppsList()
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun selectProfile(profile: Profile) {
        _currentProfile.value = profile
        observeProfileIsolatedData(profile.id)
    }

    private var noteJob: kotlinx.coroutines.Job? = null
    private var contactJob: kotlinx.coroutines.Job? = null
    private var appJob: kotlinx.coroutines.Job? = null

    private fun observeProfileIsolatedData(profileId: Int) {
        noteJob?.cancel()
        contactJob?.cancel()
        appJob?.cancel()

        noteJob = viewModelScope.launch {
            repository.getNotesForProfileFlow(profileId).collect { notes ->
                _isolatedNotes.value = notes
            }
        }

        contactJob = viewModelScope.launch {
            repository.getContactsForProfileFlow(profileId).collect { contacts ->
                _isolatedContacts.value = contacts
            }
        }

        appJob = viewModelScope.launch {
            repository.getAppsForProfileFlow(profileId).collect { apps ->
                _profileApps.value = apps
            }
        }
    }

    // --- Profile Lifecycle Operations ---

    fun createProfile(name: String, iconName: String, colorHex: Long) {
        viewModelScope.launch {
            val pId = repository.createProfile(name, iconName, colorHex)
            
            // Auto seed 2 utility systems into the newly created profile so mock looks excellent
            repository.addAppToProfile(pId, "com.android.chrome", "Google Chrome")
            repository.addAppToProfile(pId, "com.android.settings", "Settings")
        }
    }

    fun updateProfileNameAndColor(profile: Profile, newName: String, newIcon: String, newColor: Long) {
        viewModelScope.launch {
            val updated = profile.copy(name = newName, iconName = newIcon, colorHex = newColor)
            repository.updateProfile(updated)
            if (_currentProfile.value?.id == profile.id) {
                _currentProfile.value = updated
            }
        }
    }

    fun deleteProfile(profileId: Int) {
        viewModelScope.launch {
            repository.deleteProfile(profileId)
            if (_currentProfile.value?.id == profileId) {
                _currentProfile.value = null
                // Select another profile if exists
                val remainder = allProfiles.value.filter { it.id != profileId }
                if (remainder.isNotEmpty()) {
                    selectProfile(remainder.first())
                }
            }
        }
    }

    // --- Profile Applications Shortcut Management ---

    fun toggleAppInProfile(packageName: String, appName: String, isSelected: Boolean) {
        val currentProfileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            if (isSelected) {
                repository.addAppToProfile(currentProfileId, packageName, appName)
            } else {
                repository.removeAppFromProfile(currentProfileId, packageName)
            }
        }
    }

    fun toggleAppLockState(packageName: String, isLocked: Boolean) {
        val currentProfileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            repository.updateAppLockState(currentProfileId, packageName, isLocked)
        }
    }

    // --- Profile Isolated Storage: Notes ---

    fun addNote(title: String, content: String) {
        val currentProfileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            repository.saveNote(currentProfileId, title, content)
        }
    }

    fun updateNote(id: Int, title: String, content: String) {
        val currentProfileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            repository.saveNote(currentProfileId, title, content, id)
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    // --- Profile Isolated Storage: Contacts ---

    fun addContact(name: String, phone: String, email: String) {
        val currentProfileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            repository.saveContact(currentProfileId, name, phone, email)
        }
    }

    fun updateContact(id: Int, name: String, phone: String, email: String) {
        val currentProfileId = _currentProfile.value?.id ?: return
        viewModelScope.launch {
            repository.saveContact(currentProfileId, name, phone, email, id)
        }
    }

    fun deleteContact(contactId: Int) {
        viewModelScope.launch {
            repository.deleteContact(contactId)
        }
    }

    // --- Authentication & PIN Lock Controls ---

    fun setupMasterPIN(pin: String) {
        viewModelScope.launch {
            repository.saveMasterPIN(pin)
            _masterPin.value = pin
            _isAuthenticated.value = true
        }
    }

    fun changeMasterPIN(pin: String) {
        viewModelScope.launch {
            repository.saveMasterPIN(pin)
            _masterPin.value = pin
        }
    }

    fun disableMasterPIN() {
        viewModelScope.launch {
            repository.removeMasterPIN()
            _masterPin.value = null
            _isAuthenticated.value = true
        }
    }

    fun authenticatePIN(inputPin: String): Boolean {
        val isCorrect = _masterPin.value == inputPin
        if (isCorrect) {
            _isAuthenticated.value = true
        }
        return isCorrect
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch {
            repository.setBiometricEnabled(enabled)
            _isBiometricEnabled.value = enabled
        }
    }

    fun lockApp() {
        if (_masterPin.value != null) {
            _isAuthenticated.value = false
        }
    }
}

class ProfileViewModelFactory(
    private val application: Application,
    private val repository: ProfileRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
