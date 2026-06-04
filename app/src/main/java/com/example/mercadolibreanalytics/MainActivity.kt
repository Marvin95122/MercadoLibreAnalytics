package com.example.mercadolibreanalytics

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mercadolibreanalytics.data.ChartItem
import com.example.mercadolibreanalytics.data.DashboardKpi
import com.example.mercadolibreanalytics.data.KpiDetail
import com.example.mercadolibreanalytics.data.KpiOption
import com.example.mercadolibreanalytics.data.KpiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

import android.widget.Toast
import com.example.mercadolibreanalytics.pdf.PdfGenerator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.mercadolibreanalytics.data.ExecutiveAlert
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.widthIn
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.mutableStateListOf

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MercadoLibreAnalyticsTheme {
                AppNavigator()
            }
        }
    }
}

@Composable
fun MercadoLibreAnalyticsTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Color(0xFF185ABC),
        secondary = Color(0xFFFFD600),
        background = Color(0xFFF3F6FA),
        surface = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun AppNavigator() {
    var screen by remember { mutableStateOf("login") }
    var selectedKpiId by remember { mutableStateOf<String?>(null) }
    val backStack = remember { mutableStateListOf<String>() }

    fun navigateTo(destination: String) {
        if (screen != destination) {
            backStack.add(screen)
            screen = destination
        }
    }

    fun goBack() {
        when {
            backStack.isNotEmpty() -> {
                screen = backStack.removeAt(backStack.lastIndex)
            }

            screen != "dashboard" && screen != "login" -> {
                screen = "dashboard"
            }

            screen == "dashboard" -> {
                // No cerramos la app desde el inicio.
                // El usuario puede cerrar sesión con el botón correspondiente.
            }
        }
    }

    BackHandler(enabled = screen != "login") {
        goBack()
    }

    when (screen) {
        "login" -> LoginScreen(
            onLoginSuccess = {
                backStack.clear()
                screen = "dashboard"
            }
        )

        "dashboard" -> DashboardScreen(
            onOpenMenu = { navigateTo("menu") },
            onOpenWeakness = { navigateTo("weakness") },
            onOpenReports = { navigateTo("reports") },
            onLogout = {
                backStack.clear()
                selectedKpiId = null
                screen = "login"
            }
        )

        "menu" -> KpiMenuScreen(
            onBack = { goBack() },
            onOpenDashboard = { navigateTo("dashboard") },
            onOpenWeakness = { navigateTo("weakness") },
            onOpenReports = { navigateTo("reports") },
            onSelectKpi = { id ->
                selectedKpiId = id
                navigateTo("detail")
            }
        )

        "detail" -> KpiDetailScreen(
            kpiId = selectedKpiId ?: "",
            onBack = { goBack() }
        )

        "weakness" -> WeaknessScreen(
            onBack = { goBack() },
            onOpenDashboard = { navigateTo("dashboard") },
            onOpenMenu = { navigateTo("menu") },
            onOpenReports = { navigateTo("reports") }
        )

        "reports" -> ReportsScreen(
            onOpenDashboard = { navigateTo("dashboard") },
            onOpenMenu = { navigateTo("menu") },
            onOpenWeakness = { navigateTo("weakness") },
            onSelectKpi = { id ->
                selectedKpiId = id
                navigateTo("detail")
            }
        )
    }
}

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FA))
                .padding(innerPadding)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF185ABC)
                                    )
                                ),
                                shape = RoundedCornerShape(30.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                text = "MercadoLibre Analytics",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Acceso ejecutivo para análisis de KPIs, reportes y toma de decisiones.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE2E8F0)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0x26FFFFFF)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Base de datos local · Modo offline · Reportes PDF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Inicio de sesión",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Ingrese las credenciales del ejecutivo autorizado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Correo ejecutivo") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Contraseña") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    if (
                                        email.trim() == "ejecutivo@mercadolibre.com" &&
                                        password.trim() == "admin123"
                                    ) {
                                        onLoginSuccess()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Credenciales incorrectas",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF185ABC)
                                )
                            ) {
                                Text("Entrar al panel ejecutivo")
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Usuario de prueba: ejecutivo@mercadolibre.com · Contraseña: admin123",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    onOpenMenu: () -> Unit,
    onOpenWeakness: () -> Unit,
    onOpenReports: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var kpis by remember { mutableStateOf<List<DashboardKpi>>(emptyList()) }
    var alerts by remember { mutableStateOf<List<ExecutiveAlert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                val repository = KpiRepository(context)
                Pair(
                    repository.getDashboardKpis(),
                    repository.getExecutiveAlerts()
                )
            }

            kpis = result.first
            alerts = result.second
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                current = "dashboard",
                onOpenDashboard = { },
                onOpenMenu = onOpenMenu,
                onOpenWeakness = onOpenWeakness,
                onOpenReports = onOpenReports
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FA))
                .padding(innerPadding)
        ) {
            when {
                isLoading -> LoadingContent()
                errorMessage != null -> ErrorContent(errorMessage ?: "Error desconocido")
                else -> DashboardContent(
                    kpis = kpis,
                    alerts = alerts,
                    onOpenMenu = onOpenMenu,
                    onOpenWeakness = onOpenWeakness,
                    onLogout = onLogout
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    kpis: List<DashboardKpi>,
    alerts: List<ExecutiveAlert>,
    onOpenMenu: () -> Unit,
    onOpenWeakness: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ExecutiveHeader()
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Cerrar sesión")
            }
        }

        item {
            SectionTitle(
                title = "Riesgos detectados",
                subtitle = "Riesgos detectados automáticamente con base en los datos locales."
            )
        }

        items(alerts) { alert ->
            ExecutiveAlertCard(alert)
        }

        item {
            SectionTitle(
                title = "Vista general",
                subtitle = "Indicadores principales calculados desde la base SQLite local."
            )
        }

        items(kpis) { kpi ->
            ExecutiveKpiCard(kpi)
        }

        item {
            ActionPanel(
                kpis = kpis,
                onOpenMenu = onOpenMenu,
                onOpenWeakness = onOpenWeakness
            )
        }
    }
}

@Composable
fun ExecutiveHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E5AA8)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
    ) {
        Column {
            Text(
                text = "MercadoLibre Analytics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Panel ejecutivo para la toma de decisiones",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x26FFFFFF)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Modo offline activo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "La aplicación consulta información directamente desde la base de datos local del dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun ExecutiveKpiCard(kpi: DashboardKpi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFE8F0FE),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = kpi.title.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF185ABC)
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = kpi.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = kpi.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = kpi.value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF185ABC)
            )
        }
    }
}

@Composable
fun ExecutiveAlertCard(alert: ExecutiveAlert) {
    val levelColor = when (alert.level) {
        "Alto" -> Color(0xFFB91C1C)
        "Medio" -> Color(0xFFD97706)
        else -> Color(0xFF15803D)
    }

    val backgroundColor = when (alert.level) {
        "Alto" -> Color(0xFFFFE4E6)
        "Medio" -> Color(0xFFFFF7ED)
        else -> Color(0xFFEFFDF5)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = alert.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = alert.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = alert.value,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Riesgo ${alert.level}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                }
            }
        }
    }
}

@Composable
fun ActionPanel(
    kpis: List<DashboardKpi>,
    onOpenMenu: () -> Unit,
    onOpenWeakness: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Centro de decisiones",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Consulta indicadores, gráficas, interpretaciones y recomendaciones para apoyar la toma de decisiones.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onOpenMenu,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF185ABC)
                )
            ) {
                Text("Ver menú de KPIs")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onOpenWeakness,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Ver oportunidad de mejora")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    try {
                        val file = PdfGenerator.generateDashboardReport(context, kpis)
                        PdfGenerator.sharePdf(context, file)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error al generar PDF: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF0F172A)
                )
            ) {
                Text("Generar PDF general")
            }
        }
    }
}

