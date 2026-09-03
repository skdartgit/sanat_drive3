package com.sanat.drive

import android.content.Context
import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Asset(
    val id: String,
    var name: String,
    var category: String
)

data class SipEntry(
    val id: String,
    var assetId: String,
    var date: String,
    var amount: Double
)

data class Note(
    val id: String,
    var title: String,
    var html: String
)

data class Credential(
    val id: String,
    var account: String,
    var userId: String,
    var password: String,
    var other1: String,
    var other2: String
)

data class DriveFile(
    val id: String,
    var title: String,
    var originalName: String,
    var storedName: String,
    var mimeType: String,
    var date: String
)

class DataStore(context: Context) {

    private val prefs =
        context.getSharedPreferences("skd_drive_data", Context.MODE_PRIVATE)

    private val assetsKey = "assets"
    private val sipKey = "sip"
    private val notesKey = "notes"
    private val credentialsKey = "credentials"
    private val filesKey = "drive_files"

    init {
        if (!prefs.contains("pin")) {
            prefs.edit().putString("pin", "123456").apply()
        }
    }

    fun verifyPin(pin: String): Boolean =
        pin == prefs.getString("pin", "123456")

    fun setPin(pin: String) {
        if (pin.length == 6 && pin.all { it.isDigit() }) {
            prefs.edit().putString("pin", pin).apply()
        }
    }

    fun getAssets(): MutableList<Asset> {
        val result = mutableListOf<Asset>()
        val arr = JSONArray(prefs.getString(assetsKey, "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result += Asset(
                o.getString("id"),
                o.getString("name"),
                o.getString("category")
            )
        }
        return result
    }

