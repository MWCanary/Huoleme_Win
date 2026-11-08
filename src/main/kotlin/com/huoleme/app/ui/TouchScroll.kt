package com.huoleme.app.ui

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Compose Desktop only scrolls with the mouse wheel by default. Windows touch
 * input is delivered as pointer drags, so add an explicit vertical drag handler
 * that feeds the same scroll state. Mouse dragging also works, which is useful
 * for testing on a non-touch device.
 */
fun Modifier.touchScrollable(state: ScrollableState): Modifier = pointerInput(state) {
    detectVerticalDragGestures { change, dragAmount ->
        change.consume()
        state.dispatchRawDelta(-dragAmount)
    }
}
