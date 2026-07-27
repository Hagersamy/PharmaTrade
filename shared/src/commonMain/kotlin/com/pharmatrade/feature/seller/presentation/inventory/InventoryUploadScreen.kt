package com.pharmatrade.feature.seller.presentation.inventory

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.util.formatBackendTimestamp
import com.pharmatrade.core.io.rememberFilePickerLauncher
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.drugs.domain.model.UploadHistory

@Composable
fun InventoryUploadScreen(
    viewModel: InventoryUploadViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val launchFilePicker = rememberFilePickerLauncher(
        mimeTypes = listOf(
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv",
            "text/comma-separated-values"
        )
    ) { uri, displayName ->
        viewModel.onFileSelected(uri, displayName.ifBlank { "inventory_file" })
    }

    Scaffold(
        topBar = { UploadTopBar(onNavigateBack = onNavigateBack) },
        containerColor = BackgroundGray
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            UploadBanner()

            UploadZoneCard(
                uiState = uiState,
                onPickFile = launchFilePicker,
                onUpload = viewModel::upload,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            UploadStatusSection(
                uiState = uiState,
                onDismiss = viewModel::dismissResult,
                onDismissError = viewModel::dismissError,
                onRetry = viewModel::refreshStatus,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            uiState.lastUpload?.let { lastUpload ->
                LastUploadSummaryCard(
                    lastUpload = lastUpload,
                    isRefreshing = uiState.isRefreshingStatus,
                    onRefresh = viewModel::refreshStatus,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            FormatGuideCard(
                initiallyExpanded = uiState.lastUpload == null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UploadTopBar(onNavigateBack: () -> Unit) {
    Surface(shadowElevation = 4.dp, color = PrimaryBlue) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bulk Import",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = "Upload Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = ".xlsx / .csv",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ── Gradient banner ───────────────────────────────────────────────────────────

@Composable
private fun UploadBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(PrimaryBlue, PrimaryBlueDark)))
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Import your drug catalogue",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Upload an Excel or CSV file to add multiple listings at once — no manual entry needed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.5
                )
            }
            Spacer(Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.TableChart,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

// ── Upload zone card ──────────────────────────────────────────────────────────

@Composable
private fun UploadZoneCard(
    uiState: InventoryUploadUiState,
    onPickFile: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // ── File picker zone ──────────────────────────────────────────────
            val hasFile = uiState.selectedFileUri.isNotBlank()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (hasFile) 80.dp else 140.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = 2.dp,
                        brush = if (hasFile)
                            Brush.linearGradient(listOf(PrimaryBlue, PrimaryBlueLight))
                        else
                            Brush.linearGradient(listOf(DividerGray, DividerGray)),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .background(if (hasFile) PrimaryBlueContainer else CardGray)
                    .clickable(onClick = onPickFile),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = hasFile,
                    transitionSpec = {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    },
                    label = "file_zone"
                ) { fileSelected ->
                    if (fileSelected) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.InsertDriveFile,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.selectedFileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Tap to change file",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                tint = SecondaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                                initialValue = 0.92f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    tween(900, easing = EaseInOut),
                                    RepeatMode.Reverse
                                ),
                                label = "scale"
                            )
                            Icon(
                                Icons.Filled.CloudUpload,
                                null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(44.dp).scale(pulse)
                            )
                            Text(
                                "Tap to select file",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryBlue
                            )
                            Text(
                                ".xlsx  •  .xls  •  .csv",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Progress bar (visible while uploading) ────────────────────────
            AnimatedVisibility(
                visible = uiState.isUploading,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Uploading…",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Please wait",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                        color = PrimaryBlue,
                        trackColor = PrimaryBlueContainer,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            Spacer(Modifier.height(if (uiState.isUploading) 12.dp else 0.dp))

            // ── Upload button ─────────────────────────────────────────────────
            Button(
                onClick = onUpload,
                enabled = uiState.selectedFileUri.isNotBlank() && !uiState.isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    disabledContainerColor = DividerGray
                )
            ) {
                if (uiState.isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Uploading…", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.CloudUpload, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Upload Inventory",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

// ── Unified upload status section ───────────────────────────────────────────

private enum class UploadStage { NONE, ERROR, QUEUED, SUCCESS, FAILED }

@Composable
private fun UploadStatusSection(
    uiState: InventoryUploadUiState,
    onDismiss: () -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = when {
        uiState.processingResult == ProcessingResult.SUCCESS -> UploadStage.SUCCESS
        uiState.processingResult == ProcessingResult.FAILED -> UploadStage.FAILED
        uiState.uploadError != null -> UploadStage.ERROR
        uiState.uploadQueued -> UploadStage.QUEUED
        else -> UploadStage.NONE
    }
    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            (fadeIn(tween(200)) + expandVertically()) togetherWith (fadeOut(tween(150)) + shrinkVertically())
        },
        label = "upload_status",
        modifier = modifier
    ) { currentStage ->
        when (currentStage) {
            UploadStage.NONE -> Unit
            UploadStage.ERROR -> StatusCard(
                icon = Icons.Filled.ErrorOutline,
                iconTint = ErrorRed,
                iconBg = ErrorRedContainer,
                title = "Something went wrong",
                message = uiState.uploadError.orEmpty(),
                onDismiss = onDismissError,
                trailingAction = { TextButton(onClick = onRetry) { Text("Retry") } },
                modifier = Modifier.padding(bottom = 8.dp)
            )
            UploadStage.QUEUED -> StatusCard(
                icon = Icons.Filled.HourglassTop,
                iconTint = PrimaryBlue,
                iconBg = PrimaryBlueContainer,
                title = "Processing your file",
                message = "This can take a minute — you can leave this screen, it'll keep working in the background.",
                showSpinner = true,
                onDismiss = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            UploadStage.SUCCESS -> StatusCard(
                icon = Icons.Filled.CheckCircle,
                iconTint = SecondaryGreenDark,
                iconBg = SecondaryGreenContainer,
                title = "Import complete",
                message = buildString {
                    append("${uiState.processedRows} rows imported")
                    if (uiState.failedRows > 0) append(", ${uiState.failedRows} failed") else append(" successfully")
                    append(". Check your inventory on the Home screen.")
                },
                onDismiss = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            UploadStage.FAILED -> StatusCard(
                icon = Icons.Filled.Cancel,
                iconTint = ErrorRed,
                iconBg = ErrorRedContainer,
                title = "Import failed",
                message = "Check the file format and column names, then try again.",
                onDismiss = onDismiss,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun StatusCard(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    if (showSpinner) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = iconTint)
                    } else {
                        Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(2.dp))
                    Text(message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                if (onDismiss != null) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
            if (trailingAction != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    trailingAction()
                }
            }
        }
    }
}

// ── Format guide card ───────────────────────────────────────────────card──────────

private val REQUIRED_COLUMNS = listOf(
    "drug_name"        to "Drug name as it appears in the catalogue",
    "public_price"     to "Retail price shown to the public (EGP)",
    "pharmacist_price" to "Wholesale price for pharmacists (EGP)",
    "discount"         to "Discount percentage — enter 0 if none",
    "quantity"         to "Available quantity in stock"
)
private val OPTIONAL_COLUMNS = listOf(
    "limit" to "Maximum units a buyer can order per transaction"
)

@Composable
private fun FormatGuideCard(initiallyExpanded: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.TableChart, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "Sheet format guide",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextSecondary
                )
            }
            if (expanded) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = DividerGray)
                    Text(
                        "REQUIRED COLUMNS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    REQUIRED_COLUMNS.forEach { (col, desc) -> ColumnChipRow(col, desc, required = true) }
                    Text(
                        "OPTIONAL COLUMNS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    OPTIONAL_COLUMNS.forEach { (col, desc) -> ColumnChipRow(col, desc, required = false) }
                }
            }
        }
    }
}

@Composable
private fun ColumnChipRow(column: String, description: String, required: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (required) PrimaryBlueContainer else CardGray)
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                column,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (required) PrimaryBlue else TextSecondary
            )
        }
        Text(
            description,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f).padding(top = 2.dp)
        )
    }
}

