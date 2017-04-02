package com.example.younsuk.photogallery;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;

/**
 * Created by Younsuk on 10/5/2015.
 */
public class PollService extends IntentService {

    private static final String TAG = "PollService";
    private static final int POLL_INTERVAL_TEST = 1000*60;
    private static final long POLL_INTERVAL = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final String ACTION_SHOW_NOTIFICATION = "com.example.younsuk.photogallery.show_notification";
    public static final String PERMISSION_PRIVATE = "com.example.younsuk.photogallery.PRIVATE";
    public static final String REQUEST_CODE = "REQUEST_CODE";
    public static final String NOTIFICATION = "NOTIFICATION";
    //----------------------------------------------------------------------------------------------
    public static Intent newIntent(Context context){
        return new Intent(context, PollService.class);
    }
    //----------------------------------------------------------------------------------------------
    public PollService(){
        super(TAG);
    }
    //----------------------------------------------------------------------------------------------
    public static void setServiceAlarm(Context context, boolean isOn){
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        if (isOn)
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_TEST, pendingIntent);
        else {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }

        QueryPreferences.setAlarmOn(context, isOn);
    }
    //----------------------------------------------------------------------------------------------
    public static boolean isServiceAlarmOn(Context context){
        Intent intent = PollService.newIntent(context);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE);

        return pendingIntent != null;
    }
    //----------------------------------------------------------------------------------------------
    @Override
    protected void onHandleIntent(Intent intent) {
        if (!networkAvailableAndConnected())
            return;

        List<GalleryItem> items;
        String query = QueryPreferences.getStoredQuery(this);
        if (query == null)
            items = new FlickrFetchr().fetchRecentPhotos(PhotoGalleryFragment.sPageNumber);//onScroll
        else
            items = new FlickrFetchr().searchPhotos(query, PhotoGalleryFragment.sPageNumber);//onScroll

        if (items.size() == 0)
            return;

        String lastResultId = QueryPreferences.getLastResultId(this);
        String resultId = items.get(0).getId();
        if (resultId.equals(lastResultId))
            Log.i(TAG, "Got an old result: " + resultId);
        else{
            Log.i(TAG, "Got a new result: " + resultId);

            Resources resources = getResources();
            Intent i = PhotoGalleryFragment.newIntent(this);
            PendingIntent pi = PendingIntent.getActivity(this, 0, i, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setTicker(resources.getString(R.string.new_pictures_title))
                    .setSmallIcon(android.R.drawable.ic_menu_report_image)
                    .setContentTitle(resources.getString(R.string.new_pictures_title))
                    .setContentText(resources.getString(R.string.new_pictures_text))
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            showBackgroundNotification(0, notification);
        }


        QueryPreferences.setLastResultId(this, resultId);

    }
    //----------------------------------------------------------------------------------------------
    private void showBackgroundNotification(int requestCode, Notification notification){
        Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
        intent.putExtra(REQUEST_CODE, requestCode);
        intent.putExtra(NOTIFICATION, notification);
        sendOrderedBroadcast(intent, PERMISSION_PRIVATE, null, null, Activity.RESULT_OK, null, null);
    }
    //----------------------------------------------------------------------------------------------
    private boolean networkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        boolean isAvailable = cm.getActiveNetworkInfo() != null;
        boolean isConnected = isAvailable && cm.getActiveNetworkInfo().isConnected();

        return isConnected;
    }
    //----------------------------------------------------------------------------------------------
}
