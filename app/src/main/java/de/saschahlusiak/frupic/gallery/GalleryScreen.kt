package de.saschahlusiak.frupic.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
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
    var hudVisible by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            val alpha = animateFloatAsState(if (hudVisible) 1f else 0f)
            TopAppBar(
                title = { Text("#${current?.id}") },
                modifier = Modifier.alpha(alpha.value),
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "")
                    }
                },
                actions = {
                    current ?: return@TopAppBar

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
                hudVisible = hudVisible,
                modifier = Modifier
                    .padding(contentPadding)
                    .fillMaxSize()
            ) {
                hudVisible = !hudVisible
            }

            Box(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    hudVisible,
                    enter = slideIn { IntOffset(0, it.height) },
                    exit = slideOut { IntOffset(0, it.height) },
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    ShareButton(contentPadding) { onShare(current) }
                }
            }
        }
    }
}