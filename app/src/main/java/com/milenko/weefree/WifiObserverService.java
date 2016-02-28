package com.milenko.weefree;

import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import util.AccessPoint;


/**
 * Created by Milenko on 04/10/2015.
 */
public class WifiObserverService extends Service {

    String tag = "wfs";
    private Context mContext;
    private WifiManager mainWifi;
    private WifiReceiver receiverWifi;
    private int iScan = 0;
    private NotificationManager mNotificationManager;
    private boolean keepListeining = true;//cuando estamos donando, dejamos de escuhar para ver si hay otros vampiros


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myLog.add("Starting service ", tag);

        try {

            showRecordingNotification();
//            Notifications.Initialize(this);

            mContext = getApplicationContext();
            mainWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
            receiverWifi = new WifiReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            mContext.registerReceiver(receiverWifi, intentFilter);
            Toast.makeText(mContext, "Detection ON", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(mContext, "Not posstible to activate detection ", Toast.LENGTH_LONG).show();
            myLog.add("error starign " + e.getLocalizedMessage());
        }

        return super.onStartCommand(intent, flags, startId);
    }

    public void CheckScanResults(final List<ScanResult> sr) {
        boolean vampiroCerca = false;
        iScan++;

        ArrayList<String> bssids = new ArrayList<>();
        ArrayList<String> ssids = new ArrayList<>();
        StringBuilder sb = new StringBuilder(iScan + "+++++++ Scan results:+" + "\n");

        for (ScanResult r : sr) {
            bssids.add(r.BSSID);
            ssids.add(r.SSID);
            sb.append("  '" + r.SSID + "' | " + r.BSSID + " | l= " + r.level + "\n");
            if (r.SSID.equals("TengoSed")) {
                vampiroCerca = true;
            }
        }
        sb.append("+++++++++");
        myLog.add(sb.toString(), tag);

        if (vampiroCerca) {
            updateRecordingNotification("Vampiro encontrado!", "recien");
            mContext.unregisterReceiver(receiverWifi);
            util.AccessPoint.createWifiAccessPoint(util.AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mainWifi);
            keepListeining = false;// dejamos de buscar otros
        } else {
            updateRecordingNotification("Sin Vampiro", "segumos buscando");
        }
    }

    private void showRecordingNotification() {
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationCompat.Builder notif;

        notif = new NotificationCompat.Builder(this)
                .setSmallIcon(android.R.drawable.ic_media_play)
//                .setLargeIcon(we.getLogoRounded())
                .setContentTitle("Service is active")
//                .setContentText(we.getType())
//                .setAutoCancel(true)
                .setOngoing(true)
//                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.FLAG_SHOW_LIGHTS)
//                .setLights(0xE6D820, 300, 100)
                .setTicker("WIFI watching");
//                .setDeleteIntent(pendingDeleteIntent)
//                .addAction(actionSilence);
//
        mNotificationManager.notify(101, notif.build());


//        Notification not = new Notification(R.drawable.icon, "Application started", System.currentTimeMillis());
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, main.class), Notification.FLAG_ONGOING_EVENT);
//        not.flags = Notification.FLAG_ONGOING_EVENT;
//        not.setLatestEventInfo(this, "Application Name", "Application Description", contentIntent);
//        mNotificationManager.notify(1, not);
    }

    private void updateRecordingNotification(String title, String content) {
//        mNotificationManager.cancel(101);

        NotificationCompat.Builder notif;

        notif = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
//                .setLargeIcon(we.getLogoRounded())
                .setContentTitle(title)
//                .setContentText(we.getType())
//                .setAutoCancel(true)
                .setOngoing(true)
//                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.FLAG_SHOW_LIGHTS)
//                .setLights(0xE6D820, 300, 100)
                .setTicker("WIFI update");
//                .setDeleteIntent(pendingDeleteIntent)
//                .addAction(actionSilence);
        //Bigtext style
        NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();
        textStyle.setBigContentTitle("wifis around");
        textStyle.bigText(content);
//        textStyle.bigText(LogInManagement.getContabilidadString());
        notif.setStyle(textStyle);

        mNotificationManager.notify(101, notif.build());


//        Notification not = new Notification(R.drawable.icon, "Application started", System.currentTimeMillis());
//        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, main.class), Notification.FLAG_ONGOING_EVENT);
//        not.flags = Notification.FLAG_ONGOING_EVENT;
//        not.setLatestEventInfo(this, "Application Name", "Application Description", contentIntent);
//        mNotificationManager.notify(1, not);
    }

    @Override
    public void onDestroy() {
        myLog.add("Destroying ", tag);
        try {
            mNotificationManager.cancel(101);
            Toast.makeText(mContext, "Detection OFF", Toast.LENGTH_LONG).show();
            mContext.unregisterReceiver(receiverWifi);
            super.onDestroy();
        } catch (Exception e) {
            Toast.makeText(mContext, "Not possible to turn off detection", Toast.LENGTH_LONG).show();
            myLog.add("error destroying: " + e.getLocalizedMessage());
        }
    }

    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();


            if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (netInfo.getDetailedState() == (NetworkInfo.DetailedState.CONNECTED)) {
                    myLog.add("*** We just connected to wifi: " + netInfo.getExtraInfo(), "CON");
                } else if (
//                        (netInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTING)||
                        (netInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED)) {
                    myLog.add("creating DONANT accesspoint","aut");
                    AccessPoint.createWifiAccessPoint(AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mainWifi);
                }

            } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> sr = mainWifi.getScanResults();
                CheckScanResults(sr);

            } else {
                myLog.add("Entering in a different state of network: " + action, tag);
            }
        }


    }

}
