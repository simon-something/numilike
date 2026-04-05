package com.numilike.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numilike.core.dsl.DslDefinition
import com.numilike.core.dsl.DslError
import com.numilike.core.dsl.DslParser
import com.numilike.core.eval.EvalResult
import com.numilike.core.eval.Evaluator
import com.numilike.core.eval.ResultFormatter
import com.numilike.core.units.ConversionRule
import com.numilike.core.types.Dimension
import com.numilike.core.types.NumiValue
import com.numilike.core.units.UnitRegistry
import com.numilike.data.currency.CurrencyService
import com.numilike.data.customdsl.DslRepository
import com.numilike.data.persistence.ScratchpadRepository
import com.numilike.data.persistence.Settings
import com.numilike.data.persistence.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val scratchpadRepository: ScratchpadRepository,
    private val settingsRepository: SettingsRepository,
    private val dslRepository: DslRepository,
    private val currencyService: CurrencyService,
    private val unitRegistry: UnitRegistry,
) : ViewModel() {

    // ── Scratchpad ──────────────────────────────────────────────

    private val _text = MutableStateFlow("")
    val text: StateFlow<String> = _text.asStateFlow()

    private val _results = MutableStateFlow<List<EvalResult?>>(emptyList())
    val results: StateFlow<List<EvalResult?>> = _results.asStateFlow()

    // ── Settings ────────────────────────────────────────────────

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _decimalPlaces = MutableStateFlow(-1)
    val decimalPlaces: StateFlow<Int> = _decimalPlaces.asStateFlow()

    private val _useThousandsSep = MutableStateFlow(true)
    val useThousandsSep: StateFlow<Boolean> = _useThousandsSep.asStateFlow()

    // ── Navigation ──────────────────────────────────────────────

    private val _showSettings = MutableStateFlow(false)
    val showSettings: StateFlow<Boolean> = _showSettings.asStateFlow()

    private val _showDslEditor = MutableStateFlow(false)
    val showDslEditor: StateFlow<Boolean> = _showDslEditor.asStateFlow()

    // ── DSL editor ──────────────────────────────────────────────

    private val _dslText = MutableStateFlow("")
    val dslText: StateFlow<String> = _dslText.asStateFlow()

    private val _dslErrors = MutableStateFlow<List<DslError>>(emptyList())
    val dslErrors: StateFlow<List<DslError>> = _dslErrors.asStateFlow()

    // ── Snackbar messages ───────────────────────────────────────

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    // ── Clear confirmation ──────────────────────────────────────

    private val _showClearConfirmation = MutableStateFlow(false)
    val showClearConfirmation: StateFlow<Boolean> = _showClearConfirmation.asStateFlow()

    // ── Internal ────────────────────────────────────────────────

    private val dslParser = DslParser()
    private var dslDefinitions: List<DslDefinition> = emptyList()
    private val saveFlow = MutableSharedFlow<String>()

    init {
        // Load saved scratchpad text
        viewModelScope.launch {
            val saved = scratchpadRepository.text.first()
            _text.value = saved
            evaluate(saved)
        }

        // Load settings
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _themeMode.value = settings.theme
                _decimalPlaces.value = settings.decimalPlaces
                _useThousandsSep.value = settings.useThousandsSep
                // Re-evaluate with new formatting
                evaluate(_text.value)
            }
        }

        // Load DSL definitions
        viewModelScope.launch {
            val saved = dslRepository.definitions.first()
            _dslText.value = saved
            applyDslDefinitions(saved)
        }

        // Fetch currency rates
        viewModelScope.launch {
            try {
                val fiatRates = currencyService.getFiatRates()
                for ((code, rate) in fiatRates) {
                    unitRegistry.registerCurrencyRate(code, rate)
                }
            } catch (_: Exception) { /* offline is fine */ }

            try {
                val cryptoRates = currencyService.getCryptoRates()
                for ((code, rate) in cryptoRates) {
                    unitRegistry.registerCurrencyRate(code, rate)
                }
            } catch (_: Exception) { /* offline is fine */ }

            // Re-evaluate now that currencies are loaded
            evaluate(_text.value)
        }

        // Debounced save
        viewModelScope.launch {
            saveFlow.debounce(500L).collect { text ->
                scratchpadRepository.save(text)
            }
        }
    }

    // ── Public API ──────────────────────────────────────────────

    fun onTextChange(newText: String) {
        _text.value = newText
        evaluate(newText)
        viewModelScope.launch {
            saveFlow.emit(newText)
        }
    }

    fun importText(text: String) {
        _text.value = text
        evaluate(text)
        viewModelScope.launch { scratchpadRepository.save(text) }
    }

    fun showSettings() {
        _showSettings.value = true
    }

    fun hideSettings() {
        _showSettings.value = false
    }

    fun showDslEditor() {
        _showDslEditor.value = true
    }

    fun hideDslEditor() {
        _showDslEditor.value = false
    }

    fun setTheme(theme: String) {
        _themeMode.value = theme
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setDecimalPlaces(places: Int) {
        _decimalPlaces.value = places
        viewModelScope.launch {
            settingsRepository.setDecimalPlaces(places)
        }
        evaluate(_text.value)
    }

    fun setUseThousandsSep(use: Boolean) {
        _useThousandsSep.value = use
        viewModelScope.launch {
            settingsRepository.setUseThousandsSep(use)
        }
        evaluate(_text.value)
    }

    fun requestClear() {
        _showClearConfirmation.value = true
    }

    fun confirmClear() {
        _showClearConfirmation.value = false
        _text.value = ""
        _results.value = emptyList()
        viewModelScope.launch {
            scratchpadRepository.save("")
        }
    }

    fun dismissClear() {
        _showClearConfirmation.value = false
    }

    fun copyResult(resultText: String, onCopy: (String) -> Unit) {
        onCopy(resultText)
        viewModelScope.launch {
            _snackbarMessage.emit("Copied: $resultText")
        }
    }

    fun shareText(): String {
        val lines = _text.value.split("\n")
        val resultList = _results.value
        return buildString {
            for (i in lines.indices) {
                val line = lines[i]
                val result = resultList.getOrNull(i)
                if (result != null) {
                    append(line)
                    append("  =  ")
                    appendLine(result.displayText)
                } else {
                    appendLine(line)
                }
            }
        }
    }

    fun onDslTextChange(newText: String) {
        _dslText.value = newText
        val parseResult = dslParser.parseWithErrors(newText)
        _dslErrors.value = parseResult.errors
    }

    fun saveDsl() {
        val text = _dslText.value
        viewModelScope.launch {
            dslRepository.save(text)
        }
        applyDslDefinitions(text)
        evaluate(_text.value)
    }

    // ── Private ─────────────────────────────────────────────────

    private fun evaluate(text: String) {
        val formatter = ResultFormatter(
            maxDecimalPlaces = _decimalPlaces.value,
            useThousandsSep = _useThousandsSep.value,
        )
        val evaluator = Evaluator(unitRegistry, formatter = formatter)

        // Apply DSL definitions to the evaluator's environment
        for (def in dslDefinitions) {
            when (def) {
                is DslDefinition.CustomConstant -> {
                    val result = evaluator.evaluateLine(def.expression)
                    if (result != null) {
                        evaluator.environment.setVariable(def.name, result.value)
                    }
                }
                is DslDefinition.CustomUnit -> {
                    val baseUnit = unitRegistry.lookup(def.baseUnit)
                    if (baseUnit != null) {
                        // "1 horse = 2.4 m" means 1 horse = 2.4 * (m's factor to base)
                        val dslRatio = BigDecimal(def.ratio)
                        val baseUnitFactor = unitRegistry.getRatioToBase(def.baseUnit) ?: BigDecimal.ONE
                        val factor = dslRatio.multiply(baseUnitFactor, NumiValue.MATH_CTX)
                        unitRegistry.registerUnit(
                            def.name.lowercase(),
                            baseUnit.dimension,
                            def.name,
                            null,
                            ConversionRule.Ratio(factor),
                            def.name,
                        )
                    }
                }
                is DslDefinition.CustomFunction -> {
                    evaluator.registerCustomFunction(def.name, def.params, def.body)
                }
            }
        }

        // evaluateDocument handles line results and environment
        _results.value = evaluator.evaluateDocument(text)
    }

    private fun applyDslDefinitions(text: String) {
        val parseResult = dslParser.parseWithErrors(text)
        dslDefinitions = parseResult.definitions
        _dslErrors.value = parseResult.errors
    }
}