@Composable
fun KpiMenuScreen(
    onBack: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenWeakness: () -> Unit,
    onOpenReports: () -> Unit,
    onSelectKpi: (String) -> Unit
) {
    val context = LocalContext.current
    var options by remember { mutableStateOf<List<KpiOption>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        options = withContext(Dispatchers.IO) {
            KpiRepository(context).getKpiOptions()
        }
    }

    val filteredOptions = options.filter {
        it.title.contains(searchText, ignoreCase = true) ||
                it.description.contains(searchText, ignoreCase = true) ||
                it.section.contains(searchText, ignoreCase = true)
    }

    val groupedOptions = filteredOptions.groupBy { it.section }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                current = "menu",
                onOpenDashboard = onOpenDashboard,
                onOpenMenu = { },
                onOpenWeakness = onOpenWeakness,
                onOpenReports = onOpenReports
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FA))
                .padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PageHeader(
                    title = "Indicadores",
                    subtitle = "Explora los KPIs por área de análisis para interpretar inventario, ventas, operación y comportamiento."
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Buscar KPI o sección") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Regresar al inicio")
                }
            }

            groupedOptions.forEach { (section, sectionOptions) ->
                item {
                    KpiSectionHeader(
                        title = section,
                        count = sectionOptions.size
                    )
                }

                items(sectionOptions) { option ->
                    KpiOptionCard(
                        option = option,
                        onClick = { onSelectKpi(option.id) }
                    )
                }
            }

            item {
                WeaknessOptionCard(
                    onClick = onOpenWeakness
                )
            }
        }
    }
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun KpiSectionHeader(
    title: String,
    count: Int
) {
    val sectionColor = when (title) {
        "Inventario" -> Color(0xFF185ABC)
        "Ventas" -> Color(0xFF15803D)
        "Operación" -> Color(0xFFD97706)
        "Comportamiento" -> Color(0xFF7C3AED)
        "Calidad" -> Color(0xFFB91C1C)
        else -> Color(0xFF0F172A)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = sectionColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = getSectionDescription(title),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE2E8F0)
                )
            }

            Text(
                text = "$count KPIs",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = sectionColor,
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

fun getSectionDescription(section: String): String {
    return when (section) {
        "Inventario" -> "Valor, stock, marcas y categorías del catálogo."
        "Ventas" -> "Ingresos, pedidos y comportamiento comercial."
        "Operación" -> "Pagos, envíos y devoluciones."
        "Comportamiento" -> "Búsquedas, demanda y satisfacción."
        "Calidad" -> "Calificaciones, reseñas y problemas detectados."
        else -> "Indicadores generales del sistema."
    }
}

@Composable
fun KpiOptionCard(
    option: KpiOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = Color(0xFFE8F0FE),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF185ABC)
                )
            }

            Spacer(modifier = Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF185ABC)
            )
        }
    }
}

@Composable
fun WeaknessOptionCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4E5)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Debilidad empresarial detectada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Análisis de demanda no atendida mediante búsquedas con pocos resultados.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF92400E)
            )
        }
    }
}

@Composable
fun KpiDetailScreen(
    kpiId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<KpiDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var topLimit by remember { mutableStateOf(10) }
    var selectedYear by remember { mutableStateOf("Todos") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(kpiId) {
        try {
            val result = withContext(Dispatchers.IO) {
                KpiRepository(context).getKpiDetail(kpiId)
            }
            detail = result
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FA))
                .padding(innerPadding)
        ) {
            when {
                isLoading -> LoadingContent()

                errorMessage != null -> ErrorContent(errorMessage ?: "Error desconocido")

                detail != null -> {
                    val originalDetail = detail!!
                    val chartType = getChartType(originalDetail.title)

                    val filteredItems = when {
                        topLimit == 0 -> originalDetail.chartItems
                        chartType == "line" -> originalDetail.chartItems.takeLast(topLimit)
                        else -> originalDetail.chartItems.take(topLimit)
                    }

                    val isSalesKpi = originalDetail.title.contains("Ventas", ignoreCase = true)

                    val availableYears = if (isSalesKpi) {
                        originalDetail.chartItems
                            .mapNotNull { getYearFromPeriod(it.label) }
                            .distinct()
                            .sortedDescending()
                    } else {
                        emptyList()
                    }

                    val yearFilteredItems = if (isSalesKpi && selectedYear != "Todos") {
                        originalDetail.chartItems.filter {
                            getYearFromPeriod(it.label) == selectedYear
                        }
                    } else {
                        originalDetail.chartItems
                    }

                    val limitedItems = when {
                        topLimit == 0 -> yearFilteredItems
                        chartType == "line" -> yearFilteredItems.takeLast(topLimit)
                        else -> yearFilteredItems.take(topLimit)
                    }

                    val displayItems = if (isSalesKpi) {
                        limitedItems.map {
                            it.copy(label = formatPeriodForExecutive(it.label))
                        }
                    } else {
                        limitedItems
                    }

                    val displayAllItems = if (isSalesKpi) {
                        yearFilteredItems.map {
                            it.copy(label = formatPeriodForExecutive(it.label))
                        }
                    } else {
                        yearFilteredItems
                    }

                    val totalSales = yearFilteredItems.sumOf { it.value }

                    val detailForScreen = if (isSalesKpi) {
                        originalDetail.copy(
                            mainValue = formatMoneyForScreen(totalSales),
                            chartItems = displayItems
                        )
                    } else {
                        originalDetail.copy(chartItems = displayItems)
                    }

                    KpiDetailContent(
                        detail = detailForScreen,
                        allItems = displayAllItems,
                        topLimit = topLimit,
                        availableYears = availableYears,
                        selectedYear = selectedYear,
                        onYearChange = { selectedYear = it },
                        rawSalesItems = originalDetail.chartItems,
                        onTopLimitChange = { topLimit = it },
                        onBack = onBack
                    )
                }
            }
        }
    }
}

@Composable
fun KpiDetailContent(
    detail: KpiDetail,
    allItems: List<ChartItem>,
    topLimit: Int,
    availableYears: List<String>,
    selectedYear: String,
    onYearChange: (String) -> Unit,
    rawSalesItems: List<ChartItem>,
    onTopLimitChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<ChartItem?>(null) }

    LaunchedEffect(detail.title, detail.chartItems) {
        selectedItem = detail.chartItems.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageHeader(
                title = detail.title,
                subtitle = detail.description
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Regresar al menú")
            }
        }

        item {
            MainValueCard(detail)
        }

        if (availableYears.isNotEmpty()) {
            item {
                YearSelector(
                    years = availableYears,
                    selectedYear = selectedYear,
                    onYearChange = onYearChange
                )
            }

            item {
                SalesInsightCard(
                    rawItems = rawSalesItems,
                    selectedYear = selectedYear
                )
            }
        }

        item {
            TopLimitSelector(
                selectedLimit = topLimit,
                totalItems = allItems.size,
                chartType = getChartType(detail.title),
                onLimitChange = onTopLimitChange
            )
        }

        item {
            ChartCard(
                detail = detail,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }

        item {
            SelectedDataCard(
                detail = detail,
                selectedItem = selectedItem,
                allItems = allItems
            )
        }

        item {
            KpiQuickReadingCard(
                detail = detail,
                allItems = allItems
            )
        }

        item {
            DecisionSupportCard(
                detail = detail,
                allItems = allItems,
                isSalesKpi = availableYears.isNotEmpty()
            )
        }

        item {
            PdfExportCard(
                detail = detail,
                selectedItem = selectedItem,
                allItems = allItems,
                reportPeriod = buildReportPeriodText(
                    title = detail.title,
                    selectedYear = selectedYear,
                    hasYearFilter = availableYears.isNotEmpty()
                )
            )
        }
    }
}

@Composable
fun YearSelector(
    years: List<String>,
    selectedYear: String,
    onYearChange: (String) -> Unit
) {
    val options = listOf("Todos") + years

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Año analizado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Selecciona un año para identificar meses fuertes, meses débiles y caídas.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { year ->
                    Button(
                        onClick = { onYearChange(year) },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedYear == year) {
                                Color(0xFF185ABC)
                            } else {
                                Color(0xFFE2E8F0)
                            },
                            contentColor = if (selectedYear == year) {
                                Color.White
                            } else {
                                Color(0xFF0F172A)
                            }
                        )
                    ) {
                        Text(year)
                    }
                }
            }
        }
    }
}

