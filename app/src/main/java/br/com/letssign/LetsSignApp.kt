package br.com.letssign

import android.app.Application
import br.com.letssign.data.repository.AdminPadrao
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class LetsSignApp : Application() {

    @Inject
    lateinit var adminPadrao: AdminPadrao

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        escopo.launch { adminPadrao.garantir() }
    }
}
