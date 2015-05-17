package com.nico.vlcfremote.utils;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class NetworkingUtils {

    public static List<String> getLocalIPAddresses() {
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



    public static class SendSSHCommand extends AsyncTask<Void, Void, String> {
        public interface Callback {
            void onResponseReceived(final String response);
            void onConnectionFailure(final JSchException e);
            void onIOFailure(final IOException e);
            void onExecFail(final JSchException e);
        }

        private final Callback callback;
        private Session session;

        public SendSSHCommand(final String server, final String user,
                             final String password, int serverPort, Callback callback)
        {
            this.callback = callback;

            try {
                session = (new JSch()).getSession(user, server, serverPort);
                session.setPassword(password);
            } catch (JSchException e) {
                session = null;
                callback.onConnectionFailure(e);
                return;
            }

            Properties p = new Properties();
            p.put("StrictHostKeyChecking", "no");
            session.setConfig(p);
        }

        @Override
        protected String doInBackground(Void... params) {
            if (session == null) return null;

            try {
                session.connect();
            } catch (JSchException e) {
                callback.onConnectionFailure(e);
                return null;
            }

            final Channel channel;
            try {
                channel = session.openChannel("exec");
            } catch (JSchException e) {
                callback.onConnectionFailure(e);
                return null;
            }

            ((ChannelExec)channel).setCommand("ls");
            channel.setInputStream(null);

            final InputStream in;
            try {
                in = channel.getInputStream();
            } catch (IOException e) {
                callback.onIOFailure(e);
                return null;
            }

            try {
                channel.connect();
            } catch (JSchException e) {
                callback.onExecFail(e);
                return null;
            }

            try {
                byte[] tmp=new byte[1024];
                while(true){
                    while(in.available()>0){
                        int i=in.read(tmp, 0, 1024);
                        if(i<0)break;
                        Log.i("SSHTEST", "RECV: " + new String(tmp, 0, i));
                    }

                    if(channel.isClosed()){
                        if(in.available()>0) continue;
                        Log.i("SSHTEST", "FIN, retval = " + String.valueOf(channel.getExitStatus()));
                        break;
                    }
                    try{Thread.sleep(1000);}catch(Exception ee){}
                }

            } catch (IOException e) {
                Log.e("SSHTEST", "IO No pude leer resp: " + e.getMessage());
            }

            channel.disconnect();
            session.disconnect();

            return "HOLA";
        }

        @Override
        protected void onPostExecute(final String response) {
            if (response != null) {
                callback.onResponseReceived(response);
            }
        }
    }


}