@Composable
fun SalesInsightCard(
    rawItems: List<ChartItem>,
    selectedYear: String
) {
    val items = if (selectedYear == "Todos") {
        rawItems
    } else {
        rawItems.filter { getYearFromPeriod(it.label) == selectedYear }
    }

    val maxItem = items.maxByOrNull { it.value }
    val minItem = items.minByOrNull { it.value }
    val average = if (items.isNotEmpty()) items.sumOf { it.value } / items.size else 0.0

    val largestDrop = calculateLargestDrop(items)

    val previousYear = selectedYear.toIntOrNull()?.minus(1)?.toString()
    val currentYearTotal = items.sumOf { it.value }
    val previousYearTotal = if (previousYear != null) {
        rawItems
            .filter { getYearFromPeriod(it.label) == previousYear }
            .sumOf { it.value }
    } else {
        0.0
    }

    val comparisonText = when {
        selectedYear == "Todos" -> "Selecciona un año específico para comparar contra el año anterior."
        previousYearTotal <= 0.0 -> "No hay datos suficientes del año anterior para comparar."
        currentYearTotal > previousYearTotal -> {
            val percent = ((currentYearTotal - previousYearTotal) / previousYearTotal) * 100.0
            "Las ventas superan al año anterior en ${String.format("%.1f", percent)}%."
        }
        else -> {
            val percent = ((previousYearTotal - currentYearTotal) / previousYearTotal) * 100.0
            "Las ventas están por debajo del año anterior en ${String.format("%.1f", percent)}%."
        }
    }

    val recommendation = when {
        minItem == null -> "No hay información suficiente para generar una recomendación."
        largestDrop != null -> {
            "Para ${formatPeriodForExecutive(minItem.label)}, se recomienda activar cupones del 10% al 15%, envío gratis en productos seleccionados y campañas de remarketing durante las dos primeras semanas del mes."
        }
        else -> {
            "Mantener campañas de seguimiento y revisar categorías con menor rotación para reforzar ventas."
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Análisis del comportamiento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Lectura automática para detectar meses fuertes, meses débiles y caídas comerciales.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(16.dp))

            SalesInsightRow(
                title = "Mes con mayor venta",
                value = maxItem?.let { formatPeriodForExecutive(it.label) } ?: "Sin dato",
                detail = maxItem?.let { formatMoneyForScreen(it.value) } ?: "$0.00"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SalesInsightRow(
                title = "Mes con menor venta",
                value = minItem?.let { formatPeriodForExecutive(it.label) } ?: "Sin dato",
                detail = minItem?.let { formatMoneyForScreen(it.value) } ?: "$0.00"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SalesInsightRow(
                title = "Promedio mensual",
                value = formatMoneyForScreen(average),
                detail = "Referencia para detectar meses por debajo del comportamiento normal"
            )

            Spacer(modifier = Modifier.height(10.dp))

            SalesInsightRow(
                title = "Mayor caída mensual",
                value = largestDrop?.label ?: "Sin caída detectada",
                detail = largestDrop?.detail ?: "No se identificó una disminución entre meses consecutivos"
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = comparisonText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFFFD600)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Decisión sugerida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
fun SalesInsightRow(
    title: String,
    value: String,
    detail: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1)
        )
    }
}

@Composable
fun TopLimitSelector(
    selectedLimit: Int,
    totalItems: Int,
    chartType: String,
    onLimitChange: (Int) -> Unit
) {
    val isLineChart = chartType == "line"

    val options = buildList {
        if (totalItems > 5) add(5)
        if (totalItems > 10) add(10)
        if (totalItems > 15) add(15)
        add(0)
    }

    val showingCount = when {
        totalItems == 0 -> 0
        selectedLimit == 0 -> totalItems
        selectedLimit > totalItems -> totalItems
        else -> selectedLimit
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Filtro de visualización",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Mostrando $showingCount de $totalItems resultados disponibles.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                options.forEach { limit ->
                    val isSelected = when {
                        limit == 0 && selectedLimit == 0 -> true
                        selectedLimit == limit -> true
                        else -> false
                    }

                    Button(
                        onClick = { onLimitChange(limit) },
                        modifier = Modifier
                            .height(42.dp)
                            .widthIn(min = 82.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                Color(0xFF185ABC)
                            } else {
                                Color(0xFFE2E8F0)
                            },
                            contentColor = if (isSelected) {
                                Color.White
                            } else {
                                Color(0xFF0F172A)
                            }
                        )
                    ) {
                        Text(
                            text = when {
                                limit == 0 -> "Todos"
                                isLineChart -> "Últimos $limit"
                                else -> "Top $limit"
                            },
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainValueCard(detail: KpiDetail) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF185ABC),
                        Color(0xFF38BDF8)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(22.dp)
    ) {
        Column {
            Text(
                text = "Indicador principal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail.mainValue,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun KpiQuickReadingCard(
    detail: KpiDetail,
    allItems: List<ChartItem>
) {
    val quickData = buildQuickReadingData(detail, allItems)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Lectura rápida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = quickData.description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(14.dp))

            quickData.metrics.forEachIndexed { index, metric ->
                QuickMetricItem(
                    title = metric.title,
                    value = metric.value,
                    subtitle = metric.subtitle,
                    background = metric.background,
                    accent = metric.accent,
                    icon = metric.icon
                )

                if (index < quickData.metrics.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun SelectedDataCard(
    detail: KpiDetail,
    selectedItem: ChartItem?,
    allItems: List<ChartItem>
) {
    val insight = buildSelectedInsightData(
        detail = detail,
        selectedItem = selectedItem,
        allItems = allItems
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = insight.accent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = insight.icon,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Lectura del elemento seleccionado en la gráfica.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedItem == null) {
                Text(
                    text = "Selecciona una barra, punto o elemento de la gráfica para ver su detalle.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )
            } else {
                SelectedInsightBox(
                    title = insight.labelTitle,
                    value = selectedItem.label,
                    subtitle = insight.contextText,
                    accent = insight.accent
                )

                Spacer(modifier = Modifier.height(12.dp))

                SelectedInsightBox(
                    title = insight.valueTitle,
                    value = formatChartValue(selectedItem.value, detail.valuePrefix, detail.valueSuffix),
                    subtitle = insight.percentageText,
                    accent = Color(0xFFFFD600)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Lectura",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = insight.analysis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Acción sugerida",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = insight.decision,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFFFF3B0)
                )
            }
        }
    }
}

@Composable
fun SelectedInsightBox(
    title: String,
    value: String,
    subtitle: String,
    accent: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF1E293B),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFCBD5E1),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun QuickMetricItem(
    title: String,
    value: String,
    subtitle: String,
    background: Color,
    accent: Color,
    icon: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = background,
                shape = RoundedCornerShape(22.dp)
            )
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = accent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

data class SelectedInsightData(
    val title: String,
    val labelTitle: String,
    val valueTitle: String,
    val contextText: String,
    val percentageText: String,
    val analysis: String,
    val decision: String,
    val accent: Color,
    val icon: String
)

data class QuickReadingData(
    val description: String,
    val metrics: List<QuickReadingMetric>
)

data class QuickReadingMetric(
    val title: String,
    val value: String,
    val subtitle: String,
    val background: Color,
    val accent: Color,
    val icon: String
)

@Composable
fun ChartCard(
    detail: KpiDetail,
    selectedItem: ChartItem?,
    onItemSelected: (ChartItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Visualización de datos",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Toca un elemento para consultar su detalle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text = getChartBadge(detail.title),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color(0xFF185ABC),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (detail.chartItems.isEmpty()) {
                Text(
                    text = "No hay datos disponibles para graficar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            } else {
                when (getChartType(detail.title)) {
                    "line" -> ProfessionalLineChart(
                        items = detail.chartItems,
                        valuePrefix = detail.valuePrefix,
                        valueSuffix = detail.valueSuffix,
                        selectedItem = selectedItem,
                        onItemSelected = onItemSelected
                    )

                    "donut" -> ProfessionalDonutChart(
                        items = detail.chartItems,
                        valuePrefix = detail.valuePrefix,
                        valueSuffix = detail.valueSuffix,
                        selectedItem = selectedItem,
                        onItemSelected = onItemSelected
                    )

                    else -> ProfessionalBarChart(
                        items = detail.chartItems,
                        valuePrefix = detail.valuePrefix,
                        valueSuffix = detail.valueSuffix,
                        selectedItem = selectedItem,
                        onItemSelected = onItemSelected
                    )
                }
            }
        }
    }
}

fun getChartType(title: String): String {
    return when {
        title.contains("Ventas por mes", ignoreCase = true) -> "line"
        title.contains("Pedidos por estado", ignoreCase = true) -> "donut"
        title.contains("Métodos de pago", ignoreCase = true) -> "donut"
        title.contains("Envíos por estado", ignoreCase = true) -> "donut"
        title.contains("Devoluciones por motivo", ignoreCase = true) -> "donut"
        else -> "bar"
    }
}

fun getChartBadge(title: String): String {
    return when (getChartType(title)) {
        "line" -> "Línea"
        "donut" -> "Dona"
        else -> "Barras"
    }
}

fun getChartDescription(title: String): String {
    return when (getChartType(title)) {
        "line" -> "Tendencia temporal del indicador seleccionado."
        "donut" -> "Distribución proporcional de los resultados."
        else -> "Comparación de valores principales."
    }
}

@Composable
fun ProfessionalBarChart(
    items: List<ChartItem>,
    valuePrefix: String,
    valueSuffix: String,
    selectedItem: ChartItem?,
    onItemSelected: (ChartItem) -> Unit
) {
    val maxValue = max(1.0, items.maxOfOrNull { it.value } ?: 1.0)

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items.forEachIndexed { index, item ->
            val fraction = max(0.07f, (item.value / maxValue).toFloat())
            val isSelected = selectedItem?.label == item.label

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemSelected(item) }
                    .background(
                        color = if (isSelected) Color(0xFFE8F0FE) else Color.Transparent,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(if (isSelected) 10.dp else 0.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    color = if (isSelected) Color(0xFF185ABC) else Color(0xFFE8F0FE),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF185ABC)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = formatChartValue(item.value, valuePrefix, valueSuffix),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF185ABC)
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSelected) 18.dp else 16.dp)
                        .background(
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF185ABC),
                                        Color(0xFF38BDF8)
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun ProfessionalLineChart(
    items: List<ChartItem>,
    valuePrefix: String,
    valueSuffix: String,
    selectedItem: ChartItem?,
    onItemSelected: (ChartItem) -> Unit
) {
    val visibleItems = items
    val maxValue = visibleItems.maxOfOrNull { it.value } ?: 1.0
    val minValue = visibleItems.minOfOrNull { it.value } ?: 0.0
    val range = max(1.0, maxValue - minValue)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .background(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(22.dp)
                )
                .padding(12.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val paddingX = 24.dp.toPx()
            val paddingY = 26.dp.toPx()

            val availableWidth = chartWidth - paddingX * 2
            val availableHeight = chartHeight - paddingY * 2

            repeat(5) { index ->
                val y = paddingY + availableHeight * index / 4
                drawLine(
                    color = Color(0xFFE2E8F0),
                    start = Offset(paddingX, y),
                    end = Offset(chartWidth - paddingX, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            val points = visibleItems.mapIndexed { index, item ->
                val x = if (visibleItems.size == 1) {
                    paddingX + availableWidth / 2
                } else {
                    paddingX + availableWidth * index / (visibleItems.size - 1)
                }

                val normalized = ((item.value - minValue) / range).toFloat()
                val y = paddingY + availableHeight - (availableHeight * normalized)

                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF185ABC),
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                points.forEachIndexed { index, point ->
                    val item = visibleItems[index]
                    val isSelected = selectedItem?.label == item.label

                    drawCircle(
                        color = if (isSelected) Color(0xFFFFD600) else Color.White,
                        radius = if (isSelected) 9.dp.toPx() else 7.dp.toPx(),
                        center = point
                    )

                    drawCircle(
                        color = Color(0xFF185ABC),
                        radius = if (isSelected) 5.dp.toPx() else 4.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleItems.forEach { item ->
                val isSelected = selectedItem?.label == item.label

                Button(
                    onClick = { onItemSelected(item) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) {
                            Color(0xFF185ABC)
                        } else {
                            Color(0xFFE2E8F0)
                        },
                        contentColor = if (isSelected) {
                            Color.White
                        } else {
                            Color(0xFF0F172A)
                        }
                    )
                ) {
                    Text(item.label)
                }
            }
        }
    }
}

@Composable
fun ProfessionalDonutChart(
    items: List<ChartItem>,
    valuePrefix: String,
    valueSuffix: String,
    selectedItem: ChartItem?,
    onItemSelected: (ChartItem) -> Unit
) {
    val visibleItems = items
    val total = max(1.0, visibleItems.sumOf { it.value })

    val chartColors = listOf(
        Color(0xFF185ABC),
        Color(0xFFFFD600),
        Color(0xFF10B981),
        Color(0xFFF97316),
        Color(0xFF8B5CF6),
        Color(0xFFEF4444),
        Color(0xFF14B8A6),
        Color(0xFFEC4899)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(230.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.size(220.dp)
            ) {
                val strokeWidth = 34.dp.toPx()
                var startAngle = -90f

                visibleItems.forEachIndexed { index, item ->
                    val sweepAngle = ((item.value / total) * 360f).toFloat()
                    val isSelected = selectedItem?.label == item.label

                    drawArc(
                        color = chartColors[index % chartColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(
                            width = size.width - strokeWidth,
                            height = size.height - strokeWidth
                        ),
                        style = Stroke(
                            width = if (isSelected) strokeWidth + 8.dp.toPx() else strokeWidth,
                            cap = StrokeCap.Butt
                        )
                    )

                    startAngle += sweepAngle
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Text(
                    text = formatChartValue(total, valuePrefix, valueSuffix),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            visibleItems.forEachIndexed { index, item ->
                val percent = (item.value * 100.0 / total)
                val isSelected = selectedItem?.label == item.label

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemSelected(item) }
                        .background(
                            color = if (isSelected) Color(0xFFE8F0FE) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 16.dp else 12.dp)
                            .background(
                                color = chartColors[index % chartColors.size],
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${String.format("%.1f", percent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF185ABC)
                    )
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    items: List<ChartItem>,
    valuePrefix: String,
    valueSuffix: String
) {
    val maxValue = max(1.0, items.maxOfOrNull { it.value } ?: 1.0)

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items.forEach { item ->
            val fraction = max(0.06f, (item.value / maxValue).toFloat())

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = item.label.take(26),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatChartValue(item.value, valuePrefix, valueSuffix),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF185ABC)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(13.dp)
                        .background(
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(50)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF185ABC),
                                        Color(0xFF38BDF8)
                                    )
                                ),
                                shape = RoundedCornerShape(50)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun DataTableCard(detail: KpiDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Tabla resumida",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(12.dp))

            detail.chartItems.take(8).forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${index + 1}. ${item.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = formatChartValue(item.value, detail.valuePrefix, detail.valueSuffix),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF185ABC)
                    )
                }
            }
        }
    }
}

@Composable
fun ExecutiveAnalysisCard(detail: KpiDetail) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Lectura del indicador",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail.interpretation,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Recomendación",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = detail.recommendation,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )
        }
    }
}

