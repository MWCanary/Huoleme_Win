package com.huoleme.app.ui

import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.time.LocalDate
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

object BackupFiles {
    fun chooseExportFile(): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = "导出打卡记录"
            fileFilter = FileNameExtensionFilter("JSON 文件 (*.json)", "json")
            selectedFile = File("huoleme-backup-${LocalDate.now()}.json")
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return null
        val selected = chooser.selectedFile ?: return null
        return if (selected.name.endsWith(".json", ignoreCase = true)) {
            selected
        } else {
            File(selected.parentFile, "${selected.name}.json")
        }
    }

    fun chooseImportFile(): File? {
        val chooser = JFileChooser().apply {
            dialogTitle = "导入打卡记录"
            fileFilter = FileNameExtensionFilter("JSON 文件 (*.json)", "json")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return null
        return chooser.selectedFile
    }

    fun write(file: File, text: String) {
        Files.writeString(file.toPath(), text, StandardCharsets.UTF_8)
    }

    fun read(file: File): String {
        return Files.readString(file.toPath(), StandardCharsets.UTF_8)
    }
}
