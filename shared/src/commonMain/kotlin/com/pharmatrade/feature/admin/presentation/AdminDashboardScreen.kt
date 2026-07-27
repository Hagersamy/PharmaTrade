package com.pharmatrade.feature.admin.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pharmatrade.core.common.model.User
import com.pharmatrade.core.common.model.UserType
import com.pharmatrade.core.common.session.SessionManager
import com.pharmatrade.core.common.util.formatTodayLabel
import com.pharmatrade.core.ui.components.EmptyState
import com.pharmatrade.core.ui.theme.*
import com.pharmatrade.feature.admin.domain.model.PendingUser
import com.pharmatrade.feature.home.ProfileScreen
import com.pharmatrade.feature.home.ProfileViewModel

// ── Destinations ─────────────────────────────────────────────────────────────

private enum class AdminDest { HOME, REQUESTS, ANALYTICS, PROFILE }
private enum class RequestsTab { PHARMACIES, SUPPLIERS }

// ── Root screen ───────────────────────────────────────────────────────────────

@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    analyticsViewModel: AnalyticsViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by SessionManager.currentUser.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var dest by remember { mutableStateOf(AdminDest.HOME) }
    var requestsTab by remember { mutableStateOf(RequestsTab.PHARMACIES) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val totalPending = uiState.pendingPharmacies.size + uiState.pendingSuppliers.size

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AdminTopBar(
                title = when (dest) {
                    AdminDest.HOME      -> "Dashboard"
                    AdminDest.REQUESTS  -> "Registration Requests"
                    AdminDest.ANALYTICS -> "Analytics"
                    AdminDest.PROFILE   -> "Profile"
                },
                onNotificationsClick = onNavigateToNotifications
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SurfaceWhite) {
                NavigationBarItem(
                    selected = dest == AdminDest.HOME,
                    onClick = { dest = AdminDest.HOME },
                    icon = { Icon(Icons.Filled.Home, null) },
                    label = { Text("Home") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
                )
                NavigationBarItem(
                    selected = dest == AdminDest.REQUESTS,
                    onClick = { dest = AdminDest.REQUESTS },
                    icon = {
                        if (totalPending > 0) {
                            BadgedBox(badge = { Badge { Text("$totalPending") } }) {
                                Icon(Icons.Filled.HowToReg, null)
                            }
                        } else {
                            Icon(Icons.Filled.HowToReg, null)
                        }
                    },
                    label = { Text("Requests") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
                )
                NavigationBarItem(
                    selected = dest == AdminDest.ANALYTICS,
                    onClick = {
                        dest = AdminDest.ANALYTICS
                        analyticsViewModel.refresh()
                    },
                    icon = { Icon(Icons.Filled.Analytics, null) },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
                )
                NavigationBarItem(
                    selected = dest == AdminDest.PROFILE,
                    onClick = { dest = AdminDest.PROFILE },
                    icon = { Icon(Icons.Filled.Person, null) },
                    label = { Text("Profile") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryBlueContainer)
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(BackgroundGray)
        ) {
            when (dest) {
                AdminDest.HOME -> AdminHomeContent(
                    uiState = uiState,
                    onPharmaciesClick = {
                        requestsTab = RequestsTab.PHARMACIES
                        dest = AdminDest.REQUESTS
                    },
                    onSuppliersClick = {
                        requestsTab = RequestsTab.SUPPLIERS
                        dest = AdminDest.REQUESTS
                    }
                )
                AdminDest.REQUESTS -> RequestsContent(
                    pendingPharmacies = uiState.pendingPharmacies,
                    pendingSuppliers = uiState.pendingSuppliers,
                    isLoading = uiState.isLoading,
                    selectedTab = requestsTab,
                    onTabSelected = { requestsTab = it },
                    onApprove = { viewModel.approveUser(it) },
                    onReject = { user, reason -> viewModel.rejectUser(user, reason) },
                    onRefresh = viewModel::refresh
                )
                AdminDest.ANALYTICS -> AnalyticsContent(
                    viewModel = analyticsViewModel,
                    snackbarHostState = snackbarHostState
                )
                AdminDest.PROFILE -> ProfileScreen(viewModel = profileViewModel, user = currentUser, onLogout = onLogout)
            }
        }
    }
}

// ── Top bar ───────────────────────────────────────────────────────────────────

@Composable
private fun AdminTopBar(
    title: String,
    onNotificationsClick: () -> Unit
) {
    Surface(shadowElevation = 4.dp, color = PrimaryBlue) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin Panel",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(Icons.Filled.Notifications, null, tint = Color.White)
            }
        }
    }
}

