package com.milenko.weefree;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import util.AccessPoint;
import util.ClientScanResult;
import util.FinishScanListener;
import util.WifiApManager;

import static com.milenko.weefree.myLog.WriteUnhandledErrors;

public class MainActivity extends AppCompatActivity {

    WifiManager wifiManager;
    private Context mContext;
    private Switch swDetection;
    private boolean foundDonante = false;
    private Timer timerCheckClients;

    private static String currentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date()) + ": ";
    }

    //Button Vampire

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            mContext = getApplicationContext();
            wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            myLog.initialize("/WFLOG/rt.txt"); //Log in a file on the phone
            WriteUnhandledErrors(true);


            //Start service as donant

            swDetection = (Switch) findViewById(R.id.switch1);
            swDetection.setChecked(false);
            swDetection.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        myLog.type = "DON";
                        myLog.add("1. Starting Lisetening service", "aut");
                        mContext.startService(new Intent(mContext, WifiObserverService.class));
                    } else {
                        mContext.stopService(new Intent(mContext, WifiObserverService.class));
                    }

                }
            });
        } catch (Exception e) {
            myLog.add("EEEEError on create" + e.getLocalizedMessage(), "aut");
        }
    }

    /**
     * Starts the vampire mode: turn on an AP with SSID = "TengoSed"
     *
     * @param v button that send the request. Can be null
     */
    public void startVampireMode(View v) {
        try {
            myLog.type = "VAMP";
            writeScreen("2. Creating Access Point (V)");

            AccessPoint.createWifiAccessPoint(AccessPoint.SSID_VAMPIRE, AccessPoint.PASS_VAMPIRE, wifiManager);//OJO key must  ser 8 chars
            checkClientsList();

        } catch (Exception e) {
            myLog.add("EEEE in startVampireMode" + e.getLocalizedMessage(), "aut");
        }
    }

    /**
     * Created the AP, check the list of clients (looking one specific system file ec 7 secs)
     * If a "Donante" is connected (any connection) continues the workflow
     * 4. Detects the connection
     * 5. Turns off AP (V)
     */
    private void checkClientsList() {
        try {
            timerCheckClients = new Timer();
            timerCheckClients.schedule(new TimerTask() {
                @Override
                public void run() {
                    WifiApManager wifiApManager = new WifiApManager(mContext);
                    wifiApManager.getClientList(false, 300, new FinishScanListener() {

                        @Override
                        public void onFinishScan(ArrayList<ClientScanResult> clients) {
                            myLog.add("We have " + clients.size() + " clientes coneccted", "aut");
                            if (clients.size() > 0) {
                                foundDonante = true;
                            }
                        }
                    });

                    if (foundDonante) {
                        foundDonante = false;
                        writeScreen("4. Detected donantes");

                        stopCheckingClients();

                        AccessPoint.destroyWifiAccessPoint(wifiManager);
                        writeScreen("5. Apagando APV");

                        if (!wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(true);
                        }
                        keepTryingToConnectToDon();

                    } else {
                        writeScreen("There is no Donante");
                    }

                }
            }, 0, AccessPoint.REFRESH);
        } catch (Exception e) {
            myLog.add("Error CheckClientsList" + e.getLocalizedMessage(), "aut");
        }

    }

    private void stopCheckingClients() {
        myLog.add("stopping timerCheckClients the chek in file", "aut");
        timerCheckClients.cancel();
    }

    /**
     * Try to connect to connect to Donante
     * It repeats, Because we are not sure that our WIFI is turned on
     */
    private void keepTryingToConnectToDon() {
        final Timer timer = new Timer();

        new ConnectionToWifi(mContext, AccessPoint.SSID_DONANTE, new ConnectionDetectedByClient() {
            @Override
            public void OnConnection() {
                myLog.add("8. Connected to Donant (APD)", "aut");
            }
        });

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                myLog.add("UN intento de mirar las wifis", "aut");
                Boolean wifiIsOn = connectToDonante();
                myLog.add("wifi is on =" + wifiIsOn, "aut");
                if (wifiIsOn) timer.cancel();
            }
        }, 3000, 1000);
    }

    private Boolean connectToDonante() {
        writeScreen("8. connecttin to APD");

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        return AccessPoint.ConnectToWifi(AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mContext);
    }

    //Write some logs on the screen
    private void writeScreen(final String s) {
        try {
            myLog.add(s, "aut");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView textView = (TextView) findViewById(R.id.log);
                    textView.append(currentDate() + s + "\n");
                }
            });
        } catch (Exception e) {
            myLog.add("eeeee write on screen" + e.getLocalizedMessage(), "aut");
        }
    }

}


