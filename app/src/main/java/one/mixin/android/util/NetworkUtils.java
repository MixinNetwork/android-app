package one.mixin.android.util;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import timber.log.Timber;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {

    public static String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }

        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = Formatter.formatIpAddress(ipAddress);
        return ip;
    }

    public static List<String> getIpAddressList() {
        List<String> ipAddressList = Collections.emptyList();

        try {
            List<NetworkInterface> networkInterfaceList = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaceList) {
                List<InetAddress> inetAddressList = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress inetAddress : inetAddressList) {
                    if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress()) {
                        ipAddressList.add(inetAddress.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ipAddressList;
    }

    public static void printWifiInfo(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                return;
            }
            if (!wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(true);
            }
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            int signalStrength = wifiInfo.getRssi();
            int linkSpeed = wifiInfo.getLinkSpeed();
            int ip = wifiInfo.getIpAddress();
            String ipAddress = Formatter.formatIpAddress(ip);
            Timber.e("WiFi Info SSID: %s", ssid);
            Timber.e("WiFi Info Signal Strength: %s", signalStrength);
            Timber.e("WiFi Info Link Speed: %s", linkSpeed);
            Timber.e("WiFi Info IP Address: %s", ipAddress);
        } catch (Exception e) {
            Timber.e(e);
        }
    }
}