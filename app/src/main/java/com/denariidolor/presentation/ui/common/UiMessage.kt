package com.denariidolor.presentation.ui.common

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.denariidolor.R
import kotlinx.coroutines.flow.Flow

sealed interface UiMessage {
    data class Resource(@StringRes val resId: Int) : UiMessage
    data class Text(val text: String) : UiMessage

    companion object {
        fun fromResult(result: Result<*>, @StringRes successResId: Int): UiMessage =
            result.fold(
                onSuccess = { Resource(successResId) },
                onFailure = { error -> error.message?.takeIf { it.isNotBlank() }?.let(::Text) ?: Resource(R.string.generic_error) }
            )
    }
}

@Composable
fun UiMessageEffect(messages: Flow<UiMessage>) {
    val context = LocalContext.current
    LaunchedEffect(messages) {
        messages.collect { message ->
            val text = when (message) {
                is UiMessage.Resource -> context.getString(message.resId)
                is UiMessage.Text -> message.text
            }
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }
}
