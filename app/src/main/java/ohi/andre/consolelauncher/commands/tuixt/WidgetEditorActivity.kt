package ohi.andre.consolelauncher.commands.tuixt

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ohi.andre.consolelauncher.UIManager
import ohi.andre.consolelauncher.commands.tuixt.TuixtDialog.ConfirmAction
import ohi.andre.consolelauncher.commands.tuixt.TuixtDialog.showConfirm
import ohi.andre.consolelauncher.commands.tuixt.TuixtLayout.addFoldAwareHost
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.dp
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.overlayColor
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleButton
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.stylePanel
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.textColor
import ohi.andre.consolelauncher.managers.modules.ModuleManager
import ohi.andre.consolelauncher.managers.settings.LauncherSettings.refreshFromLoadedPrefs
import ohi.andre.consolelauncher.managers.widgets.LuaWidgetManager
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager
import ohi.andre.consolelauncher.tuils.LauncherSystemUi.applyFullscreen
import ohi.andre.consolelauncher.tuils.LauncherSystemUi.requestNoTitleIfFullscreen
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.File
import java.io.FileInputStream
import java.util.Arrays
import ohi.andre.consolelauncher.managers.settings.LauncherSettings
import ohi.andre.consolelauncher.tuils.LauncherSystemUi

class WidgetEditorActivity : Activity() {
    private var widgetMode = true
    private var widgetId: String? = null
    private var documentFile: File? = null
    private var header: TextView? = null
    private var documentNameEditor: EditText? = null
    private var codeEditor: EditText? = null
    private var capabilityView: TextView? = null
    private var originalDocumentName: String? = ""
    private var originalCode: String? = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        requestNoTitleIfFullscreen(this)
        super.onCreate(savedInstanceState)
        applyFullscreen(this)

        val intent = getIntent()
        val filePath = intent.getStringExtra(EXTRA_FILE_PATH)
        widgetMode = TextUtils.isEmpty(filePath)

        if (widgetMode) {
            widgetId = LuaWidgetManager.normalizeId(intent.getStringExtra(EXTRA_WIDGET_ID))
            if (TextUtils.isEmpty(widgetId)) {
                finish()
                return
            }

            originalDocumentName = LuaWidgetManager.getName(widgetId)
            originalCode = LuaWidgetManager.readScript(widgetId)
            if (TextUtils.isEmpty(originalCode)) {
                originalCode = LuaWidgetManager.newWidgetTemplate(widgetId)
            }
        } else {
            documentFile = File(filePath)
            if (documentFile!!.isDirectory()) {
                finish()
                return
            }
            originalDocumentName = documentFile!!.getName()
            try {
                FileInputStream(documentFile).use { `in` ->
                    originalCode = Tuils.convertStreamToString(`in`)
                }
            } catch (e: Exception) {
                originalCode = ""
            }
        }

        if (TextUtils.isEmpty(originalDocumentName)) {
            originalDocumentName = if (widgetMode) widgetId else "Document"
        }

        if (widgetMode && TextUtils.isEmpty(originalCode)) {
            originalCode = LuaWidgetManager.newWidgetTemplate(widgetId)
        }

        if (!widgetMode && documentFile == null) {
            finish()
            return
        }

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

        header = TextView(this)
        updateHeader()
        TuixtTheme.styleHeader(this, header!!)
        val headerParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        headerParams.gravity = Gravity.TOP or Gravity.START
        headerParams.leftMargin = panelLeft + dp(this, 38f)
        headerParams.topMargin = panelTop - dp(this, 11f)
        contentHost.addView(header, headerParams)

        documentNameEditor = EditText(this)
        documentNameEditor!!.setSingleLine(true)
        documentNameEditor!!.setHint(if (widgetMode) "Document name" else "File name")
        documentNameEditor!!.setText(originalDocumentName)
        if (!widgetMode) {
            documentNameEditor!!.setFocusable(false)
            documentNameEditor!!.setFocusableInTouchMode(false)
            documentNameEditor!!.setCursorVisible(false)
        }
        TuixtTheme.styleInput(this, documentNameEditor!!)
        val nameParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        nameParams.setMargins(0, 0, 0, dp(this, 10f))
        root.addView(documentNameEditor, nameParams)

