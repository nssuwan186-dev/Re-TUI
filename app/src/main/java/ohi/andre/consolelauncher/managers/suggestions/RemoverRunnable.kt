package ohi.andre.consolelauncher.managers.suggestions

import android.widget.LinearLayout

class RemoverRunnable(@JvmField var suggestionsView: LinearLayout) : Runnable {
    @JvmField var stop = false
    @JvmField var isGoingToRun = false

    override fun run() {
        if (stop) {
            stop = false
        } else {
            suggestionsView.removeAllViews()
        }
        isGoingToRun = false
    }
}
