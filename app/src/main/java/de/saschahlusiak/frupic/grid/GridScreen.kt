package de.saschahlusiak.frupic.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.saschahlusiak.frupic.model.Frupic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    viewModel: GridViewModel,
    onFrupicClick: (Int, Frupic, Boolean) -> Unit,
    onUpload: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FruPic") },
                actions = {
                    IconButton(onSettings) {
                        Icon(Icons.Default.Settings, "")
                    }
                }
            )
        }
    ) { contentPadding ->
        val items = viewModel.fruPics.collectAsStateWithLifecycle(emptyList()).value
        val gridState = rememberLazyGridState()
        val starred by viewModel.starred.collectAsStateWithLifecycle()
        val needsMoreData by remember(items.size) {
            derivedStateOf { gridState.firstVisibleItemIndex > items.size - 200 && !starred }
        }

        LaunchedEffect(starred) {
            if (!starred) {
                gridState.animateScrollToItem(0)
            }
        }

        LaunchedEffect(needsMoreData, items.size) {
            if (needsMoreData) {
                viewModel.needsMoreData(items.size)
            }
        }

        PullToRefreshBox(
            viewModel.synchronizing.collectAsStateWithLifecycle().value,
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            onRefresh = { viewModel.synchronize() }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                state = gridState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = items.count(),
                    key = { items[it].id }
                ) {
                    val item = items[it]

                    GridItem(
                        data = item.thumbUrl,
                        isStarred = item.isStarred,
                        modifier = Modifier.animateItem()
                    ) {
                        onFrupicClick(it, item, starred)
                    }
                }
            }

            if (items.isEmpty()) {
                if (starred) {
                    EmptyState(
                        "You don’t have any favourites yet",
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            StarredButton(
                starred = starred,
                contentPadding = contentPadding,
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                viewModel.toggleShowStarred()
            }

            UploadButton(
                contentPadding = contentPadding,
                modifier = Modifier.align(Alignment.BottomEnd),
                onClick = onUpload
            )
        }
    }
}

