package com.oldguy.markup

import androidx.test.platform.app.InstrumentationRegistry
import com.oldguy.common.io.File

class AndroidTestBase {

    init {
        File.appContext = InstrumentationRegistry.getInstrumentation().context
    }

    fun copyAssetToWorking(name: String) {
        File.appContext.assets.open(name).use { inputStream ->
            val targetFile = java.io.File(File.appContext.filesDir, name)
            targetFile.outputStream().use { outputStream ->
                val count = inputStream.copyTo(outputStream)
                println("Copied $count bytes to $targetFile")
            }
        }
    }
}