package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull

class ProfileRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val profileDao = db.profileDao()
    private val profileAppDao = db.profileAppDao()
    private val isolatedNoteDao = db.isolatedNoteDao()
    private val isolatedContactDao = db.isolatedContactDao()
    private val appSettingDao = db.appSettingDao()
    private val appScanner = AppScanner(context)

    // --- Profile Operations ---
    val allProfilesFlow: Flow<List<Profile>> = profileDao.getAllProfilesFlow()

    suspend fun createProfile(name: String, iconName: String, colorHex: Long): Int {
        val newProfile = Profile(name = name, iconName = iconName, colorHex = colorHex)
        val profileId = profileDao.insertProfile(newProfile).toInt()
        
        // Auto-initialize profile with a selection of popular standard launchable apps if first time,
        // or just scan and make everything available for shortcut toggling.
        return profileId
    }

    suspend fun getProfileById(profileId: Int): Profile? {
        return profileDao.getProfileById(profileId)
    }

    suspend fun updateProfile(profile: Profile) {
        profileDao.updateProfile(profile)
    }

    suspend fun deleteProfile(profileId: Int) {
        // Cascade clean up custom profile tables
        profileDao.deleteProfile(profileId)
        profileAppDao.clearAppsForProfile(profileId)
        isolatedNoteDao.clearNotesForProfile(profileId)
        isolatedContactDao.clearContactsForProfile(profileId)
    }

    // --- App Scan & Profile App Customizations ---
    
    fun getInstalledAppsList(): List<AppModel> {
        return appScanner.scanLaunchableApps()
    }

    fun getAppsForProfileFlow(profileId: Int): Flow<List<ProfileApp>> {
        return profileAppDao.getAppsForProfileFlow(profileId)
    }

    suspend fun getAppsForProfile(profileId: Int): List<ProfileApp> {
        return profileAppDao.getAppsForProfile(profileId)
    }

    suspend fun addAppToProfile(profileId: Int, packageName: String, appName: String) {
        val app = ProfileApp(profileId = profileId, packageName = packageName, appName = appName)
        profileAppDao.insertProfileApp(app)
    }

    suspend fun removeAppFromProfile(profileId: Int, packageName: String) {
        profileAppDao.removeAppFromProfile(profileId, packageName)
    }

    suspend fun updateAppLockState(profileId: Int, packageName: String, isLocked: Boolean) {
        profileAppDao.updateAppLockState(profileId, packageName, isLocked)
    }

    suspend fun setProfileApps(profileId: Int, appsList: List<ProfileApp>) {
        profileAppDao.clearAppsForProfile(profileId)
        profileAppDao.insertProfileApps(appsList)
    }

    // --- Isolated Profile Storage: Notes ---
    fun getNotesForProfileFlow(profileId: Int): Flow<List<IsolatedNote>> {
        return isolatedNoteDao.getNotesForProfileFlow(profileId)
    }

    suspend fun saveNote(profileId: Int, title: String, content: String, noteId: Int = 0) {
        val note = IsolatedNote(
            id = noteId,
            profileId = profileId,
            title = title,
            content = content,
            updatedAt = System.currentTimeMillis()
        )
        isolatedNoteDao.insertNote(note)
    }

    suspend fun deleteNote(noteId: Int) {
        isolatedNoteDao.deleteNote(noteId)
    }

    // --- Isolated Profile Storage: Contacts ---
    fun getContactsForProfileFlow(profileId: Int): Flow<List<IsolatedContact>> {
        return isolatedContactDao.getContactsForProfileFlow(profileId)
    }

    suspend fun saveContact(profileId: Int, name: String, phoneNumber: String, email: String, contactId: Int = 0) {
        val contact = IsolatedContact(
            id = contactId,
            profileId = profileId,
            name = name,
            phoneNumber = phoneNumber,
            email = email,
            updatedAt = System.currentTimeMillis()
        )
        isolatedContactDao.insertContact(contact)
    }

    suspend fun deleteContact(contactId: Int) {
        isolatedContactDao.deleteContact(contactId)
    }

    // --- Security Settings & App Lock Preference ---
    
    suspend fun getMasterPIN(): String? {
        return appSettingDao.getSetting("master_pin")
    }

    suspend fun saveMasterPIN(pin: String) {
        appSettingDao.saveSetting(AppSetting("master_pin", pin))
    }

    suspend fun removeMasterPIN() {
        appSettingDao.removeSetting("master_pin")
    }

    suspend fun isBiometricEnabled(): Boolean {
        return appSettingDao.getSetting("biometric_enabled") == "true"
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        appSettingDao.saveSetting(AppSetting("biometric_enabled", enabled.toString()))
    }
    
    suspend fun hasBeenInitialized(): Boolean {
        return appSettingDao.getSetting("is_initialized") == "true"
    }

    suspend fun markInitialized() {
        appSettingDao.saveSetting(AppSetting("is_initialized", "true"))
    }
}
