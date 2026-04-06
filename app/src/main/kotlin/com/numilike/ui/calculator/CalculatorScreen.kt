package com.numilike.ui.calculator

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.numilike.ui.theme.DarkLineNumbers
import com.numilike.ui.theme.DarkResultText
import com.numilike.ui.theme.LightLineNumbers
import com.numilike.ui.theme.LightResultText
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

    // Split text into per-line state
    val lines = remember(text) { text.split("\n") }

    // Track which line should receive focus and cursor position
    var focusLineIndex by remember { mutableIntStateOf(0) }
    var focusCursorPos by remember { mutableIntStateOf(0) }
    var focusTrigger by remember { mutableIntStateOf(0) } // increment to re-trigger focus

    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Auto-scroll to focused line
    LaunchedEffect(focusLineIndex, focusTrigger) {
        if (focusLineIndex in lines.indices) {
            listState.animateScrollToItem(focusLineIndex)
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
        val minRowHeight = 44.dp

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = listState,
        ) {
            itemsIndexed(
                items = lines,
                key = { index, _ -> index },
            ) { index, lineText ->
                val result = results.getOrNull(index)
                val focusRequester = remember { FocusRequester() }

                // Request focus when this is the target line
                LaunchedEffect(focusLineIndex, focusTrigger) {
                    if (index == focusLineIndex) {
                        try { focusRequester.requestFocus() } catch (_: Exception) {}
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            // Draw bottom border as subtle line separator
                            drawLine(
                                color = dividerColor.copy(alpha = 0.15f),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 0.5.dp.toPx(),
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // ── Line number ──────────────────────────────
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        color = lineNumberColor,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .width(32.dp)
                            .padding(end = 6.dp),
                    )

                    // ── Left divider ─────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(minRowHeight)
                            .drawBehind {
                                drawLine(dividerColor, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                            },
                    )

                    // ── Line input ────────────────────────────────
                    // tfValue is the source of truth for this line's text during editing.
                    // Only reset from external state on focusTrigger (Enter/Backspace/import).
                    var tfValue by remember { mutableStateOf(TextFieldValue(lineText, TextRange(lineText.length))) }

                    LaunchedEffect(focusTrigger) {
                        val currentLine = lines.getOrElse(index) { "" }
                        val cursor = if (index == focusLineIndex) {
                            focusCursorPos.coerceIn(0, currentLine.length)
                        } else {
                            currentLine.length
                        }
                        tfValue = TextFieldValue(currentLine, TextRange(cursor))
                    }

                    BasicTextField(
                        value = tfValue,
                        onValueChange = { newValue ->
                            val newText = newValue.text
                            if (newText.contains("\n")) {
                                // User pressed Enter — split into two lines
                                val cursorPos = newValue.selection.start.coerceAtMost(newText.indexOf("\n").let { if (it < 0) newText.length else it })
                                val before = newText.substring(0, newText.indexOf("\n"))
                                val after = newText.substring(newText.indexOf("\n") + 1)
                                val newLines = lines.toMutableList()
                                newLines[index] = before
                                newLines.add(index + 1, after)
                                viewModel.onTextChange(newLines.joinToString("\n"))
                                focusLineIndex = index + 1
                                focusCursorPos = 0
                                focusTrigger++
                            } else if (newText.isEmpty() && lineText.isNotEmpty() && newValue.selection.start == 0 && tfValue.selection.start == 0) {
                                // Backspace at start of non-empty line — don't merge, just clear
                                tfValue = newValue
                                val newLines = lines.toMutableList()
                                newLines[index] = newText
                                viewModel.onTextChange(newLines.joinToString("\n"))
                            } else if (newText.isEmpty() && lineText.isEmpty() && index > 0) {
                                // Backspace on empty line — merge with previous
                                val newLines = lines.toMutableList()
                                val prevLen = newLines[index - 1].length
                                newLines.removeAt(index)
                                viewModel.onTextChange(newLines.joinToString("\n"))
                                focusLineIndex = index - 1
                                focusCursorPos = prevLen
                                focusTrigger++
                            } else {
                                tfValue = newValue
                                val newLines = lines.toMutableList()
                                newLines[index] = newText
                                viewModel.onTextChange(newLines.joinToString("\n"))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .focusRequester(focusRequester),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                // Enter/Next key — create new line below
                                val cursorPos = tfValue.selection.start
                                val before = tfValue.text.substring(0, cursorPos)
                                val after = tfValue.text.substring(cursorPos)
                                val newLines = lines.toMutableList()
                                newLines[index] = before
                                newLines.add(index + 1, after)
                                viewModel.onTextChange(newLines.joinToString("\n"))
                                focusLineIndex = index + 1
                                focusCursorPos = 0
                                focusTrigger++
                            },
                        ),
                    )

                    // ── Right divider ─────────────────────────────
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(minRowHeight)
                            .drawBehind {
                                drawLine(dividerColor, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
                            },
                    )

                    // ── Result ─────────────────────────────────────
                    @OptIn(ExperimentalFoundationApi::class)
                    Box(
                        modifier = Modifier
                            .widthIn(min = 100.dp)
                            .height(minRowHeight)
                            .then(
                                if (result != null) {
                                    Modifier.combinedClickable(
                                        onClick = {
                                            viewModel.copyResult(result.displayText) { t ->
                                                clipboardManager.setText(AnnotatedString(t))
                                            }
                                        },
                                        onLongClick = {
                                            copySheetLineIndex = index
                                            showCopySheet = true
                                        },
                                    )
                                } else Modifier
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

            // Empty tap area at the bottom to create new lines
            item {
                @OptIn(ExperimentalFoundationApi::class)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .combinedClickable(
                            onClick = {
                                // Add a new empty line if last line isn't empty
                                if (lines.lastOrNull()?.isNotEmpty() == true) {
                                    viewModel.onTextChange(text + "\n")
                                }
                                focusLineIndex = lines.size.coerceAtLeast(1) - 1
                                focusCursorPos = 0
                                focusTrigger++
                            },
                            onLongClick = {},
                        ),
                )
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────
    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClear() },
            title = { Text("Clear all") },
            text = { Text("Are you sure you want to clear all content?") },
            confirmButton = { TextButton(onClick = { viewModel.confirmClear() }) { Text("Clear") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissClear() }) { Text("Cancel") } },
        )
    }

    if (showCopySheet && copySheetLineIndex >= 0) {
        val result = results.getOrNull(copySheetLineIndex)
        val line = lines.getOrNull(copySheetLineIndex) ?: ""
        if (result != null) {
            ModalBottomSheet(
                onDismissRequest = { showCopySheet = false; copySheetLineIndex = -1 },
                sheetState = rememberModalBottomSheetState(),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Copy", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                    TextButton(onClick = {
                        viewModel.copyResult(result.displayText) { t -> clipboardManager.setText(AnnotatedString(t)) }
                        showCopySheet = false
                    }, Modifier.fillMaxWidth()) { Text("Copy with unit: ${result.displayText}", Modifier.fillMaxWidth()) }
                    TextButton(onClick = {
                        val n = result.value.amount.stripTrailingZeros().toPlainString()
                        viewModel.copyResult(n) { t -> clipboardManager.setText(AnnotatedString(t)) }
                        showCopySheet = false
                    }, Modifier.fillMaxWidth()) { Text("Copy number only", Modifier.fillMaxWidth()) }
                    TextButton(onClick = {
                        viewModel.copyResult("$line = ${result.displayText}") { t -> clipboardManager.setText(AnnotatedString(t)) }
                        showCopySheet = false
                    }, Modifier.fillMaxWidth()) { Text("Copy full line", Modifier.fillMaxWidth()) }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
