package hu.ait.maral.fairshare.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://elfxwibjapbofhxemqxh.supabase.co",
        supabaseKey = "51e0cf151d16d2c7c3aa7827ec6734db"
    ) {
        install(Postgrest)
        install(Storage)
    }
}
