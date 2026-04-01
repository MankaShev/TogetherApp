package com.example.togetherapp.data.remote


import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest


object SupabaseClient {


    val supabase = createSupabaseClient(
        supabaseUrl = "",
        supabaseKey = ""
    ) {
        install(Postgrest)
    }


}
