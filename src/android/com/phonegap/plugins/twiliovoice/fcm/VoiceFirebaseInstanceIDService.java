package com.phonegap.plugins.twiliovoice.fcm;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.phonegap.plugins.twiliovoice.TwilioVoicePlugin;

public class VoiceFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = TwilioVoicePlugin.TAG;

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        // Notify Activity of FCM token
        Intent intent = new Intent(TwilioVoicePlugin.ACTION_SET_FCM_TOKEN);
        intent.putExtra(TwilioVoicePlugin.KEY_FCM_TOKEN, refreshedToken);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
