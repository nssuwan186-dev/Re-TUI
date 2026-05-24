package ohi.andre.consolelauncher.commands.tuixt

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ohi.andre.consolelauncher.commands.tuixt.TuixtDialog.ConfirmAction
import ohi.andre.consolelauncher.commands.tuixt.TuixtLayout.addFoldAwareHost
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.borderColor
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.dp
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.overlayColor
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.rect
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleButton
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleHeader
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleInput
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.stylePanel
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.surfaceColor
import ohi.andre.consolelauncher.managers.settings.LauncherSettings.refreshFromLoadedPrefs
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.XMLPrefsRoot
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave
import ohi.andre.consolelauncher.managers.xml.options.Behavior
import ohi.andre.consolelauncher.tuils.LauncherSystemUi.applyFullscreen
import ohi.andre.consolelauncher.tuils.LauncherSystemUi.requestNoTitleIfFullscreen
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import android.content.Intent
import java.util.ArrayList
import ohi.andre.consolelauncher.managers.settings.LauncherSettings
import ohi.andre.consolelauncher.tuils.LauncherSystemUi

class TuixtActivity : Activity() {
    private var file: File? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: TuixtAdapter? = null
    private var xmlRoot: XMLPrefsRoot? = null
    private var originalItems: MutableList<XMLPrefsSave>? = null
    private var plainTextEditor: EditText? = null
    private var originalRawText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        requestNoTitleIfFullscreen(this)
        super.onCreate(savedInstanceState)
        applyFullscreen(this)

        val intent = getIntent()
        val path = intent.getStringExtra(PATH)
        if (path == null) {
            finish()
            return
        }
        file = File(path)

        val screen = FrameLayout(this)
        screen.setBackgroundColor(overlayColor())
        screen.setFitsSystemWindows(true)
        val contentHost = addFoldAwareHost(this, screen, ViewGroup.LayoutParams.MATCH_PARENT)

        val root = LinearLayout(this)
        root.setOrientation(LinearLayout.VERTICAL)
        root.setPadding(dp(this, 14f), dp(this, 50f), dp(this, 14f), dp(this, 14f))
        stylePanel(this, root)

        val panelLeft = dp(this, 28f)
        val panelTop = dp(this, 34f)
        val panelParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        panelParams.setMargins(panelLeft, panelTop, dp(this, 28f), dp(this, 28f))
        contentHost.addView(root, panelParams)

        val header = TextView(this)
        header.setText("Themer/ " + file!!.getName())
        styleHeader(this, header)
        val headerParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        headerParams.gravity = Gravity.TOP or Gravity.START
        headerParams.leftMargin = panelLeft + dp(this, 38f)
        headerParams.topMargin = panelTop - dp(this, 11f)
        contentHost.addView(header, headerParams)

        // RecyclerView
        recyclerView = RecyclerView(this)
        recyclerView!!.setLayoutParams(
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        recyclerView!!.setLayoutManager(LinearLayoutManager(this))
        root.addView(recyclerView)

        // Bottom Bar (Search + Buttons)
        val bottomBar = LinearLayout(this)
        bottomBar.setOrientation(LinearLayout.VERTICAL)
        bottomBar.setPadding(10, 10, 10, 10)
        bottomBar.setBackground(rect(this, surfaceColor(), borderColor(), 1.25f))

        // Search Box
        val searchBox = EditText(this)
        searchBox.setHint("Search settings...")
        styleInput(this, searchBox)
        bottomBar.addView(searchBox)

        // Action Buttons
        val btnLayout = LinearLayout(this)
        btnLayout.setOrientation(LinearLayout.HORIZONTAL)
        btnLayout.setPadding(0, 10, 0, 0)

        val btnCancel = TextView(this)
        btnCancel.setText("CANCEL")
        styleButton(this, btnCancel, false)
        btnCancel.setOnClickListener(View.OnClickListener { v: View? -> attemptClose() })
        btnLayout.addView(btnCancel)

        val spacer = View(this)
        spacer.setLayoutParams(LinearLayout.LayoutParams(0, 1, 1f))
        btnLayout.addView(spacer)

        val btnSave = TextView(this)
        btnSave.setText("SAVE")
        styleButton(this, btnSave, true)
        btnSave.setOnClickListener(View.OnClickListener { v: View? ->
            Toast.makeText(this, "Applying changes...", Toast.LENGTH_SHORT).show()
            if (adapter != null) {
                adapter!!.saveAll(this)
            } else if (plainTextEditor != null) {
                try {
                    Tuils.write(file, "", plainTextEditor!!.getText().toString())
                    XMLPrefsManager.dispose()
                    XMLPrefsManager.loadCommons(this)
                    refreshFromLoadedPrefs()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error saving: " + e.message, Toast.LENGTH_LONG).show()
                }
            }
            setResult(SAVE_PRESSED)
            finish()
        })
        btnLayout.addView(btnSave)

        bottomBar.addView(btnLayout)
        root.addView(bottomBar)

        // Load data
        val fileName = file!!.getName().lowercase(Locale.getDefault())
        for (rootEnum in XMLPrefsRoot.entries.toTypedArray()) {
            if (fileName == rootEnum.path()) {
                xmlRoot = rootEnum
                break
            }
        }

        if (xmlRoot != null) {
            originalItems = ArrayList<XMLPrefsSave>(xmlRoot!!.enums)
            originalItems!!.remove(Behavior.toggle_output_state)
            adapter = TuixtAdapter(originalItems!!.toMutableList(), file)
            recyclerView!!.setAdapter(adapter)

            searchBox.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    filter(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            })
        } else {
            recyclerView!!.setVisibility(View.GONE)
            searchBox.setVisibility(View.GONE)

            plainTextEditor = EditText(this)
            plainTextEditor!!.setLayoutParams(
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            )
            plainTextEditor!!.setGravity(Gravity.TOP)
            TuixtTheme.styleInput(this, plainTextEditor!!)

            try {
                val fis = FileInputStream(file)
                originalRawText = Tuils.convertStreamToString(fis)
                plainTextEditor!!.setText(originalRawText)
                fis.close()
            } catch (e: Exception) {
                originalRawText = ""
                plainTextEditor!!.setText("")
            }

            root.addView(plainTextEditor, 2)
        }

        setContentView(screen)
    }

    override fun onResume() {
        super.onResume()
        applyFullscreen(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyFullscreen(this)
        }
    }

    private fun filter(query: String) {
        val filtered: MutableList<XMLPrefsSave> = ArrayList()
        for (item in originalItems!!) {
            if (item.label()!!.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault())) ||
                item.info()!!.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            ) {
                filtered.add(item)
            }
        }
        adapter!!.updateList(filtered)
    }

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        attemptClose()
    }

    private fun attemptClose() {
        if (!hasUnsavedChanges()) {
            setResult(BACK_PRESSED)
            finish()
            return
        }

        TuixtDialog.showConfirm(
            this,
            "Discard Changes?",
            "Unsaved settings changes will be lost.",
            "Discard",
            "Keep Editing",
            ConfirmAction {
                setResult(BACK_PRESSED)
                finish()
            })
    }

    private fun hasUnsavedChanges(): Boolean {
        if (adapter != null && adapter!!.hasPendingChanges()) {
            return true
        }
        if (plainTextEditor != null) {
            return !TextUtils.equals(originalRawText, plainTextEditor!!.getText().toString())
        }
        return false
    }

    companion object {
        const val PATH: String = "path"
        const val ERROR_KEY: String = "error"
        const val BACK_PRESSED: Int = 2
        const val SAVE_PRESSED: Int = 3
    }
}
