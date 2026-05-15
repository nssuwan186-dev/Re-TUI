package ohi.andre.consolelauncher.managers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.LauncherActivity;
import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.options.Behavior;
import ohi.andre.consolelauncher.tuils.Tuils;

public class TuiLocationManager {

    public static final String ACTION_GOT_PERMISSION = BuildConfig.APPLICATION_ID + ".got_location_permission";

    public static final String LATITUDE = "lat", LONGITUDE = "long", FAIL = "fail";

    private static final int MAX_DELAY = 10000;

    private final Context context;
    private WeakReference<Activity> permissionActivity;
    BroadcastReceiver receiver;

    LocationListener locationListener;

    Handler handler;

    public boolean locationAvailable = false;
    public double latitude, longitude;

    private List<String> actionsPool;

    @SuppressLint("StaticFieldLeak")
    private static TuiLocationManager instance;
    public static synchronized TuiLocationManager instance(Context context) {
        if(instance == null) instance = new TuiLocationManager(context);
        else instance.setPermissionActivity(context);
        return instance;
    }

    private TuiLocationManager(final Context context) {
        this.context = context.getApplicationContext();
        setPermissionActivity(context);
        actionsPool = new ArrayList<>();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                clearHandler();

                latitude = location.getLatitude();
                longitude = location.getLongitude();

                locationAvailable = true;

                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(TuiLocationManager.this.context);

                for(String s : actionsPool) {
                    Intent i = new Intent(s);
                    i.putExtra(LATITUDE, location.getLatitude());
                    i.putExtra(LONGITUDE, location.getLongitude());
                    localBroadcastManager.sendBroadcast(i);
                }

                dispose();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(ACTION_GOT_PERMISSION)) {
                    if (intent.getIntExtra(XMLPrefsManager.VALUE_ATTRIBUTE, 1) == PackageManager.PERMISSION_GRANTED) {
                        register();
                    }
                }
            }
        };

        registerPermissionReceiver();
    }

    private void setPermissionActivity(Context context) {
        if (context instanceof Activity) {
            permissionActivity = new WeakReference<>((Activity) context);
        }
    }

    private boolean receiverRegistered = false;

    private void registerPermissionReceiver() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_GOT_PERMISSION);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
        receiverRegistered = true;
    }

    private boolean registered = false;

    @SuppressLint("MissingPermission")
    private void register() {
        registerPermissionReceiver();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Activity activity = permissionActivity != null ? permissionActivity.get() : null;
            if (activity != null && !activity.isFinishing()) {
                ActivityCompat.requestPermissions(activity, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, LauncherActivity.LOCATION_REQUEST_PERMISSION);
            } else {
                broadcastFailure();
                dispose();
            }
            return;
        }

        if(registered) return;
        registered = true;

        Criteria c = new Criteria();
        c.setAltitudeRequired(false);
        c.setAccuracy(Criteria.ACCURACY_COARSE);
        c.setBearingRequired(false);
        c.setCostAllowed(false);
        c.setHorizontalAccuracy(Criteria.ACCURACY_LOW);
        c.setPowerRequirement(Criteria.POWER_LOW);
        c.setSpeedRequired(false);

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        try {
            manager.requestLocationUpdates(XMLPrefsManager.getInt(Behavior.location_update_mintime) * 60 * 1000, XMLPrefsManager.getInt(Behavior.location_update_mindistance),
                    c, locationListener, Looper.getMainLooper());
        } catch (Exception e) {
            Tuils.log(e);
            Tuils.toFile(e);
        }

        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                broadcastFailure();
                dispose();
            }
        }, MAX_DELAY);
    }

    private void broadcastFailure() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        for(String s : actionsPool) {
            Intent i = new Intent(s);
            i.putExtra(FAIL, true);
            localBroadcastManager.sendBroadcast(i);
        }
    }

    public void add(String action) {
        actionsPool.add(action);

        register();
    }

    public void rm(String action) {
        actionsPool.remove(action);
    }

    @SuppressLint("MissingPermission")
    private void dispose() {
        actionsPool.clear();
        if (receiverRegistered) {
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
            } catch (Exception ignored) {}
            receiverRegistered = false;
        }
        registered = false;

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(manager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                manager.removeUpdates(locationListener);
            }
        }

        clearHandler();
    }

    public static void disposeStatic() {
        if(instance != null) instance.dispose();
        instance = null;
    }

    private void clearHandler() {
        if(handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }
}
