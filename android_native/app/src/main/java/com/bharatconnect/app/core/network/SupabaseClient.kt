package com.bharatconnect.app.core.network

import android.content.Context
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

object SupabaseClient {
    const val SUPABASE_URL = "https://ykbfynoofjvibnyfkifi.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_789GLuLJL7y7BsiNnGNZ9A_9ur3AOW5"

    lateinit var client: SupabaseClient
        private set

    fun init(context: Context) {
        client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }
}
