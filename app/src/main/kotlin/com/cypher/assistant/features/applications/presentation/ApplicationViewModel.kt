package com.cypher.assistant.features.applications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cypher.assistant.features.applications.domain.model.AppInfo
import com.cypher.assistant.features.applications.domain.model.LaunchResult
import com.cypher.assistant.features.applications.domain.repository.ApplicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ApplicationViewModel @Inject constructor(
    private val applicationRepository: ApplicationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplicationUiState())
    val uiState: StateFlow<ApplicationUiState> = _uiState.asStateFlow()

    init {
        loadApplications()
    }

    fun loadApplications(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val apps = applicationRepository.getInstalledApplications(forceRefresh)
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        allApplications = apps,
                        filteredApplications = filterApps(apps, current.searchQuery)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load applications") }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredApplications = filterApps(current.allApplications, query)
            )
        }
    }

    fun launchApp(appInfo: AppInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLaunching = true, launchStatusMessage = "Opening ${appInfo.appName}...") }
            val result = applicationRepository.launchApplication(appInfo.packageName)
            _uiState.update { current ->
                when (result) {
                    is LaunchResult.Success -> current.copy(isLaunching = false, launchStatusMessage = result.message)
                    is LaunchResult.Failed -> current.copy(isLaunching = false, launchStatusMessage = result.message, errorMessage = result.error)
                    else -> current.copy(isLaunching = false, launchStatusMessage = "Unable to open ${appInfo.appName}")
                }
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(launchStatusMessage = null, errorMessage = null) }
    }

    private fun filterApps(apps: List<AppInfo>, query: String): List<AppInfo> {
        if (query.isBlank()) return apps
        val norm = query.trim().lowercase()
        return apps.filter {
            it.appName.lowercase().contains(norm) || it.packageName.lowercase().contains(norm)
        }
    }
}
