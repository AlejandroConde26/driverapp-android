package com.driverapp.repartidor.ui.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UiMessenger {
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun show(text: String) {
        _message.value = text
    }

    fun consume() {
        _message.value = null
    }
}
