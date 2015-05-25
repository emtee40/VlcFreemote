package com.nico.vlcfremote;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.widget.RemoteViews;


/**
 * Implementation of App Widget functionality.
 */
public class VlcFreemoteWidget extends AppWidgetProvider {

    static private final String EVENT_CALLBACK_BTN_PLAYPAUSE = VlcFreemoteWidget.class.getName() + "/" + String.valueOf(R.id.wPlayerWidget_BtnPlayPause);
    static private final String EVENT_CALLBACK_BTN_NEXT      = VlcFreemoteWidget.class.getName() + "/" + String.valueOf(R.id.wPlayerWidget_BtnNext);

    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            // Construct the RemoteViews object
            final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.vlc_freemote_widget);
            final ComponentName watcher = new ComponentName(context, VlcFreemoteWidget.class);

            Intent intent = new Intent("android.intent.action.MAIN");
            intent.addCategory("android.intent.category.LAUNCHER");
            intent.setComponent(new ComponentName(MainActivity.class.getPackage().getName(), MainActivity.class.getName()));
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
            views.setOnClickPendingIntent(R.id.wPlayerWidget_OpenApp, pendingIntent);

            views.setOnClickPendingIntent(R.id.wPlayerWidget_BtnPlayPause, getPendingSelfIntent(context, EVENT_CALLBACK_BTN_PLAYPAUSE));
            views.setOnClickPendingIntent(R.id.wPlayerWidget_BtnNext, getPendingSelfIntent(context, EVENT_CALLBACK_BTN_NEXT));

            // Instruct the widget manager to update the widget
            // appWidgetManager.updateAppWidget(appWidgetIds[i], views);
            appWidgetManager.updateAppWidget(watcher, views);
        }
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        if (EVENT_CALLBACK_BTN_PLAYPAUSE.equals(intent.getAction())) {
        } else if (EVENT_CALLBACK_BTN_NEXT.equals(intent.getAction())) {
        }
    }

    private PendingIntent getPendingSelfIntent(final Context context, final String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }
}


