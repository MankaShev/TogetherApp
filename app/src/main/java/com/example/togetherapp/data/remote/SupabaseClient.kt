package com.example.togetherapp.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    val supabase = createSupabaseClient(
        supabaseUrl = "https://sufzgztoymhxeajhyviw.supabase.co",
        supabaseKey = "sb_secret_4vN2Pgat6kgMx-KAca6SpA_R7JbK6kL"
    ) {
        install(Postgrest)
    }

}