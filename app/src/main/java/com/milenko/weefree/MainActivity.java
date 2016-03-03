package com.milenko.weefree;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
    private Switch swDetection;
    private Context mContext;
    private boolean foundDonante = false;
    private Timer t;
    private DetectConnectionToDonante receiverWifi;

    private static String currentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date()) + ": ";
    }


    //TODO medir el consumo de datos en el emisor preferentemente

//TODO volver a encender el wifi si lo estaba

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
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
                        myLog.type = "DON";
                        myLog.add("1. Starting Lisetening service", "aut");
                        Toast.makeText(mContext, "created the servcei", Toast.LENGTH_SHORT).show();
                        mContext.startService(new Intent(mContext, WifiObserverService.class));
                    } else {
                        Toast.makeText(mContext, "unchecked", Toast.LENGTH_SHORT).show();
                        mContext.stopService(new Intent(mContext, WifiObserverService.class));
                    }
                }
            });

//            //medida de flujo de datos
//            final long dataini = TrafficStats.getMobileRxBytes();
//            final Timer tim = new Timer();
//            tim.schedule(new TimerTask() {
//                @Override
//                public void run() {
//                    long bt = TrafficStats.getMobileRxBytes();
//                    long deltadata = (bt - dataini) / 1024;
//                    myLog.add("datos recibidos mobile:" + deltadata, "aut"); //TODO ojo que si la app no está en primer plano, no lo podrá cortar. habría que iniciar un servicio
//                    if (deltadata > 5) {
//                        tim.cancel();
//                        CortarWifiPorExceso(deltadata);
//                    }
//                    ;
//                }
//            }, 1000, 4000);


        } catch (Exception e) {
            myLog.add("EEEEE on create" + e.getLocalizedMessage(), "aut");
        }

    }

    private void CortarWifiPorExceso(long deltadata) {
        myLog.add("Cortando la wifi por exceso de datos: " + deltadata + "kb", "aut");
        //Todo, cortar realmente (apagar, poner en lista negra al user, encender si es que)
    }

    //Button
    public void BroadcastThirsty(View v) {
        try {
            myLog.type = "VAMP";
            myLog.add("2. creating APV", "aut");
            writeScreen("gritando tengo sed");


//            receiverWifi = new DetectConnectionToDonante();
//            IntentFilter intentFilter = new IntentFilter();
//            intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
//            mContext.registerReceiver(receiverWifi, intentFilter);


            AccessPoint.createWifiAccessPoint(AccessPoint.SSID_VAMPIRE, AccessPoint.PASS_VAMPIRE, wifiManager);//OJO key must  ser 8 chars

//        CheckDonantesScaningWifi();
            CheckDonantesLookingAtFile();
        } catch (Exception e) {
            myLog.add("EEEE broadcastT" + e.getLocalizedMessage(), "aut");
        }


    }

    private void CheckDonantesLookingAtFile() {
        try {
            myLog.add("entering ChekDOannates looking a file", "aut");
            //scaneear cada7 segundos si hay donanteen el file:
            t = new Timer();
            t.schedule(new TimerTask() {
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
                            //                        for (ClientScanResult c : clients) {
                            //                            myLog.add(c.getDevice());
                            //                        }
                        }
                    });

                    if (foundDonante) {
                        myLog.add("4. Detected donantes", "aut");
                        stopChekingDonantesInFile();
                        foundDonante = false;
                        writeScreen("encontrado un donante!");
                        //                    wifiApManager.setWifiApEnabled(null, false);
                        myLog.add("5. Apagando APV", "aut");
                        AccessPoint.destroyWifiAccessPoint(wifiManager);

                        if (!wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(true);
                        }

                        keepTryingToConnect();
                    } else {
                        myLog.add("No hay donantes", "aut");
                        writeScreen("no hay donantes");
                    }

                }
            }, 0, AccessPoint.REFRESH);
        } catch (Exception e) {
            myLog.add("Chekdonante error" + e.getLocalizedMessage(), "aut");
        }

    }

    private void keepTryingToConnect() {
        myLog.add("in Keeptryibg", "aut");

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //Puede que aun no esté encendida la wifi, asin qu repetimos
                myLog.add("UN intento de mirar las wifis", "aut");
                Boolean wifiIsOn = connectToDonante();
                myLog.add("wifi is on =" + wifiIsOn, "aut");
                if (wifiIsOn) timer.cancel();

            }
        }, 3000, 1000);
    }

    private void stopChekingDonantesInFile() {
        myLog.add("stopping timer the chek in file", "aut");
        t.cancel();
    }


    private Boolean connectToDonante() {
        myLog.add("8. connecttin to APD", "aut");
        writeScreen("conectando a donante");
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        return AccessPoint.ConnectToWifi(AccessPoint.SSID_DONANTE, AccessPoint.PASS_DONANTE, mContext);
    }

    private void writeScreen(final String s) {
        try {
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

    class DetectConnectionToDonante extends BroadcastReceiver {

        public void onReceive(Context c, Intent intent) {
            try {
                final String action = intent.getAction();
                if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (netInfo.getDetailedState() == (NetworkInfo.DetailedState.CONNECTED)) {
                        if (netInfo.getExtraInfo().equals("\"" + AccessPoint.SSID_DONANTE + "\"")) {
                            Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                            v.vibrate(300);
                            myLog.add("8. connected to APD", "aut");
                        }
                    }
                }
            } catch (Exception e) {
                myLog.add("EEEEE onreceive " + e.getMessage(), "aut");
            }
        }
    }
}