@Composable
fun DecisionSupportCard(
    detail: KpiDetail,
    allItems: List<ChartItem>,
    isSalesKpi: Boolean
) {
    val topItem = allItems.maxByOrNull { it.value }
    val lowItem = allItems.minByOrNull { it.value }
    val total = allItems.sumOf { it.value }
    val topPercent = if (topItem != null && total > 0) {
        topItem.value * 100.0 / total
    } else {
        0.0
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = if (isSalesKpi) "Cómo interpretar este indicador" else "Diagnóstico del indicador",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = buildDiagnosticMessage(
                    title = detail.title,
                    topItem = topItem,
                    lowItem = lowItem,
                    topPercent = topPercent,
                    isSalesKpi = isSalesKpi
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF475569)
            )

            if (!isSalesKpi) {
                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Acción sugerida",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = buildSpecificRecommendation(
                        title = detail.title,
                        topItem = topItem,
                        lowItem = lowItem
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569)
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "La decisión específica ya se muestra en la tarjeta de análisis del comportamiento, donde se identifica el mes débil y la acción comercial recomendada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun PdfExportCard(
    detail: KpiDetail,
    selectedItem: ChartItem? = null,
    allItems: List<ChartItem> = detail.chartItems,
    reportPeriod: String = "Todos los datos disponibles del indicador",
    buttonText: String = "Exportar reporte PDF"
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Reporte del indicador",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Genera un archivo PDF con el valor principal, periodo analizado, lectura rápida, detalle seleccionado, interpretación y recomendación.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Periodo: $reportPeriod",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF185ABC)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    try {
                        val file = PdfGenerator.generateKpiReport(
                            context = context,
                            detail = detail,
                            selectedItem = selectedItem,
                            allItems = allItems,
                            reportPeriod = reportPeriod
                        )

                        PdfGenerator.sharePdf(context, file)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error al generar PDF: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF185ABC)
                )
            ) {
                Text(buttonText)
            }
        }
    }
}

@Composable
fun WeaknessScreen(
    onBack: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenReports: () -> Unit
) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<KpiDetail?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                KpiRepository(context).getKpiDetail("busquedas_pocos_resultados")
            }
            detail = result
        } catch (e: Exception) {
            errorMessage = e.message
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                current = "weakness",
                onOpenDashboard = onOpenDashboard,
                onOpenMenu = onOpenMenu,
                onOpenWeakness = { },
                onOpenReports = onOpenReports
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FA))
                .padding(innerPadding)
        ) {
            when {
                isLoading -> LoadingContent()
                errorMessage != null -> ErrorContent(errorMessage ?: "Error desconocido")
                detail != null -> WeaknessContent(
                    detail = detail!!,
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
fun WeaknessContent(
    detail: KpiDetail,
    onBack: () -> Unit
) {
    var selectedItem by remember { mutableStateOf<ChartItem?>(null) }

    LaunchedEffect(detail.title, detail.chartItems) {
        selectedItem = detail.chartItems.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PageHeader(
                title = "Oportunidad de mejora",
                subtitle = "Demanda no atendida detectada mediante búsquedas con pocos resultados."
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Regresar al dashboard")
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF92400E),
                                Color(0xFFF59E0B)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "Problema detectado",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "La plataforma presenta búsquedas frecuentes con pocos resultados disponibles. Esto indica que existen usuarios interesados en ciertos productos, pero la oferta actual no cubre completamente esa demanda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }

        item {
            MainValueCard(detail)
        }

        item {
            ChartCard(
                detail = detail,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it }
            )
        }

        item {
            SelectedDataCard(
                detail = detail,
                selectedItem = selectedItem,
                allItems = detail.chartItems
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Impacto para la empresa",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Si los usuarios buscan productos y encuentran pocos resultados, pueden abandonar la plataforma o comprar en la competencia. Esto reduce oportunidades de venta y afecta la percepción del catálogo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Solución propuesta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ampliar el catálogo en las palabras clave con mayor demanda, atraer vendedores relacionados, mejorar la clasificación de productos y crear campañas dirigidas a categorías con baja oferta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
        item {
            PdfExportCard(
                detail = detail,
                selectedItem = selectedItem,
                allItems = detail.chartItems,
                buttonText = "Exportar PDF de oportunidad de mejora"
            )
        }
    }
}

@Composable
fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF185ABC)
        )
    }
}

