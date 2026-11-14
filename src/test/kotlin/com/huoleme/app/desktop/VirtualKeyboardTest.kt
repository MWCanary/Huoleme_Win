package com.huoleme.app.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 键盘设备分类：只有真正能打字的键盘才算「接着键盘」，否则拆掉键盘盖（Surface
 * 类设备）后就永远不会弹虚拟键盘。设备名取自实测的本机枚举结果。
 */
class VirtualKeyboardTest {

    @Test
    fun countsRealKeyboards() {
        // Surface 键盘盖（HID 键盘接口）
        assertTrue(
            VirtualKeyboard.looksLikeTypingKeyboard(
                "\\\\?\\HID#VID_045E&PID_096F&MI_00#7&1fd94581&0&0000#{884b96c3-56ef-11d1-bc8c-00a0c91405dd}"
            )
        )
        // 蓝牙键盘
        assertTrue(
            VirtualKeyboard.looksLikeTypingKeyboard(
                "\\\\?\\BTHENUM#{00001124-0000-1000-8000-00805F9B34FB}_VID&0001000F_PID&0000#8&1a2b3c4d&0&0000"
            )
        )
        // PS/2 键盘
        assertTrue(VirtualKeyboard.looksLikeTypingKeyboard("\\\\?\\ACPI#PNP0303#4&2a1b3c4d&0"))
        // 远程桌面键盘
        assertTrue(VirtualKeyboard.looksLikeTypingKeyboard("\\\\?\\Root#RDP_KBD#0000"))
    }

    @Test
    fun ignoresSyntheticAndButtonDevices() {
        // 音量/媒体/快捷键等 HID 顶层集合
        assertFalse(
            VirtualKeyboard.looksLikeTypingKeyboard(
                "\\\\?\\HID#ConvertedDevice&Col01#5&1568f66a&0&0000#{884b96c3-56ef-11d1-bc8c-00a0c91405dd}"
            )
        )
        assertFalse(
            VirtualKeyboard.looksLikeTypingKeyboard(
                "\\\\?\\HID#INTC816&Col01#3&36a7043c&0&0000#{884b96c3-56ef-11d1-bc8c-00a0c91405dd}"
            )
        )
        assertFalse(
            VirtualKeyboard.looksLikeTypingKeyboard(
                "\\\\?\\HID#INT33D2&Col01#6&3999b792&0&0000#{884b96c3-56ef-11d1-bc8c-00a0c91405dd}"
            )
        )
        // 系统合成的键盘栈节点
        assertFalse(VirtualKeyboard.looksLikeTypingKeyboard("\\\\?\\Microsoft Keyboard RID\\1"))
        // Surface 键盘盖拆下后仍残留的 ACPI 连接器
        assertFalse(
            VirtualKeyboard.looksLikeTypingKeyboard(
                "\\\\?\\ACPI#MSFT0001#4&25765b&0#{884b96c3-56ef-11d1-bc8c-00a0c91405dd}"
            )
        )
        assertFalse(VirtualKeyboard.looksLikeTypingKeyboard(""))
    }
}
