package com.pharmatrade.feature.notification.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pharmatrade.core.common.error.friendlyError
import com.pharmatrade.core.common.i18n.LanguageManager
import com.pharmatrade.core.common.result.Result
import com.pharmatrade.feature.notification.domain.model.Notification
import com.pharmatrade.feature.notification.domain.usecase.ClearAllNotificationsUseCase
import com.pharmatrade.feature.notification.domain.usecase.DeleteNotificationUseCase
import com.pharmatrade.feature.notification.domain.usecase.GetNotificationsUseCase
import com.pharmatrade.feature.notification.domain.usecase.GetUnreadNotificationCountUseCase
import com.pharmatrade.feature.notification.domain.usecase.MarkAllNotificationsAsReadUseCase
import com.pharmatrade.feature.notification.domain.usecase.MarkNotificationAsReadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val total: Int = 0,
    val currentPage: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
) {
    // The backend's page response only carries `total` + `current_page` (no `last_page`), so
    // "more to load" is inferred from how many rows we've accumulated so far.
    val hasMore: Boolean get() = notifications.size < total

    // Order ids with an unread notification about them — used to badge the matching row in the
    // buyer's order list / seller's order list, so "you have an update" is visible without opening
    // the bell icon. Split by type since the same order id could theoretically appear as a
    // notifiable_id on either side; matches the routing in AppNavigation.resolveNotificationDestination.
    val unreadPharmacyOrderIds: Set<String>
        get() = notifications.asSequence()
            .filter { !it.isRead && it.type in PHARMACY_ORDER_NOTIFICATION_TYPES }
            .mapNotNull { it.notifiableId }
            .toSet()

    val unreadSupplierOrderIds: Set<String>
        get() = notifications.asSequence()
            .filter { !it.isRead && it.type == "new_order" }
            .mapNotNull { it.notifiableId }
            .toSet()

    private companion object {
        val PHARMACY_ORDER_NOTIFICATION_TYPES =
            setOf("order_confirmed", "shortage_reported", "order_shipped", "order_delivered")
    }
}

// Hoisted once at the app-navigation level (like CartViewModel) so the Home bell icon's badge
// and the notifications list screen share the same unread count — opening the list and marking
// everything read there must be reflected back on the badge without recreating this VM.
class NotificationViewModel(
    private val getNotificationsUseCase: GetNotificationsUseCase,
    private val getUnreadCountUseCase: GetUnreadNotificationCountUseCase,
    private val markAsReadUseCase: MarkNotificationAsReadUseCase,
    private val markAllAsReadUseCase: MarkAllNotificationsAsReadUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
    private val clearAllNotificationsUseCase: ClearAllNotificationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    // Deliberately does NOT call refreshUnreadCount()/refreshList() here. This VM is hoisted at
    // AppNavigation's top level and constructed immediately regardless of which screen is showing
    // (Login, PendingApproval, Home, ...). The Home/AdminDashboard entries' own
    // DisposableEffect(lifecycleOwner) already triggers both on ON_RESUME — and since the
    // Activity's Lifecycle is already RESUMED by the time those entries compose, attaching that
    // observer synchronously replays ON_RESUME once immediately, which covers "first load" on its
    // own. Calling these here too used to double every request (two independent trigger paths
    // firing back to back) without adding a case neither of them already covers.

    // Called on Home resume to keep the bell badge current — deliberately does not touch the
    // notification list or mark anything read.
    fun refreshUnreadCount() {
        viewModelScope.launch {
            when (val result = getUnreadCountUseCase()) {
                is Result.Success -> update { copy(unreadCount = result.data) }
                else -> Unit
            }
        }
    }

    // Silently (re)loads page 1 in the background — used so unreadPharmacyOrderIds/
    // unreadSupplierOrderIds are populated for the order-list "new" badges even before the user
    // ever opens the Notifications screen. Unlike openScreen(), this never marks anything read and
    // never touches isLoading/error, so it can't interfere with the actual notifications screen.
    fun refreshList() {
        viewModelScope.launch {
            when (val result = getNotificationsUseCase(page = FIRST_PAGE, perPage = PAGE_SIZE)) {
                is Result.Success -> update {
                    copy(notifications = result.data.notifications, total = result.data.total, currentPage = FIRST_PAGE)
                }
                else -> Unit
            }
        }
    }

    // Matches the backend's documented Flutter flow for opening the notifications screen:
    // fetch the list, then mark everything read so the badge clears. Always (re)starts from page 1.
    fun openScreen() {
        viewModelScope.launch {
            update { copy(isLoading = true, error = null) }
            when (val result = getNotificationsUseCase(page = FIRST_PAGE, perPage = PAGE_SIZE)) {
                is Result.Success -> {
                    update {
                        copy(
                            isLoading = false,
                            notifications = result.data.notifications,
                            total = result.data.total,
                            currentPage = FIRST_PAGE
                        )
                    }
                    if (result.data.notifications.any { !it.isRead }) markAllAsRead()
                }
                is Result.Error -> update {
                    copy(isLoading = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun retry() = openScreen()

    // Called as the list scrolls near its end. Guards against duplicate/overlapping requests and
    // against fetching past the last page.
    fun loadMore() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore) return
        val nextPage = state.currentPage + 1
        viewModelScope.launch {
            update { copy(isLoadingMore = true) }
            when (val result = getNotificationsUseCase(page = nextPage, perPage = PAGE_SIZE)) {
                is Result.Success -> update {
                    copy(
                        isLoadingMore = false,
                        notifications = notifications + result.data.notifications,
                        total = result.data.total,
                        currentPage = nextPage
                    )
                }
                is Result.Error -> update {
                    copy(isLoadingMore = false, error = LanguageManager.strings.friendlyError(result.message))
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun markAsRead(id: String) {
        val target = _uiState.value.notifications.find { it.id == id } ?: return
        if (target.isRead) return
        viewModelScope.launch {
            when (markAsReadUseCase(id)) {
                is Result.Success -> {
                    update { copy(notifications = notifications.map { if (it.id == id) it.copy(isRead = true) else it }) }
                    refreshUnreadCount()
                }
                else -> Unit
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (markAllAsReadUseCase()) {
                is Result.Success -> update { copy(notifications = notifications.map { it.copy(isRead = true) }, unreadCount = 0) }
                else -> Unit
            }
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            when (deleteNotificationUseCase(id)) {
                is Result.Success -> {
                    update {
                        copy(
                            notifications = notifications.filterNot { it.id == id },
                            total = (total - 1).coerceAtLeast(0)
                        )
                    }
                    refreshUnreadCount()
                }
                else -> Unit
            }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            when (clearAllNotificationsUseCase()) {
                is Result.Success -> update {
                    copy(notifications = emptyList(), unreadCount = 0, total = 0, currentPage = FIRST_PAGE)
                }
                else -> Unit
            }
        }
    }

    private fun update(block: NotificationsUiState.() -> NotificationsUiState) {
        _uiState.value = _uiState.value.block()
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val PAGE_SIZE = 20
    }
}
