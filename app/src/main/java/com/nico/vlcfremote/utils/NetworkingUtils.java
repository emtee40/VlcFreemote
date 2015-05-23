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

    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    public static List<String> getLocalIPAddresses() {
        final Pattern ip_pattern = Pattern.compile(IPADDRESS_PATTERN);

        List<String> addresses = new ArrayList<>();
        final List<NetworkInterface> interfaces;

        try {
            interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
        } catch (SocketException e) {
            return addresses;
        }

        for (NetworkInterface netInterface : interfaces) {
            for (InetAddress address : Collections.list(netInterface.getInetAddresses())) {
                if (address.isLoopbackAddress()) {
                    continue;
                }

                final String ip = address.getHostAddress().toUpperCase();
                if (!ip_pattern.matcher(ip).matches()) {
                    Log.w(ServerScanner.class.getName(), "IP " + ip + " is not of a supported type (IPv6?)");
                    continue;
                }

                addresses.add(ip);
            }
        }

        return addresses;
    }

    static public class ScannedServer {
        public String ip;
        public Integer sshPort;
        public Integer vlcPort;
    }

    public static class ServerScanner extends AsyncTask<Void, ScannedServer , List<ScannedServer>> {
        public interface Callback {
            void onServerDiscovered(final ScannedServer srv);
            void onScanFinished(final List<ScannedServer> ip);
        }

        private final Callback callback;
        private final List<String> localIps;
        private final int vlcPort;
        private final int sshPort;

        // A small scan timeout should be enough for LANs
        private static final int SERVER_SCAN_TIMEOUT = 10;

        /**
         * @param localIps List of IPv4
         */
        public ServerScanner(List<String> localIps, int vlcPort, int sshPort, Callback callback) {
            this.callback = callback;
            this.localIps = localIps;
            this.vlcPort = vlcPort;
            this.sshPort = sshPort;
        }

        private ScannedServer scanSshIpPort(final String ip, int sshPort) {
            final SocketAddress address = new InetSocketAddress(ip, sshPort);
            final Socket serverConn = new Socket();
            try {
                serverConn.connect(address, SERVER_SCAN_TIMEOUT);
            } catch (IOException e) {
                return null;
            }

            Log.d(ServerScanner.class.getName(), "There is an SSH server @ "  + ip + ":" + String.valueOf(sshPort));
            ScannedServer srv = new ScannedServer();
            srv.ip = ip;
            srv.sshPort = sshPort;
            srv.vlcPort = null;
            return srv;
        }

        private ScannedServer scanIpPort(final String ip, int sshPort, int vlcPort) {
            final SocketAddress address = new InetSocketAddress(ip, vlcPort);
            final Socket serverConn = new Socket();
            try {
                serverConn.connect(address, SERVER_SCAN_TIMEOUT );
            } catch (IOException e) {
                return scanSshIpPort(ip, sshPort);
            }

            Log.d(ServerScanner.class.getName(), "There is a VLC server @ "  + ip + ":" + String.valueOf(vlcPort));
            ScannedServer srv = new ScannedServer();
            srv.ip = ip;
            srv.vlcPort = vlcPort;
            srv.sshPort = null;
            return srv;
        }

        @Override
        protected List<ScannedServer> doInBackground(Void... params) {
            List<ScannedServer> servers = new ArrayList<>();

            for (final String localIp : localIps) {
                // This assumes a /24 is used: if that's not the case, we won't support it anyway
                // as scanning would be too slow. Also, if the user is not using a /24 he's probably
                // smart enough to manually enter his IP
                final String subnet = localIp.substring(0, localIp.lastIndexOf(".")+1);
                for (int i = 1; i < 255; i++) {
                    if (isCancelled()) break;

                    final String serverIp = subnet + String.valueOf(i);
                    ScannedServer srv = scanIpPort(serverIp, sshPort, vlcPort);
                    if (srv != null) {
                        servers.add(srv);
                        publishProgress(srv);
                    }
                }
            }

            return servers;
        }

        @Override
        protected void onPostExecute(final List<ScannedServer> discoveredServers) {
            callback.onScanFinished(discoveredServers);
        }

        @Override
        protected void onProgressUpdate(ScannedServer... srvLst) {
            for (ScannedServer srv : srvLst) {
                if (srv == null) throw new RuntimeException("Bad programmer error: Found a null server, this shouldn't happen.");
                callback.onServerDiscovered(srv);
            }
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
        private final String cmd;

        public SendSSHCommand(final String cmd, final String server, final String user,
                             final String password, int serverPort, Callback callback)
        {
            this.callback = callback;
            this.cmd = cmd;

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
                session.disconnect();
                return null;
            }

            ((ChannelExec)channel).setCommand(cmd);
            channel.setInputStream(null);

            try {
                channel.connect();
            } catch (JSchException e) {
                callback.onExecFail(e);
                session.disconnect();
                return null;
            }

            String msg = "";
            try {
                final InputStream in = channel.getInputStream();
                byte[] tmp=new byte[1024];

                while(true){
                    while(in.available()>0){
                        int i=in.read(tmp, 0, 1024);
                        if(i<0)break;
                        msg += new String(tmp, 0, i);
                    }

                    if(channel.isClosed()){
                        if(in.available()>0) continue;
                        // Log.i("SSHTEST", "FIN, retval = " + String.valueOf(channel.getExitStatus()));
                        break;
                    }
                }

            } catch (IOException e) {
                callback.onIOFailure(e);
                channel.disconnect();
                session.disconnect();
                return null;
            }

            channel.disconnect();
            session.disconnect();

            return msg;
        }

        @Override
        protected void onPostExecute(final String response) {
            if (response != null) {
                callback.onResponseReceived(response);
            }
        }
    }


}
