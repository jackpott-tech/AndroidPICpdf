package com.example.androidpicpdf.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Delete
    suspend fun deleteProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<PhotoEntity>)

    @Query("SELECT * FROM pages WHERE projectId = :projectId ORDER BY pageIndex ASC")
    fun observePages(projectId: Long): Flow<List<PageEntity>>

    @Query("SELECT * FROM photos WHERE pageId IN (:pageIds) ORDER BY pageId ASC, position ASC")
    fun observePhotos(pageIds: List<Long>): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProject(projectId: Long): ProjectEntity?

    @Query("SELECT * FROM pages WHERE projectId = :projectId ORDER BY pageIndex ASC")
    suspend fun getPages(projectId: Long): List<PageEntity>

    @Query("SELECT * FROM photos WHERE pageId = :pageId ORDER BY position ASC")
    suspend fun getPhotos(pageId: Long): List<PhotoEntity>

    @Query("DELETE FROM pages WHERE projectId = :projectId")
    suspend fun deletePagesForProject(projectId: Long)

    @Query("DELETE FROM photos WHERE pageId IN (:pageIds)")
    suspend fun deletePhotosForPages(pageIds: List<Long>)

    @Query("UPDATE photos SET caption = :caption WHERE id = :photoId")
    suspend fun updatePhotoCaption(photoId: Long, caption: String)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deletePhoto(photoId: Long)

    @Query("UPDATE pages SET title = :title WHERE id = :pageId")
    suspend fun updatePageTitle(pageId: Long, title: String)

    @Transaction
    suspend fun replaceProjectContent(
        projectId: Long,
        pages: List<PageEntity>,
        photos: List<PhotoEntity>
    ) {
        val existingPages = getPages(projectId)
        if (existingPages.isNotEmpty()) {
            deletePhotosForPages(existingPages.map { it.id })
            deletePagesForProject(projectId)
        }
        insertPages(pages)
        insertPhotos(photos)
    }
}
