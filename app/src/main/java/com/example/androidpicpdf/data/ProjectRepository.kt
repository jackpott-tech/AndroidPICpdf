package com.example.androidpicpdf.data

import android.content.ContentResolver
import android.net.Uri
import com.example.androidpicpdf.model.FrameStyle
import com.example.androidpicpdf.model.Page
import com.example.androidpicpdf.model.Photo
import com.example.androidpicpdf.model.Project
import com.example.androidpicpdf.util.MediaUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProjectRepository(
    private val dao: ProjectDao
) {
    fun observeProjects(): Flow<List<Project>> = dao.observeProjects().map { entities ->
        entities.map { it.toModel() }
    }

    fun observeProjectPages(projectId: Long): Flow<List<Page>> {
        val pagesFlow = dao.observePages(projectId)
        return pagesFlow.combine(pagesFlow.flatMapPhotos(dao)) { pages, photos ->
            pages.map { page ->
                val pagePhotos = photos.filter { it.pageId == page.id }.sortedBy { it.position }
                page.toModel(pagePhotos)
            }
        }
    }

    suspend fun createProject(name: String): Long {
        val now = System.currentTimeMillis()
        return dao.insertProject(
            ProjectEntity(
                name = name,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun updateProject(project: Project) {
        dao.updateProject(project.toEntity())
    }

    suspend fun deleteProject(project: Project) {
        dao.deleteProject(project.toEntity())
    }

    suspend fun addPhotosToProject(
        contentResolver: ContentResolver,
        projectId: Long,
        uris: List<Uri>
    ) {
        val project = dao.getProject(projectId) ?: return
        val existingPages = dao.getPages(projectId)
        val existingPhotos = existingPages.sortedBy { it.pageIndex }.flatMap { page ->
            dao.getPhotos(page.id).sortedBy { it.position }
        }
        val resolved = uris.map { uri ->
            contentResolver.takePersistableUriPermissionSafe(uri)
            val dateTaken = MediaUtils.resolveDateTaken(contentResolver, uri)
            PhotoInput(uri.toString(), dateTaken)
        }
        val combined = existingPhotos.map { PhotoInput(it.uri, it.dateTaken, it.position) } + resolved
        val sorted = if (project.sortAscending) combined.sortedBy { it.dateTaken } else combined.sortedByDescending { it.dateTaken }
        val chunked = sorted.chunked(project.imagesPerPage)
        val pages = chunked.mapIndexed { index, photos ->
            PageEntity(
                projectId = projectId,
                title = "Seite ${index + 1}",
                pageIndex = index
            )
        }
        if (existingPages.isNotEmpty()) {
            dao.deletePhotosForPages(existingPages.map { it.id })
            dao.deletePagesForProject(projectId)
        }
        val pageIds = dao.insertPages(pages)
        val photoEntities = pageIds.flatMapIndexed { pageIndex, pageId ->
            val photos = chunked[pageIndex]
            photos.mapIndexed { position, input ->
                PhotoEntity(
                    pageId = pageId,
                    uri = input.uri,
                    caption = existingPhotos.firstOrNull { it.uri == input.uri }?.caption ?: "",
                    position = position,
                    dateTaken = input.dateTaken
                )
            }
        }
        dao.insertPhotos(photoEntities)
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()).toEntity())
    }

    suspend fun updatePageTitle(pageId: Long, title: String) {
        dao.updatePageTitle(pageId, title)
    }

    suspend fun updatePhotoCaption(photoId: Long, caption: String) {
        dao.updatePhotoCaption(photoId, caption)
    }

    suspend fun removePhoto(pageId: Long, photoId: Long) {
        dao.deletePhoto(photoId)
        val remaining = dao.getPhotos(pageId).sortedBy { it.position }
        val reordered = remaining.mapIndexed { index, photo ->
            photo.copy(position = index)
        }
        dao.insertPhotos(reordered)
    }

    suspend fun reorderPhotos(pageId: Long, ordered: List<Photo>) {
        val page = dao.getPhotos(pageId)
        if (page.isEmpty()) return
        val updates = ordered.mapIndexed { index, photo ->
            PhotoEntity(
                id = photo.id,
                pageId = photo.pageId,
                uri = photo.uri,
                caption = photo.caption,
                position = index,
                dateTaken = photo.dateTaken
            )
        }
        dao.insertPhotos(updates)
    }

    suspend fun updateProjectSettings(project: Project) {
        dao.updateProject(project.toEntity())
    }

    suspend fun reflowPages(projectId: Long, imagesPerPage: Int) {
        val pages = dao.getPages(projectId)
        if (pages.isEmpty()) return
        val allPhotos = pages.sortedBy { it.pageIndex }.flatMap { page ->
            dao.getPhotos(page.id).sortedBy { it.position }
        }
        val chunked = allPhotos.chunked(imagesPerPage)
        val newPages = chunked.mapIndexed { index, _ ->
            PageEntity(projectId = projectId, title = "Seite ${index + 1}", pageIndex = index)
        }
        dao.deletePhotosForPages(pages.map { it.id })
        dao.deletePagesForProject(projectId)
        val pageIds = dao.insertPages(newPages)
        val newPhotos = pageIds.flatMapIndexed { pageIndex, pageId ->
            chunked[pageIndex].mapIndexed { position, photo ->
                photo.copy(id = 0, pageId = pageId, position = position)
            }
        }
        dao.insertPhotos(newPhotos)
    }

    private data class PhotoInput(val uri: String, val dateTaken: Long, val position: Int = 0)

    private fun ProjectEntity.toModel() = Project(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        imagesPerPage = imagesPerPage,
        sortAscending = sortAscending,
        frameEnabled = frameEnabled,
        frameStyle = FrameStyle.values().firstOrNull { it.label == frameStyle } ?: FrameStyle.MEDIUM,
        frameColorHex = frameColorHex,
        frameWidthDp = frameWidthDp
    )

    private fun Project.toEntity() = ProjectEntity(
        id = id,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt,
        imagesPerPage = imagesPerPage,
        sortAscending = sortAscending,
        frameEnabled = frameEnabled,
        frameStyle = frameStyle.label,
        frameColorHex = frameColorHex,
        frameWidthDp = frameWidthDp
    )

    private fun PageEntity.toModel(photos: List<PhotoEntity>) = Page(
        id = id,
        projectId = projectId,
        title = title,
        pageIndex = pageIndex,
        photos = photos.map { it.toModel() }
    )

    private fun PhotoEntity.toModel() = Photo(
        id = id,
        pageId = pageId,
        uri = uri,
        caption = caption,
        position = position,
        dateTaken = dateTaken
    )

    private fun Flow<List<PageEntity>>.flatMapPhotos(dao: ProjectDao): Flow<List<PhotoEntity>> {
        return this.map { pages -> pages.map { it.id } }
            .map { ids -> if (ids.isEmpty()) kotlinx.coroutines.flow.flowOf(emptyList()) else dao.observePhotos(ids) }
            .flatMapLatestSafe()
    }
}

private fun ContentResolver.takePersistableUriPermissionSafe(uri: Uri) {
    runCatching {
        takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun <T> Flow<Flow<T>>.flatMapLatestSafe(): Flow<T> = kotlinx.coroutines.flow.flatMapLatest { it }
