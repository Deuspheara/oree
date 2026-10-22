package fr.deuspheara.oree

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import fr.deuspheara.oree.core.designsystem.R
import fr.deuspheara.oree.core.platform.image.impl.ArtworkReflections
import fr.deuspheara.oree.core.platform.launch.SystemActionLauncher
import fr.deuspheara.oree.diagnostics.InputDiagnostics
import fr.deuspheara.oree.feature.home.presentation.viewmodel.HomeViewModel
import fr.deuspheara.oree.feature.settings.presentation.viewmodel.SettingsViewModel
import fr.deuspheara.oree.presentation.LauncherRoot
import fr.deuspheara.oree.presentation.LauncherViewModel

class MainActivity : ComponentActivity() {
    private val resumeCounter = mutableIntStateOf(0)
    private val actions by lazy { SystemActionLauncher(this) }

    override fun onResume() {
        super.onResume()
        actions.resumed()
        resumeCounter.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        InputDiagnostics.install(window)
        val app = application as LauncherApplication
        val graph = app.dependencies
        val artwork = graph.koin.get<ArtworkReflections>()
        setContent {
            org.koin.compose.KoinIsolatedContext(graph) {
                val vm: HomeViewModel = org.koin.compose.viewmodel.koinViewModel()
                val launcher: LauncherViewModel = org.koin.compose.viewmodel.koinViewModel()
                val settings: SettingsViewModel = org.koin.compose.viewmodel.koinViewModel()
                LauncherRoot(
                    launcher,
                    graph.koin.get(),
                    settings,
                    actions,
                    artwork,
                    vm,
                    resumeCounter.intValue,
                    onOpen = {
                        if (!actions.open(it)) {
                            Toast.makeText(this, R.string.launch_error, Toast.LENGTH_LONG).show()
                            vm.reload()
                        }
                    },
                    onAndroidSettings = {
                        if (!actions.settings()) {
                            Toast.makeText(this, R.string.settings_error, Toast.LENGTH_LONG).show()
                        }
                    },
                )
            }
        }
    }
}