@Composable
fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No se pudo leer la información",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Red
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.DarkGray
        )
    }
}

@Composable
fun AppBottomBar(
    current: String,
    onOpenDashboard: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenWeakness: () -> Unit,
    onOpenReports: () -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = current == "dashboard",
            onClick = onOpenDashboard,
            icon = { Text("🏠") },
            label = { Text("Inicio") }
        )

        NavigationBarItem(
            selected = current == "menu",
            onClick = onOpenMenu,
            icon = { Text("📊") },
            label = { Text("KPIs") }
        )

        NavigationBarItem(
            selected = current == "weakness",
            onClick = onOpenWeakness,
            icon = { Text("⚠️") },
            label = { Text("Riesgos") }
        )

        NavigationBarItem(
            selected = current == "reports",
            onClick = onOpenReports,
            icon = { Text("📄") },
            label = { Text("PDF") }
        )
    }
}

@Composable
fun ReportsScreen(
    onOpenDashboard: () -> Unit,
    onOpenMenu: () -> Unit,
    onOpenWeakness: () -> Unit,
    onSelectKpi: (String) -> Unit
) {
    val context = LocalContext.current
    var kpis by remember { mutableStateOf<List<DashboardKpi>>(emptyList()) }
    var options by remember { mutableStateOf<List<KpiOption>>(emptyList()) }

    LaunchedEffect(Unit) {
        val repository = withContext(Dispatchers.IO) {
            KpiRepository(context)
        }

        kpis = withContext(Dispatchers.IO) {
            repository.getDashboardKpis()
        }

        options = withContext(Dispatchers.IO) {
            repository.getKpiOptions()
        }
    }

    val groupedOptions = options.groupBy { it.section }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(
                current = "reports",
                onOpenDashboard = onOpenDashboard,
                onOpenMenu = onOpenMenu,
                onOpenWeakness = onOpenWeakness,
                onOpenReports = { }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3F6FA))
                .padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PageHeader(
                    title = "Reportes PDF",
                    subtitle = "Genera reportes generales o abre un indicador específico para exportar su análisis."
                )
            }

            item {
                GeneralReportCard(
                    kpis = kpis
                )
            }

            groupedOptions.forEach { (section, sectionOptions) ->
                item {
                    KpiSectionHeader(
                        title = section,
                        count = sectionOptions.size
                    )
                }

                items(sectionOptions) { option ->
                    ReportOptionCard(
                        option = option,
                        onClick = { onSelectKpi(option.id) }
                    )
                }
            }

            item {
                OpportunityReportCard(
                    onClick = onOpenWeakness
                )
            }
        }
    }
}

@Composable
fun ReportOptionCard(
    option: KpiOption,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        color = Color(0xFFE8F0FE),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PDF",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF185ABC)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Abrir indicador para visualizar datos y exportar su reporte PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Text(
                text = "›",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF185ABC)
            )
        }
    }
}

@Composable
fun OpportunityReportCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF4E5)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "Reporte de oportunidad de mejora",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF92400E)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Abre el análisis de demanda no atendida para exportar un PDF con problema, impacto y solución propuesta.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF92400E)
            )
        }
    }
}

@Composable
fun GeneralReportCard(
    kpis: List<DashboardKpi>
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F172A)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Reporte general",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Incluye los indicadores principales del panel de inicio: ventas, pedidos, productos, usuarios, tiendas, pagos, devoluciones y calificación promedio.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    try {
                        val file = PdfGenerator.generateDashboardReport(context, kpis)
                        PdfGenerator.sharePdf(context, file)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error al generar PDF: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFFD600),
                    contentColor = Color(0xFF0F172A)
                )
            ) {
                Text("Generar PDF general")
            }
        }
    }
}

data class DropInfo(
    val label: String,
    val detail: String
)

fun getYearFromPeriod(label: String): String? {
    return if (label.length >= 4) label.substring(0, 4) else null
}

fun formatPeriodForExecutive(period: String): String {
    if (!Regex("\\d{4}-\\d{2}").matches(period)) {
        return period
    }

    val parts = period.split("-")
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return period

    val monthName = when (month) {
        1 -> "enero"
        2 -> "febrero"
        3 -> "marzo"
        4 -> "abril"
        5 -> "mayo"
        6 -> "junio"
        7 -> "julio"
        8 -> "agosto"
        9 -> "septiembre"
        10 -> "octubre"
        11 -> "noviembre"
        12 -> "diciembre"
        else -> return period
    }

    return "$monthName de $year"
}

fun formatMoneyForScreen(value: Double): String {
    return "$" + String.format("%,.2f", value)
}

fun calculateLargestDrop(items: List<ChartItem>): DropInfo? {
    if (items.size < 2) return null

    var largestDropValue = 0.0
    var result: DropInfo? = null

    for (i in 1 until items.size) {
        val previous = items[i - 1]
        val current = items[i]

        val difference = previous.value - current.value

        if (difference > largestDropValue && previous.value > 0) {
            largestDropValue = difference

            val percent = (difference / previous.value) * 100.0

            result = DropInfo(
                label = "${formatPeriodForExecutive(previous.label)} → ${formatPeriodForExecutive(current.label)}",
                detail = "Bajó ${formatMoneyForScreen(difference)} (${String.format("%.1f", percent)}%)"
            )
        }
    }

    return result
}

fun buildReportPeriodText(
    title: String,
    selectedYear: String,
    hasYearFilter: Boolean
): String {
    return when {
        hasYearFilter && selectedYear == "Todos" -> {
            "Todos los años disponibles en la base de datos"
        }
        hasYearFilter -> {
            "Año $selectedYear"
        }
        title.contains("Ventas", ignoreCase = true) -> {
            "Todos los meses y años disponibles"
        }
        else -> {
            "Todos los registros disponibles del indicador"
        }
    }
}

fun formatChartValue(
    value: Double,
    prefix: String,
    suffix: String
): String {
    return when {
        prefix == "$" -> "$" + String.format("%,.0f", value)
        suffix.isNotEmpty() -> String.format("%.2f", value) + suffix
        value >= 1000 -> String.format("%,.0f", value)
        else -> String.format("%.0f", value)
    }
}

fun buildDiagnosticMessage(
    title: String,
    topItem: ChartItem?,
    lowItem: ChartItem?,
    topPercent: Double,
    isSalesKpi: Boolean
): String {
    if (topItem == null || lowItem == null) {
        return "No hay información suficiente para generar un diagnóstico confiable."
    }

    if (isSalesKpi) {
        return "Este KPI debe leerse comparando el mes con mayor venta, el mes con menor venta y la mayor caída mensual. Una caída ocurre cuando un mes vende menos que el mes anterior o queda por debajo del promedio anual. La información sirve para decidir en qué periodo reforzar promociones, campañas o disponibilidad de productos."
    }

    return when {
        title.contains("Valor del inventario", ignoreCase = true) -> {
            "La mayor concentración económica se encuentra en ${topItem.label}, con ${formatChartValue(topItem.value, "$", "")}. Esto indica dónde está acumulado el mayor valor del inventario y dónde conviene vigilar la rotación."
        }

        title.contains("Stock disponible", ignoreCase = true) -> {
            "La categoría con más unidades disponibles es ${topItem.label}. Un stock alto puede ser positivo si existe demanda, pero también puede representar inventario detenido si las ventas no avanzan al mismo ritmo."
        }

        title.contains("Precio promedio", ignoreCase = true) -> {
            "${topItem.label} presenta el precio promedio más alto. Esto puede indicar productos de mayor valor, pero también exige estrategias que faciliten la compra."
        }

        title.contains("Productos por estado", ignoreCase = true) -> {
            "El estado con mayor presencia es ${topItem.label}. Si predominan productos pausados, agotados o eliminados, la disponibilidad del catálogo puede verse afectada."
        }

        title.contains("Marcas con más productos", ignoreCase = true) -> {
            "${topItem.label} concentra la mayor cantidad de productos. Si pocas marcas dominan el catálogo, puede existir dependencia comercial o poca variedad para el comprador."
        }

        title.contains("Valor por categoría y marca", ignoreCase = true) -> {
            "La combinación con mayor valor es ${topItem.label}. Esta relación permite detectar qué categoría y marca concentran más capital dentro del inventario."
        }

        title.contains("Pedidos por estado", ignoreCase = true) -> {
            "El estado más frecuente es ${topItem.label}. Este dato permite conocer dónde se concentra el flujo de pedidos y detectar posibles acumulaciones operativas."
        }

        title.contains("Productos por categoría", ignoreCase = true) -> {
            "${topItem.label} concentra más productos que las demás categorías. Esto puede indicar una categoría fuerte, pero también una posible saturación del catálogo."
        }

        title.contains("Stock crítico", ignoreCase = true) -> {
            "Los productos mostrados tienen los niveles de inventario más bajos. Estos casos requieren atención porque pueden provocar pérdida de ventas por falta de disponibilidad."
        }

        title.contains("Uso de métodos de pago", ignoreCase = true) || title.contains("Métodos de pago", ignoreCase = true) -> {
            "El método más utilizado es ${topItem.label}. Si un método concentra demasiado uso, conviene asegurar su estabilidad; si otros métodos tienen baja participación, puede existir poca confianza o baja visibilidad."
        }

        title.contains("Envíos por estado", ignoreCase = true) -> {
            "El estado logístico más frecuente es ${topItem.label}. Este indicador ayuda a detectar si la operación se concentra en entregas completadas, pendientes o con posibles retrasos."
        }

        title.contains("Búsquedas con pocos resultados", ignoreCase = true) -> {
            "La búsqueda más repetida con baja oferta es ${topItem.label}. Esto muestra demanda no atendida: usuarios interesados en productos que tienen pocos resultados disponibles."
        }

        title.contains("Devoluciones por motivo", ignoreCase = true) -> {
            "El motivo de devolución más frecuente es ${topItem.label}. Este dato permite identificar problemas de calidad, descripción, entrega o expectativa del comprador."
        }

        title.contains("Productos peor calificados", ignoreCase = true) -> {
            "El producto con menor calificación dentro del análisis es ${lowItem.label}. Una baja calificación puede afectar la confianza del comprador y reducir la recompra."
        }

        else -> {
            "${topItem.label} concentra el valor más alto del indicador, con una participación aproximada de ${String.format("%.1f", topPercent)}%. Este resultado ayuda a priorizar decisiones sobre los elementos con mayor peso."
        }
    }
}

