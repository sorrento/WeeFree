package com.milenko.weefree;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    private boolean keepListening = true;//cuando estamos donando, dejamos de atender los escaneos automaitcos
    private boolean hasBeenConnectedToVamp = false;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myLog.add("Starting service ", tag);

        try {
            showServiceActiveNotification();

            mContext = getApplicationContext();
            mainWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

            //encender eil wifi si esta apagado
            if (!mainWifi.isWifiEnabled()) {
                mainWifi.setWifiEnabled(true);
            }

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

    /**
     * Analize the list of available WIFIS around. If one of them is the vampire, continues the workflow
     *
     * @param sr detected spots
     */
    public void CheckScanResults(final List<ScanResult> sr) {
        boolean vampiroCerca = false;
        iScan++;

        ArrayList<String> bssids = new ArrayList<String>();
        ArrayList<String> ssids = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(iScan + "+++++++ Scan results:+" + "\n");

        for (ScanResult r : sr) {
            bssids.add(r.BSSID);
            ssids.add(r.SSID);
            sb.append("  '" + r.SSID + "' | " + r.BSSID + " | l= " + r.level + "\n");
            if (r.SSID.equals("TengoSed")) {
                myLog.add("3. Detected Vamp", "aut");
                vampiroCerca = true;
            }
        }
        sb.append("+++++++++");
        myLog.add(sb.toString(), tag);

        if (vampiroCerca) {
            updateRecordingNotification("Vampiro encontrado!", "ahorita mismo", R.mipmap.ic_weefree, true);
//            mContext.unregisterReceiver(receiverWifi);
//            util.AccessPoint.createWifiAccessPoint(util.AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mainWifi);
            myLog.add("3. Connecting to vampire", "aut");
            AccessPoint.ConnectToWifi(AccessPoint.SSID_VAMPIRE, AccessPoint.PASS_VAMPIRE, mContext);
            keepListening = false;// dejamos de buscar otros
        } else {
            updateRecordingNotification("Sin Vampiro", "segumos buscando", android.R.drawable.ic_media_play, false);
        }
    }

    private void showServiceActiveNotification() {
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
    }

    private void updateRecordingNotification(String title, String content, int smallIcon, boolean vibrate) {
//        mNotificationManager.cancel(101);

        NotificationCompat.Builder notif;

        notif = new NotificationCompat.Builder(this)
                .setSmallIcon(smallIcon)
//                .setLargeIcon(we.getLogoRounded())
                .setContentTitle(title)
//                .setContentText(we.getType())
//                .setAutoCancel(true)
                .setOngoing(true)

//                .setLights(0xE6D820, 300, 100)
                .setTicker("WIFI update");
//                .setDeleteIntent(pendingDeleteIntent)
//                .addAction(actionSilence);

        if (vibrate) {
            notif.setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND | Notification.FLAG_SHOW_LIGHTS);
        }
        //Bigtext style
        NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();
        textStyle.setBigContentTitle("wifis around");
        textStyle.bigText(content);
//        textStyle.bigText(LogInManagement.getContabilidadString());
        notif.setStyle(textStyle);

        mNotificationManager.notify(101, notif.build());

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

    private void StartCountingData() {
        final long dataini = TrafficStats.getMobileRxBytes();
        final Timer tim = new Timer();
        tim.schedule(new TimerTask() {
            @Override
            public void run() {
                long bt = TrafficStats.getMobileRxBytes();
                long deltadata = (bt - dataini) / 1024;
                myLog.add("datos recibidos mobile:" + deltadata, "aut"); //TODO ojo que si la app no está en primer plano, no lo podrá cortar. habría que iniciar un servicio
                if (deltadata > 100) {
                    tim.cancel();
                    CortarWifiPorExceso(deltadata);
                }
            }
        }, 1000, 4000);

    }

    private void CortarWifiPorExceso(long deltadata) {
        myLog.add("[en service] Cortando la wifi por exceso de datos: " + deltadata + "kb", "aut");
        //Todo, cortar realmente (apagar, poner en lista negra al user, encender si es que)
    }

    /**
     * For detection of connect/disconnet or autoscanning of wifis
     */
    class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();

            try {
                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                    myLog.add("---state=" + netInfo.toString() + "hasbeenconnected to vamp: " + hasBeenConnectedToVamp, "aut");
                    if (netInfo.getDetailedState() == (NetworkInfo.DetailedState.CONNECTED)) {
                        myLog.add("*** We just connected to wifi: " + netInfo.getExtraInfo(), "CON");

                        if (netInfo.getExtraInfo() == null) {
                            //TODO check if in lower version, getExtraIfo is null
                            myLog.add("netInfo.getExtraInfo()==null, deberia ser el nombre de SSID", "aut");
                            return;
                        }

                        if (netInfo.getExtraInfo().equals("\"" + AccessPoint.SSID_VAMPIRE + "\"")) {
                            myLog.add("3. connected to APV", "aut");
                            hasBeenConnectedToVamp = true;
                        } else {
                            hasBeenConnectedToVamp = false;
                        }
                    } else if (
                        //                        (netInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTING)||
                            (netInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED && hasBeenConnectedToVamp)) {
                        hasBeenConnectedToVamp = false;
                        myLog.add("6. Desconectando de APV", "aut");
                        myLog.add("7. Crando APD DONANT accesspoint", "aut");
                        StartCountingData();
                        AccessPoint.createWifiAccessPoint(AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mainWifi);
                    }

                } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && keepListening) {
                    List<ScanResult> sr = mainWifi.getScanResults();
                    CheckScanResults(sr);

                } else {
                    myLog.add("Entering in a different state of network: " + action, tag);
                }
            } catch (Exception e) {
                myLog.add("EEE in service , on receive" + e.getLocalizedMessage(), "aut");
            }
        }
    }
}
