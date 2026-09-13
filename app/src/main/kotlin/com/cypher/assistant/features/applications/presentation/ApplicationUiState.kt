package com.cypher.assistant.features.applications.presentation

import com.cypher.assistant.features.applications.domain.model.AppInfo

data class ApplicationUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val allApplications: List<AppInfo> = emptyList(),
    val filteredApplications: List<AppInfo> = emptyList(),
    val launchStatusMessage: String? = null,
    val isLaunching: Boolean = false,
    val errorMessage: String? = null
)
