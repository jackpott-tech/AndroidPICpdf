package com.example.androidpicpdf.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.androidpicpdf.data.ProjectRepository
import com.example.androidpicpdf.model.FrameStyle
import com.example.androidpicpdf.model.Page
import com.example.androidpicpdf.model.Photo
import com.example.androidpicpdf.model.Project
import com.example.androidpicpdf.ui.ProjectEditorViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditorScreen(
    projectId: Long,
    repository: ProjectRepository,
    viewModel: ProjectEditorViewModel,
    onNavigateBack: () -> Unit,
    onExport: () -> Unit
) {
    val context = LocalContext.current
    val projects by repository.observeProjects().collectAsStateWithLifecycle(emptyList())
    val project = remember(projects, projectId) { projects.firstOrNull { it.id == projectId } }

    LaunchedEffect(project) {
        project?.let { viewModel.loadProject(it) }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("Keine Bilder ausgewählt.") }
        } else {
            viewModel.addPhotos(context.contentResolver, uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projekt bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProjectSettingsCard(
                project = uiState.project,
                onAddPhotos = {
                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onImagesPerPageChange = { viewModel.updateSettings(imagesPerPage = it) },
                onSortOrderChange = { viewModel.updateSettings(sortAscending = it) },
                onFrameEnabledChange = { viewModel.updateSettings(frameEnabled = it) },
                onFrameStyleChange = { viewModel.updateSettings(frameStyle = it) },
                onExport = onExport
            )

            if (uiState.pages.isEmpty()) {
                Text("Noch keine Bilder hinzugefügt.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(uiState.pages, key = { it.id }) { page ->
                        PageCard(page = page, project = uiState.project, viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectSettingsCard(
    project: Project?,
    onAddPhotos: () -> Unit,
    onImagesPerPageChange: (Int) -> Unit,
    onSortOrderChange: (Boolean) -> Unit,
    onFrameEnabledChange: (Boolean) -> Unit,
    onFrameStyleChange: (FrameStyle) -> Unit,
    onExport: () -> Unit
) {
    Card {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Einstellungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onAddPhotos) { Text("Bilder hinzufügen") }
                OutlinedButton(onClick = onExport) { Text("Exportieren") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Bilder/Seite:")
                listOf(4, 5, 6).forEach { value ->
                    val isSelected = project?.imagesPerPage == value
                    OutlinedButton(onClick = { onImagesPerPageChange(value) }) {
                        Text(if (isSelected) "$value ✓" else "$value")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Sortierung:")
                OutlinedButton(onClick = { onSortOrderChange(true) }) {
                    Text(if (project?.sortAscending == true) "Alt → Neu ✓" else "Alt → Neu")
                }
                OutlinedButton(onClick = { onSortOrderChange(false) }) {
                    Text(if (project?.sortAscending == false) "Neu → Alt ✓" else "Neu → Alt")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rahmen:")
                OutlinedButton(onClick = { onFrameEnabledChange(!(project?.frameEnabled ?: true)) }) {
                    Text(if (project?.frameEnabled == true) "An ✓" else "Aus")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rahmenstärke:")
                FrameStyle.values().forEach { style ->
                    OutlinedButton(onClick = { onFrameStyleChange(style) }) {
                        Text(if (project?.frameStyle == style) "${style.label} ✓" else style.label)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageCard(
    page: Page,
    project: Project?,
    viewModel: ProjectEditorViewModel
) {
    var title by remember(page.id) { mutableStateOf(page.title) }
    var selectedPhoto by remember { mutableStateOf<Photo?>(null) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Seite ${page.pageIndex + 1}", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    viewModel.updatePageTitle(page.id, it)
                },
                label = { Text("Seitenüberschrift") },
                modifier = Modifier.fillMaxWidth()
            )

            val columns = if ((project?.imagesPerPage ?: 4) >= 5) 3 else 2
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.height(220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(page.photos) { _, photo ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEFEFEF))
                            .height(100.dp)
                            .fillMaxWidth()
                            .clickable { selectedPhoto = photo }
                    ) {
                        AsyncImage(
                            model = Uri.parse(photo.uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Text("Reihenfolge per Drag & Drop anpassen:")
            ReorderablePhotoList(
                photos = page.photos,
                onMove = { from, to ->
                    val reordered = page.photos.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                    viewModel.reorderPhotos(page.id, reordered)
                }
            )
        }
    }

    selectedPhoto?.let { photo ->
        EditPhotoDialog(
            photo = photo,
            onDismiss = { selectedPhoto = null },
            onSave = { caption ->
                viewModel.updatePhotoCaption(photo.id, caption)
                selectedPhoto = null
            },
            onRemove = {
                viewModel.removePhoto(photo.pageId, photo.id)
                selectedPhoto = null
            }
        )
    }
}

@Composable
private fun EditPhotoDialog(
    photo: Photo,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit
) {
    var caption by remember(photo.id) { mutableStateOf(photo.caption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bild bearbeiten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Bildunterschrift (optional)")
                OutlinedTextField(value = caption, onValueChange = { caption = it })
            }
        },
        confirmButton = {
            Button(onClick = { onSave(caption) }) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onRemove) { Text("Entfernen") }
        }
    )
}

@Composable
private fun ReorderablePhotoList(
    photos: List<Photo>,
    onMove: (Int, Int) -> Unit
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            val index = photos.indexOf(photo)
            DraggablePhotoRow(
                index = index,
                photo = photo,
                listState = listState,
                onDragStart = {},
                onDragEnd = {},
                onMove = onMove
            )
        }
    }
}

@Composable
private fun DraggablePhotoRow(
    index: Int,
    photo: Photo,
    listState: LazyListState,
    onDragStart: () -> Unit,
    onDragEnd: () -> Unit,
    onMove: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val threshold = 24f
                        val direction = when {
                            dragAmount.y > threshold -> 1
                            dragAmount.y < -threshold -> -1
                            else -> 0
                        }
                        if (direction != 0) {
                            val target = (index + direction).coerceIn(0, listState.layoutInfo.totalItemsCount - 1)
                            if (target != index) onMove(index, target)
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEFEFEF))
            ) {
                AsyncImage(
                    model = Uri.parse(photo.uri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Bild ${index + 1}")
                if (photo.caption.isNotBlank()) {
                    Text(photo.caption, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("☰")
        }
    }
}
