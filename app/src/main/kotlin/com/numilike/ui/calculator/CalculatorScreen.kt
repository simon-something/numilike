package com.numilike.ui.calculator

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.numilike.ui.theme.DarkLineNumbers
import com.numilike.ui.theme.DarkResultText
import com.numilike.ui.theme.LightLineNumbers
import com.numilike.ui.theme.LightResultText
import com.numilike.viewmodel.MainViewModel

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
                title = { Text("NumiLike", style = MaterialTheme.typography.bodyLarge) },
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
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, viewModel.shareText())
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
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val lineNumberColor = if (isDark) DarkLineNumbers else LightLineNumbers
        val resultColor = if (isDark) DarkResultText else LightResultText
        val dividerColor = MaterialTheme.colorScheme.outline
        val density = LocalDensity.current
        val gutterWidth = 36.dp
        val resultWidth = 140.dp
        val scrollState = rememberScrollState()

        val lineMetrics = remember(textLayoutResult, text) {
            computeLineMetrics(text, textLayoutResult)
        }

        // The BasicTextField uses decorationBox to embed line numbers and results
        // inside the same scrollable area. This way the text field owns focus and
        // scrolling, and line numbers/results are part of its decoration.
        BasicTextField(
            value = text,
            onValueChange = { viewModel.onTextChange(it) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            onTextLayout = { textLayoutResult = it },
            decorationBox = { innerTextField ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    // ── Line number gutter ──────────────────────
                    Box(modifier = Modifier.width(gutterWidth)) {
                        lineMetrics.forEachIndexed { index, metrics ->
                            val yDp = with(density) { metrics.yOffset.toDp() }
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = lineNumberColor,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = yDp)
                                    .padding(end = 6.dp),
                            )
                        }
                        // Reserve height
                        textLayoutResult?.let {
                            Spacer(Modifier.height(with(density) { it.size.height.toDp() }))
                        }
                    }

                    // ── Thin divider line ────────────────────────
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .then(
                                textLayoutResult?.let { layout ->
                                    Modifier.height(with(density) { layout.size.height.toDp() })
                                } ?: Modifier.fillMaxHeight()
                            )
                            .drawBehind {
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                    )

                    // ── Input text field ─────────────────────────
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = "Start typing...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = lineNumberColor,
                            )
                        }
                        innerTextField()
                    }

                    // ── Thin divider line ────────────────────────
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .then(
                                textLayoutResult?.let { layout ->
                                    Modifier.height(with(density) { layout.size.height.toDp() })
                                } ?: Modifier.fillMaxHeight()
                            )
                            .drawBehind {
                                drawLine(
                                    color = dividerColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                    )

                    // ── Results column ────────────────────────────
                    Box(modifier = Modifier.widthIn(min = resultWidth)) {
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
                            Spacer(Modifier.height(with(density) { it.size.height.toDp() }))
                        }
                    }
                }
            },
        )
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
                    Text("Copy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TextButton(
                        onClick = {
                            viewModel.copyResult(result.displayText) { t -> clipboardManager.setText(AnnotatedString(t)) }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy with unit: ${result.displayText}", Modifier.fillMaxWidth()) }
                    TextButton(
                        onClick = {
                            val n = result.value.amount.stripTrailingZeros().toPlainString()
                            viewModel.copyResult(n) { t -> clipboardManager.setText(AnnotatedString(t)) }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy number only: ${result.value.amount.stripTrailingZeros().toPlainString()}", Modifier.fillMaxWidth()) }
                    TextButton(
                        onClick = {
                            val f = "$line = ${result.displayText}"
                            viewModel.copyResult(f) { t -> clipboardManager.setText(AnnotatedString(t)) }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Copy full line", Modifier.fillMaxWidth()) }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun computeLineMetrics(text: String, layout: TextLayoutResult?): List<LineMetrics> {
    if (layout == null || text.isEmpty()) {
        val count = text.split("\n").size.coerceAtLeast(1)
        return List(count) { i -> LineMetrics(yOffset = i * 56f, height = 56f) }
    }

    val lines = text.split("\n")
    val metrics = mutableListOf<LineMetrics>()
    var charOffset = 0
    val maxOffset = layout.layoutInput.text.length.coerceAtLeast(1) - 1

    for (logicalLine in lines) {
        val safeOffset = charOffset.coerceIn(0, maxOffset)
        val visualLine = layout.getLineForOffset(safeOffset)
        val yTop = layout.getLineTop(visualLine)

        val endOffset = (charOffset + logicalLine.length).coerceIn(0, maxOffset)
        val lastVisualLine = layout.getLineForOffset(endOffset)
        val yBottom = layout.getLineBottom(lastVisualLine)

        metrics.add(LineMetrics(yOffset = yTop, height = (yBottom - yTop).coerceAtLeast(24f)))
        charOffset += logicalLine.length + 1
    }

    return metrics
}

private fun Color.luminance(): Float {
    return 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
