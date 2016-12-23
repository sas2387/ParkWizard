package edu.columbia.coms6998.parkwizard;


import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class GcmMessageHandler extends IntentService {

    public static int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    String FILE_PERSONAL = "PersonalDetails";

    public GcmMessageHandler() {
        super("GcmMessageHandler");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.

        Log.d("NOTIFICATION", "Obtained");
        Log.d("N", "" + Boolean.parseBoolean(extras.getString("success")));
        Log.d("N", extras.getString("message"));


        String message = extras.getString("message");

        buildNotification(message);


       /*buildNotification(title,type,studentID,id);
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            Log.d("NOTIFICATION", String.format("%s %s (%s)", key,
                    value.toString(), value.getClass().getName()));
        }*/
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    public void buildNotification(String message) {

        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent i = new Intent(this, LoginActivity.class);
        //i.putExtra("EVENT","1");
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Notification")
                .setContentText(message)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setPriority(Notification.PRIORITY_MAX); // requires VIBRATE permission
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        incrementNOTID();

    }

    synchronized public void incrementNOTID() {
        NOTIFICATION_ID++;
    }
}
