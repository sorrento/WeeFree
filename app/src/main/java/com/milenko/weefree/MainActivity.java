package com.milenko.weefree;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import util.AccessPoint;
import util.ClientScanResult;
import util.FinishScanListener;
import util.WifiApManager;

import static com.milenko.weefree.myLog.WriteUnhandledErrors;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager;
    private Switch swDetection;
    private Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        myLog.initialize("/WFLOG/rt.txt"); //Log in a file on the phone
        WriteUnhandledErrors(true);

        mContext = getApplicationContext();
        //Start service as donant
        swDetection = (Switch) findViewById(R.id.switch1);
        swDetection.setChecked(false);
        swDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(mContext, "created the servcei", Toast.LENGTH_SHORT).show();
                    mContext.startService(new Intent(mContext, WifiObserverService.class));
                    Log.d("MHP", "starting service");
                } else {
                    Toast.makeText(mContext, "unchecked", Toast.LENGTH_SHORT).show();
                    mContext.stopService(new Intent(mContext, WifiObserverService.class));
                }
            }
        });

    }


    //TODO medir el consumo de datos en el emisor preferentemente

//TODO volver a encender el wifi si lo estaba

    //Button
    public void BroadcastThirsty(View v) {
        writeScreen("gritando tengo sed");
        util.AccessPoint.createWifiAccessPoint(AccessPoint.SSID_VAMPIRE, AccessPoint.PASS_VAMPIRE, wifiManager);//OJO key must  ser 8 chars

//        CheckDonantesScaningWifi();
        CheckDonantesLookingAtFile();


    }

    private void CheckDonantesLookingAtFile() {
        //scaneear cada7 segundos si hay donanteen el file:
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
                final boolean[] foundDonante = {false};

                WifiApManager wifiApManager = new WifiApManager(mContext);
                wifiApManager.getClientList(true, 300, new FinishScanListener() {
                    @Override
                    public void onFinishScan(ArrayList<ClientScanResult> clients) {
                        myLog.add("We have " + clients.size() + " clientes coneccted", "aut");

                        foundDonante[0] = true;
//                        for (ClientScanResult c : clients) {
//                            myLog.add(c.getDevice());
//                        }
                    }
                });


                if (foundDonante[0]) {
                    writeScreen("encontrado un donante!");
                    wifiApManager.setWifiApEnabled(null, false);
                    connectToDonante();
                } else {
                    writeScreen("no hay donantes");
                }

            }
        }, 0, AccessPoint.REFRESH);

    }

    private void CheckDonantesScaningWifi() {
        //scaneear cada7 segundos si hay donante:
        final Timer t = new Timer();
        t.schedule(new TimerTask() {
            @Override
            public void run() {
//                WifiManager wifiManager2= (WifiManager) getSystemService(Context.WIFI_SERVICE);
                boolean foundDonante = false;
                AccessPoint.destroyWifiAccessPoint(wifiManager);
                wifiManager.startScan();
                final List<ScanResult> sr = wifiManager.getScanResults();

                for (ScanResult r : sr) {
                    if (r.SSID.equals(AccessPoint.SSID_DONANTE)) {
                        foundDonante = true;
                        break;
                    }
                }
                if (foundDonante) {
                    writeScreen("encontrado un donante!");
                    connectToDonante();
                } else {
                    writeScreen("no hay donantes");
                    AccessPoint.createWifiAccessPoint(AccessPoint.SSID_VAMPIRE, AccessPoint.PASS_VAMPIRE, wifiManager);
                }

            }
        }, 0, 7000);
    }

    private void connectToDonante() {
        writeScreen("conectando a donante");
              ConnectToWifi(AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mContext);
    }

    /***
     * connect to a protected wifi
     *
     * @param networkSSID
     * @param networkPass
     * @param mContext
     */
    public static void ConnectToWifi(String networkSSID, String networkPass, Context mContext) {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        myLog.add("en action connect to wifi");
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";

        //WEP In case of WEP, if your password is in hex, you do not need to surround it with quotes.
//        conf.wepKeys[0] = "\"" + networkPass + "\"";
//        conf.wepTxKeyIndex = 0;
//        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//        conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

        //WPA
        conf.preSharedKey = "\"" + networkPass + "\"";

        //open
//        conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();

        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }

    }

    private void writeScreen(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = (TextView) findViewById(R.id.log);
                textView.append(currentDate() + s + "\n");
            }
        });

    }

    private static String currentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date()) + ": ";
    }
}


