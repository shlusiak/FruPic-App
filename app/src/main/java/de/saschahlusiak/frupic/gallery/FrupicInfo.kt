package de.saschahlusiak.frupic.gallery

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.saschahlusiak.frupic.model.Frupic
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun FrupicInfo(frupic: Frupic, modifier: Modifier = Modifier) {
    val transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = { fadeIn().togetherWith(fadeOut()) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier.padding(16.dp)
    ) {
        AnimatedContent(frupic.username, transitionSpec = transitionSpec) { username ->
            if (username?.isNotBlank() == true) {
                Surface(
                    shape = AssistChipDefaults.shape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Box(Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            username,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        AnimatedContent(frupic.tags, transitionSpec = transitionSpec) { tags ->
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 150.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = AssistChipDefaults.shape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Box(Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        val formatter = remember {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        }
        val formatted = frupic.dateTime.format(formatter)

        AnimatedContent(formatted, transitionSpec = transitionSpec) { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}