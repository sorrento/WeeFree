package com.milenko.weefree;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Vibrator;

/**
 * Created by Milenko on 03/03/2016.
 */
public class ConnectionToWifi {
    private final String ssid;
    private final Context context;
    private final wifiScannerListener connectionToDonListener;
    private final ConnectionDetectedByClient c;

    public ConnectionToWifi(Context context, String ssid, ConnectionDetectedByClient c) {
        this.ssid = ssid;
        this.context = context;
        this.c = c;

        connectionToDonListener = new wifiScannerListener();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(connectionToDonListener, intentFilter);
    }

    class wifiScannerListener extends BroadcastReceiver {

        public void onReceive(Context ctx, Intent intent) {
            try {
                final String action = intent.getAction();
                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (netInfo.getDetailedState() == (NetworkInfo.DetailedState.CONNECTED)) {

                        if (netInfo.getExtraInfo() == null) {
                            //TODO check if in lower version, getExtraIfo is null
                            //Another option to get ssids is here:http:
                            // stackoverflow.com/questions/21391395/get-ssid-when-wifi-is-connected
                            myLog.add("netInfo.getExtraInfo()==null, deberia ser el nombre de SSID", "aut");
                            return;
                        }

                        if (netInfo.getExtraInfo().equals("\"" + ssid + "\"")) {

                            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(300);
                            myLog.add(" Connected to " + ssid, "aut");
                            c.OnConnection();
                            context.unregisterReceiver(this);
                        }
                    }
                }
            } catch (Exception e) {
                myLog.add("EEEEE wifiScannerListener " + e.getMessage(), "aut");
            }
        }
    }
}
