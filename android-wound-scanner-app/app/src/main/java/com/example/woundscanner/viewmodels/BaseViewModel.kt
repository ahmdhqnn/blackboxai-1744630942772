package com.example.woundscanner.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.woundscanner.ui.state.UiEffect
import com.example.woundscanner.ui.state.UiEvent
import com.example.woundscanner.ui.state.UiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : UiState, Event : UiEvent> : ViewModel() {

    private val initialState: State by lazy { createInitialState() }
    abstract fun createInitialState(): State

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<Event>()
    val event = _event.asSharedFlow()

    private val _effect = Channel<UiEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        subscribeToEvents()
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            event.collect {
                handleEvent(it)
            }
        }
    }

    abstract fun handleEvent(event: Event)

    protected fun setState(reduce: State.() -> State) {
        val newState = currentState().reduce()
        _uiState.value = newState
    }

    protected fun setEvent(event: Event) {
        val newEvent = event
        viewModelScope.launch { _event.emit(newEvent) }
    }

    protected fun setEffect(builder: () -> UiEffect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }

    protected fun currentState() = uiState.value

    protected fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null
    ) {
        setEffect {
            UiEffect.ShowSnackbar(
                message = message,
                actionLabel = actionLabel,
                onAction = onAction
            )
        }
    }

    protected fun showDialog(
        title: String,
        message: String,
        confirmText: String = "OK",
        dismissText: String = "Cancel",
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        setEffect {
            UiEffect.ShowDialog(
                title = title,
                message = message,
                confirmText = confirmText,
                dismissText = dismissText,
                onConfirm = onConfirm,
                onDismiss = onDismiss
            )
        }
    }

    protected fun navigateUp() {
        setEffect { UiEffect.NavigateUp }
    }

    protected fun navigateTo(route: String) {
        setEffect { UiEffect.NavigateTo(route) }
    }

    protected fun setLoading(isLoading: Boolean) {
        setState { copy(isLoading = isLoading) as State }
    }

    protected fun handleError(error: Throwable) {
        val errorMessage = error.localizedMessage ?: "An unexpected error occurred"
        setState {
            copy(
                isLoading = false,
                error = errorMessage
            ) as State
        }
        showSnackbar(errorMessage)
    }

    protected fun clearError() {
        setState { copy(error = null) as State }
    }

    protected fun <T> Flow<T>.stateIn(
        initialValue: T
    ): StateFlow<T> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = initialValue
    )

    protected fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    protected fun launchWithLoading(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                setLoading(true)
                block()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                setLoading(false)
            }
        }
    }

    protected suspend fun <T> withLoading(block: suspend () -> T): T {
        setLoading(true)
        return try {
            block()
        } finally {
            setLoading(false)
        }
    }

    protected fun <T> Flow<T>.onEachLoading(): Flow<T> = onEach {
        setLoading(false)
    }.onStart {
        setLoading(true)
    }.catch { error ->
        handleError(error)
        setLoading(false)
    }

    protected fun <T> Flow<T>.onEachResource(
        onSuccess: (T) -> Unit,
        onError: (Throwable) -> Unit = { handleError(it) },
        onLoading: () -> Unit = { setLoading(true) }
    ): Flow<T> = onStart {
        onLoading()
    }.onEach { result ->
        setLoading(false)
        onSuccess(result)
    }.catch { error ->
        setLoading(false)
        onError(error)
    }

    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
