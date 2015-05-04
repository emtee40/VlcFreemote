package com.nico.vlcfremote.utils;

import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkingUtils {

    public static List<String> getAddresses() {
        List<String> addresses = new ArrayList<>();
        final List<NetworkInterface> interfaces;

        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            return addresses;
        }

        for (NetworkInterface netInterface : interfaces) {
            for (InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                if (!address.isLoopbackAddress()) {
                    addresses.add(address.getHostAddress().toUpperCase());
                }
            }
        }

        return addresses;
    }

    public static class ServerScanner extends AsyncTask<Void, Void, List<String>> {
        public interface Callback {
            void onServerDiscovered(final List<String> ip);
        }

        private final Callback callback;
        private final List<String> localIps;
        private final int serverPort;

        // A small scan timeout should be enough for LANs
        private static final int SERVER_SCAN_TIMEOUT = 10;

        private final Pattern ip_pattern;
        private static final String IPADDRESS_PATTERN =
                "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                        "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";


        public ServerScanner(List<String> localIps, int serverPort, Callback callback) {
            this.callback = callback;
            this.localIps = localIps;
            this.serverPort = serverPort;
            this.ip_pattern = Pattern.compile(IPADDRESS_PATTERN);
        }

        @Override
        protected List<String> doInBackground(Void... params) {
            List<String> servers = new ArrayList<>();

            for (final String localIp : localIps) {
                if (!ip_pattern.matcher(localIp).matches()) {
                    Log.w(ServerScanner.class.getName(), "IP " + localIp + " is not of a supported type (IPv6?)");
                    continue;
                }

                // This assumes a /24 is used: if that's not the case, we won't support it anyway
                // as scanning would be too slow. Also, if the user is not using a /24 he's probably
                // smart enough to manually enter his IP
                final String subnet = localIp.substring(0, localIp.lastIndexOf(".")+1);
                for (int i = 1; i < 255; i++) {
                    if (isCancelled()) break;

                    final String serverIp = subnet + String.valueOf(i);

                    final SocketAddress address = new InetSocketAddress(serverIp, serverPort);
                    final Socket serverConn = new Socket();
                    try {
                        serverConn.connect(address, SERVER_SCAN_TIMEOUT);
                    } catch (IOException e) {
                        // Log.d(ServerScanner.class.getName(), "No servers running @ "  + serverIp);
                        continue;
                    }

                    Log.d(ServerScanner.class.getName(), "There is a server @ "  + serverIp);
                    servers.add(serverIp);
                }
            }

            return servers;
        }

        @Override
        protected void onPostExecute(final List<String> discoveredServers) {
            callback.onServerDiscovered(discoveredServers);
        }
    }
}
