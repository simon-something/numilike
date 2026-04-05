package com.numilike.ui.calculator

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import android.content.Intent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.numilike.ui.theme.DarkResultText
import com.numilike.ui.theme.DarkLineNumbers
import com.numilike.ui.theme.LightResultText
import com.numilike.ui.theme.LightLineNumbers
import com.numilike.viewmodel.MainViewModel

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

    // Collect snackbar messages
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
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                            )
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
        val lines = text.split("\n")
        val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
        val lineNumberColor = if (isDark) DarkLineNumbers else LightLineNumbers
        val resultColor = if (isDark) DarkResultText else LightResultText
        val lineHeight = 48.dp

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
        ) {
            // Line numbers column
            Column(
                modifier = Modifier
                    .widthIn(min = 32.dp)
                    .padding(end = 4.dp),
                horizontalAlignment = Alignment.End,
            ) {
                lines.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier.height(lineHeight),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            color = lineNumberColor,
                            textAlign = TextAlign.End,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }

            // Input area
            Box(
                modifier = Modifier.weight(1f),
            ) {
                if (text.isEmpty()) {
                    Text(
                        text = "Start typing...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = lineNumberColor,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { viewModel.onTextChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }

            // Divider
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline,
            )

            // Results column
            Column(
                modifier = Modifier
                    .widthIn(min = 80.dp, max = 160.dp)
                    .padding(start = 8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                lines.forEachIndexed { index, line ->
                    val result = results.getOrNull(index)
                    Box(
                        modifier = Modifier
                            .height(lineHeight)
                            .fillMaxWidth()
                            .then(
                                if (result != null) {
                                    @OptIn(ExperimentalFoundationApi::class)
                                    Modifier.combinedClickable(
                                        onClick = {
                                            viewModel.copyResult(result.displayText) { text ->
                                                clipboardManager.setText(AnnotatedString(text))
                                            }
                                        },
                                        onLongClick = {
                                            copySheetLineIndex = index
                                            showCopySheet = true
                                        },
                                    )
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        if (result != null) {
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
                }
            }
        }
    }

    // Clear confirmation dialog
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClear() },
            title = { Text("Clear all") },
            text = { Text("Are you sure you want to clear all content?") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClear() }) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClear() }) {
                    Text("Cancel")
                }
            },
        )
    }

    // Copy bottom sheet
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
                            viewModel.copyResult(result.displayText) { text ->
                                clipboardManager.setText(AnnotatedString(text))
                            }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Copy with unit: ${result.displayText}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    TextButton(
                        onClick = {
                            val numberOnly = result.value.amount.toPlainString()
                            viewModel.copyResult(numberOnly) { text ->
                                clipboardManager.setText(AnnotatedString(text))
                            }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Copy number only: ${result.value.amount.toPlainString()}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    TextButton(
                        onClick = {
                            val fullLine = "$line = ${result.displayText}"
                            viewModel.copyResult(fullLine) { text ->
                                clipboardManager.setText(AnnotatedString(text))
                            }
                            showCopySheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Copy full line",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    val r = red
    val g = green
    val b = blue
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}
