package ohi.andre.consolelauncher.managers.notifications.reply;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsList;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsEntry;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;
import ohi.andre.consolelauncher.managers.xml.options.Reply;
import ohi.andre.consolelauncher.tuils.PrivateIOReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;

import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.VALUE_ATTRIBUTE;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.set;
import static ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.writeTo;

/**
 * Created by francescoandreuzzi on 17/01/2018.
 */

public class ReplyManager implements XMLPrefsElement {

    public static String PATH = "reply.xml";
    public static String NAME = "REPLY";
    public static String ACTION = BuildConfig.APPLICATION_ID + ".reply";
    public static String ID = "id";
    public static String WHAT = "what";
    public static String ACTION_UPDATE = BuildConfig.APPLICATION_ID + ".update";
    public static String ACTION_LS = BuildConfig.APPLICATION_ID + ".lsreplies";

    private static final String ID_ATTRIBUTE = "id";

    private Set<NotificationWear> notificationWears;
    public static List<BoundApp> boundApps;

    private BroadcastReceiver receiver;

    private static WeakReference<ReplyManager> instance;
    private XMLPrefsList values;

    private boolean enabled;

    private Context context;

    public static int nextUsableId;

    @Override
    public String path() {
        return PATH;
    }

    public ReplyManager(Context context) {
        enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH;
        if(!enabled) return;

        notificationWears = new HashSet<>();
        values = new XMLPrefsList();
        this.context = context.getApplicationContext();

        instance = new WeakReference<>(this);

        load(true);

        XMLPrefsEntry enabledEntry = values.get(Reply.reply_enabled);
        enabled = enabledEntry != null && Boolean.parseBoolean(enabledEntry.value);
        if(!enabled) {
            notificationWears = null;
            boundApps = null;
        } else {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION);
            filter.addAction(ACTION_UPDATE);
            filter.addAction(ACTION_LS);

            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if(intent.getAction().equals(ACTION)) {
                        String app = intent.getStringExtra(ID);
                        String what = intent.getStringExtra(WHAT);
                        Log.i("RetuiReplyDebug", "ReplyManager ACTION received id=" + app
                                + " hasText=" + (what != null));

                        int id;
                        try {
                            id = Integer.parseInt(app);
                        } catch (Exception e) {
                            BoundApp bapp = findApp(app);
                            if(bapp == null) {
                                Log.w("RetuiReplyDebug", "ReplyManager app not bound pkg=" + app);
                                Tuils.sendOutput(context, context.getString(R.string.reply_app_not_found) + Tuils.SPACE + app);
                                return;
                            }

                            id = bapp.applicationId;
                        }

                        if(what == null) {
                            check(id);
                        } else {
                            if(id == -1) return;
                            Log.i("RetuiReplyDebug", "ReplyManager dispatching reply appId=" + id
                                    + " text=" + what);
                            replyTo(ReplyManager.this.context, id, what);
                        }
                    } else if(intent.getAction().equals(ACTION_UPDATE)) {
                        if(notificationWears != null) {
                            notificationWears.clear();
                        }
                        load(false);
                    } else if(intent.getAction().equals(ACTION_LS)) {
                        ls(context);
                    }
                }
            };

            LocalBroadcastManager.getInstance(this.context).registerReceiver(receiver, filter);
        }
    }

    public static ReplyManager getInstance() {
        return instance != null ? instance.get() : null;
    }

    private void load(boolean loadPrefs) {
        if(boundApps != null) boundApps.clear();
        else boundApps = new ArrayList<>();

        List<Reply> enums = new ArrayList<>(Arrays.asList(Reply.values()));

        File file = new File(Tuils.getFolder(), PATH);
        if (!file.exists()) {
            XMLPrefsManager.resetFile(file, NAME);
        }

        Object[] o;
        try {
            o = XMLPrefsManager.buildDocument(file, NAME);
            if(o == null) {
                Tuils.sendXMLParseError(context, PATH);
                return;
            }
        } catch (SAXParseException e) {
            Tuils.sendXMLParseError(context, PATH, e);
            return;
        } catch (Exception e) {
            Tuils.log(e);
            return;
        }

        Document d = (Document) o[0];
        Element root = (Element) o[1];

        NodeList nodes = root.getElementsByTagName("*");

        PackageManager mgr = context.getPackageManager();

        try {
            for (int count = 0; count < nodes.getLength(); count++) {
                final Node node = nodes.item(count);
                String nn = node.getNodeName();

                if (Tuils.find(nn, enums) != -1) {
                    if(loadPrefs) {
                        values.add(nn, node.getAttributes().getNamedItem(VALUE_ATTRIBUTE).getNodeValue());

                        for (int en = 0; en < enums.size(); en++) {
                            if (enums.get(en).label().equals(nn)) {
                                enums.remove(en);
                                break;
                            }
                        }
                    }
                } else {
                    int id = XMLPrefsManager.getIntAttribute((Element) node, ID_ATTRIBUTE);

                    ApplicationInfo info;
                    try {
                        info = mgr.getApplicationInfo(nn, 0);
                    } catch (Exception e) {
                        Tuils.log(e);
                        continue;
                    }

                    String label = info.loadLabel(mgr).toString();
                    if (id != -1) boundApps.add(new BoundApp(id, nn, label));
                }
            }

            if (loadPrefs && enums.size() > 0) {
                for (XMLPrefsSave s : enums) {
                    String value = s.defaultValue();

                    Element em = d.createElement(s.label());
                    em.setAttribute(VALUE_ATTRIBUTE, value);
                    root.appendChild(em);

                    values.add(s.label(), value);
                }

                writeTo(d, file);
            }
        } catch (Exception e) {
            Tuils.log(e);
        }

        nextUsableId = nextUsableId();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void onNotification(StatusBarNotification notification, CharSequence text) {
        if(!enabled) return;

        BoundApp app = findApp(notification.getPackageName());
        if(app == null) return;

        NotificationWear w = extractWearNotification(notification);
        if(w == null) return;

        NotificationWear old = findNotificationWear(app);

        if(old != null && (w.pendingIntent == null || w.remoteInputs == null || w.remoteInputs.length == 0)) return;
        if(old != null) notificationWears.remove(old);

        w.text = text;
        w.app = app;

        notificationWears.add(w);
    }

    private void replyTo(Context context, int applicationId, String what) {
        if(!enabled) return;

        BoundApp app = findApp(applicationId);
        if(app == null) {
            Tuils.sendOutput(context, context.getString(R.string.reply_id_not_found) + Tuils.SPACE + applicationId);
            return;
        }

        NotificationWear wear = findNotificationWear(applicationId);
        if(wear != null) {
            Log.i("RetuiReplyDebug", "ReplyManager found notification wear appId=" + applicationId
                    + " remoteInputCount=" + (wear.remoteInputs == null ? 0 : wear.remoteInputs.length)
                    + " actionTitle=" + wear.actionTitle);
            replyTo(context, wear, what);
        } else {
            Log.w("RetuiReplyDebug", "ReplyManager no notification wear appId=" + applicationId);
            Tuils.sendOutput(context, R.string.reply_notification_not_found);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private void replyTo(Context context, NotificationWear notificationWear, String what) {
        RemoteInput[] remoteInputs = notificationWear.remoteInputs;

        Bundle localBundle = notificationWear.bundle;
        Log.i("RetuiReplyDebug", "ReplyManager sending to PrivateIO id=" + notificationWear.id
                + " app=" + (notificationWear.app == null ? "null" : notificationWear.app.packageName)
                + " actionTitle=" + notificationWear.actionTitle
                + " remoteInputCount=" + (remoteInputs == null ? 0 : remoteInputs.length));

        Intent i = new Intent(PrivateIOReceiver.ACTION_REPLY);
        i.putExtra(PrivateIOReceiver.BUNDLE, localBundle);
        i.putExtra(PrivateIOReceiver.REMOTE_INPUTS, remoteInputs);
        i.putExtra(PrivateIOReceiver.TEXT, what);
        i.putExtra(PrivateIOReceiver.PENDING_INTENT, notificationWear.pendingIntent);
        i.putExtra(PrivateIOReceiver.ID, notificationWear.id);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private NotificationWear extractWearNotification(StatusBarNotification statusBarNotification) {
        if(statusBarNotification == null || statusBarNotification.getNotification() == null) {
            return null;
        }

        NotificationWear notificationWear = null;
        Notification notification = statusBarNotification.getNotification();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH && notification.actions != null) {
            for(int i = 0; i < notification.actions.length; i++) {
                Notification.Action action = notification.actions[i];
                if(action == null) continue;
                notificationWear = betterReplyAction(notificationWear, action);

                NotificationCompat.Action compatAction = NotificationCompat.getAction(notification, i);
                notificationWear = betterReplyAction(notificationWear, compatAction);
            }
        }

        Notification.WearableExtender wearableExtender = new Notification.WearableExtender(notification);
        for(Notification.Action action : wearableExtender.getActions()) {
            if(action == null) continue;
            notificationWear = betterReplyAction(notificationWear, action);
        }

        if(notificationWear == null || notificationWear.pendingIntent == null || notificationWear.remoteInputs == null || notificationWear.remoteInputs.length == 0) {
            Log.i("RetuiReplyDebug", "no reply action captured pkg=" + statusBarNotification.getPackageName()
                    + " actionCount=" + (notification.actions == null ? 0 : notification.actions.length)
                    + " wearableActionCount=" + wearableExtender.getActions().size()
                    + " hasWearableExtras=" + notification.extras.containsKey("android.wearable.EXTENSIONS"));
            return null;
        }

        notificationWear.bundle = notification.extras;
        notificationWear.id = statusBarNotification.getId();
        Log.i("RetuiReplyDebug", "captured reply action pkg=" + statusBarNotification.getPackageName()
                + " id=" + notificationWear.id
                + " title=" + notificationWear.actionTitle
                + " semantic=" + notificationWear.semanticAction
                + " remoteInputCount=" + notificationWear.remoteInputs.length);

        return notificationWear;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private NotificationWear betterReplyAction(NotificationWear current, Notification.Action action) {
        if(action == null || action.actionIntent == null) return current;
        RemoteInput[] remoteInputs = action.getRemoteInputs();
        if(remoteInputs == null || remoteInputs.length == 0) return current;

        NotificationWear candidate = new NotificationWear();
        candidate.remoteInputs = remoteInputs;
        candidate.pendingIntent = action.actionIntent;
        candidate.actionTitle = action.title;
        candidate.semanticAction = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? action.getSemanticAction() : 0;

        if(current == null || replyActionScore(candidate) > replyActionScore(current)) {
            return candidate;
        }
        return current;
    }

    private NotificationWear betterReplyAction(NotificationWear current, NotificationCompat.Action action) {
        if(action == null || action.getActionIntent() == null) return current;
        androidx.core.app.RemoteInput[] compatRemoteInputs = action.getRemoteInputs();
        if(compatRemoteInputs == null || compatRemoteInputs.length == 0) return current;

        RemoteInput[] remoteInputs = toPlatformRemoteInputs(compatRemoteInputs);
        if(remoteInputs == null || remoteInputs.length == 0) return current;

        NotificationWear candidate = new NotificationWear();
        candidate.remoteInputs = remoteInputs;
        candidate.pendingIntent = action.getActionIntent();
        candidate.actionTitle = action.getTitle();
        candidate.semanticAction = action.getSemanticAction();

        if(current == null || replyActionScore(candidate) > replyActionScore(current)) {
            return candidate;
        }
        return current;
    }

    private RemoteInput[] toPlatformRemoteInputs(androidx.core.app.RemoteInput[] compatRemoteInputs) {
        if(compatRemoteInputs == null || compatRemoteInputs.length == 0) return null;

        RemoteInput[] out = new RemoteInput[compatRemoteInputs.length];
        for(int i = 0; i < compatRemoteInputs.length; i++) {
            androidx.core.app.RemoteInput compat = compatRemoteInputs[i];
            if(compat == null || compat.getResultKey() == null) continue;

            RemoteInput.Builder builder = new RemoteInput.Builder(compat.getResultKey())
                    .setLabel(compat.getLabel())
                    .setChoices(compat.getChoices())
                    .setAllowFreeFormInput(compat.getAllowFreeFormInput())
                    .addExtras(compat.getExtras());

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && compat.getAllowedDataTypes() != null) {
                for(String dataType : compat.getAllowedDataTypes()) {
                    builder.setAllowDataType(dataType, true);
                }
            }

            out[i] = builder.build();
        }
        return out;
    }

    private int replyActionScore(NotificationWear wear) {
        int score = 0;
        String title = wear.actionTitle == null ? "" : wear.actionTitle.toString().toLowerCase();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && wear.semanticAction == Notification.Action.SEMANTIC_ACTION_REPLY) {
            score += 100;
        }
        if(title.contains("reply") || title.contains("respond") || title.contains("message")) {
            score += 50;
        }
        if(title.contains("mark") || title.contains("read") || title.contains("archive")
                || title.contains("delete") || title.contains("mute")) {
            score -= 50;
        }
        return score;
    }

    private BoundApp findApp(int applicationId) {
        if(boundApps != null) {
            for(BoundApp a : boundApps) {
                if(a.applicationId == applicationId) return a;
            }
        }

        return null;
    }

    private BoundApp findApp(String pkg) {
        if(boundApps != null) {
            for(BoundApp a : boundApps) {
                if(a.packageName.equals(pkg) || a.label.equalsIgnoreCase(pkg)) return a;
            }
        }

        return null;
    }

    private NotificationWear findNotificationWear(BoundApp bapp) {
        for(NotificationWear h : notificationWears) {
            if(h.app != null && h.app.packageName.equals(bapp.packageName)) return h;
        }
        return null;
    }

    private NotificationWear findNotificationWear(int id) {
        for(NotificationWear h : notificationWears) {
            if(h.app != null && h.app.applicationId == id) return h;
        }
        return null;
    }

    public void dispose(Context context) {
        try {
            Context appContext = this.context != null ? this.context : context.getApplicationContext();
            if (receiver != null) {
                LocalBroadcastManager.getInstance(appContext).unregisterReceiver(receiver);
            }
        } catch (Exception e) {}

        if(notificationWears != null) {
            notificationWears.clear();
            notificationWears = null;
        }
        if(boundApps != null) {
            boundApps.clear();
            boundApps = null;
        }
        if(values != null) {
            values.list.clear();
            values = null;
        }

        ReplyManager current = getInstance();
        if(current == this && instance != null) {
            instance.clear();
            instance = null;
        }
        this.context = null;
    }

    @Override
    public XMLPrefsList getValues() {
        return values;
    }

    @Override
    public void write(XMLPrefsSave save, String value) {
        set(new File(Tuils.getFolder(), PATH), save.label(), new String[] {VALUE_ATTRIBUTE}, new String[] {value});
        values.add(save.label(), value);
    }

    @Override
    public String[] delete() {
        return null;
    }

    public void check(int id) {
        if(!enabled) return;

        BoundApp app = findApp(id);
        if(app == null) {
            Tuils.sendOutput(context, context.getString(R.string.reply_id_not_found) + Tuils.SPACE + id);
            return;
        }

        NotificationWear wear = findNotificationWear(app);
        if(wear == null) {
            Tuils.sendOutput(context, R.string.reply_notification_not_found);
            return;
        }

        Tuils.sendOutput(context, wear.text);
    }

    public boolean canReplyTo(String pkg) {
        if(!enabled || pkg == null) return false;

        BoundApp app = findApp(pkg);
        if(app == null) return false;

        NotificationWear wear = findNotificationWear(app);
        return wear != null && wear.pendingIntent != null && wear.remoteInputs != null && wear.remoteInputs.length > 0;
    }

    public static String bind(String pkg) {
        return XMLPrefsManager.set(new File(Tuils.getFolder(), PATH), pkg, new String[] {ID_ATTRIBUTE}, new String[] {String.valueOf(nextUsableId)});
    }

    public static String unbind(String pkg) {
        return XMLPrefsManager.removeNode(new File(Tuils.getFolder(), PATH), pkg);
    }

    private int nextUsableId() {
        int nextUsableID = 0;
        while (true) {
            boolean shouldRestart = false;

            for(BoundApp b : boundApps) {
                if(b.applicationId == nextUsableID) {
                    shouldRestart = true;
                    break;
                }
            }

            if(!shouldRestart) return nextUsableID;

            nextUsableID++;
        }
    }

    public void ls(Context c) {
        if(!enabled) return;

        StringBuilder builder = new StringBuilder();
        if(getInstance() != null) {
            for(BoundApp a : boundApps) builder.append(a.packageName).append(" -> ").append(a.applicationId).append(Tuils.NEWLINE);
        }
        String s = builder.toString();
        if(s.length() == 0) s = "[]";

        Tuils.sendOutput(context, s);
    }

//    private static class NotificationHolder {
//        BindedApp app;
//
//        List<RemoteInput> remoteInputs;
//        Bundle bundle;
//        PendingIntent pendingIntent;
//
//        public NotificationHolder(BindedApp app, List<RemoteInput> remoteInputs, Bundle bundle, PendingIntent pendingIntent) {
//            this.app = app;
//            this.remoteInputs = remoteInputs;
//            this.bundle = bundle;
//            this.pendingIntent = pendingIntent;
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            NotificationHolder h = (NotificationHolder) obj;
//            return h.app.equals(app);
//        }
//    }
}
