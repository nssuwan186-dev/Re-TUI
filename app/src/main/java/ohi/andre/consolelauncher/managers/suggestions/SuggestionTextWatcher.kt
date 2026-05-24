package ohi.andre.consolelauncher.managers.suggestions

import android.text.Editable
import android.text.TextWatcher
import ohi.andre.consolelauncher.tuils.interfaces.OnTextChanged

class SuggestionTextWatcher(
    private val suggestionsManager: SuggestionsManager,
    private val textChanged: OnTextChanged
) : TextWatcher {
    private var before = Int.MIN_VALUE

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {
        val text = s?.toString().orEmpty()
        suggestionsManager.requestSuggestion(text)
        textChanged.textChanged(text, before)
        before = s?.length ?: 0
    }

    override fun afterTextChanged(s: Editable?) {}
}
