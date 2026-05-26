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
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import org.w3c.dom.Node

class TuixtActivity : Activity() {
    private var file: File? = null
    private var recyclerView: RecyclerView? = null
    private var adapter: TuixtAdapter? = null
    private var xmlRoot: XMLPrefsRoot? = null
    private var originalRows: MutableList<TuixtAdapter.SettingsRow>? = null
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
            originalRows = buildRows(xmlRoot!!, file!!)
            adapter = TuixtAdapter(originalRows!!.toMutableList(), file)
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
        val rows = originalRows ?: return
        if (query.trim().isEmpty()) {
            adapter!!.updateRows(rows.toMutableList())
            return
        }

        val filtered: MutableList<TuixtAdapter.SettingsRow> = ArrayList()
        var pendingSection: TuixtAdapter.SettingsRow? = null
        val lower = query.lowercase(Locale.getDefault())
        for (row in rows) {
            if (row.sectionHeader) {
                pendingSection = row
                continue
            }

            val item = row.item ?: continue
            val label = item.label()!!.lowercase(Locale.getDefault())
            val info = item.info()!!.lowercase(Locale.getDefault())
            if (label.contains(lower) || info.contains(lower)) {
                if (pendingSection != null && (filtered.isEmpty() || filtered.last().section != pendingSection.section)) {
                    filtered.add(pendingSection)
                }
                filtered.add(row)
            }
        }
        adapter!!.updateRows(filtered)
    }

    private fun buildRows(root: XMLPrefsRoot, source: File): MutableList<TuixtAdapter.SettingsRow> {
        val remaining: LinkedHashMap<String, XMLPrefsSave> = LinkedHashMap<String, XMLPrefsSave>()
        for (save in root.enums) {
            if (save === Behavior.toggle_output_state) {
                continue
            }
            remaining[save.label()!!] = save
        }

        val groupedRows: LinkedHashMap<String, MutableList<XMLPrefsSave>> =
            LinkedHashMap<String, MutableList<XMLPrefsSave>>()
        try {
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(source)
            val xmlRoot = doc.documentElement
            val children = xmlRoot.childNodes
            if (!hasSectionComments(children)) {
                val rows: MutableList<TuixtAdapter.SettingsRow> = ArrayList()
                addDefaultSectionRows(rows, remaining)
                return rows
            }

            var activeSection: String? = null
            for (index in 0..<children.length) {
                val node = children.item(index)
                if (node.nodeType == Node.COMMENT_NODE) {
                    val section = parseSectionComment(node.nodeValue)
                    if (section != null) {
                        activeSection = section
                    }
                } else if (node.nodeType == Node.ELEMENT_NODE) {
                    val element = node as Element
                    val save = remaining.remove(element.nodeName) ?: continue
                    val section = activeSection ?: XMLPrefsManager.sectionFor(save)
                    addGroupedRow(groupedRows, section, save)
                }
            }
        } catch (ignored: Exception) {
        }

        for (save in remaining.values) {
            val section = XMLPrefsManager.sectionFor(save)
            addGroupedRow(groupedRows, section, save)
        }

        return flattenGroupedRows(groupedRows)
    }

    private fun addDefaultSectionRows(
        rows: MutableList<TuixtAdapter.SettingsRow>,
        remaining: LinkedHashMap<String, XMLPrefsSave>
    ) {
        val groupedRows: LinkedHashMap<String, MutableList<XMLPrefsSave>> =
            LinkedHashMap<String, MutableList<XMLPrefsSave>>()
        val iterator = remaining.entries.iterator()
        while (iterator.hasNext()) {
            val save = iterator.next().value
            val section = XMLPrefsManager.sectionFor(save)
            addGroupedRow(groupedRows, section, save)
            iterator.remove()
        }
        rows.addAll(flattenGroupedRows(groupedRows))
    }

    private fun hasSectionComments(children: org.w3c.dom.NodeList): Boolean {
        for (index in 0..<children.length) {
            val node = children.item(index)
            if (node.nodeType == Node.COMMENT_NODE && parseSectionComment(node.nodeValue) != null) {
                return true
            }
        }
        return false
    }

    private fun parseSectionComment(raw: String?): String? {
        val value = raw?.trim() ?: return null
        if (!value.startsWith("#")) {
            return null
        }
        val label = value.removePrefix("#").trim()
        return if (label.isEmpty()) null else label
    }

    private fun addSection(rows: MutableList<TuixtAdapter.SettingsRow>, section: String) {
        if (rows.isNotEmpty() && rows.last().section == section) {
            return
        }
        rows.add(TuixtAdapter.SettingsRow.section(section))
    }

    private fun addGroupedRow(
        rows: LinkedHashMap<String, MutableList<XMLPrefsSave>>,
        section: String,
        save: XMLPrefsSave
    ) {
        var bucket = rows[section]
        if (bucket == null) {
            bucket = ArrayList<XMLPrefsSave>()
            rows[section] = bucket
        }
        bucket.add(save)
    }

    private fun flattenGroupedRows(
        groupedRows: LinkedHashMap<String, MutableList<XMLPrefsSave>>
    ): MutableList<TuixtAdapter.SettingsRow> {
        val rows: MutableList<TuixtAdapter.SettingsRow> = ArrayList()
        for (entry in groupedRows.entries) {
            val section = entry.key
            addSection(rows, section)
            for (save in entry.value) {
                rows.add(TuixtAdapter.SettingsRow.setting(save, section))
            }
        }
        return rows
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
