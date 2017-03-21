package com.phonegap.plugins.twiliovoice.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import com.phonegap.plugins.twiliovoice.TwilioVoicePlugin;

/*
 * Based on https://github.com/twilio/voice-quickstart-android/blob/master/app/src/main/java/com/twilio/voice/quickstart/gcm/GCMRegistrationService.java
 * From Twilio
 */
public class GCMRegistrationService extends IntentService {

    private static final String TAG = TwilioVoicePlugin.TAG;

    public GCMRegistrationService() {
        super("GCMRegistrationService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG,"GCMRegistrationService: onHandleIntent()");
        try {
            InstanceID instanceID = InstanceID.getInstance(this);
            int gcmSenderIDIdentifier = getResources().getIdentifier("gcm_sender_id", "string", getPackageName());
            String gcmSenderId = getString(gcmSenderIDIdentifier);
            Log.d(TAG, "GCM Sender ID: " + gcmSenderId);
            String token = instanceID.getToken(gcmSenderId,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE,
                    null);
            Log.d(TAG, "Retrieved GCM Token: " + token);
            sendGCMTokenToActivity(token);
        } catch (Exception e) {
            /*
             * If we are unable to retrieve the GCM token we notify the Plugin
             * letting the user know this step failed.
             */
            Log.e(TAG, "Failed to retrieve GCM token", e);
            sendGCMTokenToActivity(null);
        }
    }

    /**
     * Send the GCM Token to the Voice Plugin.
     *
     * @param gcmToken The new token.
     */
    private void sendGCMTokenToActivity(String gcmToken) {
        Intent intent = new Intent(TwilioVoicePlugin.ACTION_SET_GCM_TOKEN);
        intent.putExtra(TwilioVoicePlugin.KEY_GCM_TOKEN, gcmToken);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}