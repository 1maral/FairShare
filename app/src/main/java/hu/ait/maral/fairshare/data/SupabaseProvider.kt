package hu.ait.maral.fairshare.data

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {
    val supabase = createSupabaseClient(
        supabaseUrl = "https://elfxwibjapbofhxemqxh.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVsZnh3aWJqYXBib2ZoeGVtcXhoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjUwMTczMzksImV4cCI6MjA4MDU5MzMzOX0.S7w7iujhQCx_9c2Farz6Ay2DJ8zBFg2ssXwoRwjWAl8"
    ) {
        install(Postgrest)
        install(Storage)
    }
}
