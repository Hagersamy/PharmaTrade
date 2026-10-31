package com.pharmatrade.feature.admin.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.i18n.LocalStrings
import com.pharmatrade.core.common.i18n.Strings
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.components.ErrorScreen
import com.pharmatrade.core.ui.components.LoadingScreen
import com.pharmatrade.core.ui.components.PharmaButton
import com.pharmatrade.core.ui.components.PharmaTextField
import com.pharmatrade.core.ui.components.PharmaTopBar
import com.pharmatrade.core.ui.components.rememberToast
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.admin.domain.model.DrugEnglishNameUpdate
import com.pharmatrade.feature.admin.domain.model.DrugField
import com.pharmatrade.feature.admin.domain.model.DrugFieldError
import com.pharmatrade.feature.admin.domain.model.MissingEnglishDrug

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingEnglishDrugsScreen(
    viewModel: MissingEnglishDrugsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val listState = rememberLazyListState()

    // Fresh every visit — this screen is also opened from a push, after new drugs were added.
    LaunchedEffect(Unit) { viewModel.refresh() }

    val showToast = rememberToast()
    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let {
            showToast(it)
            viewModel.clearToast()
        }
    }

    // Infinite scroll: fetch the next page once the last visible row is within 3 of the end.
    val shouldLoadMore by remember(uiState.drugs.size) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            uiState.drugs.isNotEmpty() && lastVisible >= uiState.drugs.lastIndex - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Column(modifier = Modifier.fillMaxSize().background(BackgroundGray)) {
        PharmaTopBar(title = strings.adMissingEnglishTitle, onNavigateBack = onNavigateBack)

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh(pull = true) },
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) {
            when {
                uiState.isLoading && uiState.drugs.isEmpty() -> LoadingScreen()
                uiState.error != null && uiState.drugs.isEmpty() -> ErrorScreen(
                    message = uiState.error.orEmpty(),
                    onRetry = { viewModel.refresh() }
                )
                uiState.loaded && uiState.drugs.isEmpty() -> EmptyState(
                    title = strings.adMissingEnglishEmptyTitle,
                    message = strings.adMissingEnglishEmptyMessage,
                    icon = Icons.Filled.Translate,
                    // Scrollable so pull-to-refresh still works on the empty state.
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.drugs, key = { it.id }) { drug ->
                        MissingEnglishDrugRow(drug = drug, onClick = { viewModel.openEditor(drug) })
                    }
                    if (uiState.isLoadingMore) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                            }
                        }
                    }
                }
            }
        }
    }

    uiState.editing?.let { drug ->
        EditEnglishNameSheet(
            drug = drug,
            state = uiState,
            onFormChange = viewModel::onFormChange,
            onToggleMoreDetails = viewModel::toggleMoreDetails,
            onSave = viewModel::save,
            onDismiss = viewModel::dismissEditor
        )
    }
}

@Composable
private fun MissingEnglishDrugRow(drug: MissingEnglishDrug, onClick: () -> Unit) {
    val strings = LocalStrings.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(WarningAmberContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Medication, null, tint = WarningAmber, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                ArabicName(drug.nameAr, style = MaterialTheme.typography.titleMedium)
                if (drug.createdAt.isNotBlank()) {
                    Text(
                        strings.adMissingEnglishAddedOn(formatBackendTimestamp(drug.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = TextSecondary)
        }
    }
}

// Always laid out right-to-left, whatever the app language, so the Arabic name reads correctly.
@Composable
private fun ArabicName(name: String, style: TextStyle) {
    Text(
        text = name,
        style = style.copy(textDirection = TextDirection.Rtl),
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        textAlign = TextAlign.Start,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditEnglishNameSheet(
    drug: MissingEnglishDrug,
    state: MissingEnglishDrugsUiState,
    onFormChange: (DrugEnglishNameUpdate) -> Unit,
    onToggleMoreDetails: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    val form = state.form
    fun errorFor(field: DrugField) = state.fieldErrors[field]?.let { strings.drugFieldErrorMessage(field, it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = SurfaceWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                strings.adMissingEnglishTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Read-only reference for the admin while typing the English name.
            Surface(shape = RoundedCornerShape(12.dp), color = BackgroundGray) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(strings.adArabicNameLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    ArabicName(drug.nameAr, style = MaterialTheme.typography.titleMedium)
                }
            }

            // Drug names are English, so these inputs stay left-to-right in the Arabic UI too.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PharmaTextField(
                        value = form.englishName,
                        onValueChange = { onFormChange(form.copy(englishName = it)) },
                        label = strings.adEnglishNameLabel,
                        isError = errorFor(DrugField.ENGLISH_NAME) != null,
                        errorMessage = errorFor(DrugField.ENGLISH_NAME),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorFor(DrugField.ENGLISH_NAME) == null) {
                        Text(
                            strings.adEnglishNameHint,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            TextButton(onClick = onToggleMoreDetails, contentPadding = PaddingValues(horizontal = 4.dp)) {
                Text(strings.adMoreDetails, color = PrimaryBlue)
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (state.showMoreDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (state.showMoreDetails) strings.commonCollapse else strings.commonExpand,
                    tint = PrimaryBlue
                )
            }

            AnimatedVisibility(visible = state.showMoreDetails) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OptionalField(form.scientificName, strings.adScientificNameLabel, errorFor(DrugField.SCIENTIFIC_NAME)) {
                            onFormChange(form.copy(scientificName = it))
                        }
                        OptionalField(form.manufacturer, strings.adManufacturerLabel, errorFor(DrugField.MANUFACTURER)) {
                            onFormChange(form.copy(manufacturer = it))
                        }
                        OptionalField(form.dosageForm, strings.adDosageFormLabel, errorFor(DrugField.DOSAGE_FORM)) {
                            onFormChange(form.copy(dosageForm = it))
                        }
                        OptionalField(form.strength, strings.adStrengthLabel, errorFor(DrugField.STRENGTH)) {
                            onFormChange(form.copy(strength = it))
                        }
                        OptionalField(form.barcode, strings.adBarcodeLabel, errorFor(DrugField.BARCODE)) {
                            onFormChange(form.copy(barcode = it))
                        }
                    }
                }
            }

            state.saveError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = ErrorRed)
            }

            PharmaButton(
                text = strings.commonSave,
                onClick = onSave,
                isLoading = state.isSaving,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OptionalField(value: String, label: String, error: String?, onValueChange: (String) -> Unit) {
    PharmaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        isError = error != null,
        errorMessage = error,
        modifier = Modifier.fillMaxWidth()
    )
}

private fun Strings.drugFieldErrorMessage(field: DrugField, error: DrugFieldError): String = when (error) {
    DrugFieldError.REQUIRED -> adFieldRequired
    DrugFieldError.ENGLISH_ONLY -> adFieldEnglishOnly
    DrugFieldError.TOO_LONG -> adFieldTooLong
    DrugFieldError.ALREADY_TAKEN -> if (field == DrugField.BARCODE) adBarcodeTaken else adFieldInvalid
    DrugFieldError.INVALID -> adFieldInvalid
}
