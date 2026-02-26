package hu.ait.maral.fairshare.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import hu.ait.maral.fairshare.BuildConfig.SUPABASE_API_KEY

object SupabaseProvider {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://elfxwibjapbofhxemqxh.supabase.co",
        supabaseKey = SUPABASE_API_KEY
    ) {
        install(Postgrest)
        install(Storage)
    }
}
