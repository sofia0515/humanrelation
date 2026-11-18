package com.renqing.zhang

import android.app.Application
import com.renqing.zhang.data.AppDatabase
import com.renqing.zhang.data.Repository
import com.renqing.zhang.data.SettingsStore
import kotlinx.coroutines.runBlocking

class RenqingApp : Application() {
    lateinit var repo: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.create(this)
        repo = Repository(database, SettingsStore(this))
        runBlocking { repo.ensureSeeded() }
    }
}
