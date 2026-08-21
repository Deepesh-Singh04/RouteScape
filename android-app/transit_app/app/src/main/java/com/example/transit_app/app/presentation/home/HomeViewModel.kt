package com.example.transit_app.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.transit_app.core.network.ApiClient
import com.example.transit_app.data.models.LocalDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// This defines the three possible states your screen can be in
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val data: LocalDataResponse) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel : ViewModel() {

    // The internal state that can be changed
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)

    // The public state that the UI observes
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun fetchExploreData(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Pass the coordinates to the new endpoint
                val response = ApiClient.apiService.getExploreData(lat, lon)
                _uiState.value = HomeUiState.Success(response)
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = HomeUiState.Error("Failed to load data: ${e.message}")
            }
        }
    }
}