        capabilityView = TextView(this)
        capabilityView!!.setSingleLine(false)
        capabilityView!!.setTextSize(11f)
        capabilityView!!.setTypeface(Tuils.getTypeface(this))
        capabilityView!!.setTextColor(textColor())
        capabilityView!!.setAlpha(if (widgetMode) 0.82f else 0.55f)
        val capabilityParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        capabilityParams.setMargins(0, 0, 0, dp(this, 10f))
        root.addView(capabilityView, capabilityParams)

        codeEditor = EditText(this)
        codeEditor!!.setGravity(Gravity.TOP or Gravity.START)
        codeEditor!!.setSingleLine(false)
        codeEditor!!.setHorizontallyScrolling(true)
        codeEditor!!.setTypeface(Typeface.MONOSPACE)
        codeEditor!!.setTextSize(13f)
        codeEditor!!.setText(originalCode)
        TuixtTheme.styleInput(this, codeEditor!!)
        root.addView(
            codeEditor, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )
        codeEditor!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCapabilityPreview()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        documentNameEditor!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCapabilityPreview()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
        updateCapabilityPreview()

        val bottomBar = LinearLayout(this)
        bottomBar.setOrientation(LinearLayout.HORIZONTAL)
        bottomBar.setGravity(Gravity.CENTER_VERTICAL)
        bottomBar.setPadding(0, dp(this, 10f), 0, 0)

        val cancel = button("CANCEL", false)
        cancel.setOnClickListener(View.OnClickListener { v: View? -> attemptClose() })
        bottomBar.addView(cancel)

        val spacer = View(this)
        bottomBar.addView(spacer, LinearLayout.LayoutParams(0, 1, 1f))

        val save = button("SAVE", false)
        save.setOnClickListener(View.OnClickListener { v: View? -> save(false) })
        bottomBar.addView(save)

        if (widgetMode) {
            val run = button("SAVE/RUN", true)
            run.setOnClickListener(View.OnClickListener { v: View? -> save(true) })
            bottomBar.addView(run)
        }

