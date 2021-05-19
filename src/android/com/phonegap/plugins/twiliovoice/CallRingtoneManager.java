package com.phonegap.plugins.twiliovoice;
import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class CallRingtoneManager{
  private RingtoneManager ringtoneManager;
  private Ringtone ringtone;
  private Vibrator vibrator;
  private AudioManager audioManager;
  private long pattern[] = { 0, 100, 200, 300, 400 };
  public static CallRingtoneManager instance;

  private final static String LOG = "CallRingtoneManager";

  public static CallRingtoneManager getInstance(Context context) {
    if (instance == null) {
      instance = new CallRingtoneManager(context);
    }
    return instance;
  }

  private CallRingtoneManager(Context context) {
    ringtoneManager = new RingtoneManager(context);
    ringtoneManager.setType(RingtoneManager.TYPE_RINGTONE);
    ringtone = ringtoneManager.getRingtone(context, setDefaultRingtone(context));
    ringtone.setStreamType(AudioManager.STREAM_RING);


    audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
  }

  private Uri setDefaultRingtone(Context context){
    Uri rintonesound = ringtoneManager.getActualDefaultRingtoneUri(context, ringtoneManager.TYPE_RINGTONE);

    if(rintonesound == null){
      // alert is null, using backup
      rintonesound = ringtoneManager.getActualDefaultRingtoneUri(context, ringtoneManager.TYPE_NOTIFICATION);

      // I can't see this ever being null (as always have a default notification)
      // but just incase
      if(rintonesound == null) {
        // alert backup is null, using 2nd backup
        rintonesound = ringtoneManager.getActualDefaultRingtoneUri(context, ringtoneManager.TYPE_ALARM);
      }
    }
    return rintonesound;
  }

  public void play(Context context){
    if(ringtone != null){
      Log.d(LOG, "Play ringer");
      checkPhoneState(context);
      ringtone.stop();
      ringtone.play();
    }
  }

  public void stop(){
    if(ringtone != null){
      Log.d(LOG, "Stop ringer");
      ringtone.stop();
      stopVibrate();
    }
  }

  public void startVibrate() {
//    vibrator.vibrate(pattern, 0);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      // New vibrate method for API Level 26 or higher
      vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));

    } else {
      vibrator.vibrate(500);// Vibrate method for below API Level 26
    }
  }

  public void stopVibrate() {
    vibrator.cancel();
  }

  private void checkPhoneState(Context context){
    int mode = audioManager.getRingerMode();

    switch (mode) {
      case AudioManager.RINGER_MODE_NORMAL:
        if ((1 == Settings.System.getInt(context.getContentResolver(), Settings.System.VIBRATE_WHEN_RINGING, 0))) {
          // ring + vibrate mode
          Log.d(LOG, "ring + vibrate mode");
          startVibrate();
        } else {
          // ring + no vibrate mode
          Log.d(LOG, "ring + no vibrate mode");
          stopVibrate();
        }
        break;

      case AudioManager.RINGER_MODE_SILENT:
        // in silent mode
        Log.d(LOG, "silent mode");
        break;

      case AudioManager.RINGER_MODE_VIBRATE:
        // in vibrate mode
        Log.d(LOG, "vibrate mode");
        startVibrate();
        break;
    }
  }
}
