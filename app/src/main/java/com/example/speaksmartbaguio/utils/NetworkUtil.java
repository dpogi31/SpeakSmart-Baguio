
package com.example.speaksmartbaguio.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Build;

public final class NetworkUtil {

    private NetworkUtil() {
        // Utility class.
    }

    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager manager =
                (ConnectivityManager) context.getApplicationContext()
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = manager.getActiveNetwork();

            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities =
                    manager.getNetworkCapabilities(network);

            return capabilities != null
                    && capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }

        Network network = manager.getActiveNetwork();

        if (network == null) {
            return false;
        }

        NetworkCapabilities capabilities =
                manager.getNetworkCapabilities(network);

        return capabilities != null
                && capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