        root.addView(bottomBar)
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

    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        attemptClose()
    }

    private fun button(label: String?, primary: Boolean): TextView {
        val view = TextView(this)
        view.setText(label)
        styleButton(this, view, primary)
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(dp(this, 6f), 0, 0, 0)
        view.setLayoutParams(params)
        return view
    }

    private fun save(run: Boolean) {
        try {
            val code = codeEditor!!.getText().toString()

            if (!widgetMode) {
                Tuils.write(documentFile, "", code)
                XMLPrefsManager.dispose()
                XMLPrefsManager.loadCommons(this)
                refreshFromLoadedPrefs()
                setResult(TuixtActivity.SAVE_PRESSED)
                finish()
                return
            }

            val name = documentNameEditor!!.getText().toString().trim { it <= ' ' }
            require(!TextUtils.isEmpty(name)) { "Document name is required" }
            if (!TextUtils.equals(originalDocumentName, name)) {
                val newId = LuaWidgetManager.idFromName(name)
                require(!TextUtils.isEmpty(newId)) { "Document name needs letters or numbers" }
                if (!TextUtils.equals(widgetId, newId)) {
                    require(
                        !(ModuleManager.isKnown(
                            this,
                            newId
                        ) || LuaWidgetManager.exists(newId))
                    ) { "Widget id already exists: " + newId }
                    LuaWidgetManager.rename(widgetId, newId)
                    ModuleManager.renameScriptModule(
                        this,
                        widgetId,
                        newId,
                        LuaWidgetManager.SOURCE_PREFIX + newId
                    )
                    widgetId = newId
                }
            }

            LuaWidgetManager.save(widgetId, name, code)
            val dockable = LuaWidgetManager.isDockableScript(code)
            if (dockable) {
                ModuleManager.setScriptModule(
                    this,
                    widgetId,
                    LuaWidgetManager.SOURCE_PREFIX + widgetId
                )
                ModuleManager.addToDock(this, Arrays.asList<String?>(widgetId))
            } else {
                ModuleManager.removeScriptModule(this, widgetId)
            }
            sendModule("rebuild")
            if (run && dockable) {
                sendModule("show")
                sendModule("refresh")
                finish()
                return
            } else if (run) {
                Toast.makeText(this, "Suggestion script saved: " + name, Toast.LENGTH_SHORT).show()
            }

            originalDocumentName = LuaWidgetManager.getName(widgetId)
            originalCode = code
            documentNameEditor!!.setText(originalDocumentName)
            updateHeader()
            Toast.makeText(this, "Document saved: " + originalDocumentName, Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: " + e.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun sendModule(command: String?) {
        val intent = Intent(UIManager.ACTION_MODULE_COMMAND)
        intent.putExtra(UIManager.EXTRA_MODULE_COMMAND, command)
        intent.putExtra(UIManager.EXTRA_MODULE_NAME, widgetId)
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent)
    }

    private fun updateHeader() {
        if (header != null) {
            header!!.setText(if (widgetMode) "Widgets" else "Documents")
        }
    }

    private fun updateCapabilityPreview() {
        if (capabilityView == null) {
            return
        }
        if (!widgetMode) {
            capabilityView!!.setText("Document editor")
            return
        }

        val code = if (codeEditor == null) "" else codeEditor!!.getText().toString()
        val meta = LuaWidgetManager.metadata(code)
        val type = LuaWidgetManager.getScriptTypeFromScript(code)
        val capabilities = LuaWidgetManager.describeCapabilities(code)
        val permissions = LuaWidgetManager.describeRequiredPermissions(code)
        val api = LuaWidgetManager.apiVersionFromScript(code)
        val name = if (documentNameEditor == null) "" else documentNameEditor!!.getText().toString()
            .trim { it <= ' ' }
        val id = LuaWidgetManager.idFromName(name)
        capabilityView!!.setText(
            ("Type: " + type
                    + "  |  ID: " + (if (TextUtils.isEmpty(id)) "n/a" else id)
                    + "  |  API: " + api
                    + "  |  Capabilities: " + capabilities
                    + "  |  Permissions: " + permissions
                    + capabilityWarning(meta, code))
        )
    }

    private fun capabilityWarning(meta: MutableMap<String?, String?>, code: String?): String {
        val declared: String = meta["permissions"] ?: ""
        val missing = LuaWidgetManager.missingPermissionDeclarations(code)
        val unsupported = LuaWidgetManager.unsupportedPermissions(code)
        if (!unsupported.isEmpty()) {
            return "  |  Unsupported: " + TextUtils.join(", ", unsupported)
        }
        if (!missing.isEmpty()) {
            return "  |  Declare: " + TextUtils.join(", ", missing)
        }
        if (TextUtils.isEmpty(declared)) {
            return "  |  Metadata: inferred"
        }
        return "  |  Metadata: " + declared
    }

    private fun attemptClose() {
        if (!hasUnsavedChanges()) {
            finish()
            return
        }
        showConfirm(
            this,
            "Discard Changes?",
            "Unsaved document changes will be lost.",
            "Discard",
            "Keep Editing",
            ConfirmAction { this.finish() })
    }

    private fun hasUnsavedChanges(): Boolean {
        return !TextUtils.equals(
            originalDocumentName,
            documentNameEditor!!.getText().toString().trim { it <= ' ' })
                || !TextUtils.equals(originalCode, codeEditor!!.getText().toString())
    }

    companion object {
        const val EXTRA_WIDGET_ID: String = "widget_id"
        const val EXTRA_FILE_PATH: String = "file_path"
    }
}
