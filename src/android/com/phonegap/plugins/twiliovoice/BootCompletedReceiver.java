package com.phonegap.plugins.twiliovoice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.phonegap.plugins.twiliovoice.gcm.VoiceGCMListenerService;

public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent("com.phonegap.plugins.twiliovoice.gcm.VoiceGCMListenerService");
        i.setClass(context, VoiceGCMListenerService.class);
        context.startService(i);
    }
}