    fun saveAssets(list: List<Asset>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("category", it.category)
            )
        }
        prefs.edit().putString(assetsKey, arr.toString()).apply()
    }

    fun addAsset(name: String, category: String) {
        val list = getAssets()
        list += Asset(UUID.randomUUID().toString(), name.trim(), category)
        saveAssets(list)
    }

    fun updateAsset(id: String, name: String, category: String) {
        val list = getAssets()
        list.find { it.id == id }?.apply {
            this.name = name.trim()
            this.category = category
        }
        saveAssets(list)
    }

    fun deleteAsset(id: String) {
        saveAssets(getAssets().filter { it.id != id })
        saveSips(getSips().filter { it.assetId != id })
    }

    fun getSips(): MutableList<SipEntry> {
        val result = mutableListOf<SipEntry>()
        val arr = JSONArray(prefs.getString(sipKey, "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result += SipEntry(
                o.getString("id"),
                o.getString("assetId"),
                o.getString("date"),
                o.getDouble("amount")
            )
        }
        return result
    }

    fun saveSips(list: List<SipEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("assetId", it.assetId)
                    .put("date", it.date)
                    .put("amount", it.amount)
            )
        }
        prefs.edit().putString(sipKey, arr.toString()).apply()
    }

    fun addSip(assetId: String, date: String, amount: Double) {
        val list = getSips()
        list += SipEntry(
            UUID.randomUUID().toString(),
            assetId,
            date.trim(),
            amount
        )
        saveSips(list)
    }

    fun updateSip(id: String, date: String, amount: Double) {
        val list = getSips()
        list.find { it.id == id }?.apply {
            this.date = date.trim()
            this.amount = amount
        }
        saveSips(list)
    }

    fun deleteSip(id: String) {
        saveSips(getSips().filter { it.id != id })
    }

    fun getNotes(): MutableList<Note> {
        val result = mutableListOf<Note>()
        val arr = JSONArray(prefs.getString(notesKey, "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result += Note(
                o.getString("id"),
                o.getString("title"),
                o.getString("html")
            )
        }
        return result
    }

    fun saveNotes(list: List<Note>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("html", it.html)
            )
        }
        prefs.edit().putString(notesKey, arr.toString()).apply()
    }

    fun addNote(title: String, html: String) {
        val list = getNotes()
        list += Note(UUID.randomUUID().toString(), title.trim(), html)
        saveNotes(list)
    }

    fun updateNote(id: String, title: String, html: String) {
        val list = getNotes()
        list.find { it.id == id }?.apply {
            this.title = title.trim()
            this.html = html
        }
        saveNotes(list)
    }

    fun deleteNote(id: String) {
        saveNotes(getNotes().filter { it.id != id })
    }

    fun getCredentials(): MutableList<Credential> {
        val result = mutableListOf<Credential>()
        val arr = JSONArray(prefs.getString(credentialsKey, "[]"))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result += Credential(
                o.getString("id"),
                o.getString("account"),
                o.getString("userId"),
                o.getString("password"),
                o.optString("other1"),
                o.optString("other2")
            )
        }
        return result
    }

    fun saveCredentials(list: List<Credential>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("account", it.account)
                    .put("userId", it.userId)
                    .put("password", it.password)
                    .put("other1", it.other1)
                    .put("other2", it.other2)
            )
        }
        prefs.edit().putString(credentialsKey, arr.toString()).apply()
    }

    fun addCredential(
        account: String,
        userId: String,
        password: String,
        other1: String,
        other2: String
    ) {
        val list = getCredentials()
        list += Credential(
            UUID.randomUUID().toString(),
            account.trim(),
            userId,
            password,
            other1,
            other2
        )
        saveCredentials(list)
    }

    fun updateCredential(
        id: String,
        account: String,
        userId: String,
        password: String,
        other1: String,
        other2: String
    ) {
        val list = getCredentials()
        list.find { it.id == id }?.apply {
            this.account = account.trim()
            this.userId = userId
            this.password = password
            this.other1 = other1
            this.other2 = other2
        }
        saveCredentials(list)
    }

    fun deleteCredential(id: String) {
        saveCredentials(getCredentials().filter { it.id != id })
    }



    // ========================================================
    // Uploaded Files
    // ========================================================

    fun getDriveFiles(): MutableList<DriveFile> {
        val result = mutableListOf<DriveFile>()
        val arr = JSONArray(prefs.getString(filesKey, "[]"))

        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)

            result += DriveFile(
                id = o.getString("id"),
                title = o.getString("title"),
                originalName = o.getString("originalName"),
                storedName = o.getString("storedName"),
                mimeType = o.optString(
                    "mimeType",
                    "application/octet-stream"
                ),
                date = o.getString("date")
            )
        }

        return result
    }

    fun saveDriveFiles(list: List<DriveFile>) {
        val arr = JSONArray()

        list.forEach { file ->
            arr.put(
                JSONObject()
                    .put("id", file.id)
                    .put("title", file.title)
                    .put("originalName", file.originalName)
                    .put("storedName", file.storedName)
                    .put("mimeType", file.mimeType)
                    .put("date", file.date)
            )
        }

        prefs.edit()
            .putString(filesKey, arr.toString())
            .apply()
    }

    fun addDriveFile(
        title: String,
        originalName: String,
        storedName: String,
        mimeType: String
    ) {
        val list = getDriveFiles()

        list += DriveFile(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            originalName = originalName,
            storedName = storedName,
            mimeType = mimeType,
            date = now()
        )

        saveDriveFiles(list)
    }

    fun deleteDriveFile(id: String) {
        saveDriveFiles(
            getDriveFiles().filter { it.id != id }
        )
    }	    

    fun getAngelValue(): Pair<String, Double>? {
        val date = prefs.getString("angel_date", null) ?: return null
        if (!prefs.contains("angel_value")) return null
        return date to prefs.getFloat("angel_value", 0f).toDouble()
    }

    fun setAngelValue(date: String, value: Double) {
        prefs.edit()
            .putString("angel_date", date)
            .putFloat("angel_value", value.toFloat())
            .apply()
    }

    fun exportAll(): String {
        val root = JSONObject()
        root.put("format", "SKD_DATA_DRIVE")
        root.put("version", 1)
        root.put("exportedAt", now())

        root.put("assets", JSONArray(prefs.getString(assetsKey, "[]")))
        root.put("sipEntries", JSONArray(prefs.getString(sipKey, "[]")))
        root.put("notes", JSONArray(prefs.getString(notesKey, "[]")))
        root.put("credentials", JSONArray(prefs.getString(credentialsKey, "[]")))
        root.put("driveFiles", JSONArray(prefs.getString(filesKey, "[]")))

        val angel = getAngelValue()
        if (angel != null) {
            root.put(
                "angelOne",
                JSONObject()
                    .put("date", angel.first)
                    .put("value", angel.second)
            )
        } else {
            root.put("angelOne", JSONObject.NULL)
        }

        /*
         * The PIN is deliberately not exported.
         * Restoring a backup must not silently change the current
         * authentication PIN.
         */
        return root.toString(2)
    }

    fun restoreAll(json: String): Boolean {
        return try {
            val root = JSONObject(json)

            if (root.optString("format") != "SKD_DATA_DRIVE") {
                return false
            }

            val assets = root.optJSONArray("assets") ?: JSONArray()
            val sips = root.optJSONArray("sipEntries") ?: JSONArray()
            val notes = root.optJSONArray("notes") ?: JSONArray()
            val credentials = root.optJSONArray("credentials") ?: JSONArray()
            val driveFiles =
                root.optJSONArray("driveFiles") ?: JSONArray()

            prefs.edit()
                .putString(assetsKey, assets.toString())
                .putString(sipKey, sips.toString())
                .putString(notesKey, notes.toString())
                .putString(credentialsKey, credentials.toString())
                .putString(credentialsKey, credentials.toString())
                .putString(filesKey, driveFiles.toString())
                .apply()

            val angel = root.optJSONObject("angelOne")
            if (angel != null) {
                setAngelValue(
                    angel.optString("date"),
                    angel.optDouble("value", 0.0)
                )
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun now(): String =
        SimpleDateFormat(
            "dd-MM-yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())
}
