package de.saschahlusiak.frupic.grid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.model.Frupic

@Composable
fun GridItem(
    frupic: Frupic,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier.padding(4.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(frupic.thumbUrl)
                .placeholder(R.drawable.frupic)
                .error(R.drawable.broken_frupic)
                .crossfade(200)
                .build(),
            contentDescription = "",
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .size(88.dp, 70.dp)
                .clickable(onClick = onClick),
            contentScale = ContentScale.Crop
        )
    }
}