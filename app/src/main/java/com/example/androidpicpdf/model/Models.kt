package com.example.androidpicpdf.model

data class Project(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val imagesPerPage: Int,
    val sortAscending: Boolean,
    val frameEnabled: Boolean,
    val frameStyle: FrameStyle,
    val frameColorHex: String,
    val frameWidthDp: Float
)

data class Page(
    val id: Long,
    val projectId: Long,
    val title: String,
    val pageIndex: Int,
    val photos: List<Photo>
)

data class Photo(
    val id: Long,
    val pageId: Long,
    val uri: String,
    val caption: String,
    val position: Int,
    val dateTaken: Long
)

enum class FrameStyle(val label: String, val widthDp: Float) {
    THIN("Dünn", 1f),
    MEDIUM("Mittel", 1.5f),
    THICK("Dick", 2.5f)
}
