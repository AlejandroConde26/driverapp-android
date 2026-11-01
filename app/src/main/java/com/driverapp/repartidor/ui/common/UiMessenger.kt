package com.driverapp.repartidor.ui.common

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Mensaje para la UI. Los ViewModels nunca construyen texto en español: emiten
 * un recurso ([Res]) y la capa Compose lo resuelve con `stringResource`, de modo
 * que los avisos también se traducen es/en.
 */
sealed interface UiMessage {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiMessage
    data class Raw(val text: String) : UiMessage
}

class UiMessenger {

    private val _message = MutableStateFlow<UiMessage?>(null)
    val message: StateFlow<UiMessage?> = _message

    fun show(@StringRes id: Int, vararg args: Any) {
        _message.value = UiMessage.Res(id, args.toList())
    }

    /** Sólo para textos que vienen del servidor, no traducibles. */
    fun showRaw(text: String) {
        _message.value = UiMessage.Raw(text)
    }

    fun consume() {
        _message.value = null
    }
}
