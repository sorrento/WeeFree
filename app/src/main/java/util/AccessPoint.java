package util;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.milenko.weefree.myLog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Milenko on 27/02/2016.
 */
public class AccessPoint {
    public static final String SSID_DONANTE = "ParaVampiro";
    public static final String PASS_DONANTE = "ParaVampiro";
    public static final String SSID_VAMPIRE = "TengoSed";
    public static final String PASS_VAMPIRE = "whoCares";
    public static final int REFRESH = 7000;//millisecond between check list of connected devices (donants) to vampire

    public static void createWifiAccessPoint(String ssid, String key, WifiManager mainWifi) {
        if (mainWifi.isWifiEnabled()) {
            mainWifi.setWifiEnabled(false);
        }

        Method[] wmMethods = mainWifi.getClass().getDeclaredMethods();
        boolean methodFound = false;

        for (Method method : wmMethods) {
            if (method.getName().equals("setWifiApEnabled")) {
                methodFound = true;
                WifiConfiguration wifiConfiguration = new WifiConfiguration();
                wifiConfiguration.SSID = ssid;
//                wifiConfiguration.BSSID=bssid;//TODO put mac
//                wifiConfiguration.isPasspoint(); Interesting

                wifiConfiguration.preSharedKey = key;//password
                wifiConfiguration.hiddenSSID = false;
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wifiConfiguration.allowedKeyManagement.set(4);
                wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);


                try {
                    boolean apstatus = (Boolean) method.invoke(mainWifi, wifiConfiguration, true);

                    for (Method isWifiApEnabledmethod : wmMethods) {
                        if (isWifiApEnabledmethod.getName().equals("isWifiApEnabled")) {

                            while (!(Boolean) isWifiApEnabledmethod.invoke(mainWifi)) {
                            }

                            for (Method method1 : wmMethods) {
                                if (method1.getName().equals("getWifiApState")) {
                                    int apstate; //TODO what for?
                                    apstate = (Integer) method1.invoke(mainWifi);
                                }
                            }
                        }
                    }
                    if (apstatus) {
                        Log.d("Splash Activity", "Access Point created");
//                        Toast.makeText(mContext, "created", Toast.LENGTH_SHORT).show();
                    } else {
                        Log.d("Splash Activity", "Access Point creation failed");
//                        Toast.makeText(mContext, "NOT created", Toast.LENGTH_SHORT).show();
                    }

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!methodFound) {
            Log.d("Splash Activity", "cannot configure an access point");
        }
    }

    public static void destroyWifiAccessPoint(WifiManager wifiManager) {

        Method[] wmMethods = wifiManager.getClass().getDeclaredMethods();
        boolean methodFound = false;

        for (Method method : wmMethods) {
            if (method.getName().equals("setWifiApEnabled")) {
                methodFound = true;
                WifiConfiguration wifiConfiguration = new WifiConfiguration();

                try {
                    boolean apstatus = (Boolean) method.invoke(wifiManager, wifiConfiguration, false);

                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!methodFound) {
            Log.d("Splash Activity", "cannot configure an access point");
        }

        if (!wifiManager.isWifiEnabled()) {
            //le encendemos el wifi
            wifiManager.setWifiEnabled(true);
        }

    }

    /***
     * connect to a protected wifi
     *
     * @param networkSSID
     * @param networkPass
     * @param mContext
     */
    public static boolean ConnectToWifi(final String networkSSID, String networkPass, Context mContext) {
        final WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

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

        wifiManager.addNetwork(conf);//todo verificar si está par no agregarla deniuevo
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        if (list == null) {
            myLog.add("la lista de networks da null aun", "aut");
            return false;
        } else {
            myLog.add("networks found: " + list.size(), "aut");

            for (WifiConfiguration i : list) {
                if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();

                    break;
                }
            }
            return true;
        }
    }
}
