package com.bharatconnect.app.core.network

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds

object SupabaseClient {
    const val SUPABASE_URL = "https://ykbfynoofjvibnyfkifi.supabase.co"
    const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InlrYmZ5bm9vZmp2aWJueWZraWZpIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYzNzAxNjQsImV4cCI6MjEwMTk0NjE2NH0.XDeixsULEe8Z03OsxTOeACHXGkQU30MbuOvXWQrO9xw"

    lateinit var client: SupabaseClient
        private set

    @OptIn(SupabaseInternal::class)
    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            requestTimeout = 60.seconds
            httpConfig {
                install(HttpTimeout) {
                    requestTimeoutMillis = 60_000
                    connectTimeoutMillis = 30_000
                    socketTimeoutMillis = 60_000
                }
            }
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
