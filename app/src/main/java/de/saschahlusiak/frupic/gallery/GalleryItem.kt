package de.saschahlusiak.frupic.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.saschahlusiak.frupic.R
import de.saschahlusiak.frupic.app.FrupicDownloadManager
import de.saschahlusiak.frupic.app.JobStatus
import de.saschahlusiak.frupic.model.Frupic
import me.saket.telephoto.zoomable.ZoomSpec
import me.saket.telephoto.zoomable.coil.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState
import java.io.File
import java.util.Locale

@Composable
fun GalleryItem(
    frupic: Frupic,
    downloadManager: FrupicDownloadManager,
    hudVisible: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier,
    onToggleHud: () -> Unit
) {
    val job = remember(frupic.fullUrl) { downloadManager.getJob(frupic) }
    val status = job.status.collectAsStateWithLifecycle().value

    DisposableEffect(frupic.fullUrl) {
        onDispose {
            downloadManager.cancel(frupic)
        }
    }

    Box(
        modifier.padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            JobStatus.Scheduled -> Progress(null)

            is JobStatus.InProgress -> Progress(status)

            JobStatus.Cancelled -> {
                Text("Cancelled")
            }

            JobStatus.Failed -> {
                Text("Failed")
            }

            is JobStatus.Success -> {
                ImageView(status.file, onToggleHud)


            }
        }
    }
}

@Composable
private fun ImageView(file: File, onToggleHud: () -> Unit) {
    val state = rememberZoomableImageState(rememberZoomableState(ZoomSpec(maxZoomFactor = 4f)))
    ZoomableAsyncImage(
        modifier = Modifier
            .fillMaxSize(),
        model = file,
        state = state,
        contentDescription = "",
        clipToBounds = false,
        onClick = { onToggleHud() }
    )

    if (!state.isImageDisplayed) {
        CircularProgressIndicator()
    }
}

@Composable
private fun Progress(status: JobStatus.InProgress?) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = spacedBy(8.dp)
    ) {
        if (status == null) {
            // indeterminate
            LinearProgressIndicator(Modifier.width(300.dp))

            Text(
                stringResource(R.string.waiting_to_start),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary
            )
            return
        }

        val animatedProgress by animateFloatAsState(
            targetValue = status.progress.toFloat() / status.max.coerceAtLeast(1),
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.width(300.dp)
        )

        val text = String.format(
            Locale.getDefault(),
            "%dkb / %dkb (%d%%)",
            status.progress / 1024,
            status.max / 1024,
            if (status.max > 0) status.progress * 100 / status.max else 0
        )

        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}