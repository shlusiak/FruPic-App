package de.saschahlusiak.frupic.grid

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.saschahlusiak.frupic.utils.AppTheme

@Composable
fun StarredButton(
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
        modifier = modifier,
        shape = RoundedCornerShape(topEndPercent = 100, topStartPercent = 20),
        color = if (starred) colors.checkedContainerColor else colors.containerColor,
        contentColor = if (starred) colors.checkedContentColor else colors.contentColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(
                    bottom = contentPadding.calculateBottomPadding(),
                    end = 32.dp
                )
                .size(120.dp, 100.dp)
        ) {
            AnimatedContent(
                starred,
                transitionSpec = { scaleIn().togetherWith(scaleOut()) }
            ) { starred ->
                Icon(
                    if (starred) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    "",
                    modifier = Modifier.size(42.dp)
                )
            }
        }
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview() {
    AppTheme {
        StarredButton(true, PaddingValues()) {}
    }
}