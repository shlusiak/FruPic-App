package de.saschahlusiak.frupic.grid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        val items = viewModel.fruPics.collectAsStateWithLifecycle(emptyList()).value

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            contentPadding = contentPadding
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
                    onFrupicClick(it, item)
                }
            }
        }


        Box(modifier = Modifier.fillMaxSize()) {
            val starred = viewModel.starred.collectAsStateWithLifecycle()

            StarredButton(
                starred.value, contentPadding,
                Modifier.align(Alignment.BottomStart)
            ) {
                viewModel.toggleShowStarred()
            }
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
    val colors = IconButtonDefaults.filledTonalIconToggleButtonColors()

    Surface(
        checked = starred,
        onCheckedChange = onStarredChange,
        modifier = modifier
            .size(180.dp, 130.dp),
        shape = RoundedCornerShape(topEndPercent = 100, topStartPercent = 20),
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
                modifier = Modifier.size(48.dp)
            )
        }
    }
}