fun buildSpecificRecommendation(
    title: String,
    topItem: ChartItem?,
    lowItem: ChartItem?
): String {
    if (topItem == null) {
        return "Revisar los datos del indicador antes de tomar una decisión."
    }

    return when {
        title.contains("Valor del inventario", ignoreCase = true) -> {
            "Para ${topItem.label}, aplicar descuentos controlados del 5% al 10%, paquetes promocionales y envío gratis en productos seleccionados para aumentar rotación sin reducir demasiado el margen."
        }

        title.contains("Stock disponible", ignoreCase = true) -> {
            "En ${topItem.label}, revisar productos con mucho stock y baja venta. Se recomienda crear promociones por volumen, combos de productos y campañas de liquidación por temporada."
        }

        title.contains("Precio promedio", ignoreCase = true) -> {
            "Para categorías con precio alto como ${topItem.label}, ofrecer meses sin intereses, cupones por compra mínima y envío gratis para reducir la barrera de compra."
        }

        title.contains("Productos por estado", ignoreCase = true) -> {
            "Revisar los productos en estado ${topItem.label}. Si son productos pausados o agotados, actualizar publicación, precio, fotografías, descripción y disponibilidad."
        }

        title.contains("Marcas con más productos", ignoreCase = true) -> {
            "Mantener seguimiento a ${topItem.label}, pero también impulsar marcas con menor presencia para mejorar variedad, reducir dependencia y ampliar opciones para el comprador."
        }

        title.contains("Valor por categoría y marca", ignoreCase = true) -> {
            "Para ${topItem.label}, crear campañas de rotación, descuentos graduales y promociones por paquete, ya que concentra alto valor económico dentro del inventario."
        }

        title.contains("Pedidos por estado", ignoreCase = true) -> {
            "Si ${topItem.label} representa pedidos pendientes o cancelados, revisar tiempos de procesamiento, confirmación de pago y comunicación con el comprador."
        }

        title.contains("Productos por categoría", ignoreCase = true) -> {
            "Para ${topItem.label}, revisar si existe saturación. Si la categoría tiene alta venta, mantener surtido; si no, aplicar promociones y depurar productos con baja rotación."
        }

        title.contains("Stock crítico", ignoreCase = true) -> {
            "Priorizar reabastecimiento de los productos con stock menor o igual a 5 unidades, especialmente aquellos con ventas recientes o buena calificación."
        }

        title.contains("Uso de métodos de pago", ignoreCase = true) || title.contains("Métodos de pago", ignoreCase = true) -> {
            "Promover el método ${topItem.label} si tiene buena aceptación. Para tarjetas de débito y crédito, mostrar mensajes claros de rechazo y ofrecer Mercado Pago o transferencia como alternativa."
        }

        title.contains("Envíos por estado", ignoreCase = true) -> {
            "Si ${topItem.label} corresponde a envíos pendientes o retrasados, revisar tiempos de preparación, empresa logística y zonas con mayor acumulación."
        }

        title.contains("Búsquedas con pocos resultados", ignoreCase = true) -> {
            "Para la búsqueda ${topItem.label}, atraer vendedores relacionados, ampliar catálogo y crear campañas para productos asociados a esa palabra clave."
        }

        title.contains("Devoluciones por motivo", ignoreCase = true) -> {
            "Atender el motivo ${topItem.label}. Se recomienda revisar descripción, fotografías, calidad del producto, empaque y tiempos de entrega según la causa detectada."
        }

        title.contains("Productos peor calificados", ignoreCase = true) -> {
            val product = lowItem?.label ?: topItem.label
            "Revisar el producto $product. Se recomienda mejorar descripción, fotografías, control de calidad, empaque y atención postventa."
        }

        else -> {
            "Priorizar acciones sobre ${topItem.label}, porque representa el valor más relevante dentro del indicador."
        }
    }
}

