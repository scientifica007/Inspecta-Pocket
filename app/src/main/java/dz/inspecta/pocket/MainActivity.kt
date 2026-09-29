package dz.inspecta.pocket

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.LocaleList
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dz.inspecta.pocket.data.toShareText
import dz.inspecta.pocket.ui.DetailsScreen
import dz.inspecta.pocket.ui.HomeScreen
import dz.inspecta.pocket.ui.InspectionScreen
import dz.inspecta.pocket.ui.NewVisitScreen
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        // The MVP is Arabic regardless of the system language, including native date dialogs.
        val configuration = Configuration(newBase.resources.configuration)
        val arabic = Locale.forLanguageTag("ar")
        configuration.setLocales(LocaleList(arabic))
        configuration.setLayoutDirection(arabic)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF14685E),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD3EEE7),
                background = Color(0xFFF4F8F7),
                surface = Color(0xFFF4F8F7),
                surfaceContainerLow = Color.White,
            )) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize()) { InspectaApp() }
                }
            }
        }
    }
}

@Composable
private fun InspectaApp(model: InspectaViewModel = viewModel()) {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val visits by model.visits.collectAsStateWithLifecycle()
    val detail by model.detail.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    val creating by model.creating.collectAsStateWithLifecycle()
    val createdVisit by model.createdVisit.collectAsStateWithLifecycle()
    val pending by model.pending.collectAsStateWithLifecycle()
    val saveError by model.saveError.collectAsStateWithLifecycle()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            if (visits == null) {
                LoadingOrError(error)
            } else {
                HomeScreen(visits.orEmpty(), { nav.navigate("new") }, { nav.navigate("details/$it") })
            }
        }
        composable("new") {
            BackHandler(enabled = creating) { /* Wait for the visit transaction. */ }
            LaunchedEffect(createdVisit) {
                createdVisit?.let { id ->
                    nav.navigate("edit/$id") { popUpTo("new") { inclusive = true } }
                    model.createdVisitHandled()
                }
            }
            NewVisitScreen(
                onBack = { if (!creating) nav.popBackStack() },
                onCreate = { institution, date, subject ->
                    model.create(institution, date, subject)
                },
                creating = creating,
                error = error,
            )
        }
        composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            LaunchedEffect(entry) { model.open(id) }
            val visit = detail?.takeIf { it.visit.id == id }
            if (visit == null) {
                LoadingOrError(error ?: saveError) { nav.popBackStack() }
            } else {
                DetailsScreen(visit, { nav.popBackStack() }, { nav.navigate("edit/$id") }, {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Inspecta Pocket — ${visit.visit.subject}")
                        putExtra(Intent.EXTRA_TEXT, visit.toShareText())
                    }
                    try {
                        context.startActivity(Intent.createChooser(send, "مشاركة تقرير الزيارة"))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "لا يتوفر تطبيق لاستقبال المشاركة", Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
        composable("edit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            LaunchedEffect(entry) { model.open(id) }
            val leave: () -> Unit = {
                scope.launch {
                    if (model.flush()) {
                        nav.navigate("details/$id") { popUpTo("home") }
                    }
                }
            }
            BackHandler(onBack = leave)
            val visit = detail?.takeIf { it.visit.id == id }
            if (visit == null) {
                LoadingOrError(error ?: saveError) { nav.popBackStack() }
            } else {
                InspectionScreen(visit, leave, model::setState, model::setNote, leave, pending > 0, saveError)
            }
        }
    }
}

@Composable
private fun LoadingOrError(error: String?, onBack: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error == null) CircularProgressIndicator() else Text(error)
        if (onBack != null) TextButton(onClick = onBack) { Text("العودة إلى القائمة") }
    }
}
