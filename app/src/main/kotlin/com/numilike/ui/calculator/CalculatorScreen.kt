package com.numilike.ui.calculator

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.numilike.ui.theme.DarkLineNumbers
import com.numilike.ui.theme.DarkResultText
import com.numilike.ui.theme.LightLineNumbers
import com.numilike.ui.theme.LightResultText
import com.numilike.viewmodel.MainViewModel

/**
 * Per-logical-line layout info derived from [TextLayoutResult].
 * [yOffset] is the top of the first visual line for this logical line.
 * [height] is the total height of all visual lines belonging to this logical line.
 */
private data class LineMetrics(val yOffset: Float, val height: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: MainViewModel) {
    val text by viewModel.text.collectAsState()
    val results by viewModel.results.collectAsState()
    val showClearConfirmation by viewModel.showClearConfirmation.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showCopySheet by remember { mutableStateOf(false) }
    var copySheetLineIndex by remember { mutableIntStateOf(-1) }

    // Text layout measurement
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NumiLike",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.showSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = {
                                    showOverflowMenu = false
                                    val shareText = viewModel.shareText()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share"))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear all") },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.requestClear()
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val lineNumberColor = if (isDark) DarkLineNumbers else LightLineNumbers
        val resultColor = if (isDark) DarkResultText else LightResultText
        val dividerColor = MaterialTheme.colorScheme.outline
        val density = LocalDensity.current

        // Compute per-logical-line metrics from the TextLayoutResult
        val lines = text.split("\n")
        val lineMetrics: List<LineMetrics> = remember(textLayoutResult, text) {
            computeLineMetrics(text, textLayoutResult)
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            verticalAlignment = Alignment.Top,
        ) {
            // ── Line number gutter ──────────────────────────────
            Box(
                modifier = Modifier
                    .widthIn(min = 32.dp)
                    .padding(end = 4.dp),
            ) {
                lineMetrics.forEachIndexed { index, metrics ->
                    val yDp = with(density) { metrics.yOffset.toDp() }
                    val hDp = with(density) { metrics.height.toDp() }
                    Box(
                        modifier = Modifier
                            .offset(y = yDp)
                            .height(hDp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = lineNumberColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(end = 4.dp, top = 2.dp),
                        )
                    }
                }
                // Reserve height so the Box is as tall as the TextField
                textLayoutResult?.let {
                    Spacer(
                        modifier = Modifier.height(
                            with(density) { it.size.height.toDp() }
                        )
                    )
                }
            }

            // ── Input area ──────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (text.isEmpty()) {
                    Text(
                        text = "Start typing...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = lineNumberColor,
                        modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { viewModel.onTextChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    onTextLayout = { textLayoutResult = it },
                )
            }

            // ── Divider ─────────────────────────────────────────
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = dividerColor,
            )

            // ── Results column ───────────────────────────────────
            Box(
                modifier = Modifier.widthIn(min = 80.dp, max = 180.dp),
            ) {
                lineMetrics.forEachIndexed { index, metrics ->
                    val result = results.getOrNull(index) ?: return@forEachIndexed
                    val yDp = with(density) { metrics.yOffset.toDp() }
                    val hDp = with(density) { metrics.height.toDp() }

                    @OptIn(ExperimentalFoundationApi::class)
                    Box(
                        modifier = Modifier
                            .offset(y = yDp)
                            .height(hDp)
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    viewModel.copyResult(result.displayText) { t ->
                                        clipboardManager.setText(AnnotatedString(t))
                                    }
                                },
                                onLongClick = {
                                    copySheetLineIndex = index
                                    showCopySheet = true
                                },
                            ),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = result.displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = resultColor,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                }
                // Reserve height
                textLayoutResult?.let {
                    Spacer(
                        modifier = Modifier.height(
                            with(density) { it.size.height.toDp() }
                        )
                    )
                }
            }
        }
    }

    // ── Clear confirmation dialog ────────────────────────────────
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClear() },
            title = { Text("Clear all") },
            text = { Text("Are you sure you want to clear all content?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClear() }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClear() }) { Text("Cancel") }
            },
        )
    }

    // ── Copy bottom sheet ────────────────────────────────────────
    if (showCopySheet && copySheetLineIndex >= 0) {
        val result = results.getOrNull(copySheetLineIndex)
        val line = text.split("\n").getOrNull(copySheetLineIndex) ?: ""

        if (result != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showCopySheet = false
                    copySheetLineIndex = -1
                },
                sheetState = rememberModalBottomSheetState(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Text(
                        text = "Copy",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    TextButton(
                        onClick = {
                            viewModel.copyResult(result.displayText) { t ->
                                clipboardManager.setText(AnnotatedString(t))
                            }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Copy with unit: ${result.displayText}", Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val numberOnly = result.value.amount.stripTrailingZeros().toPlainString()
                            viewModel.copyResult(numberOnly) { t ->
                                clipboardManager.setText(AnnotatedString(t))
                            }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Copy number only: ${result.value.amount.stripTrailingZeros().toPlainString()}", Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val fullLine = "$line = ${result.displayText}"
                            viewModel.copyResult(fullLine) { t ->
                                clipboardManager.setText(AnnotatedString(t))
                            }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Copy full line", Modifier.fillMaxWidth())
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ── Helpers ──────────────────────────────────────────────────────

/**
 * Given the raw text and its [TextLayoutResult], compute the Y offset and height
 * for each logical line (delimited by '\n').
 *
 * When a logical line wraps to multiple visual lines, the height covers all of them.
 */
private fun computeLineMetrics(text: String, layout: TextLayoutResult?): List<LineMetrics> {
    if (layout == null || text.isEmpty()) {
        // Fallback: equal height per line before first layout pass
        val count = text.split("\n").size.coerceAtLeast(1)
        return List(count) { i -> LineMetrics(yOffset = i * 56f, height = 56f) }
    }

    val lines = text.split("\n")
    val metrics = mutableListOf<LineMetrics>()
    var charOffset = 0

    for (logicalLine in lines) {
        // Clamp to valid range
        val safeOffset = charOffset.coerceIn(0, layout.layoutInput.text.length.coerceAtLeast(1) - 1)
        val visualLine = layout.getLineForOffset(safeOffset)
        val yTop = layout.getLineTop(visualLine)

        // Find the last visual line belonging to this logical line
        val endOffset = (charOffset + logicalLine.length).coerceIn(0, layout.layoutInput.text.length.coerceAtLeast(1) - 1)
        val lastVisualLine = layout.getLineForOffset(endOffset)
        val yBottom = layout.getLineBottom(lastVisualLine)

        metrics.add(LineMetrics(yOffset = yTop, height = (yBottom - yTop).coerceAtLeast(24f)))

        charOffset += logicalLine.length + 1 // +1 for the '\n'
    }

    return metrics
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
