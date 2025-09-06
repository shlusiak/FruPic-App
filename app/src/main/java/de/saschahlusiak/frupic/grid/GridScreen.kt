package de.saschahlusiak.frupic.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.model.cloudfront

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
                        Icon(Icons.Default.Settings, "Settings")
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
                viewModel.needsMoreData(offset = items.size)
            }
        }

        PullToRefreshBox(
            viewModel.synchronizing.collectAsStateWithLifecycle().value,
            modifier = Modifier
                .padding(top = contentPadding.calculateTopPadding())
                .fillMaxSize(),
            onRefresh = { viewModel.synchronize() }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 88.dp),
                state = gridState,
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding(),
                    start = contentPadding.calculateStartPadding(LayoutDirection.Ltr),
                    end = contentPadding.calculateEndPadding(LayoutDirection.Ltr)
                ),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = items.count(),
                    key = { items[it].id }
                ) {
                    val item = items[it]

                    GridItem(
                        data = item.thumbUrl.cloudfront,
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
                        stringResource(R.string.no_favourites),
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
                checked = starred,
                contentPadding = contentPadding,
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                viewModel.toggleShowStarred()
            }

            FloatingActionButton(
                onClick = onUpload,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(contentPadding)
                    .padding(bottom = 110.dp, end = 10.dp),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    ""
                )
            }
        }
    }
}

@Composable
private fun FilterMenu(
    current: String?,
    all: List<Pair<String?, Int>>,
    modifier: Modifier,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        ElevatedFilterChip(
            selected = current != null,
            onClick = { expanded = !expanded },
            colors = FilterChipDefaults.elevatedFilterChipColors(),
            label = {
                Text(current ?: stringResource(R.string.all))
                Icon(Icons.Default.KeyboardArrowUp, "")
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            all.forEach { (label, count) ->
                DropdownMenuItem(
                    text = { Text(label ?: stringResource(R.string.all)) },
                    onClick = {
                        onSelect(label)
                        expanded = false
                    }
                )
            }
        }
    }
}