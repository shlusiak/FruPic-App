package de.saschahlusiak.frupic.upload

import java.io.File

internal data class UploadJob(
    val username: String,
    val tags: String,
    val file: File
) {
    fun delete() {
        file.delete()
    }
}