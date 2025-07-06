package de.saschahlusiak.frupic.grid

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.model.Frupic
import de.saschahlusiak.frupic.utils.AppTheme

@Composable
fun GridItem(
    data: Any,
    isStarred: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier = modifier.padding(4.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(data)
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

        if (isStarred) {
            Icon(
                Icons.Filled.Favorite, "",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(5.dp, -7.dp)
                    .shadow(4.dp, shape = CircleShape)
            )
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview() {
    AppTheme {
        GridItem(R.drawable.frupic, true) { }
    }
}