package com.nicolasbrailo.vlcfreemote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import com.nicolasbrailo.vlcfreemote.model.Server;
import com.nicolasbrailo.vlcfreemote.model.VlcStatus;
import com.nicolasbrailo.vlcfreemote.vlc_connector.Cmd_TogglePlay;
import com.nicolasbrailo.vlcfreemote.vlc_connector.RemoteVlc;
import com.nicolasbrailo.vlcfreemote.vlc_connector.VlcCommand;

/**
 * TODO
 * Note: If the device is asleep when it is time for an update (as defined by updatePeriodMillis),
 * then the device will wake up in order to perform the update. If you don't update more than once
 * per hour, this probably won't cause significant problems for the battery life. If, however, you
 * need to update more frequently and/or you do not need to update while the device is asleep, then
 * you can instead perform updates based on an alarm that will not wake the device. To do so, set an
 * alarm with an Intent that your AppWidgetProvider receives, using the AlarmManager. Set the alarm
 * type to either ELAPSED_REALTIME or RTC, which will only deliver the alarm when the device is awake.
 * Then set updatePeriodMillis to zero ("0").
 */
public class MiniPlayerControllerWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        Intent intent2 = new Intent(context, MiniPlayerControllerWidget.class);
        intent2.setAction("com.nicolasbrailo.vlcfreemote.FOOO");
        PendingIntent pendingIntent2 = PendingIntent.getBroadcast(context, 42, intent2, 0);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.mini_player_controller_widget);
        views.setOnClickPendingIntent(R.id.wMiniPlayerController_Open, pendingIntent);
        views.setOnClickPendingIntent(R.id.wMiniPlayerController_BtnPlayPause, pendingIntent2);

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {

    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static class Foo implements    VlcCommand.GeneralCallback, VlcStatus.Observer {
        @Override
        public void onAuthError() {
            Log.e("XXXXXXXXXXX", "onAuthError");
        }

        @Override
        public void onConnectionError() {
            Log.e("XXXXXXXXXXX", "onConnectionError");
        }

        @Override
        public void onSystemError(Exception e) {
            Log.e("XXXXXXXXXXX", "onSystemError");
        }

        @Override
        public void onVlcStatusUpdate(VlcStatus results) {
            Log.e("XXXXXXXXXXX", "onVlcStatusUpdate");
        }

        @Override
        public void onVlcStatusFetchError() {
            Log.e("XXXXXXXXXXX", "onVlcStatusFetchError");
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("XXXXXXXXXXX", "XXXXXXXXXX");

        if ("com.nicolasbrailo.vlcfreemote.FOOO".equals(intent.getAction())) {
            Log.e("XXXXXXXXXXX", "IT LIVES!");
            Server srv = ServerSelectView.getLastUsedServer(context);
            if (srv != null) {
                Log.e("XXXXXXXXX", "Last srv is " + srv.ip);

                RemoteVlc vlc = new RemoteVlc(srv, new Foo());
                vlc.exec(new Cmd_TogglePlay(vlc));

            } else {
                Log.e("XXXXXXXXX", "Last srv is null");
            }
        }

        super.onReceive(context, intent);
    }
}

