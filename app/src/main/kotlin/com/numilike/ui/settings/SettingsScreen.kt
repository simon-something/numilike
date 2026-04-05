package com.numilike.ui.settings

import com.numilike.BuildConfig
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.numilike.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    val decimalPlaces by viewModel.decimalPlaces.collectAsState()
    val useThousandsSep by viewModel.useThousandsSep.collectAsState()
    val showDslEditor by viewModel.showDslEditor.collectAsState()

    if (showDslEditor) {
        DslEditorScreen(viewModel = viewModel, onBack = { viewModel.hideDslEditor() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Theme section
            Text(
                "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp),
            )
            val themeOptions = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
            themeOptions.forEach { (value, label) ->
                ListItem(
                    headlineContent = { Text(label) },
                    trailingContent = {
                        RadioButton(
                            selected = themeMode == value,
                            onClick = { viewModel.setTheme(value) },
                        )
                    },
                    modifier = Modifier.clickable { viewModel.setTheme(value) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Number format section
            Text(
                "Number Format",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
            )
            ListItem(
                headlineContent = { Text("Thousands separator") },
                trailingContent = {
                    Switch(
                        checked = useThousandsSep,
                        onCheckedChange = { viewModel.setUseThousandsSep(it) },
                    )
                },
            )

            var decExpanded by remember { mutableStateOf(false) }
            val decLabel = if (decimalPlaces < 0) "Auto" else "$decimalPlaces"
            ListItem(
                headlineContent = { Text("Decimal places") },
                trailingContent = {
                    TextButton(onClick = { decExpanded = true }) { Text(decLabel) }
                    DropdownMenu(
                        expanded = decExpanded,
                        onDismissRequest = { decExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Auto") },
                            onClick = { viewModel.setDecimalPlaces(-1); decExpanded = false },
                        )
                        (0..10).forEach { n ->
                            DropdownMenuItem(
                                text = { Text("$n") },
                                onClick = { viewModel.setDecimalPlaces(n); decExpanded = false },
                            )
                        }
                    }
                },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Custom definitions
            ListItem(
                headlineContent = { Text("Custom Definitions") },
                supportingContent = { Text("Define custom units, functions, and constants") },
                modifier = Modifier.clickable { viewModel.showDslEditor() },
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About
            Text(
                "About",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 8.dp),
            )
            ListItem(
                headlineContent = { Text("NumiLike v${BuildConfig.VERSION_NAME}") },
                supportingContent = { Text("A Numi-inspired natural language calculator") },
            )
        }
    }
}
