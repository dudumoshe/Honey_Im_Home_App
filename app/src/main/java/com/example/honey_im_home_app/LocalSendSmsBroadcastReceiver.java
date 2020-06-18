package com.example.honey_im_home_app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class LocalSendSmsBroadcastReceiver extends BroadcastReceiver {
    public static final String phoneNumberKey = "PHONE";
    public static final String smsMessageContentKey = "CONTENT";
    public static final String actionName = "POST_PC.ACTION_SEND_SMS";
    public static final String notificationsID = "43434";
    public static final int smsNotificationID = 4334;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(actionName)) {
            return;
        }
        boolean hasPermissions = ActivityCompat.checkSelfPermission(context,
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        if(!hasPermissions) {
            Log.e("LocalSendSmsBR", "No SMS permissions");
            return;
        }
        String number = intent.getStringExtra(phoneNumberKey);
        String content = intent.getStringExtra(smsMessageContentKey);

        if (number == null || content == null) {
            Log.e("LocalSendSmsBR", "No phone number or content");
            return;
        }
        SmsManager.getDefault().sendTextMessage(number, null, content, null,
                null);

        createNotificationChannel(context);

        Notification smsNotification = new NotificationCompat.Builder(context, notificationsID)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Honey Sending SMS")
                .setContentText("sending sms to " + number + ": " + content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(smsNotificationID, smsNotification);
    }

    private void createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "homeSMSNotifications";
            String description = "homeSMSNotifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(notificationsID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
