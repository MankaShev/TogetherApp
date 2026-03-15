package com.example.togetherapp.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    val supabase = createSupabaseClient(
        supabaseUrl = "https://sufzgztoymhxeajhyviw.supabase.co",
        supabaseKey = "sb_publishable_bWTOTs1I_NY7feW0jiQnog_bj7SrUVb"
    ) {
        install(Postgrest)
    }

}