// ── Last upload summary card ──────────────────────────────────────────────────

@Composable
private fun LastUploadSummaryCard(
    lastUpload: UploadHistory,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusBg, statusLabel) = when (lastUpload.status.lowercase()) {
        "success", "completed", "done" -> Triple(SecondaryGreenDark, SecondaryGreenContainer, "Completed")
        "failed", "error"              -> Triple(ErrorRed, ErrorRedContainer, "Failed")
        "processing"                   -> Triple(PrimaryBlue, PrimaryBlueContainer, "Processing")
        else -> Triple(WarningAmber, WarningAmberContainer, lastUpload.status.replaceFirstChar { it.uppercase() })
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Last Upload",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Surface(shape = RoundedCornerShape(20.dp), color = statusBg) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                } else {
                    IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh status",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (lastUpload.createdAt.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(formatBackendTimestamp(lastUpload.createdAt), style = MaterialTheme.typography.labelSmall, color = TextHint)
            }
            if (lastUpload.totalRows > 0) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = DividerGray)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    RowStat("Total Rows", lastUpload.totalRows.toString(), TextPrimary, CardGray, Modifier.weight(1f))
                    RowStat("Imported", lastUpload.processedRows.toString(), SecondaryGreenDark, SecondaryGreenContainer, Modifier.weight(1f))
                    RowStat(
                        "Failed", lastUpload.failedRows.toString(),
                        if (lastUpload.failedRows > 0) ErrorRed else TextSecondary,
                        if (lastUpload.failedRows > 0) ErrorRedContainer else CardGray,
                        Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RowStat(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}
