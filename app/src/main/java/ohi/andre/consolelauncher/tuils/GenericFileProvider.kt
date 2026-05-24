package ohi.andre.consolelauncher.tuils

import androidx.core.content.FileProvider
import ohi.andre.consolelauncher.BuildConfig

class GenericFileProvider : FileProvider() {
    companion object {
        @JvmField
        val PROVIDER_NAME: String = BuildConfig.APPLICATION_ID + ".FILE_PROVIDER"
    }
}
