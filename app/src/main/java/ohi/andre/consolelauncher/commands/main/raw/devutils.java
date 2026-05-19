package ohi.andre.consolelauncher.commands.main.raw;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.RemoteInput;
import androidx.core.content.ContextCompat;

import java.util.List;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.commands.main.MainPack;
import ohi.andre.consolelauncher.commands.main.specific.ParamCommand;
import ohi.andre.consolelauncher.managers.notifications.DevReplyReceiver;
import ohi.andre.consolelauncher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 22/08/2017.
 */

public class devutils extends ParamCommand {

    private enum Param implements ohi.andre.consolelauncher.commands.main.Param {
        notify {
            @Override
            @android.annotation.SuppressLint("MissingPermission")
            public String exec(ExecutePack pack) {
                List<String> text = pack.getList();

                String title, txt = null;
                if(text.size() == 0) return null;
                else {
                    title = text.remove(0);
                    if(text.size() >= 2) txt = Tuils.toPlanString(text, Tuils.SPACE);
                }

                String channelId = "dev_utils_channel";
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Dev Utils", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                    android.app.NotificationManager notificationManager = (android.app.NotificationManager) pack.context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(channel);
                    }
                }

                if (!canPostNotifications(pack.context)) {
                    return "Notification permission is not granted.";
                }

                NotificationManagerCompat.from(pack.context).notify(200,
                        new NotificationCompat.Builder(pack.context, channelId)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(title)
                                .setContentText(txt)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .build());

                return null;
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.TEXTLIST};
            }
        },
        notify_reply {
            @Override
            @android.annotation.SuppressLint("MissingPermission")
            public String exec(ExecutePack pack) {
                List<String> text = pack.getList();

                String title, txt = null;
                if(text.size() == 0) return "Usage: devutils -notify_reply <title> <text>";
                else {
                    title = text.remove(0);
                    if(text.size() > 0) txt = Tuils.toPlanString(text, Tuils.SPACE);
                }

                String channelId = "dev_utils_channel";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    android.app.NotificationChannel channel = new android.app.NotificationChannel(channelId, "Dev Utils", android.app.NotificationManager.IMPORTANCE_DEFAULT);
                    android.app.NotificationManager notificationManager = (android.app.NotificationManager) pack.context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                    if (notificationManager != null) {
                        notificationManager.createNotificationChannel(channel);
                    }
                }

                RemoteInput remoteInput = new RemoteInput.Builder(DevReplyReceiver.RESULT_KEY)
                        .setLabel("Reply")
                        .build();

                Intent replyIntent = new Intent(pack.context, DevReplyReceiver.class)
                        .setAction(DevReplyReceiver.ACTION_DEV_REPLY);
                PendingIntent replyPendingIntent = PendingIntent.getBroadcast(
                        pack.context,
                        DevReplyReceiver.NOTIFICATION_ID,
                        replyIntent,
                        pendingIntentFlags());

                NotificationCompat.Action replyAction = new NotificationCompat.Action.Builder(
                        R.mipmap.ic_launcher,
                        "Reply",
                        replyPendingIntent)
                        .addRemoteInput(remoteInput)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .setAllowGeneratedReplies(true)
                        .build();

                if (!canPostNotifications(pack.context)) {
                    return "Notification permission is not granted.";
                }

                NotificationManagerCompat.from(pack.context).notify(DevReplyReceiver.NOTIFICATION_ID,
                        new NotificationCompat.Builder(pack.context, channelId)
                                .setSmallIcon(R.mipmap.ic_launcher)
                                .setContentTitle(title)
                                .setContentText(txt)
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .addAction(replyAction)
                                .build());

                return "Dev reply notification posted.";
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.TEXTLIST};
            }
        },
        check_notifications {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                return "Notification access: " + NotificationManagerCompat.getEnabledListenerPackages(pack.context).contains(BuildConfig.APPLICATION_ID) + Tuils.NEWLINE + "Notification service running: " + Tuils.notificationServiceIsRunning(pack.context);
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.context.getString(R.string.help_devutils);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return null;
        }
    }

    @Override
    protected ohi.andre.consolelauncher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return R.string.help_devutils;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    private static int pendingIntentFlags() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        return flags;
    }

    private static boolean canPostNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }
}
