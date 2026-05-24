package ohi.andre.consolelauncher.managers.flashlight

import android.content.Context
import android.os.Build

class TorchManager private constructor() {
    private val flashType: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) Flashlight2.TYPE else Flashlight.TYPE
    private var mTorch: Torch? = null
    private var torchType: String? = null

    init {
        setTorchType(Constants.ID_DEVICE_OUTPUT_TORCH_FLASH)
    }

    fun isOn(): Boolean = mTorch != null

    fun setTorchType(torchType: String?) {
        this.torchType = torchType
    }

    fun turnOn(context: Context) {
        if (mTorch == null) {
            if (torchType == Flashlight.TYPE) {
                mTorch = if (flashType == Flashlight.TYPE) {
                    Flashlight1(context)
                } else if (flashType == Flashlight2.TYPE) {
                    Flashlight2(context)
                } else {
                    null
                }
            }
        }
        mTorch!!.setEnabled(true)
        mTorch!!.start(true)
    }

    fun turnOff() {
        if (mTorch != null) {
            mTorch!!.start(false)
            mTorch = null
        }
    }

    fun toggle(context: Context) {
        if (mTorch == null) {
            turnOn(context)
        } else if (mTorch!!.getStatus()) {
            turnOff()
        } else {
            turnOn(context)
        }
    }

    companion object {
        private var mInstance: TorchManager? = null

        @JvmStatic
        fun getInstance(): TorchManager {
            if (mInstance == null) {
                mInstance = TorchManager()
            }
            return mInstance!!
        }
    }
}
