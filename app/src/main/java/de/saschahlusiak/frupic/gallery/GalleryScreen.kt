package de.saschahlusiak.frupic.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.grid.StarredButton
import de.saschahlusiak.frupic.model.Frupic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    onToggleFavourite: (Frupic) -> Unit,
    onShare: (Frupic) -> Unit,
    onDownload: (Frupic) -> Unit,
    onDetails: (Frupic) -> Unit,
    onOpenInBrowser: (Frupic) -> Unit
) {
    val items = viewModel.frupics.collectAsStateWithLifecycle(emptyList()).value
    val pagerState = rememberPagerState(viewModel.initialPosition) { items.size }
    val current by remember(items) { derivedStateOf { items.getOrNull(pagerState.currentPage) } }
    var hudVisible by rememberSaveable { mutableStateOf(true) }

    val backgroundColor by animateColorAsState(
        if (hudVisible) MaterialTheme.colorScheme.background else Color.Black
    )

    Scaffold(
        containerColor = backgroundColor,
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
                        IconButton({ onDownload(current) }) {
                            Icon(painterResource(R.drawable.ic_file_download), "")
                        }
                        IconButton({ onOpenInBrowser(current) }) {
                            Icon(painterResource(R.drawable.ic_launch), "")
                        }
                        IconButton({ onDetails(current) }) {
                            Icon(Icons.Outlined.Info, "")
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
                    enter = slideIn { IntOffset(it.width, 0) } + fadeIn(),
                    exit = slideOut { IntOffset(it.width, 0) } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(contentPadding)
                        .padding(bottom = 110.dp, end = 10.dp)
                ) {
                    FloatingActionButton(
                        onClick = { onShare(current) },
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.Share, "",)
                    }
                }

                AnimatedVisibility(
                    hudVisible,
                    enter = slideIn { IntOffset(0, it.height) } + fadeIn(),
                    exit = slideOut { IntOffset(0, it.height) } + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    StarredButton(
                        checked = current.isStarred,
                        contentPadding = contentPadding,
                    ) {
                        onToggleFavourite(current)
                    }
                }
            }
        }
    }
}