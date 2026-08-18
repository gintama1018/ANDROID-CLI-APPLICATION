package com.gintama.nlcli.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gintama.nlcli.data.AppDatabase
import com.gintama.nlcli.data.dao.CommandHistoryDao
import com.gintama.nlcli.data.entity.CommandHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL,
    SUCCESS_ONLY,
    FAILED_ONLY
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val historyDao: CommandHistoryDao = AppDatabase.getInstance(application).commandHistoryDao()
    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val historyItems: StateFlow<List<CommandHistoryEntity>> = combine(
        historyDao.getAllHistory(),
        _filter,
        _searchQuery
    ) { list, filter, query ->
        list.filter { item ->
            val matchesFilter = when (filter) {
                HistoryFilter.ALL -> true
                HistoryFilter.SUCCESS_ONLY -> item.success
                HistoryFilter.FAILED_ONLY -> !item.success
            }
            val matchesQuery = if (query.isBlank()) true else {
                item.rawInput.contains(query, ignoreCase = true) ||
                (item.contact?.contains(query, ignoreCase = true) == true) ||
                item.app.contains(query, ignoreCase = true)
            }
            matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            historyDao.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyDao.clearAll()
        }
    }
}
