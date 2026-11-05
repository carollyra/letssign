package br.com.letssign

import android.app.Application
import br.com.letssign.data.repository.AdminPadrao
import br.com.letssign.di.EscopoAplicacao
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@HiltAndroidApp
class LetsSignApp : Application() {

    @Inject
    lateinit var adminPadrao: AdminPadrao

    @Inject
    @EscopoAplicacao
    lateinit var escopo: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        escopo.launch { adminPadrao.garantir() }
    }
}
