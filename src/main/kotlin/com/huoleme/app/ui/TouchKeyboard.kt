package com.huoleme.app.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.huoleme.app.desktop.VirtualKeyboard

/**
 * 输入框获得焦点时拉起 Windows 触摸键盘（未接实体键盘的平板场景），失去焦点
 * 后收起。系统没有可用的触摸键盘、或接实体键盘时会自动跳过，不打扰。
 */
fun Modifier.touchKeyboard(): Modifier = this
    .onFocusChanged { state ->
        if (state.isFocused) {
            VirtualKeyboard.onFieldFocused()
        } else if (!state.hasFocus) {
            VirtualKeyboard.onFieldBlurred()
        }
    }
    .pointerInput(Unit) {
        // 告知窗口级监听「这次按下落在输入框里」，避免误收起键盘。
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type == PointerEventType.Press) {
                    VirtualKeyboard.onTextFieldPressed()
                }
            }
        }
    }
