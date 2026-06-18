package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconName: String,
    val colorHex: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "profile_apps", primaryKeys = ["profileId", "packageName"])
data class ProfileApp(
    val profileId: Int,
    val packageName: String,
    val appName: String,
    val isLocked: Boolean = false,
    val isPrimary: Boolean = true
)

@Entity(tableName = "isolated_notes")
data class IsolatedNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val title: String,
    val content: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "isolated_contacts")
data class IsolatedContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileId: Int,
    val name: String,
    val phoneNumber: String,
    val email: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

// --- DAOS ---

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun getAllProfilesFlow(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    suspend fun getAllProfiles(): List<Profile>

    @Query("SELECT * FROM profiles WHERE id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: Int): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    @Query("DELETE FROM profiles WHERE id = :profileId")
    suspend fun deleteProfile(profileId: Int)

    @Update
    suspend fun updateProfile(profile: Profile)
}

@Dao
interface ProfileAppDao {
    @Query("SELECT * FROM profile_apps WHERE profileId = :profileId")
    fun getAppsForProfileFlow(profileId: Int): Flow<List<ProfileApp>>

    @Query("SELECT * FROM profile_apps WHERE profileId = :profileId")
    suspend fun getAppsForProfile(profileId: Int): List<ProfileApp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileApps(apps: List<ProfileApp>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileApp(app: ProfileApp)

    @Query("DELETE FROM profile_apps WHERE profileId = :profileId AND packageName = :packageName")
    suspend fun removeAppFromProfile(profileId: Int, packageName: String)

    @Query("DELETE FROM profile_apps WHERE profileId = :profileId")
    suspend fun clearAppsForProfile(profileId: Int)
    
    @Query("UPDATE profile_apps SET isLocked = :isLocked WHERE profileId = :profileId AND packageName = :packageName")
    suspend fun updateAppLockState(profileId: Int, packageName: String, isLocked: Boolean)
}

@Dao
interface IsolatedNoteDao {
    @Query("SELECT * FROM isolated_notes WHERE profileId = :profileId ORDER BY updatedAt DESC")
    fun getNotesForProfileFlow(profileId: Int): Flow<List<IsolatedNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: IsolatedNote)

    @Query("DELETE FROM isolated_notes WHERE id = :noteId")
    suspend fun deleteNote(noteId: Int)

    @Query("DELETE FROM isolated_notes WHERE profileId = :profileId")
    suspend fun clearNotesForProfile(profileId: Int)
}

@Dao
interface IsolatedContactDao {
    @Query("SELECT * FROM isolated_contacts WHERE profileId = :profileId ORDER BY name ASC")
    fun getContactsForProfileFlow(profileId: Int): Flow<List<IsolatedContact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: IsolatedContact)

    @Query("DELETE FROM isolated_contacts WHERE id = :contactId")
    suspend fun deleteContact(contactId: Int)

    @Query("DELETE FROM isolated_contacts WHERE profileId = :profileId")
    suspend fun clearContactsForProfile(profileId: Int)
}

@Dao
interface AppSettingDao {
    @Query("SELECT value FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSetting)

    @Query("DELETE FROM app_settings WHERE `key` = :key")
    suspend fun removeSetting(key: String)
}

// --- DATABASE ---

@Database(
    entities = [
        Profile::class,
        ProfileApp::class,
        IsolatedNote::class,
        IsolatedContact::class,
        AppSetting::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun profileAppDao(): ProfileAppDao
    abstract fun isolatedNoteDao(): IsolatedNoteDao
    abstract fun isolatedContactDao(): IsolatedContactDao
    abstract fun appSettingDao(): AppSettingDao
}
