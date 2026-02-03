package com.example.androidpicpdf.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val imagesPerPage: Int = 4,
    val sortAscending: Boolean = true,
    val frameEnabled: Boolean = true,
    val frameStyle: String = "Mittel",
    val frameColorHex: String = "#000000",
    val frameWidthDp: Float = 1.5f
)

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]) ]
)
data class PageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val title: String,
    val pageIndex: Int
)

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["pageId"]) ]
)
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pageId: Long,
    val uri: String,
    val caption: String,
    val position: Int,
    val dateTaken: Long
)
