package edu.columbia.coms6998.parkwizard;


import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class GcmMessageHandler extends IntentService {
	
	public static int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    String FILE_PERSONAL="PersonalDetails";
     
    public GcmMessageHandler() {
        super("GcmMessageHandler");
    }
   
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();

        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.

        String title = extras.getString("title");
        String type=extras.getString("type");
        String studentID = extras.getString("studentid");
        int id = Integer.parseInt(extras.getString("id"));

       buildNotification(title,type,studentID,id);
        for (String key : extras.keySet()) {
            Object value = extras.get(key);
            Log.d("NOTIFICATION", String.format("%s %s (%s)", key,
                    value.toString(), value.getClass().getName()));
        }
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    public void buildNotification(String title,String type,String studentID,int id){
        
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent i=new Intent(this, LoginActivity.class);
        //i.putExtra("EVENT","1");
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        i.putExtra("notsid", studentID);
        i.putExtra("id", id);
        i.setData(Uri.parse(studentID+type+id));
        Log.d("HANDLER", studentID);
        
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);


        /*NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.i1)
                        .setColor(getResources().getColor(R.color.theme_blue))
                        .setContentTitle(type)
                        .setContentText(title)
                        .setAutoCancel(true)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setPriority(Notification.PRIORITY_MAX); // requires VIBRATE permission
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);

        NotificationCompat.InboxStyle inboxStyle =
                new NotificationCompat.InboxStyle();

        inboxStyle.setBigContentTitle(type);
        inboxStyle.addLine(title);
        try {
            SharedPreferences sp = getSharedPreferences(FILE_PERSONAL + studentID, 0);
            JSONObject jsonObject = new JSONObject(sp.getString("personal-details", null));
            String name = jsonObject.getString("firstname")+" "+jsonObject.getString("middlename")+" "+jsonObject.getString("lastname");
            inboxStyle.setSummaryText(name);
        }catch (Exception e){
            e.printStackTrace();
        }
        mBuilder.setStyle(inboxStyle);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        incrementNOTID();*/

    }

    synchronized public void incrementNOTID(){
        NOTIFICATION_ID++;
    }
}
