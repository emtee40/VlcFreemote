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


import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nico.vlcfremote.utils.NetworkingUtils;

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.List;
import java.util.Properties;


public class ServerSelectView extends Fragment implements View.OnClickListener {

    private static final int DEFAULT_SERVER_SCAN_PORT = 8080;

    public interface OnServerSelectedCallback {
        void onNewServerSelected(final String ip, final String port, final String password);
    }

    private AsyncTask<Void, Void, List<String>> scannerService;
    private OnServerSelectedCallback onServerSelectCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_server_select, container, false);
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

        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (activity == null) return;
        activity.findViewById(R.id.wServerSelect_ToggleServerScanning).setOnClickListener(this);
        activity.findViewById(R.id.wServerSelect_CustomServerSet).setOnClickListener(this);

        scanServers();
    }

    private void toggleServerScanning() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        if (activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).getVisibility() == View.GONE) {
            // No scanning ongoing, start one
            scanServers();
        } else {
            // Currently scanning, we should cancel
            this.scannerService.cancel(true);
            activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
            activity.findViewById(R.id.wServerSelect_ScannedServersList).setVisibility(View.VISIBLE);
            ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_start);
        }
    }

    synchronized void scanServers() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.wServerSelect_ScannedServersList).setVisibility(View.GONE);
        ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_stop);

        final List<String> interfaces = NetworkingUtils.getLocalIPAddresses();
        if (interfaces.size() == 0) {
            CharSequence msg = getResources().getString(R.string.status_no_network_detected);
            Toast toast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        final ServerSelectView self = this;
        this.scannerService = new NetworkingUtils.ServerScanner(interfaces, DEFAULT_SERVER_SCAN_PORT, new NetworkingUtils.ServerScanner.Callback() {
            @Override
            public void onServerDiscovered(List<String> ips) {
                if (ips == null) throw new RuntimeException(ServerSelectView.class.getName() + " got null list of IPs.");

                // If there's no activity we're not being displayed, so it's better not to update the UI
                final FragmentActivity activity = getActivity();
                if (activity == null) return;

                final Servers_ViewAdapter adapt = new Servers_ViewAdapter(self, activity, ips);
                final ListView lst = (ListView) activity.findViewById(R.id.wServerSelect_ScannedServersList);
                if (lst != null) {
                    lst.setAdapter(adapt);
                    ((ArrayAdapter) lst.getAdapter()).notifyDataSetChanged();

                    activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
                    activity.findViewById(R.id.wServerSelect_ScannedServersList).setVisibility(View.VISIBLE);
                    ((Button) activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_start);
                }
            }
        });
        this.scannerService.execute();
    }

    private void setNewServer(final String ip, final String port) {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        final String savedPwd = activity.getPreferences(0).getString("VLC_" + ip + ":" + String.valueOf(port), "");

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
                final FragmentActivity activity = getActivity();
                if (activity == null) return;

                final String password = input.getText().toString();
                SharedPreferences.Editor cfg = activity.getPreferences(0).edit();
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
        NetworkingUtils.SendSSHCommand cmd = new NetworkingUtils.SendSSHCommand("192.168.1.11", "laptus", "123", 22,
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


        /*
        // If there's no activity we're not being displayed, so it's better not to update the UI
        final FragmentActivity activity = getActivity();
        if (activity == null) return;

        final String ip = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerIp)).getText().toString();
        final String port = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerPort)).getText().toString();
        setNewServer(ip, port);
        */
    }

    private void useScannedServer(final String ip) {
        setNewServer(ip, String.valueOf(DEFAULT_SERVER_SCAN_PORT));
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
                useScannedServer((String) v.getTag());
                break;
            default:
                throw new RuntimeException(ServerSelectView.class.getName() + " received an event it doesn't know how to handle.");
        }
    }

    private static class Servers_ViewAdapter extends ArrayAdapter<String> {
        private final List<String> items;
        private static final int layoutResourceId = R.layout.fragment_server_select_element;
        private final Context context;
        private final View.OnClickListener onClickCallback;

        public static class Row {
            String value;
            ImageView wServerSelect_PrettyUselessIcon;
            TextView wServerSelect_ScannedServerAddress;
            ImageButton wServerSelect_ScannedServerSelect;
        }

        public Servers_ViewAdapter(View.OnClickListener onClickCallback, Context context, List<String> items) {
            super(context, layoutResourceId, items);
            this.context = context;
            this.items = items;
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
            holder.value = items.get(position);

            holder.wServerSelect_PrettyUselessIcon = (ImageView)row.findViewById(R.id.wServerSelect_PrettyUselessIcon);
            holder.wServerSelect_PrettyUselessIcon.setOnClickListener(onClickCallback);


            holder.wServerSelect_ScannedServerAddress = (TextView)row.findViewById(R.id.wServerSelect_ScannedServerAddress);
            holder.wServerSelect_ScannedServerAddress.setText(String.valueOf(holder.value));
            holder.wServerSelect_ScannedServerAddress.setTag(holder.value);
            holder.wServerSelect_ScannedServerAddress.setOnClickListener(onClickCallback);

            holder.wServerSelect_ScannedServerSelect = (ImageButton)row.findViewById(R.id.wServerSelect_ScannedServerSelect);
            holder.wServerSelect_ScannedServerSelect.setTag(String.valueOf(holder.value));
            holder.wServerSelect_ScannedServerSelect.setOnClickListener(onClickCallback);

            row.setTag(holder);

            return row;
        }
    }
}
