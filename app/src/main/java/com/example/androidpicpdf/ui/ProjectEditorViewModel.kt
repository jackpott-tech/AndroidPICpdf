package com.example.androidpicpdf.ui

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.androidpicpdf.data.ProjectRepository
import com.example.androidpicpdf.model.FrameStyle
import com.example.androidpicpdf.model.Page
import com.example.androidpicpdf.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectEditorViewModel(
    private val repository: ProjectRepository,
    private val projectId: Long
) : ViewModel() {
    private val projectState = MutableStateFlow<Project?>(null)

    val pages: StateFlow<List<Page>> = repository.observeProjectPages(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<ProjectEditorState> = combine(projectState, pages) { project, pageList ->
        ProjectEditorState(project = project, pages = pageList)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectEditorState())

    fun loadProject(project: Project) {
        projectState.value = project
    }

    fun addPhotos(contentResolver: ContentResolver, uris: List<Uri>) {
        viewModelScope.launch {
            repository.addPhotosToProject(contentResolver, projectId, uris)
        }
    }

    fun updatePageTitle(pageId: Long, title: String) {
        viewModelScope.launch { repository.updatePageTitle(pageId, title) }
    }

    fun updatePhotoCaption(photoId: Long, caption: String) {
        viewModelScope.launch { repository.updatePhotoCaption(photoId, caption) }
    }

    fun removePhoto(pageId: Long, photoId: Long) {
        viewModelScope.launch { repository.removePhoto(pageId, photoId) }
    }

    fun reorderPhotos(pageId: Long, ordered: List<com.example.androidpicpdf.model.Photo>) {
        viewModelScope.launch { repository.reorderPhotos(pageId, ordered) }
    }

    fun updateSettings(
        imagesPerPage: Int? = null,
        sortAscending: Boolean? = null,
        frameEnabled: Boolean? = null,
        frameStyle: FrameStyle? = null
    ) {
        val current = projectState.value ?: return
        val updated = current.copy(
            imagesPerPage = imagesPerPage ?: current.imagesPerPage,
            sortAscending = sortAscending ?: current.sortAscending,
            frameEnabled = frameEnabled ?: current.frameEnabled,
            frameStyle = frameStyle ?: current.frameStyle,
            frameWidthDp = (frameStyle ?: current.frameStyle).widthDp
        )
        projectState.value = updated
        viewModelScope.launch {
            repository.updateProjectSettings(updated)
            if (imagesPerPage != null && imagesPerPage != current.imagesPerPage) {
                repository.reflowPages(projectId, imagesPerPage)
            }
        }
    }
}

data class ProjectEditorState(
    val project: Project? = null,
    val pages: List<Page> = emptyList()
)
