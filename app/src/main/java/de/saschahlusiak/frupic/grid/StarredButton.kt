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
    checked: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onClicked: (Boolean) -> Unit
) {
    val colors = IconButtonDefaults.filledTonalIconToggleButtonColors(
        checkedContentColor = MaterialTheme.colorScheme.error
    )

    Surface(
        checked = checked,
        onCheckedChange = onClicked,
        modifier = modifier,
        shape = RoundedCornerShape(topEndPercent = 10, topStartPercent = 100),
        color = if (checked) colors.checkedContainerColor else colors.containerColor,
        contentColor = if (checked) colors.checkedContentColor else colors.contentColor
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .padding(
                    bottom = contentPadding.calculateBottomPadding(),
                    start = 32.dp
                )
                .size(120.dp, 100.dp)
        ) {
            AnimatedContent(
                checked,
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