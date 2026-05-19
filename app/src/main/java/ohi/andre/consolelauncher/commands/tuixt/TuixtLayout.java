package ohi.andre.consolelauncher.commands.tuixt;

import android.content.Context;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import ohi.andre.consolelauncher.UIManager;
import ohi.andre.consolelauncher.managers.settings.LauncherSettings;
import ohi.andre.consolelauncher.managers.xml.options.Ui;
import ohi.andre.consolelauncher.tuils.Tuils;

final class TuixtLayout {

    private TuixtLayout() {
    }

    static FrameLayout addFoldAwareHost(Context context, FrameLayout screen, int height) {
        int gutterWidth = getLandscapeGutterWidth(context);
        if (gutterWidth <= 0) {
            return screen;
        }

        LinearLayout splitHost = new LinearLayout(context);
        splitHost.setOrientation(LinearLayout.HORIZONTAL);
        splitHost.setClipChildren(false);
        splitHost.setClipToPadding(false);
        screen.addView(splitHost, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                height));

        int childHeight = height == ViewGroup.LayoutParams.MATCH_PARENT
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        FrameLayout leftPane = createPane(context);
        FrameLayout rightPane = createPane(context);
        View gutter = new View(context);

        splitHost.addView(leftPane, new LinearLayout.LayoutParams(
                0,
                childHeight,
                1));
        splitHost.addView(gutter, new LinearLayout.LayoutParams(
                gutterWidth,
                childHeight));
        splitHost.addView(rightPane, new LinearLayout.LayoutParams(
                0,
                childHeight,
                1));

        return UIManager.DUO_LAYOUT_LEFT.equals(UIManager.resolveSavedDuoSide(context))
                ? leftPane
                : rightPane;
    }

    private static FrameLayout createPane(Context context) {
        FrameLayout pane = new FrameLayout(context);
        pane.setClipChildren(false);
        pane.setClipToPadding(false);
        return pane;
    }

    private static int getLandscapeGutterWidth(Context context) {
        if (context == null || context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
            return 0;
        }

        int gutterMm = Math.max(0, Math.min(LauncherSettings.getInt(Ui.landscape_fold_gutter_mm), UIManager.MAX_LANDSCAPE_FOLD_GUTTER_MM));
        if (gutterMm == 0) {
            return 0;
        }

        int gutterPx = Tuils.mmToPx(context.getResources().getDisplayMetrics(), gutterMm);
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        if (screenWidth > 0) {
            gutterPx = Math.min(gutterPx, screenWidth / 3);
        }
        return gutterPx;
    }
}
