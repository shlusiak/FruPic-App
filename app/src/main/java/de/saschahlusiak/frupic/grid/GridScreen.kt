package de.saschahlusiak.frupic.grid

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import de.saschahlusiak.frupic.model.Frupic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    viewModel: GridViewModel,
    onFrupicClick: (Int, Frupic) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("FruPic") })
        }
    ) { contentPadding ->
        val lazyPagingItems = viewModel.fruPics.collectAsLazyPagingItems()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            contentPadding = contentPadding
        ) {
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id }
            ) {
                val item = lazyPagingItems[it]
                GridItem(item) { frupic ->
                    onFrupicClick(it, frupic)
                }
            }
        }
    }
}