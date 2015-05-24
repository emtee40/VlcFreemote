package com.nico.vlcfremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.JSchException;
import com.nico.vlcfremote.utils.NetworkingUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ServerSelectView extends Fragment implements View.OnClickListener {

    private static final int DEFAULT_VLC_SERVER_SCAN_PORT = 8080;
    private static final int DEFAULT_SSH_SERVER_SCAN_PORT = 22;

    public interface OnServerSelectedCallback {
        void onNewServerSelected(final String ip, final String port, final String password);
    }

    private NetworkingUtils.ServerScanner scannerService;
    private OnServerSelectedCallback onServerSelectCallback;
    private Servers_ViewAdapter listViewAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_server_select, container, false);
        this.listViewAdapter = new Servers_ViewAdapter(this, getActivity());
        ((ListView) v.findViewById(R.id.wServerSelect_ScannedServersList)).setAdapter(listViewAdapter);

        v.findViewById(R.id.wServerSelect_ToggleServerScanning).setOnClickListener(this);
        v.findViewById(R.id.wServerSelect_CustomServerSet).setOnClickListener(this);
        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            onServerSelectCallback = (OnServerSelectedCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onServerSelectCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        onServerSelectCallback = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        // If we're not visible, don't start scanning
        if (!getUserVisibleHint()) return;
        scanServers();
    }

    private void toggleServerScanning() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        if (activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).getVisibility() == View.GONE) {
            // No scanning ongoing, start one
            scanServers();
        } else {
            // Currently scanning, we should cancel
            if (this.scannerService != null) this.scannerService.cancel(true);
            activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
            ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_start);
        }
    }

    synchronized public void scanServers() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.VISIBLE);
        ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_stop);

        final List<String> interfaces = NetworkingUtils.getLocalIPAddresses();
        if (interfaces.size() == 0) {
            CharSequence msg = getResources().getString(R.string.status_no_network_detected);
            Toast toast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG);
            toast.show();
            activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
            ((Button) activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_start);
            return;
        }

        // Clean old servers list
        this.listViewAdapter.clear();

        this.scannerService = new NetworkingUtils.ServerScanner(interfaces, DEFAULT_VLC_SERVER_SCAN_PORT, DEFAULT_SSH_SERVER_SCAN_PORT,
                new NetworkingUtils.ServerScanner.Callback() {

            @Override
            public void onServerDiscovered(NetworkingUtils.ScannedServer srv) {
                listViewAdapter.add(srv);
            }

            @Override
            public void onScanFinished(List<NetworkingUtils.ScannedServer> ips) {
                if (ips == null) throw new RuntimeException(ServerSelectView.class.getName() + " got null list of servers.");

                // If there's no activity we're not being displayed, so it's better not to update the UI
                if (!isAdded()) return;
                final FragmentActivity activity = getActivity();

                activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
                ((Button) activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_start);
            }
        });

        // Exec this on the background, so we don't block possible BG ops for main VLC connection
        this.scannerService.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void)null);
    }

    private void setNewServer(final String ip, final String port) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        final String savedPwd = activity.getPreferences(Context.MODE_PRIVATE).getString("VLC_" + ip + ":" + String.valueOf(port), "");

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(getString(R.string.server_select_request_password_dialog_title));
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText(savedPwd);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // If there's no activity we're not being displayed, so it's better not to update the UI
                if (!isAdded()) return;
                final FragmentActivity activity = getActivity();

                final String password = input.getText().toString();
                SharedPreferences.Editor cfg = activity.getPreferences(Context.MODE_PRIVATE).edit();
                cfg.putString("VLC_" + ip + ":" + String.valueOf(port), password);
                cfg.apply();
                onServerSelectCallback.onNewServerSelected(ip, port, password);
            }
        });

        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void useCustomServer() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        final String ip = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerIp)).getText().toString();
        final String port = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerPort)).getText().toString();
        setNewServer(ip, port);
    }

    private void useScannedServer(final NetworkingUtils.ScannedServer srv) {
        if (srv.vlcPort != null) {
            setNewServer(srv.ip, String.valueOf(srv.vlcPort));
        } else {
            // TODO
            Log.e("TAG", "Starting SSH servers not yet supported");
            final String START_VLC = "export DISPLAY=:0; vlc -f &";

            NetworkingUtils.SendSSHCommand cmd = new NetworkingUtils.SendSSHCommand(START_VLC, srv.ip, "blaptus", "qwepoi", 22,
                    new NetworkingUtils.SendSSHCommand.Callback() {
                        @Override
                        public void onResponseReceived(String response) {
                            Log.i("Funciono!", response);
                        }

                        @Override
                        public void onConnectionFailure(JSchException e) {
                            Log.i("Connect fail", e.getCause() + "/" + e.getMessage());
                        }

                        @Override
                        public void onIOFailure(IOException e) {
                            Log.i("IO Fail", e.getMessage());
                        }

                        @Override
                        public void onExecFail(JSchException e) {
                            Log.i("Exec fail", e.getCause() + "/" + e.getMessage());
                        }
                    });
            cmd.execute();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wServerSelect_CustomServerSet:
                useCustomServer();
                break;
            case R.id.wServerSelect_ToggleServerScanning:
                toggleServerScanning();
                break;
            case R.id.wServerSelect_ScannedServerAddress:
            case R.id.wServerSelect_ScannedServerSelect:
                useScannedServer((NetworkingUtils.ScannedServer) v.getTag());
                break;
            default:
                throw new RuntimeException(ServerSelectView.class.getName() + " received an event it doesn't know how to handle.");
        }
    }

    private static class Servers_ViewAdapter extends ArrayAdapter<NetworkingUtils.ScannedServer> {
        private static final int layoutResourceId = R.layout.fragment_server_select_element;
        private final Context context;
        private final View.OnClickListener onClickCallback;

        public static class Row {
            NetworkingUtils.ScannedServer value;
            ImageView wServerSelect_ServerTypeIcon;
            TextView wServerSelect_ScannedServerAddress;
            ImageButton wServerSelect_ScannedServerSelect;
        }

        public Servers_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<NetworkingUtils.ScannedServer>());
            this.context = context;
            this.onClickCallback = onClickCallback;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final View row;
            if (convertView == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
            } else {
                row = convertView;
            }

            Row holder = new Row();
            holder.value = this.getItem(position);

            holder.wServerSelect_ServerTypeIcon = (ImageView)row.findViewById(R.id.wServerSelect_ServerTypeIcon);
            holder.wServerSelect_ServerTypeIcon.setOnClickListener(onClickCallback);
            if (holder.value.vlcPort != null) {
                holder.wServerSelect_ServerTypeIcon.setImageResource(R.mipmap.ic_vlc_icon);
            } else {
                holder.wServerSelect_ServerTypeIcon.setImageResource(R.mipmap.ic_tux_icon);
            }

            holder.wServerSelect_ScannedServerAddress = (TextView)row.findViewById(R.id.wServerSelect_ScannedServerAddress);
            holder.wServerSelect_ScannedServerAddress.setText(String.valueOf(holder.value.ip));
            holder.wServerSelect_ScannedServerAddress.setTag(holder.value);
            holder.wServerSelect_ScannedServerAddress.setOnClickListener(onClickCallback);

            holder.wServerSelect_ScannedServerSelect = (ImageButton)row.findViewById(R.id.wServerSelect_ScannedServerSelect);
            holder.wServerSelect_ScannedServerSelect.setTag(holder.value);
            holder.wServerSelect_ScannedServerSelect.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
