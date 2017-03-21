package com.phonegap.plugins.twiliovoice.gcm;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.iid.InstanceIDListenerService;

import com.phonegap.plugins.twiliovoice.TwilioVoicePlugin;


/*
 * Based on https://github.com/twilio/voice-quickstart-android/blob/master/app/src/main/java/com/twilio/voice/quickstart/gcm/VoiceInstanceIDListenerService.java
 * From Twilio
 */

public class VoiceInstanceIDListenerService extends InstanceIDListenerService {

    private static final String TAG = TwilioVoicePlugin.TAG;

    @Override
    public void onTokenRefresh() {
        super.onTokenRefresh();

        Log.d(TAG, "onTokenRefresh");

        Intent intent = new Intent(this, GCMRegistrationService.class);
        startService(intent);
    }
}