package de.saschahlusiak.frupic.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.saschahlusiak.frupic.model.Frupic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onToggleFavourite: (Frupic) -> Unit,
    onShare: (Frupic) -> Unit
) {
    val current = viewModel.currentFrupic.collectAsStateWithLifecycle().value

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#${current?.id}") },
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "")
                    }
                },
                actions = {
                    current ?: return@TopAppBar

                    IconButton({ onShare(current)}) {
                        Icon(Icons.Default.Share, "")
                    }


                    IconButton({ onToggleFavourite(current) }) {
                        AnimatedContent(current.isStarred) { starred ->
                            if (starred) {
                                Icon(
                                    Icons.Default.Favorite,
                                    "",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Icon(Icons.Default.FavoriteBorder, "")
                            }
                        }
                    }
                }

            )
        }
    ) { contentPadding ->

        if (current != null) {
            GalleryItem(
                frupic = current,
                downloadManager = viewModel.downloadManager,
                modifier = Modifier.padding(contentPadding).fillMaxSize()
            )
        }
    }
}