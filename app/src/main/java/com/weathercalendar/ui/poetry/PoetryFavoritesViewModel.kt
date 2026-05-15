package com.weathercalendar.ui.poetry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weathercalendar.data.repository.FavoritePoetry
import com.weathercalendar.data.repository.PoetryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PoetryFavoritesViewModel @Inject constructor(
    private val poetryRepository: PoetryRepository,
) : ViewModel() {

    val favorites: StateFlow<List<FavoritePoetry>> = poetryRepository.observeFavorites()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    fun removeFavorite(id: Long) {
        viewModelScope.launch {
            poetryRepository.removeFavorite(id)
        }
    }
}
