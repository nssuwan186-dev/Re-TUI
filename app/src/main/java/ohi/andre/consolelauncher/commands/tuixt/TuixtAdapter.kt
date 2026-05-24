package ohi.andre.consolelauncher.commands.tuixt

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import ohi.andre.consolelauncher.R
import ohi.andre.consolelauncher.commands.tuixt.TuixtDialog.ConfirmAction
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.accentColor
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.borderColor
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.dp
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.rect
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleButton
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleColorPreview
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleInput
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.styleToggle
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.surfaceColor
import ohi.andre.consolelauncher.commands.tuixt.TuixtTheme.textColor
import ohi.andre.consolelauncher.managers.settings.LauncherSettings.get
import ohi.andre.consolelauncher.managers.settings.LauncherSettings.set
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave
import ohi.andre.consolelauncher.managers.xml.options.Behavior
import ohi.andre.consolelauncher.tuils.Tuils
import java.io.File
import androidx.annotation.NonNull
import java.util.ArrayList
import java.util.HashMap
import java.util.Map
import ohi.andre.consolelauncher.managers.settings.LauncherSettings

class TuixtAdapter(items: MutableList<XMLPrefsSave>, private val file: File?) :
    RecyclerView.Adapter<TuixtAdapter.ViewHolder>() {
    private var items: MutableList<XMLPrefsSave>
    private val pendingChanges: MutableMap<XMLPrefsSave?, String?> =
        HashMap<XMLPrefsSave?, String?>()

    init {
        this.items = ArrayList<XMLPrefsSave>(items)
    }

    fun updateList(newList: MutableList<XMLPrefsSave>) {
        this.items = ArrayList<XMLPrefsSave>(newList)
        notifyDataSetChanged()
    }

    @JvmOverloads
    fun saveAll(context: Context? = null) {
        for (entry in pendingChanges.entries) {
            val item = entry.key
            val value = entry.value
            set(context, item, value)
        }
        pendingChanges.clear()
    }

    fun hasPendingChanges(): Boolean {
        return !pendingChanges.isEmpty()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.getContext()).inflate(R.layout.tuixt_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.get(position)
        holder.title.setText(item.label())
        holder.description.setText(item.info())

        val currentValue = getCurrentValue(item)

        holder.input.removeTextChangedListener(holder.textWatcher)
        holder.toggle.setOnClickListener(null)
        holder.colorPreview.setOnClickListener(null)
        holder.options.removeAllViews()
        holder.options.setVisibility(View.GONE)
        holder.itemView.setBackground(
            rect(
                holder.itemView.getContext(),
                surfaceColor(),
                borderColor(),
                1.25f
            )
        )
        holder.title.setTextColor(accentColor())
        holder.description.setTextColor(textColor())
        styleInput(holder.itemView.getContext(), holder.input)

        if (item === Behavior.output_tray_mode) {
            holder.toggle.setVisibility(View.GONE)
            holder.colorPreview.setVisibility(View.GONE)
            holder.input.setVisibility(View.GONE)
            holder.options.setVisibility(View.VISIBLE)
            bindOptionSwitch(holder, item, arrayOf<String>("native", "auto", "toggled"))
        } else if (item === Behavior.output_header_mode) {
            holder.toggle.setVisibility(View.GONE)
            holder.colorPreview.setVisibility(View.GONE)
            holder.input.setVisibility(View.GONE)
            holder.options.setVisibility(View.VISIBLE)
            bindOptionSwitch(holder, item, arrayOf<String>("normal", "arrows", "none"))
        } else if (XMLPrefsSave.BOOLEAN == item.type()) {
            holder.toggle.setVisibility(View.VISIBLE)
            holder.colorPreview.setVisibility(View.GONE)
            holder.input.setVisibility(View.GONE)
            val checked = currentValue.toBoolean()
            styleToggle(holder.itemView.getContext(), holder.toggle, checked)
            holder.toggle.setOnClickListener(View.OnClickListener { v: View? ->
                val next = !getCurrentValue(item).toBoolean()
                pendingChanges.put(item, next.toString())
                styleToggle(holder.itemView.getContext(), holder.toggle, next)
            })
        } else if (XMLPrefsSave.COLOR == item.type()) {
            holder.toggle.setVisibility(View.GONE)
            holder.colorPreview.setVisibility(View.VISIBLE)
            holder.input.setVisibility(View.VISIBLE)
            holder.input.setText(currentValue)
            updateColorPreview(holder.colorPreview, currentValue)

            holder.colorPreview.setOnClickListener(View.OnClickListener { v: View? ->
                showColorPicker(
                    holder,
                    item,
                    holder.input.getText().toString()
                )
            })

            holder.textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    val `val` = s.toString()
                    if (`val`.matches("^#[0-9A-Fa-f]{6,8}$".toRegex())) {
                        updateColorPreview(holder.colorPreview, `val`)
                        pendingChanges.put(item, `val`)
                    }
                }
            }
            holder.input.addTextChangedListener(holder.textWatcher)
        } else {
            holder.toggle.setVisibility(View.GONE)
            holder.colorPreview.setVisibility(View.GONE)
            holder.input.setVisibility(View.VISIBLE)
            holder.input.setText(currentValue)
            holder.textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable) {
                    pendingChanges.put(item, s.toString())
                }
            }
            holder.input.addTextChangedListener(holder.textWatcher)
        }
    }

    private fun bindOptionSwitch(holder: ViewHolder, item: XMLPrefsSave, options: Array<String>) {
        var current = getCurrentValue(item)
        if (current == null) {
            current = item.defaultValue()
        }
        current = current!!.trim { it <= ' ' }.lowercase()

        for (option in options) {
            val button = TextView(holder.itemView.getContext())
            val params = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            if (holder.options.getChildCount() > 0) {
                params.leftMargin = dp(holder.itemView.getContext(), 6f)
            }
            button.setLayoutParams(params)
            button.setText(option.uppercase())
            button.setGravity(Gravity.CENTER)
            button.setSingleLine(true)
            button.setPadding(
                dp(holder.itemView.getContext(), 6f),
                dp(holder.itemView.getContext(), 8f),
                dp(holder.itemView.getContext(), 6f),
                dp(holder.itemView.getContext(), 8f)
            )
            styleButton(holder.itemView.getContext(), button, option == current)
            button.setOnClickListener(View.OnClickListener { v: View? ->
                pendingChanges.put(item, option)
                notifyDataSetChanged()
            })
            holder.options.addView(button)
        }
    }

    private fun updateColorPreview(view: View, hex: String?) {
        try {
            styleColorPreview(view.getContext(), view, Color.parseColor(hex))
        } catch (e: Exception) {
            styleColorPreview(view.getContext(), view, Color.BLACK)
        }
    }

    private fun getCurrentValue(item: XMLPrefsSave?): String? {
        return if (pendingChanges.containsKey(item)) pendingChanges.get(item) else get(item)
    }

    private fun showColorPicker(holder: ViewHolder, item: XMLPrefsSave?, currentHex: String?) {
        val dialogView = LayoutInflater.from(holder.itemView.getContext())
            .inflate(R.layout.color_picker_dialog, null)
        styleColorPicker(dialogView)
        val preview = dialogView.findViewById<View>(R.id.color_preview)
        val seekAlpha = dialogView.findViewById<SeekBar>(R.id.seek_alpha)
        val seekHue = dialogView.findViewById<SeekBar>(R.id.seek_hue)
        val seekSat = dialogView.findViewById<SeekBar>(R.id.seek_sat)
        val seekVal = dialogView.findViewById<SeekBar>(R.id.seek_val)
        val hexText = dialogView.findViewById<TextView>(R.id.hex_preview)

        var initialColor: Int
        try {
            initialColor = Color.parseColor(currentHex)
        } catch (e: Exception) {
            initialColor = Color.WHITE
        }

        val hsv = FloatArray(3)
        Color.colorToHSV(initialColor, hsv)
        val alpha = Color.alpha(initialColor)

        seekAlpha.setProgress(alpha)
        seekHue.setProgress(hsv[0].toInt())
        seekSat.setProgress((hsv[1] * 100).toInt())
        seekVal.setProgress((hsv[2] * 100).toInt())

        val listener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val newHsv = floatArrayOf(
                    seekHue.getProgress().toFloat(),
                    seekSat.getProgress().toFloat() / 100f,
                    seekVal.getProgress().toFloat() / 100f
                )
                val newAlpha = seekAlpha.getProgress()
                val newColor = Color.HSVToColor(newAlpha, newHsv)
                preview.setBackgroundColor(newColor)
                val hex = String.format("#%08X", newColor)
                hexText.setText(hex)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        seekAlpha.setOnSeekBarChangeListener(listener)
        seekHue.setOnSeekBarChangeListener(listener)
        seekSat.setOnSeekBarChangeListener(listener)
        seekVal.setOnSeekBarChangeListener(listener)

        // Initial trigger
        listener.onProgressChanged(null, 0, false)

        TuixtDialog.showContent(
            holder.itemView.getContext(),
            "Pick Color",
            dialogView,
            "OK",
            "Cancel",
            ConfirmAction {
                val finalHex = hexText.getText().toString()
                holder.input.setText(finalHex)
                pendingChanges.put(item, finalHex)
            })
    }

    private fun styleColorPicker(dialogView: View) {
        val context = dialogView.getContext()
        dialogView.setBackgroundColor(Color.TRANSPARENT)

        val title = dialogView.findViewById<TextView?>(R.id.picker_title)
        if (title != null) {
            title.setVisibility(View.GONE)
        }

        val accent = borderColor()
        val text = textColor()
        tintSeekBar(dialogView.findViewById<SeekBar?>(R.id.seek_alpha), accent)
        tintSeekBar(dialogView.findViewById<SeekBar?>(R.id.seek_hue), accent)
        tintSeekBar(dialogView.findViewById<SeekBar?>(R.id.seek_sat), accent)
        tintSeekBar(dialogView.findViewById<SeekBar?>(R.id.seek_val), accent)

        stylePickerLabels(dialogView, text)
        val hexText = dialogView.findViewById<TextView?>(R.id.hex_preview)
        if (hexText != null) {
            hexText.setTextColor(text)
            hexText.setTypeface(Tuils.getTypeface(context), Typeface.BOLD)
            hexText.setBackground(rect(context, surfaceColor(), borderColor(), 1.25f))
            hexText.setPadding(
                dp(context, 8f),
                dp(context, 8f),
                dp(context, 8f),
                dp(context, 8f)
            )
        }
    }

    private fun stylePickerLabels(root: View?, textColor: Int) {
        if (root !is ViewGroup) {
            return
        }
        val group = root
        for (i in 0..<group.getChildCount()) {
            val child = group.getChildAt(i)
            if (child is TextView && child.getId() != R.id.hex_preview && child.getId() != R.id.picker_title) {
                val label = child
                label.setTextColor(textColor)
                label.setTypeface(Tuils.getTypeface(root.getContext()), Typeface.BOLD)
            } else {
                stylePickerLabels(child, textColor)
            }
        }
    }

    private fun tintSeekBar(seekBar: SeekBar?, color: Int) {
        if (seekBar == null) {
            return
        }
        val tint = ColorStateList.valueOf(color)
        seekBar.setProgressTintList(tint)
        seekBar.setThumbTintList(tint)
        seekBar.setProgressBackgroundTintList(ColorStateList.valueOf(surfaceColor()))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var title: TextView
        var description: TextView
        var toggle: TextView
        var options: LinearLayout
        var colorPreview: View
        var input: EditText
        var textWatcher: TextWatcher? = null

        init {
            title = itemView.findViewById<TextView>(R.id.setting_title)
            description = itemView.findViewById<TextView>(R.id.setting_description)
            toggle = itemView.findViewById<TextView>(R.id.setting_switch)
            options = itemView.findViewById<LinearLayout>(R.id.setting_options)
            colorPreview = itemView.findViewById<View>(R.id.setting_color_preview)
            input = itemView.findViewById<EditText>(R.id.setting_input)
        }
    }
}
