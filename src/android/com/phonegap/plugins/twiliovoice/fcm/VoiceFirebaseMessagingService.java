package com.phonegap.plugins.twiliovoice.fcm;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.net.Uri;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.twilio.voice.CallInvite;
import com.twilio.voice.MessageException;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;
import static android.R.attr.data;

import com.ignitras.loudcloud.R;
import static android.R.attr.icon;
import static android.R.attr.mipMap;
import static android.R.attr.resource;

import com.phonegap.plugins.twiliovoice.SoundPoolManager;
import com.phonegap.plugins.twiliovoice.TwilioVoicePlugin;

import java.util.Map;

public class VoiceFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "VoiceFCMService";
    private static final String NOTIFICATION_ID_KEY = "NOTIFICATION_ID";
    private static final String CALL_SID_KEY = "CALL_SID";
    private static final String VOICE_CHANNEL = "default";

    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received onMessageReceived()");
        Log.d(TAG, "Bundle data: " + remoteMessage.getData());
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            final int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            Voice.handleMessage(this, data, new MessageListener() {
                @Override
                public void onCallInvite(CallInvite callInvite) {
                    VoiceFirebaseMessagingService.this.notify(callInvite, notificationId);
                    VoiceFirebaseMessagingService.this.sendCallInviteToPlugin(callInvite, notificationId);
                }

                @Override
                public void onError(MessageException messageException) {
                    Log.e(TAG, messageException.getLocalizedMessage());
                }
            });
        }
    }

    private void notify(CallInvite callInvite, int notificationId) {
        String callSid = callInvite.getCallSid();
        Notification notification = null;

        if (callInvite.getState() == CallInvite.State.PENDING) {
            Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            intent.setAction(TwilioVoicePlugin.ACTION_INCOMING_CALL);
            intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_NOTIFICATION_ID, notificationId);
            intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_INVITE, callInvite);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent =
                    PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            /*
             * Pass the notification id and call sid to use as an identifier to cancel the
             * notification later
             */
            Bundle extras = new Bundle();
            extras.putInt(NOTIFICATION_ID_KEY, notificationId);
            extras.putString(CALL_SID_KEY, callSid);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel callInviteChannel = new NotificationChannel(VOICE_CHANNEL,
                        "Primary Voice Channel", NotificationManager.IMPORTANCE_DEFAULT);
                callInviteChannel.setLightColor(Color.RED);
                callInviteChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                notificationManager.createNotificationChannel(callInviteChannel);

                notification = buildNotification("Call Invite from : " + callInvite.getFrom(), pendingIntent, extras);
                notificationManager.notify(notificationId, notification);
            } else {
                int iconIdentifier = getResources().getIdentifier("icon", "mipmap", getPackageName());
                int incomingCallAppNameId = (int) getResources().getIdentifier("incoming_call_app_name", "string", getPackageName());
                String contentTitle = getString(incomingCallAppNameId);
                
                if (contentTitle == null) {
                    contentTitle = "Incoming Call";
                }
                final String from = "Call Invite from : " + callInvite.getFrom();

                NotificationCompat.Builder notificationBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_stat_onesignal_default)
                                .setContentTitle(contentTitle)
                                .setContentText(from)
                                .setAutoCancel(true)
                                .setExtras(extras)
                                .setContentIntent(pendingIntent)
                                .setGroup("voice_app_notification")
                                .setColor(Color.parseColor("#CE2E42"));

                notificationManager.notify(notificationId, notificationBuilder.build());

            }
        } else {
            SoundPoolManager.getInstance(this).stopRinging();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                /*
                 * If the incoming call was cancelled then remove the notification by matching
                 * it with the call sid from the list of notifications in the notification drawer.
                 */
                StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
                for (StatusBarNotification statusBarNotification : activeNotifications) {
                    notification = statusBarNotification.getNotification();
                    Bundle extras = notification.extras;
                    String notificationCallSid = extras.getString(CALL_SID_KEY);

                    if (callSid.equals(notificationCallSid)) {
                        notificationManager.cancel(extras.getInt(NOTIFICATION_ID_KEY));
                    } else {
                        sendCallInviteToPlugin(callInvite, notificationId);
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

    /**
     * Build a notification.
     *
     * @param text the text of the notification
     * @param pendingIntent the body, pending intent for the notification
     * @param extras extras passed with the notification
     * @return the builder
     */
    @TargetApi(Build.VERSION_CODES.O)
    public Notification buildNotification(String text, PendingIntent pendingIntent, Bundle extras) {
        int iconIdentifier = getResources().getIdentifier("icon", "mipmap", getPackageName());
        int incomingCallAppNameId = getResources().getIdentifier("incoming_call_app_name", "string", getPackageName());
        String contentTitle = getString(incomingCallAppNameId);
        return new Notification.Builder(getApplicationContext(), VOICE_CHANNEL)
                .setSmallIcon(iconIdentifier)
                .setContentTitle(contentTitle)
                .setContentText(text)
                .setContentIntent(pendingIntent)
                .setExtras(extras)
                .setAutoCancel(true)
                .build();
    }

     /*
     * Send the IncomingCallMessage to the Plugin
     */
    private void sendCallInviteToPlugin(CallInvite incomingCallMessage, int notificationId) {
        Intent intent = new Intent(TwilioVoicePlugin.ACTION_INCOMING_CALL);
        intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_INVITE, incomingCallMessage);
        intent.putExtra(TwilioVoicePlugin.INCOMING_CALL_NOTIFICATION_ID, notificationId);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
