package com.nuvio.app.features.home.category

internal expect object HomeCategorySettingsStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)
}
