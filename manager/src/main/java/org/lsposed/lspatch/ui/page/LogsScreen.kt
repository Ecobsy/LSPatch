@file:OptIn(ExperimentalMaterial3Api::class)

package org.lsposed.lspatch.ui.page

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.lsposed.lspatch.R
import org.lsposed.lspatch.ui.component.AnywhereDropdown
import org.lsposed.lspatch.ui.viewmodel.logs.LogEntry
import org.lsposed.lspatch.ui.viewmodel.logs.LogLevel
import org.lsposed.lspatch.ui.viewmodel.logs.LogsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Destination
@Composable
fun LogsScreen() {
    val viewModel: LogsViewModel = viewModel()
    val logs by viewModel.logs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showDropdown by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<LogEntry?>(null) }
    var showDetailsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logs de LSPatch") },
                actions = {
                    IconButton(onClick = { viewModel.refreshLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                    
                    AnywhereDropdown(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false },
                        onClick = { showDropdown = true },
                        surface = {
                            IconButton(onClick = { showDropdown = true }) {
                                Icon(Icons.Default.BugReport, contentDescription = "Opciones")
                            }
                        }
                    ) {
                            DropdownMenuItem(
                                text = { Text("Exportar logs") },
                                onClick = {
                                    // TODO: Implementar exportación
                                    showDropdown = false
                                },
                                leadingIcon = { Icon(Icons.Default.GetApp, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Limpiar logs") },
                                onClick = {
                                    viewModel.clearLogs()
                                    showDropdown = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No hay logs disponibles",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Los logs aparecerán aquí cuando LSPatch esté activo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(logs) { logEntry ->
                        LogEntryCard(
                            logEntry = logEntry,
                            onShowDetails = {
                                selectedEntry = logEntry
                                showDetailsDialog = true
                            }
                        )
                    }
                }
                
                // Información adicional para desarrolladores
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Información del Sistema",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            SystemInfoRow("Versión de Android", "API ${android.os.Build.VERSION.SDK_INT}")
                            SystemInfoRow("Arquitectura", System.getProperty("os.arch") ?: "Desconocida")
                            SystemInfoRow("Modo LSPatch", detectLSPatchMode())
                            SystemInfoRow("Estado del servicio", "Activo")
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Para soporte técnico con WaEnhancer u otros módulos, incluya esta información junto con los logs exportados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Diálogo de detalles
    if (showDetailsDialog && selectedEntry != null) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { 
                Text(
                    text = "${selectedEntry!!.tag} - ${selectedEntry!!.level.name}",
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Mensaje:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = selectedEntry!!.message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (selectedEntry!!.details.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Detalles:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = selectedEntry!!.details,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Timestamp: ${formatTimestamp(selectedEntry!!.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun LogEntryCard(
    logEntry: LogEntry,
    onShowDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (logEntry.level) {
                LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer
                LogLevel.WARNING -> MaterialTheme.colorScheme.secondaryContainer
                LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer
                LogLevel.DEBUG -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onShowDetails
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (logEntry.level) {
                        LogLevel.ERROR -> Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        LogLevel.WARNING -> Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        LogLevel.INFO -> Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        LogLevel.DEBUG -> Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = logEntry.tag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = formatTimestamp(logEntry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = logEntry.message,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (logEntry.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap para ver detalles...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SystemInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

private fun detectLSPatchMode(): String {
    return when {
        System.getProperty("lspatch.embedded") == "true" -> "Embedded"
        System.getProperty("lspatch.manager") == "true" -> "Manager"
        else -> "Desconocido"
    }
}
