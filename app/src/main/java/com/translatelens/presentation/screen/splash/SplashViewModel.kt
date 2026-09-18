package com.translatelens.presentation.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.translatelens.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val loading: Boolean = true,
    val showGetStarted: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    private val _ui = MutableStateFlow(SplashUiState())
    val ui: StateFlow<SplashUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val seen = try {
                settings.introSeen.first()
            } catch (_: Exception) {
                false
            }
            _ui.value = SplashUiState(loading = false, showGetStarted = !seen)
        }
    }

    fun onGetStarted() {
        viewModelScope.launch {
            try {
                settings.setIntroSeen()
            } catch (_: Exception) {
            }
        }
    }
}
