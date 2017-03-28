package com.phonegap.plugins.twiliovoice.gcm;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.twilio.voice.CallInvite;

import com.google.android.gms.gcm.GcmListenerService;

import com.phonegap.plugins.twiliovoice.TwilioVoicePlugin;


import static android.R.attr.data;

/*
 * Based on https://github.com/twilio/voice-quickstart-android/blob/master/app/src/main/java/com/twilio/voice/quickstart/gcm/VoiceGCMListenerService.java
 * From Twilio
 */

public class VoiceGCMListenerService extends GcmListenerService {

    private static final String TAG = TwilioVoicePlugin.TAG;

    /*
     * Notification related keys
     */
    private static final String NOTIFICATION_ID_KEY = "NOTIFICATION_ID";
    private static final String CALL_SID_KEY = "CALL_SID";

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onMessageReceived(String from, Bundle bundle) {
        Log.d(TAG, "onMessageReceived " + from);

        Log.d(TAG, "Received onMessageReceived()");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Bundle data: " + bundle.toString());

        if (CallInvite.isValidMessage(bundle)) {
             /*
             * Generate a unique notification id using the system time
             */
            int notificationId = (int) System.currentTimeMillis();
            /*
             * Create an CallInvite from the bundle
             */
            CallInvite callInvite = CallInvite.create(bundle);
            sendCallInviteToPlugin(callInvite, notificationId);
            showNotification(callInvite, notificationId);
        }
    }

    /*
     * Show the notification in the Android notification drawer
     */
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private void showNotification(CallInvite callInvite, int notificationId) {
        String callSid = callInvite.getCallSid();

        Log.d(TAG, "showNotification()");

        if (!callInvite.isCancelled()) {
            /*
             * Create a PendingIntent to specify the action when the notification is
             * selected in the notification drawer
             */
            
            //start up the launch activity for the app (Cordova)
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            intent.setAction(TwilioVoicePlugin.ACTION_INCOMING_CALL);
            intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_INVITE, callInvite);
            intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_NOTIFICATION_ID, notificationId);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

            Log.d(TAG, "showNotification(): Created pending intent");


            /*
             * Pass the notification id and call sid to use as an identifier to cancel the
             * notification later
             */
            Bundle extras = new Bundle();
            extras.putInt(NOTIFICATION_ID_KEY, notificationId);
            extras.putString(CALL_SID_KEY, callSid);

            /*
             * Create the notification shown in the notification drawer
             */
            int iconIdentifier = getResources().getIdentifier("icon", "mipmap", getPackageName());
            int ringingResourceId =  getResources().getIdentifier("ringing", "raw", getPackageName());
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(iconIdentifier)
                            .setContentTitle("Incoming Call")
                            .setContentText(callInvite.getFrom() + " is calling.")
                            .setAutoCancel(true)
                            .setSound(Uri.parse("android.resource://"
                                + getPackageName() + "/" + ringingResourceId))
                            .setExtras(extras)
                            .setContentIntent(pendingIntent)
                            .setGroup("voice_app_notification")
                            .setColor(Color.rgb(225, 225, 225));

            Log.d(TAG, "showNotification(): built notification");

            notificationManager.notify(notificationId, notificationBuilder.build());

            Log.d(TAG, "showNotification(): show notification");
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                /*
                 * If the incoming call was cancelled then remove the notification by matching
                 * it with the call sid from the list of notifications in the notification drawer.
                 */
                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                for (StatusBarNotification statusBarNotification : activeNotifications) {
                    Notification notification = statusBarNotification.getNotification();
                    Bundle extras = notification.extras;
                    String notificationCallSid = extras.getString(CALL_SID_KEY);
                    if (callSid.equals(notificationCallSid)) {
                        notificationManager.cancel(extras.getInt(NOTIFICATION_ID_KEY));
                    }
                }
            } else {
                /*
                 * Prior to Android M the notification manager did not provide a list of
                 * active notifications so we lazily clear all the notifications when
                 * receiving a cancelled call.
                 *
                 * In order to properly cancel a notification using
                 * NotificationManager.cancel(notificationId) we should store the call sid &
                 * notification id of any incoming calls using shared preferences or some other form
                 * of persistent storage.
                 */
                notificationManager.cancelAll();
            }
        }
    }

    /*
     * Send the IncomingCallMessage to the Plugin
     */
    private void sendCallInviteToPlugin(CallInvite incomingCallMessage, int notificationId) {



        Intent intent = new Intent(TwilioVoicePlugin.ACTION_INCOMING_CALL);
        intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_INVITE, incomingCallMessage);
        intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}