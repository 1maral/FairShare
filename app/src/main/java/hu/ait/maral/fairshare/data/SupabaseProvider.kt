package hu.ait.maral.fairshare.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://tubkxasgdmgjcgnrkxgc.storage.supabase.co",
        supabaseKey = "5310560d0e7a2293e03e6312f7ac37cc"
    ) {
        install(Postgrest)
        install(Storage)
    }
}
