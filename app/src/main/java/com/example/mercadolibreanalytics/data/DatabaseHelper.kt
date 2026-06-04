package com.example.mercadolibreanalytics.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.FileOutputStream

class DatabaseHelper(private val context: Context) {

    private val databaseName = "kpis.db"
    private val assetPath = "databases/kpis.db"

    // Cada vez que reemplaces kpis.db en assets, aumenta este número.
    private val databaseVersion = 2

    fun getDatabase(): SQLiteDatabase {
        val databaseFile = context.getDatabasePath(databaseName)

        val prefs = context.getSharedPreferences(
            "database_preferences",
            Context.MODE_PRIVATE
        )

        val savedVersion = prefs.getInt("database_version", 0)

        if (databaseFile.exists() && savedVersion < databaseVersion) {
            databaseFile.delete()
        }

        if (!databaseFile.exists()) {
            databaseFile.parentFile?.mkdirs()

            context.assets.open(assetPath).use { input ->
                FileOutputStream(databaseFile).use { output ->
                    input.copyTo(output)
                }
            }

            prefs.edit()
                .putInt("database_version", databaseVersion)
                .apply()
        }

        return SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
    }
}