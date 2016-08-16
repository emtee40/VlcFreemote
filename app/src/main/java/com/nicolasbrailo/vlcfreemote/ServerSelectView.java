package com.nicolasbrailo.vlcfreemote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import com.nicolasbrailo.vlcfreemote.local_settings.LocalSettings;
import com.nicolasbrailo.vlcfreemote.local_settings.RememberedServers;
import com.nicolasbrailo.vlcfreemote.model.Server;
import com.nicolasbrailo.vlcfreemote.net_utils.ServerScanner;

import java.util.ArrayList;


public class ServerSelectView extends Fragment implements View.OnClickListener {

    public interface ServerSelectionCallback {
        void onNewServerSelected(final Server srv);
    }

    private ServerScanner serverScanner = null;
    private Servers_ViewAdapter listViewAdapter;
    private ServerSelectionCallback callback;

    /************************************************************/
    /* Android stuff                                            */
    /************************************************************/

    public ServerSelectView() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_server_select_view, container, false);

        this.listViewAdapter = new Servers_ViewAdapter(this, getActivity());
        ((ListView) v.findViewById(R.id.wServerSelect_ScannedServersList)).setAdapter(listViewAdapter);

        v.findViewById(R.id.wServerSelect_ToggleServerScanning).setOnClickListener(this);
        v.findViewById(R.id.wServerSelect_CustomServerSet).setOnClickListener(this);

        return v;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);

        try {
            callback = (ServerSelectionCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement onServerSelectCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback = null;
    }

    /************************************************************/
    /* Event handlers                                           */
    /************************************************************/

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wServerSelect_ToggleServerScanning:
                toggleServerScanning();
                break;

            case R.id.wServerSelect_CustomServerSet:
                onCustomServerSelected();
                break;

            case R.id.wServerSelect_ScannedServerAddress:
            case R.id.wServerSelect_ScannedServerSelect:
                onServerSelected((Server) v.getTag());
                break;

            default:
                throw new RuntimeException(getClass().getName() + " received an event it doesn't know how to handle.");
        }
    }

    private void onCustomServerSelected() {
        final FragmentActivity activity = getActivity();
        final String ip = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerIp)).getText().toString();
        final String str_port = ((EditText)activity.findViewById(R.id.wServerSelect_CustomServerPort)).getText().toString();

        // If port is not a valid int, the connection will just fail later on.
        final Server srv = new Server(ip, Integer.parseInt(str_port), null);

        onServerSelected(srv);
    }

    private void onServerSelected(final Server srv) {
        setScanModeOff();
        Log.i(getClass().getSimpleName(), "Selected server " + srv.ip + ":" + srv.vlcPort);

        if (srv.vlcPort == null) {
            CharSequence msg = "You can't use this server: functionality unimplemented (yet).";
            Toast toast = Toast.makeText(getActivity().getApplicationContext(), msg, Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        // Get last used pass for this server, if known. This server will have some settings saved
        // (Like last used path)
        final RememberedServers db = new RememberedServers(getContext());
        Server dbServer;
        try {
            dbServer = db.getRememberedServer(srv);
            if (dbServer == null) dbServer = srv;
        } catch (LocalSettings.LocalSettingsError localSettingsError) {
            Log.e(getClass().getSimpleName(), localSettingsError.getMessage());
            dbServer = srv;
        }

        // Show a dialog to confirm the pass (or enter a new one)
        final Server rememberedServer = dbServer;
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.server_select_request_password_dialog_title));
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setText(rememberedServer.getPassword());
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                final String password = input.getText().toString();
                rememberedServer.setPassword(password);

                db.rememberServer(rememberedServer);
                callback.onNewServerSelected(rememberedServer);
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

    @Override
    public void setUserVisibleHint(boolean visible)
    {
        super.setUserVisibleHint(visible);

        if (visible && isResumed())
        {
            toggleServerScanning();
        } else {
            setScanModeOff();
        }
    }

    /************************************************************/
    /* UI Logic                                                 */
    /************************************************************/
    synchronized private void toggleServerScanning() {
        if (serverScanner != null) {
            Log.i(getClass().getSimpleName(), "Cancelling network scan");
            serverScanner.cancel(true);
            return;
        }

        Log.i(getClass().getSimpleName(), "Start network scan");
        startScanMode();

        serverScanner = new ServerScanner(new ServerScanner.Callback() {
            @Override
            public void onServerDiscovered(Server srv) {
                Log.i(getClass().getSimpleName(), "Found server " + srv.ip);
                listViewAdapter.add(srv);
            }

            @Override
            public void onScanFinished() {
                setScanModeOff();
                Log.i(getClass().getSimpleName(), "Completed network scan");
            }

            @Override
            public void onScanCancelled() {
                setScanModeOff();
                Log.i(getClass().getSimpleName(), "Cancelled network scan");
            }

            @Override
            public void onNoNetworkAvailable() {
                setScanModeOff();

                // If there's no activity we're not being displayed, so it's better not to update the UI
                if (!isAdded()) return;
                final FragmentActivity activity = getActivity();

                CharSequence msg = getResources().getString(R.string.status_no_network_detected);
                Toast toast = Toast.makeText(activity.getApplicationContext(), msg, Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    synchronized private void startScanMode() {
        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.VISIBLE);
        ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning)).setText(R.string.server_select_toggle_scanning_stop);

        // Clean old servers list
        this.listViewAdapter.clear();
    }

    synchronized private void setScanModeOff() {
        if (serverScanner != null) serverScanner.cancel(true);
        serverScanner = null;

        // If there's no activity we're not being displayed, so it's better not to update the UI
        if (!isAdded()) return;
        final FragmentActivity activity = getActivity();

        activity.findViewById(R.id.wServerSelect_ScanningServersIndicator).setVisibility(View.GONE);
        ((Button)activity.findViewById(R.id.wServerSelect_ToggleServerScanning))
                .setText(R.string.server_select_toggle_scanning_start);
    }

    /**
     * Returns the last server the user tried to connect to. A connection attempt is defined as
     * "the user supplied a password for the server", regardless of the attempt success.
     * @param context Since this method may be called before onAttach, there may be no activity
     *                known. Caller should specify activity context.
     * @return last used server
     */
    public Server getLastUsedServer(Context context) {
        try {
            return (new RememberedServers(context)).getLastUsedServer();
        } catch (LocalSettings.LocalSettingsError localSettingsError) {
            Log.e(getClass().getSimpleName(), localSettingsError.getMessage());
            return null;
        }
    }

    /************************************************************/
    /* List view stuff                                          */
    /************************************************************/

    private static class Servers_ViewAdapter extends ArrayAdapter<Server> {
        private static final int layoutResourceId = R.layout.fragment_server_select_element;
        private final Context context;
        private final View.OnClickListener onClickCallback;

        public static class Row {
            Server value;
            ImageView wServerSelect_ServerTypeIcon;
            TextView wServerSelect_ScannedServerAddress;
            ImageButton wServerSelect_ScannedServerSelect;
        }

        public Servers_ViewAdapter(View.OnClickListener onClickCallback, Context context) {
            super(context, layoutResourceId, new ArrayList<Server>());
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