fun buildQuickReadingData(
    detail: KpiDetail,
    allItems: List<ChartItem>
): QuickReadingData {
    val total = allItems.sumOf { it.value }
    val maxItem = allItems.maxByOrNull { it.value }
    val minItem = allItems.minByOrNull { it.value }
    val dominantPercent = if (total > 0 && maxItem != null) {
        maxItem.value * 100.0 / total
    } else {
        0.0
    }

    if (allItems.isEmpty()) {
        return QuickReadingData(
            description = "No hay datos suficientes para generar una lectura automática.",
            metrics = listOf(
                QuickReadingMetric(
                    title = "Sin información",
                    value = "No disponible",
                    subtitle = "El indicador no tiene registros para analizar.",
                    background = Color(0xFFF8FAFC),
                    accent = Color(0xFF64748B),
                    icon = "—"
                )
            )
        )
    }

    return when {
        detail.title.contains("Ventas", ignoreCase = true) -> {
            QuickReadingData(
                description = "Resumen de meses fuertes, meses débiles y peso del mejor mes dentro del total vendido.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Mes con mayor venta",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(maxItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "↑"
                    ),
                    QuickReadingMetric(
                        title = "Mes con menor venta",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(minItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFFFF7ED),
                        accent = Color(0xFFD97706),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Peso del mejor mes",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Participación del mes más fuerte dentro del total analizado.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Valor del inventario", ignoreCase = true) -> {
            QuickReadingData(
                description = "Identifica dónde se concentra el mayor valor económico del inventario.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Categoría con mayor valor",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(maxItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "$"
                    ),
                    QuickReadingMetric(
                        title = "Categoría con menor valor",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(minItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↘"
                    ),
                    QuickReadingMetric(
                        title = "Concentración principal",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Porcentaje del valor total concentrado en la categoría principal.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Stock disponible", ignoreCase = true) -> {
            QuickReadingData(
                description = "Permite ubicar categorías con más unidades disponibles y posibles acumulaciones.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Categoría con mayor stock",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(maxItem?.value ?: 0.0, "", "")} unidades disponibles",
                        background = Color(0xFFEFFDF5),
                        accent = Color(0xFF15803D),
                        icon = "S"
                    ),
                    QuickReadingMetric(
                        title = "Categoría con menor stock",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(minItem?.value ?: 0.0, "", "")} unidades disponibles",
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Peso del mayor stock",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Indica qué tanto inventario concentra la categoría principal.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Precio promedio", ignoreCase = true) -> {
            QuickReadingData(
                description = "Compara las categorías con precios promedio más altos y más bajos.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Precio promedio más alto",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(maxItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "$"
                    ),
                    QuickReadingMetric(
                        title = "Precio promedio más bajo",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(minItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Diferencia de precio",
                        value = formatChartValue((maxItem?.value ?: 0.0) - (minItem?.value ?: 0.0), detail.valuePrefix, detail.valueSuffix),
                        subtitle = "Brecha entre la categoría más cara y la más económica.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "Δ"
                    )
                )
            )
        }

        detail.title.contains("Productos por estado", ignoreCase = true) ||
                detail.title.contains("Pedidos por estado", ignoreCase = true) ||
                detail.title.contains("Envíos por estado", ignoreCase = true) -> {
            QuickReadingData(
                description = "Muestra el estado dominante y permite detectar acumulaciones operativas.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Estado dominante",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(maxItem?.value ?: 0.0, "", "")} registros",
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "E"
                    ),
                    QuickReadingMetric(
                        title = "Estado menos frecuente",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(minItem?.value ?: 0.0, "", "")} registros",
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Concentración del estado principal",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Peso del estado dominante dentro del total analizado.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Marcas con más productos", ignoreCase = true) -> {
            QuickReadingData(
                description = "Identifica marcas con mayor presencia dentro del catálogo analizado.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Marca con más productos",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(maxItem?.value ?: 0.0, "", "")} productos",
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "M"
                    ),
                    QuickReadingMetric(
                        title = "Marca con menor presencia",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(minItem?.value ?: 0.0, "", "")} productos",
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Peso de la marca principal",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Mide si el catálogo depende demasiado de una sola marca.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Uso de métodos de pago", ignoreCase = true) ||
                detail.title.contains("Métodos de pago", ignoreCase = true) -> {
            QuickReadingData(
                description = "Resume las formas de pago más usadas y su nivel de concentración.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Método más utilizado",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(maxItem?.value ?: 0.0, "", "")} pagos registrados",
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "P"
                    ),
                    QuickReadingMetric(
                        title = "Método menos utilizado",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(minItem?.value ?: 0.0, "", "")} pagos registrados",
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Concentración del método principal",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Permite saber si la operación depende demasiado de un solo método.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Búsquedas con pocos resultados", ignoreCase = true) -> {
            QuickReadingData(
                description = "Detecta palabras clave con demanda, pero con baja oferta disponible.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Búsqueda crítica principal",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(maxItem?.value ?: 0.0, "", "")} búsquedas con pocos resultados",
                        background = Color(0xFFFFE4E6),
                        accent = Color(0xFFB91C1C),
                        icon = "!"
                    ),
                    QuickReadingMetric(
                        title = "Búsqueda crítica menor",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(minItem?.value ?: 0.0, "", "")} registros",
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Peso de la demanda principal",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Indica qué tanto domina la búsqueda crítica principal.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Devoluciones por motivo", ignoreCase = true) -> {
            QuickReadingData(
                description = "Permite ubicar la causa más frecuente de devolución.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Motivo más frecuente",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(maxItem?.value ?: 0.0, "", "")} devoluciones",
                        background = Color(0xFFFFE4E6),
                        accent = Color(0xFFB91C1C),
                        icon = "D"
                    ),
                    QuickReadingMetric(
                        title = "Motivo menos frecuente",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${formatChartValue(minItem?.value ?: 0.0, "", "")} devoluciones",
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Concentración del problema",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Peso del motivo principal dentro del total de devoluciones.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }

        detail.title.contains("Productos peor calificados", ignoreCase = true) -> {
            QuickReadingData(
                description = "Identifica productos con menor satisfacción del comprador.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Producto con menor calificación",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = "${String.format("%.2f", minItem?.value ?: 0.0)}/5",
                        background = Color(0xFFFFE4E6),
                        accent = Color(0xFFB91C1C),
                        icon = "!"
                    ),
                    QuickReadingMetric(
                        title = "Producto mejor evaluado del grupo",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = "${String.format("%.2f", maxItem?.value ?: 0.0)}/5",
                        background = Color(0xFFEFFDF5),
                        accent = Color(0xFF15803D),
                        icon = "★"
                    ),
                    QuickReadingMetric(
                        title = "Riesgo de reputación",
                        value = "Alto si baja de 3.5",
                        subtitle = "Productos con baja calificación pueden reducir confianza y recompra.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "R"
                    )
                )
            )
        }

        else -> {
            QuickReadingData(
                description = "Resumen automático para identificar el elemento con mayor peso dentro del indicador.",
                metrics = listOf(
                    QuickReadingMetric(
                        title = "Mayor valor",
                        value = maxItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(maxItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFE8F0FE),
                        accent = Color(0xFF185ABC),
                        icon = "↑"
                    ),
                    QuickReadingMetric(
                        title = "Menor valor",
                        value = minItem?.label ?: "Sin dato",
                        subtitle = formatChartValue(minItem?.value ?: 0.0, detail.valuePrefix, detail.valueSuffix),
                        background = Color(0xFFF8FAFC),
                        accent = Color(0xFF475569),
                        icon = "↓"
                    ),
                    QuickReadingMetric(
                        title = "Participación principal",
                        value = "${String.format("%.1f", dominantPercent)}%",
                        subtitle = "Peso del mayor valor dentro del total analizado.",
                        background = Color(0xFFFFF8D6),
                        accent = Color(0xFF92400E),
                        icon = "%"
                    )
                )
            )
        }
    }
}

