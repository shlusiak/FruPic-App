package de.saschahlusiak.frupic.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
                        frupic = item,
                        modifier = Modifier.animateItem()
                    ) {
                        onFrupicClick(it, item, starred)
                    }
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

@Composable
private fun StarredButton(
    starred: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onStarredChange: (Boolean) -> Unit
) {
    val colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
        checkedContentColor = MaterialTheme.colorScheme.error
    )

    Surface(
        checked = starred,
        onCheckedChange = onStarredChange,
        modifier = modifier
            .size(170.dp, 130.dp),
        shape = RoundedCornerShape(topEndPercent = 100, topStartPercent = 25),
        color = if (starred) colors.checkedContainerColor else colors.containerColor,
        contentColor = if (starred) colors.checkedContentColor else colors.contentColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(
                bottom = contentPadding.calculateBottomPadding(),
                end = 32.dp
            ),
        ) {
            Icon(
                if (starred) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                "",
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun UploadButton(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = IconButtonDefaults.filledIconButtonColors()

    Surface(
        onClick = onClick,
        modifier = modifier
            .size(170.dp, 130.dp),
        shape = RoundedCornerShape(topStartPercent = 100, topEndPercent = 25),
        color = colors.containerColor,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(
                bottom = contentPadding.calculateBottomPadding(),
                start = 32.dp
            ),
        ) {
            Icon(
                Icons.Filled.Add,
                "",
                modifier = Modifier.size(42.dp)
            )
        }
    }
}