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

    init {
        // Fetch data immediately when the screen loads
        fetchExploreData()
    }

    fun fetchExploreData() {
        // Launch a coroutine to do network work in the background
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                // Try to get the JSON from the Python backend
                val response = ApiClient.apiService.getExploreData()

                // If successful, update the state with the new data
                _uiState.value = HomeUiState.Success(response)
            } catch (e: Exception) {
                // If the server is down or there's an error, show the error message
                _uiState.value = HomeUiState.Error(e.localizedMessage ?: "Failed to connect to backend")
            }
        }
    }
}