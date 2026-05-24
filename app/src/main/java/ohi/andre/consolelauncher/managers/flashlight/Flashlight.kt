package ohi.andre.consolelauncher.managers.flashlight

import android.content.Context

abstract class Flashlight(context: Context) : Torch(context) {
    companion object {
        const val TYPE: String = Constants.ID_DEVICE_OUTPUT_TORCH_FLASH
    }
}