fun buildSelectedInsightData(
    detail: KpiDetail,
    selectedItem: ChartItem?,
    allItems: List<ChartItem>
): SelectedInsightData {
    val total = allItems.sumOf { it.value }
    val average = if (allItems.isNotEmpty()) total / allItems.size else 0.0
    val percentage = if (selectedItem != null && total > 0) {
        selectedItem.value * 100.0 / total
    } else {
        0.0
    }

    val percentageText = "Representa aproximadamente ${String.format("%.2f", percentage)}% del total analizado."

    if (selectedItem == null) {
        return SelectedInsightData(
            title = "Detalle seleccionado",
            labelTitle = "Elemento",
            valueTitle = "Valor",
            contextText = "Sin elemento seleccionado.",
            percentageText = "Selecciona un dato para calcular su participación.",
            analysis = "Todavía no hay información seleccionada para interpretar.",
            decision = "Toca un elemento de la gráfica para generar una lectura automática.",
            accent = Color(0xFF185ABC),
            icon = "i"
        )
    }

    return when {
        detail.title.contains("Ventas", ignoreCase = true) -> {
            val isAboveAverage = selectedItem.value >= average

            SelectedInsightData(
                title = "Detalle del periodo",
                labelTitle = "Mes seleccionado",
                valueTitle = "Ingreso registrado",
                contextText = "Periodo analizado dentro del comportamiento mensual de ventas.",
                percentageText = percentageText,
                analysis = if (isAboveAverage) {
                    "Este mes está por encima del promedio mensual analizado. Puede considerarse un periodo fuerte porque generó más ingresos que el comportamiento normal del año o periodo seleccionado."
                } else {
                    "Este mes está por debajo del promedio mensual analizado. Puede considerarse un periodo débil y requiere atención para evitar que la caída se repita."
                },
                decision = if (isAboveAverage) {
                    "Revisar qué campañas, categorías o disponibilidad de productos favorecieron este mes y replicar esa estrategia en meses con menor rendimiento."
                } else {
                    "Aplicar cupones del 10% al 15%, envío gratis en productos seleccionados y campañas de remarketing durante las dos primeras semanas de este mes."
                },
                accent = if (isAboveAverage) Color(0xFF15803D) else Color(0xFFD97706),
                icon = if (isAboveAverage) "↑" else "↓"
            )
        }

        detail.title.contains("Valor del inventario", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de inventario",
                labelTitle = "Categoría seleccionada",
                valueTitle = "Valor económico",
                contextText = "Valor calculado con precio por stock disponible.",
                percentageText = percentageText,
                analysis = "Este resultado indica cuánto capital está concentrado en la categoría seleccionada. Mientras mayor sea el valor, más importante es vigilar su rotación para evitar inventario costoso detenido.",
                decision = "Aplicar campañas de rotación, descuentos controlados del 5% al 10%, paquetes promocionales o envío gratis para productos de esta categoría.",
                accent = Color(0xFF185ABC),
                icon = "$"
            )
        }

        detail.title.contains("Stock disponible", ignoreCase = true) -> {
            val isHighStock = selectedItem.value >= average

            SelectedInsightData(
                title = "Detalle de stock",
                labelTitle = "Categoría seleccionada",
                valueTitle = "Unidades disponibles",
                contextText = "Cantidad de unidades disponibles dentro del inventario.",
                percentageText = percentageText,
                analysis = if (isHighStock) {
                    "Esta categoría tiene un stock superior al promedio. Puede ser positivo si existe demanda, pero también puede representar acumulación de inventario si la rotación es baja."
                } else {
                    "Esta categoría tiene un stock menor al promedio. Si tiene demanda alta, podría existir riesgo de falta de disponibilidad."
                },
                decision = if (isHighStock) {
                    "Revisar productos con baja rotación y activar promociones por volumen, descuentos por temporada o combos para liberar inventario."
                } else {
                    "Revisar si los productos de esta categoría tienen ventas recientes. Si existe demanda, priorizar reabastecimiento."
                },
                accent = if (isHighStock) Color(0xFFD97706) else Color(0xFF15803D),
                icon = "S"
            )
        }

        detail.title.contains("Precio promedio", ignoreCase = true) -> {
            val isHighPrice = selectedItem.value >= average

            SelectedInsightData(
                title = "Detalle de precio",
                labelTitle = "Categoría seleccionada",
                valueTitle = "Precio promedio",
                contextText = "Promedio de precios dentro de la categoría seleccionada.",
                percentageText = "Comparado contra el promedio de categorías analizadas.",
                analysis = if (isHighPrice) {
                    "Esta categoría tiene un precio promedio superior al comportamiento general. Puede requerir facilidades de pago para reducir la barrera de compra."
                } else {
                    "Esta categoría tiene un precio promedio menor al general. Puede utilizarse para campañas de atracción o compras rápidas."
                },
                decision = if (isHighPrice) {
                    "Ofrecer meses sin intereses, cupones por compra mínima y envío gratis para facilitar la conversión."
                } else {
                    "Impulsar esta categoría en campañas de volumen, promociones relámpago o recomendaciones dentro de la app."
                },
                accent = if (isHighPrice) Color(0xFF185ABC) else Color(0xFF15803D),
                icon = "$"
            )
        }

        detail.title.contains("Productos por estado", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle del estado",
                labelTitle = "Estado seleccionado",
                valueTitle = "Productos registrados",
                contextText = "Distribución del catálogo según el estado del producto.",
                percentageText = percentageText,
                analysis = "Este estado representa una parte del catálogo. Si corresponde a productos pausados, agotados o eliminados, puede afectar la disponibilidad para el comprador.",
                decision = "Revisar publicaciones en este estado. Si son agotados, priorizar reabastecimiento; si son pausados, actualizar precio, descripción, fotografías y condiciones.",
                accent = Color(0xFF185ABC),
                icon = "E"
            )
        }

        detail.title.contains("Marcas con más productos", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de marca",
                labelTitle = "Marca seleccionada",
                valueTitle = "Productos asociados",
                contextText = "Cantidad de productos publicados por la marca seleccionada.",
                percentageText = percentageText,
                analysis = "Esta marca tiene presencia dentro del catálogo. Una participación alta puede indicar fortaleza comercial, pero también dependencia si pocas marcas concentran muchos productos.",
                decision = "Mantener seguimiento a esta marca y comparar su presencia con ventas, stock y calificación. También conviene impulsar marcas con menor cobertura.",
                accent = Color(0xFF185ABC),
                icon = "M"
            )
        }

        detail.title.contains("Valor por categoría y marca", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de concentración",
                labelTitle = "Categoría y marca",
                valueTitle = "Valor acumulado",
                contextText = "Relación entre categoría, marca, precio y stock disponible.",
                percentageText = percentageText,
                analysis = "Esta combinación concentra valor económico dentro del inventario. Si el monto es alto, debe cuidarse la rotación para evitar acumulación de capital detenido.",
                decision = "Crear campañas específicas para esta combinación: paquetes, descuentos graduales, envío gratis o promociones por temporada.",
                accent = Color(0xFF185ABC),
                icon = "$"
            )
        }

        detail.title.contains("Pedidos por estado", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de pedidos",
                labelTitle = "Estado seleccionado",
                valueTitle = "Pedidos registrados",
                contextText = "Cantidad de pedidos agrupados por estado.",
                percentageText = percentageText,
                analysis = "Este estado muestra en qué parte del flujo comercial se concentran los pedidos. Una acumulación en estados pendientes o cancelados puede afectar la operación.",
                decision = "Si el estado indica pendiente o cancelado, revisar confirmación de pago, tiempos de atención y comunicación con el comprador.",
                accent = Color(0xFF185ABC),
                icon = "P"
            )
        }

        detail.title.contains("Productos por categoría", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de categoría",
                labelTitle = "Categoría seleccionada",
                valueTitle = "Productos registrados",
                contextText = "Cantidad de productos asociados a la categoría.",
                percentageText = percentageText,
                analysis = "Esta categoría concentra productos dentro del catálogo. Una cantidad alta puede representar fortaleza o saturación, dependiendo de su venta y rotación.",
                decision = "Comparar esta categoría con ventas y stock. Si hay saturación, aplicar promociones o depurar productos con baja rotación.",
                accent = Color(0xFF185ABC),
                icon = "C"
            )
        }

        detail.title.contains("Stock crítico", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de producto crítico",
                labelTitle = "Producto seleccionado",
                valueTitle = "Stock disponible",
                contextText = "Producto con baja disponibilidad en inventario.",
                percentageText = "Mientras menor sea el stock, mayor es el riesgo de perder ventas.",
                analysis = "Este producto puede quedarse sin disponibilidad pronto. Si tiene demanda o buena calificación, representa una prioridad de reabastecimiento.",
                decision = "Reabastecer este producto o pausar temporalmente campañas para evitar vender sin disponibilidad suficiente.",
                accent = Color(0xFFB91C1C),
                icon = "!"
            )
        }

        detail.title.contains("Uso de métodos de pago", ignoreCase = true) ||
                detail.title.contains("Métodos de pago", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle del método de pago",
                labelTitle = "Método seleccionado",
                valueTitle = "Pagos registrados",
                contextText = "Forma de pago utilizada por los compradores.",
                percentageText = percentageText,
                analysis = "Este método refleja una preferencia de pago. Si concentra muchos pagos, debe mantenerse estable; si tiene poca participación, puede requerir mayor visibilidad o confianza.",
                decision = "Promover métodos confiables y mostrar alternativas claras cuando un pago con tarjeta sea rechazado, como Mercado Pago o transferencia bancaria.",
                accent = Color(0xFF185ABC),
                icon = "P"
            )
        }

        detail.title.contains("Envíos por estado", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle logístico",
                labelTitle = "Estado de envío",
                valueTitle = "Envíos registrados",
                contextText = "Estado actual de los envíos dentro del proceso logístico.",
                percentageText = percentageText,
                analysis = "Este estado ayuda a entender el desempeño de la logística. Si hay concentración en estados pendientes o retrasados, puede afectar la experiencia del cliente.",
                decision = "Revisar tiempos de preparación, empresa de envío y zonas con mayor acumulación para reducir retrasos.",
                accent = Color(0xFF185ABC),
                icon = "L"
            )
        }

        detail.title.contains("Búsquedas con pocos resultados", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de demanda no atendida",
                labelTitle = "Búsqueda seleccionada",
                valueTitle = "Veces registrada",
                contextText = "Palabra clave buscada con pocos resultados disponibles.",
                percentageText = percentageText,
                analysis = "Esta búsqueda indica interés de usuarios, pero baja oferta dentro del catálogo. Puede representar una oportunidad comercial no cubierta.",
                decision = "Atraer vendedores relacionados, ampliar catálogo y crear campañas para productos asociados a esta palabra clave.",
                accent = Color(0xFFB91C1C),
                icon = "!"
            )
        }

        detail.title.contains("Devoluciones por motivo", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de devolución",
                labelTitle = "Motivo seleccionado",
                valueTitle = "Devoluciones registradas",
                contextText = "Causa reportada por los compradores.",
                percentageText = percentageText,
                analysis = "Este motivo muestra una causa de insatisfacción. Si concentra muchas devoluciones, puede afectar costos, reputación y confianza.",
                decision = "Revisar productos asociados a este motivo, mejorar fotografías, descripción, empaque, calidad y tiempos de entrega.",
                accent = Color(0xFFB91C1C),
                icon = "D"
            )
        }

        detail.title.contains("Productos peor calificados", ignoreCase = true) -> {
            SelectedInsightData(
                title = "Detalle de satisfacción",
                labelTitle = "Producto seleccionado",
                valueTitle = "Calificación promedio",
                contextText = "Evaluación recibida por el producto.",
                percentageText = "Calificación sobre una escala de 1 a 5.",
                analysis = "Una baja calificación puede reducir confianza, conversión y recompra. Este producto requiere revisión prioritaria.",
                decision = "Mejorar descripción, fotografías, calidad, empaque y atención postventa. Si el problema continúa, considerar retirar o pausar la publicación.",
                accent = Color(0xFFB91C1C),
                icon = "!"
            )
        }

        else -> {
            SelectedInsightData(
                title = "Detalle seleccionado",
                labelTitle = "Elemento seleccionado",
                valueTitle = "Valor registrado",
                contextText = "Dato seleccionado dentro del indicador.",
                percentageText = percentageText,
                analysis = "Este dato permite identificar el peso del elemento seleccionado dentro del indicador.",
                decision = "Priorizar la revisión de este elemento si representa una participación alta.",
                accent = Color(0xFF185ABC),
                icon = "i"
            )
        }
    }
}