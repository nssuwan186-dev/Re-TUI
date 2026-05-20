package ohi.andre.consolelauncher.commands.tuixt;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.AliasManager;
import ohi.andre.consolelauncher.managers.PinnedShortcutManager;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.tuils.LauncherSystemUi;
import ohi.andre.consolelauncher.tuils.Tuils;

@TargetApi(Build.VERSION_CODES.O)
public class PinShortcutConfirmActivity extends Activity {

    private LauncherApps.PinItemRequest request;
    private ShortcutInfo shortcutInfo;
    private EditText aliasInput;
    private TextView aliasToggle;
    private boolean createAlias = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LauncherSystemUi.requestNoTitleIfFullscreen(this);
        super.onCreate(savedInstanceState);
        LauncherSystemUi.applyFullscreen(this);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O || !loadRequest()) {
            finishWithToast("Shortcut pin request unavailable.");
            return;
        }

        setContentView(buildContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LauncherSystemUi.applyFullscreen(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus) {
            LauncherSystemUi.applyFullscreen(this);
        }
    }

    @Override
    @android.annotation.SuppressLint("GestureBackNavigation")
    public void onBackPressed() {
        finish();
    }

    private boolean loadRequest() {
        LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if(launcherApps == null) return false;

        request = launcherApps.getPinItemRequest(getIntent());
        if(request == null || !request.isValid()) return false;
        if(request.getRequestType() != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) return false;

        shortcutInfo = request.getShortcutInfo();
        return shortcutInfo != null;
    }

    private View buildContent() {
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(TuixtTheme.overlayColor());
        screen.setFitsSystemWindows(true);
        FrameLayout contentHost = TuixtLayout.addFoldAwareHost(this, screen, ViewGroup.LayoutParams.MATCH_PARENT);

        FrameLayout panelShell = new FrameLayout(this);
        panelShell.setClipChildren(false);
        panelShell.setClipToPadding(false);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(TuixtTheme.dp(this, 14), TuixtTheme.dp(this, 46), TuixtTheme.dp(this, 14), TuixtTheme.dp(this, 14));
        TuixtTheme.stylePanel(this, root);

        FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        panelParams.gravity = Gravity.CENTER;
        panelParams.setMargins(TuixtTheme.dp(this, 28), TuixtTheme.dp(this, 28), TuixtTheme.dp(this, 28), TuixtTheme.dp(this, 28));
        contentHost.addView(panelShell, panelParams);

        FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        rootParams.topMargin = TuixtTheme.dp(this, 11);
        panelShell.addView(root, rootParams);

        TextView header = new TextView(this);
        header.setText("Pin Shortcut");
        TuixtTheme.styleHeader(this, header);
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        headerParams.gravity = Gravity.TOP | Gravity.START;
        headerParams.leftMargin = TuixtTheme.dp(this, 38);
        panelShell.addView(header, headerParams);

        TextView title = terminalText(labelFor(shortcutInfo));
        title.setTextSize(16);
        root.addView(title, blockParams());

        TextView source = terminalText("From " + shortcutInfo.getPackage());
        source.setTextColor(ColorUtils.setAlphaComponent(TuixtTheme.textColor(), 190));
        root.addView(source, blockParams());

        aliasInput = new EditText(this);
        TuixtTheme.styleInput(this, aliasInput);
        aliasInput.setSingleLine(true);
        aliasInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        aliasInput.setText(PinnedShortcutManager.defaultHandle(shortcutInfo.getShortLabel()));
        aliasInput.setSelectAllOnFocus(true);
        root.addView(aliasInput, blockParams());

        LinearLayout toggleRow = new LinearLayout(this);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = terminalText("Create alias");
        toggleRow.addView(label);

        View spacer = new View(this);
        toggleRow.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        aliasToggle = new TextView(this);
        updateAliasToggle();
        aliasToggle.setOnClickListener(v -> {
            createAlias = !createAlias;
            updateAliasToggle();
        });
        toggleRow.addView(aliasToggle);
        root.addView(toggleRow, blockParams());

        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setGravity(Gravity.CENTER_VERTICAL);
        bottomBar.setPadding(0, TuixtTheme.dp(this, 8), 0, 0);

        TextView cancel = button("CANCEL", false);
        cancel.setOnClickListener(v -> finish());
        bottomBar.addView(cancel);

        View bottomSpacer = new View(this);
        bottomBar.addView(bottomSpacer, new LinearLayout.LayoutParams(0, 1, 1));

        TextView pin = button("PIN", true);
        pin.setOnClickListener(v -> acceptAndClose());
        bottomBar.addView(pin);
        root.addView(bottomBar);

        return screen;
    }

    private void acceptAndClose() {
        if(request == null || !request.isValid()) {
            finishWithToast("Shortcut pin request expired.");
            return;
        }

        String handle = PinnedShortcutManager.normalizeHandle(aliasInput.getText().toString());
        if(handle.length() == 0) {
            handle = PinnedShortcutManager.defaultHandle(shortcutInfo.getShortLabel());
        }

        boolean accepted;
        try {
            accepted = request.accept();
        } catch (Exception e) {
            Tuils.log(e);
            accepted = false;
        }

        if(!accepted) {
            finishWithToast("Shortcut pin request was cancelled.");
            return;
        }

        try {
            PinnedShortcutManager.save(this, handle, shortcutInfo, labelFor(shortcutInfo));
            if(createAlias) {
                addAlias(handle, "shortcut -use @" + handle);
            }
            Toast.makeText(this, "Pinned @" + handle, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Tuils.log(e);
            finishWithToast("Pinned, but mapping save failed.");
        }
    }

    private void addAlias(String name, String value) {
        Intent intent = new Intent(AliasManager.ACTION_ADD);
        intent.putExtra(AliasManager.NAME, name);
        intent.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, value);
        boolean delivered = LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        if(delivered) return;

        AliasManager manager = new AliasManager(getApplicationContext());
        try {
            manager.add(getApplicationContext(), name, value);
        } finally {
            manager.dispose();
        }
    }

    private void updateAliasToggle() {
        TuixtTheme.styleToggle(this, aliasToggle, createAlias);
    }

    private TextView terminalText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(TuixtTheme.textColor());
        view.setTypeface(Tuils.getTypeface(this), android.graphics.Typeface.BOLD);
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    private TextView button(String label, boolean primary) {
        TextView view = new TextView(this);
        view.setText(label);
        TuixtTheme.styleButton(this, view, primary);
        return view;
    }

    private LinearLayout.LayoutParams blockParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, TuixtTheme.dp(this, 10));
        return params;
    }

    private String labelFor(ShortcutInfo info) {
        CharSequence shortLabel = info.getShortLabel();
        if(shortLabel != null && shortLabel.length() > 0) return shortLabel.toString();
        CharSequence longLabel = info.getLongLabel();
        if(longLabel != null && longLabel.length() > 0) return longLabel.toString();
        return info.getId();
    }

    private void finishWithToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
