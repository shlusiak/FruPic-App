package de.saschahlusiak.frupic.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.model.Frupic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onToggleFavourite: (Frupic) -> Unit,
    onShare: (Frupic) -> Unit,
    onDownload: (Frupic) -> Unit
) {
    val items = viewModel.frupics.collectAsStateWithLifecycle(emptyList()).value
    val pagerState = rememberPagerState(viewModel.initialPosition) { items.size }
    val current by remember(items) { derivedStateOf { items.getOrNull(pagerState.currentPage) } }
    var hudVisible by rememberSaveable { mutableStateOf(true) }

    Scaffold(
        topBar = {
            val alpha = animateFloatAsState(if (hudVisible) 1f else 0f)
            TopAppBar(
                title = { Text("#${current?.id}") },
                modifier = Modifier.alpha(alpha.value),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(
                        alpha = 0.5f
                    )
                ),
                navigationIcon = {
                    IconButton(onBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, "")
                    }
                },
                actions = {
                    current?.let { current ->
                        IconButton({onDownload(current)}) {
                            Icon(
                                painterResource(R.drawable.ic_file_download), ""
                            )
                        }

                        IconButton({ onToggleFavourite(current) }) {
                            AnimatedContent(
                                current.isStarred,
                                transitionSpec = { scaleIn().togetherWith(scaleOut()) }
                            ) { starred ->
                                if (starred) {
                                    Icon(
                                        Icons.Default.Favorite, "",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Icon(Icons.Default.FavoriteBorder, "")
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { contentPadding ->

        HorizontalPager(pagerState) { position ->
            GalleryItem(
                frupic = items[position],
                downloadManager = viewModel.downloadManager,
                hudVisible = hudVisible,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                hudVisible = !hudVisible
            }
        }

        current?.let { current ->
            Box(Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    hudVisible,
                    enter = slideIn { IntOffset(0, it.height) } + fadeIn(),
                    exit = slideOut { IntOffset(0, it.height) } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    ShareButton(contentPadding) { onShare(current) }
                }
            }
        }
    }
}