// ── Requests screen (Pharmacies + Suppliers tabs) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestsContent(
    pendingPharmacies: List<PendingUser>,
    pendingSuppliers: List<PendingUser>,
    isLoading: Boolean,
    selectedTab: RequestsTab,
    onTabSelected: (RequestsTab) -> Unit,
    onApprove: (PendingUser) -> Unit,
    onReject: (PendingUser, String) -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = if (selectedTab == RequestsTab.PHARMACIES) 0 else 1,
            containerColor = SurfaceWhite,
            contentColor = PrimaryBlue
        ) {
            Tab(
                selected = selectedTab == RequestsTab.PHARMACIES,
                onClick = { onTabSelected(RequestsTab.PHARMACIES) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Pharmacies")
                        if (pendingPharmacies.isNotEmpty()) {
                            Surface(shape = CircleShape, color = PrimaryBlue) {
                                Text(
                                    text = "${pendingPharmacies.size}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == RequestsTab.SUPPLIERS,
                onClick = { onTabSelected(RequestsTab.SUPPLIERS) },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Suppliers")
                        if (pendingSuppliers.isNotEmpty()) {
                            Surface(shape = CircleShape, color = WarningAmber) {
                                Text(
                                    text = "${pendingSuppliers.size}",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            )
        }
        when (selectedTab) {
            RequestsTab.PHARMACIES -> PendingListContent(
                label = "pharmacy",
                list = pendingPharmacies,
                isLoading = isLoading,
                onApprove = onApprove,
                onReject = onReject,
                onRefresh = onRefresh
            )
            RequestsTab.SUPPLIERS -> PendingListContent(
                label = "supplier",
                list = pendingSuppliers,
                isLoading = isLoading,
                onApprove = onApprove,
                onReject = onReject,
                onRefresh = onRefresh
            )
        }
    }
}

// ── Home dashboard ────────────────────────────────────────────────────────────

@Composable
private fun AdminHomeContent(
    uiState: AdminUiState,
    onPharmaciesClick: () -> Unit,
    onSuppliersClick: () -> Unit
) {
    val currentUser by SessionManager.currentUser.collectAsStateWithLifecycle()
    val today = remember { formatTodayLabel() }
    val recent = remember(uiState.pendingPharmacies, uiState.pendingSuppliers) {
        (uiState.pendingPharmacies + uiState.pendingSuppliers).take(5)
    }
    val totalPending = uiState.pendingPharmacies.size + uiState.pendingSuppliers.size

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        // Welcome banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryBlue, PrimaryBlueDark))
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = currentUser?.name ?: "Admin",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CalendarToday, null,
                                tint = Color.White.copy(0.7f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = today,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                            .size(90.dp)
                            .align(Alignment.CenterEnd)
                    )
                }
            }
        }

        // Stats 2×2 grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        icon = Icons.Filled.Group,
                        label = "Total Registered",
                        value = uiState.totalRegistered.toString(),
                        tint = PrimaryBlue,
                        bg = PrimaryBlueContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.HourglassTop,
                        label = "Pending Approval",
                        value = totalPending.toString(),
                        tint = WarningAmber,
                        bg = WarningAmberContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        icon = Icons.Filled.CheckCircle,
                        label = "Approved This Month",
                        value = uiState.approvedThisMonth.toString(),
                        tint = SecondaryGreen,
                        bg = SecondaryGreenContainer,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Filled.Verified,
                        label = "Active Users",
                        value = uiState.activeUsers.toString(),
                        tint = PrimaryBlue,
                        bg = PrimaryBlueContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Pending approvals section
        item {
            HomeSectionHeader(title = "Pending Approvals")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PendingSummaryCard(
                    icon = Icons.Filled.LocalPharmacy,
                    label = "Pharmacies",
                    count = uiState.pendingPharmacies.size,
                    tint = PrimaryBlue,
                    bg = PrimaryBlueContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onPharmaciesClick
                )
                PendingSummaryCard(
                    icon = Icons.Filled.Inventory,
                    label = "Suppliers",
                    count = uiState.pendingSuppliers.size,
                    tint = WarningAmber,
                    bg = WarningAmberContainer,
                    modifier = Modifier.weight(1f),
                    onClick = onSuppliersClick
                )
            }
        }

        // Recent registrations
        if (recent.isNotEmpty()) {
            item {
                HomeSectionHeader(title = "Recent Registrations")
            }
            items(recent, key = { it.id }) { user ->
                RecentRegistrationRow(user = user)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Stat card ─────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}

// ── Pending summary card ──────────────────────────────────────────────────────

@Composable
private fun PendingSummaryCard(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            Icon(
                Icons.Filled.ChevronRight, null,
                tint = TextHint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ── Home section header ───────────────────────────────────────────────────────

@Composable
private fun HomeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = TextPrimary
    )
}

// ── Recent registration row ───────────────────────────────────────────────────

@Composable
private fun RecentRegistrationRow(user: PendingUser) {
    val isPharmacy = user.userType == UserType.BUYER
    val accentColor = if (isPharmacy) PrimaryBlue else WarningAmber
    val accentBg = if (isPharmacy) PrimaryBlueContainer else WarningAmberContainer

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = user.phone,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(shape = RoundedCornerShape(20.dp), color = accentBg) {
                    Text(
                        text = if (isPharmacy) "Pharmacy" else "Supplier",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Surface(shape = RoundedCornerShape(20.dp), color = WarningAmberContainer) {
                    Text(
                        text = "Pending",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ── Pending list (Pharmacies / Suppliers) ─────────────────────────────────────

@Composable
private fun PendingListContent(
    label: String,
    list: List<PendingUser>,
    isLoading: Boolean,
    onApprove: (PendingUser) -> Unit,
    onReject: (PendingUser, String) -> Unit,
    onRefresh: () -> Unit
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    if (list.isEmpty()) {
        EmptyState(
            title = "All Clear!",
            message = "No pending ${label}s to review",
            icon = Icons.Filled.CheckCircle,
            action = {
                OutlinedButton(onClick = onRefresh) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refresh")
                }
            }
        )
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list, key = { it.id }) { user ->
            PendingUserCard(user = user, onApprove = onApprove, onReject = { reason -> onReject(user, reason) })
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
}

// ── Pending user card ─────────────────────────────────────────────────────────

@Composable
private fun PendingUserCard(
    user: PendingUser,
    onApprove: (PendingUser) -> Unit,
    onReject: (reason: String) -> Unit
) {
    var showRejectDialog by remember { mutableStateOf(false) }
    var rejectReason by remember { mutableStateOf("") }

    if (showRejectDialog) {
        AlertDialog(
            onDismissRequest = { showRejectDialog = false },
            icon = { Icon(Icons.Filled.Cancel, null, tint = ErrorRed) },
            title = { Text("Reject Registration") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Please provide a reason for rejecting ${user.name}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    OutlinedTextField(
                        value = rejectReason,
                        onValueChange = { rejectReason = it },
                        label = { Text("Reason *") },
                        placeholder = { Text("e.g. Licence image is unclear. Please resubmit.") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 3,
                        isError = rejectReason.isEmpty(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ErrorRed,
                            focusedLabelColor = ErrorRed
                        )
                    )
                    if (rejectReason.isEmpty()) {
                        Text("A reason is required", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rejectReason.isNotBlank()) {
                            showRejectDialog = false
                            onReject(rejectReason)
                            rejectReason = ""
                        }
                    },
                    enabled = rejectReason.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) { Text("Reject") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRejectDialog = false; rejectReason = "" }) { Text("Cancel") }
            }
        )
    }

    val isPharmacy = user.userType == UserType.BUYER
    val accentColor = if (isPharmacy) PrimaryBlue else WarningAmber
    val accentBg    = if (isPharmacy) PrimaryBlueContainer else WarningAmberContainer
    val typeLabel   = if (isPharmacy) "Pharmacy" else "Supplier"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header ──────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = user.businessName,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Surface(shape = RoundedCornerShape(20.dp), color = accentBg) {
                    Text(
                        text = typeLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }

            // ── Account Details ─────────────────────────────────────────────
            CardSection(title = "Account Details") {
                InfoRow(Icons.Filled.Person,     "Full Name",     user.name)
                InfoRow(Icons.Filled.Business,   "Business Name", user.businessName)
                InfoRow(Icons.Filled.Phone,      "Phone",         user.phone)
                if (user.email.isNotBlank())
                    InfoRow(Icons.Filled.Email,  "Email",         user.email)
                user.licenceNumber?.takeIf { it.isNotBlank() }?.let {
                    InfoRow(Icons.Filled.Badge, "Licence No.", it)
                }
                user.zoneId?.takeIf { it.isNotBlank() }?.let {
                    InfoRow(Icons.Filled.LocationCity, "Zone ID", it)
                }
                user.address?.takeIf { it.isNotBlank() }?.let {
                    InfoRow(Icons.Filled.LocationOn, "Address", it)
                }
            }

            // ── Licence Images ──────────────────────────────────────────────
            CardSection(title = "Licence Images") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LicenceImageBox(
                        url = user.licenceFrontUrl,
                        label = "Front",
                        modifier = Modifier.weight(1f)
                    )
                    LicenceImageBox(
                        url = user.licenceBackUrl,
                        label = "Back",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Supplier Details (SELLER only) ──────────────────────────────
            if (user.userType == UserType.SELLER) {
                CardSection(title = "Supplier Details") {
                    if (!user.minOrderValue.isNullOrBlank())
                        InfoRow(Icons.Filled.Payments,  "Min Order Value", "EGP ${user.minOrderValue}")
                    user.minOrderQty?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(Icons.Filled.Inventory2, "Min Order Qty", it)
                    }
                    if (user.additionalZoneIds.isNotEmpty())
                        InfoRow(Icons.Filled.Map, "Delivery Zones",
                            user.additionalZoneIds.joinToString(", "))
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Action buttons ───────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { showRejectDialog = true; rejectReason = "" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed)
                ) {
                    Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reject", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onApprove(user) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryGreen)
                ) {
                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Approve", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ── Card section (label + divider + content) ──────────────────────────────────

@Composable
private fun CardSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.height(12.dp))
    Divider(color = DividerGray)
    Spacer(Modifier.height(10.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
    )
    Spacer(Modifier.height(8.dp))
    Column(verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
}

// ── Licence image box ─────────────────────────────────────────────────────────

@Composable
private fun LicenceImageBox(url: String?, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardGray),
            contentAlignment = Alignment.Center
        ) {
            if (!url.isNullOrBlank()) {
                coil3.compose.SubcomposeAsyncImage(
                    model = url,
                    contentDescription = "Licence $label",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = PrimaryBlue, strokeWidth = 2.dp)
                        }
                    },
                    error = {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Filled.BrokenImage, null, tint = ErrorRed, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.height(4.dp))
                            Text("Failed to load", style = MaterialTheme.typography.labelSmall, color = ErrorRed)
                        }
                    }
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Image, null,
                        tint = TextHint,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Not uploaded",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextHint
                    )
                }
            }
        }
    }
}

// ── Info row ──────────────────────────────────────────────────────────────────

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(15.dp).padding(top = 1.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
