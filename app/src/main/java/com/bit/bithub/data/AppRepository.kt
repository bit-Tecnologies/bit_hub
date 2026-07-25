package com.bit.bithub.data

import com.bit.bithub.BitHubApplication
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val application: BitHubApplication) {

    suspend fun fetchApps(): List<App> = withContext(Dispatchers.IO) {
        val supabase = application.supabase
        supabase.from("apps").select(
            columns = Columns.list("*", "app_releases(*)")
        ).decodeList<App>()
    }
}
