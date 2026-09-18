package com.translatelens.presentation.screen.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.translatelens.data.entity.TranslationHistory
import com.translatelens.domain.repository.HistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository
) : ViewModel() {
    private val favoritesOnly = MutableStateFlow(false)

    val paging: Flow<PagingData<TranslationHistory>> = favoritesOnly.flatMapLatest { fav ->
        if (fav) repo.getFavorites() else repo.getAllHistory()
    }.cachedIn(viewModelScope)

    fun showFavoritesOnly(fav: Boolean) {
        favoritesOnly.value = fav
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            repo.deleteHistory(id)
        }
    }

    fun toggleFavorite(h: TranslationHistory) {
        viewModelScope.launch {
            repo.updateHistory(h.copy(isFavorite = !h.isFavorite))
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.clearAllHistory()
        }
    }
}
