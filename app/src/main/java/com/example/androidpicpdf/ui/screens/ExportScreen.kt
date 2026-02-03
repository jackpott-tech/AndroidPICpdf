package com.example.androidpicpdf.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.androidpicpdf.data.ProjectRepository
import com.example.androidpicpdf.util.PdfExporter
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    projectId: Long,
    repository: ProjectRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val projects by repository.observeProjects().collectAsStateWithLifecycle(emptyList())
    val project = projects.firstOrNull { it.id == projectId }
    val pages by repository.observeProjectPages(projectId).collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    var exportFile by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export & Teilen") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("←") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PDF-Export in A4 mit hoher Druckqualität (300 DPI).")
            Button(
                onClick = {
                    scope.launch {
                        runCatching {
                            PdfExporter.exportProject(
                                context = context,
                                pages = pages,
                                frameEnabled = project?.frameEnabled ?: true,
                                frameStyle = project?.frameStyle ?: com.example.androidpicpdf.model.FrameStyle.MEDIUM,
                                frameColorHex = project?.frameColorHex ?: "#000000"
                            )
                        }.onSuccess {
                            exportFile = it
                            error = null
                        }.onFailure {
                            error = it.localizedMessage ?: "Export fehlgeschlagen."
                        }
                    }
                },
                enabled = pages.isNotEmpty()
            ) {
                Text("PDF exportieren")
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            exportFile?.let { file ->
                Spacer(modifier = Modifier.height(12.dp))
                Text("Export bereit: ${file.name}")
                RowButtons(file = file, context = context)
            }
        }
    }
}

@Composable
private fun RowButtons(file: File, context: Context) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { shareFile(context, file) }, modifier = Modifier.fillMaxWidth()) {
            Text("Teilen (E-Mail, Apps)")
        }
        Button(onClick = { openFile(context, file) }, modifier = Modifier.fillMaxWidth()) {
            Text("Öffnen mit…")
        }
    }
}

private fun shareFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "PDF teilen"))
}

private fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "PDF öffnen"))
}
