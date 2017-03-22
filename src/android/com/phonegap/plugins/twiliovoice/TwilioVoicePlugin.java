package com.phonegap.plugins.twiliovoice;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.phonegap.plugins.twiliovoice.gcm.GCMRegistrationService;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CallState;
import com.twilio.voice.RegistrationException;
import com.twilio.voice.RegistrationListener;
import com.twilio.voice.VoiceClient;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//import android.R;

/**
 * Twilio Voice Plugin for Cordova/PhoneGap
 *
 * Based on Twilio's Voice Quickstart for Android
 * https://github.com/twilio/voice-quickstart-android/blob/master/app/src/main/java/com/twilio/voice/quickstart/VoiceActivity.java
 * 
 * @author Jeff Linwood, https://github.com/jefflinwood
 * 
 */
public class TwilioVoicePlugin extends CordovaPlugin {

	public final static String TAG = "TwilioVoicePlugin";

	private CallbackContext mInitCallbackContext;
	private JSONArray mInitDeviceSetupArgs;
	private int mCurrentNotificationId = 1;
	private String mCurrentNotificationText;

	// Twilio Voice Member Variables
	private Call mCall;
	private CallInvite mCallInvite;

	// Access Token
	private String mAccessToken;

    // GCM Token
    private String mGCMToken;

	// Has the plugin been initialized
	private boolean mInitialized = false;

	// Marshmallow Permissions
	public static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
	public static final int RECORD_AUDIO_REQ_CODE = 0;
    
    // Google Play Services Request Magic Number
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;


    // Constants for Intents and Broadcast Receivers
    public static final String ACTION_SET_GCM_TOKEN = "SET_GCM_TOKEN";
    public static final String INCOMING_CALL_INVITE = "INCOMING_CALL_INVITE";
    public static final String INCOMING_CALL_NOTIFICATION_ID = "INCOMING_CALL_NOTIFICATION_ID";
    public static final String ACTION_INCOMING_CALL = "INCOMING_CALL";

    public static final String KEY_GCM_TOKEN = "GCM_TOKEN";


	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_SET_GCM_TOKEN)) {
                String gcmToken = intent.getStringExtra(KEY_GCM_TOKEN);
                Log.i(TAG, "GCM Token : " + gcmToken);
                mGCMToken = gcmToken;
                if(gcmToken == null) {
                    javascriptErrorback(0, "Did not receive GCM Token - unable to receive calls", mInitCallbackContext);
                }
                //callActionFab.show();
                if (mGCMToken != null) {
                    register();
                }
            } else if (action.equals(ACTION_INCOMING_CALL)) {
                /*
                 * Handle the incoming call invite
                 */
                handleIncomingCallIntent(intent);
            }
		}
	};

	/**
	 * Android Cordova Action Router
	 * 
	 * Executes the request.
	 * 
	 * This method is called from the WebView thread. To do a non-trivial amount
	 * of work, use: cordova.getThreadPool().execute(runnable);
	 * 
	 * To run on the UI thread, use:
	 * cordova.getActivity().runOnUiThread(runnable);
	 * 
	 * @param action
	 *            The action to execute.
	 * @param args
	 *            The exec() arguments in JSON form.
	 * @param callbackContext
	 *            The callback context used when calling back into JavaScript.
	 * @return Whether the action was valid.
	 */
	@Override
	public boolean execute(final String action, final JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
		if ("initializeWithAccessToken".equals(action)) {
            Log.d(TAG, "Initializing with Access Token");

			mAccessToken = args.optString(0);

			mInitCallbackContext = callbackContext;

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_SET_GCM_TOKEN);
            intentFilter.addAction(ACTION_INCOMING_CALL);
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(cordova.getActivity());
            lbm.registerReceiver(mBroadcastReceiver, intentFilter);

            // initialize sound SoundPoolManager
            SoundPoolManager.getInstance(cordova.getActivity());

			if(cordova.hasPermission(RECORD_AUDIO))
			{
				startGCMRegistration();
			}
			else
			{
				cordova.requestPermission(this, RECORD_AUDIO_REQ_CODE, RECORD_AUDIO);
			}
			return true;

		} else if ("call".equals(action)) {
			call(args, callbackContext);
			return true;
		} else if ("acceptCallInvite".equals(action)) {
			acceptCallInvite(args, callbackContext);
			return true;
		} else if ("disconnect".equals(action)) {
			disconnect(args, callbackContext);
			return true;
		} else if ("sendDigits".equals(action)) {
			sendDigits(args, callbackContext);
			return true;
		} else if ("muteCall".equals(action)) {
			muteCall(callbackContext);
			return true;
		}  else if ("unmuteCall".equals(action)) {
			unmuteCall(callbackContext);
			return true;
		}  else if ("isCallMuted".equals(action)) {
			isCallMuted(callbackContext);
			return true;
		} else if ("callStatus".equals(action)) {
			callStatus(callbackContext);
			return true;
		}else if ("rejectCallInvite".equals(action)) {
			rejectCallInvite(args, callbackContext);
			return true;
		} else if ("showNotification".equals(action)) {
			showNotification(args,callbackContext);
			return true;
		} else if ("cancelNotification".equals(action)) {
			cancelNotification(args,callbackContext);
			return true;
		} else if ("setSpeaker".equals(action)) {
			setSpeaker(args,callbackContext);
			return true;
		}

		return false; 
	}

	/**
	 * Initialize Twilio Voice Client to receive phone calls
	 * 
	 */
	private void initTwilioVoiceClient(CallbackContext callbackContext) {
		//Twilio.initialize(cordova.getActivity().getApplicationContext(), this);
	}

	/**
	 * Set up the Twilio device with a capability token
	 * 
	 * @param arguments JSONArray with a Twilio capability token
	 */
	/*private void deviceSetup(JSONArray arguments,
			final CallbackContext callbackContext) {
		if (arguments == null || arguments.length() < 1) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
        if (arguments.optString(0).equals("")) {
			Log.d("TCPlugin","Releasing device");
			cordova.getThreadPool().execute(new Runnable(){
				public void run() {
					mDevice.release();
				}
			});
			javascriptCallback("onoffline", callbackContext);
			return;
		}
		mDevice = Twilio.createDevice(arguments.optString(0), this);

		Intent intent = new Intent(this.cordova.getActivity(), IncomingConnectionActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this.cordova.getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		mDevice.setIncomingIntent(pendingIntent);
		
		
		// delay one second to give Twilio device a change to change status (similar to iOS plugin)
		cordova.getThreadPool().execute(new Runnable(){
				public void run() {
					try {
						Thread.sleep(1000);
						deviceStatusEvent(callbackContext);
					} catch (InterruptedException ex) {
						Log.e(TAG,"InterruptedException: " + ex.getMessage(),ex);
					}
				}
			});
	}*/


	private void call(JSONArray arguments, CallbackContext callbackContext) {
		String accessToken = arguments.optString(0,mAccessToken);
		JSONObject options = arguments.optJSONObject(1);
		Map<String, String> map = getMap(options);
		if (mCall != null && mCall.getState().equals(CallState.CONNECTED)) {
			mCall.disconnect();
		}
		mCall = VoiceClient.call(cordova.getActivity(),accessToken, map, mCallListener);
		Log.d(TAG, "Placing call with params: " + map.toString());
	}

	// helper method to get a map of strings from a JSONObject
	public Map<String, String> getMap(JSONObject object) {
		if (object == null) {
			return null;
		}

		Map<String, String> map = new HashMap<String, String>();

		@SuppressWarnings("rawtypes")
		Iterator keys = object.keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();
			map.put(key, object.optString(key));
		}
		return map;
	}
	
	// helper method to get a JSONObject from a Map of Strings
	public JSONObject getJSONObject(Map<String, String> map) throws JSONException {
		if (map == null) {
			return null;
		}

		JSONObject json = new JSONObject();
		for (String key : map.keySet()) {
			json.putOpt(key, map.get(key));
		}
		return json;
	}

	private void acceptCallInvite(JSONArray arguments, CallbackContext callbackContext) {
		if (mCallInvite == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCallInvite.accept(cordova.getActivity(),mCallListener);
		callbackContext.success(); 
	}
	
	private void rejectCallInvite(JSONArray arguments, CallbackContext callbackContext) {
		if (mCallInvite == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCallInvite.reject(cordova.getActivity());
		callbackContext.success(); 
	}
	
	private void disconnect(JSONArray arguments, CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCall.disconnect();
		callbackContext.success();
	}

	private void sendDigits(JSONArray arguments,
			CallbackContext callbackContext) {
		if (arguments == null || arguments.length() < 1 || mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCall.sendDigits(arguments.optString(0));
	}
	
	private void muteCall(CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCall.mute(true);
		callbackContext.success();
	}

    private void unmuteCall(CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCall.mute(false);
		callbackContext.success();
	}

    private void isCallMuted(CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
                PluginResult.Status.OK,false));
			return;
		}
		PluginResult result = new PluginResult(PluginResult.Status.OK,mCall.isMuted());
		callbackContext.sendPluginResult(result);
	}


	private void callStatus(CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		String state = getCallState(mCall.getState());
		if (state == null) {
			state = "";
		}
		PluginResult result = new PluginResult(PluginResult.Status.OK,state);
		callbackContext.sendPluginResult(result);
	}


	
	private void showNotification(JSONArray arguments, CallbackContext context) {
		Context acontext = TwilioVoicePlugin.this.webView.getContext();
		NotificationManager mNotifyMgr = 
		        (NotificationManager) acontext.getSystemService(Activity.NOTIFICATION_SERVICE);
		mNotifyMgr.cancelAll();
		mCurrentNotificationText = arguments.optString(0);		
		
		
		PackageManager pm = acontext.getPackageManager();
        Intent notificationIntent = pm.getLaunchIntentForPackage(acontext.getPackageName());
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.putExtra("notificationTag", "BVNotification");
        
	    PendingIntent pendingIntent = PendingIntent.getActivity(acontext, 0, notificationIntent, 0);  
	    int notification_icon = acontext.getResources().getIdentifier("notification", "drawable", acontext.getPackageName());
		NotificationCompat.Builder mBuilder =
			    new NotificationCompat.Builder(acontext)
				.setSmallIcon(notification_icon)
			    .setContentTitle("Incoming Call")
			    .setContentText(mCurrentNotificationText)
			    .setContentIntent(pendingIntent);
		mNotifyMgr.notify(mCurrentNotificationId, mBuilder.build());
		
		context.success();
	}
	
	private void cancelNotification(JSONArray arguments, CallbackContext context) {
		NotificationManager mNotifyMgr = 
		        (NotificationManager) TwilioVoicePlugin.this.webView.getContext().getSystemService(Activity.NOTIFICATION_SERVICE);
		mNotifyMgr.cancel(mCurrentNotificationId);
		context.success();
	}
	
	/**
	 * 	Changes sound from earpiece to speaker and back
	 * 
	 * 	@param mode	Speaker Mode
	 * */
	public void setSpeaker(JSONArray arguments, final CallbackContext callbackContext) {
		Context context = cordova.getActivity().getApplicationContext();
		AudioManager m_amAudioManager;
        m_amAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        String mode = arguments.optString(0);
        if(mode.equals("on")) {
        	Log.d(TAG, "SPEAKER");
        	m_amAudioManager.setMode(AudioManager.MODE_NORMAL);
        	m_amAudioManager.setSpeakerphoneOn(true);        	
        }
        else {
        	Log.d(TAG, "EARPIECE");
        	m_amAudioManager.setMode(AudioManager.MODE_IN_CALL); 
        	m_amAudioManager.setSpeakerphoneOn(false);
        }
	}

	// Plugin-to-Javascript communication methods
	private void javascriptCallback(String event, JSONObject arguments,
			CallbackContext callbackContext) {
		if (callbackContext == null) {
			return;
		}
		JSONObject options = new JSONObject();
		try {
			options.putOpt("callback", event);
			options.putOpt("arguments", arguments);
		} catch (JSONException e) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.JSON_EXCEPTION));
			return;
		}
		PluginResult result = new PluginResult(Status.OK, options);
		result.setKeepCallback(true);
		callbackContext.sendPluginResult(result);

	}

	private void javascriptCallback(String event,
			CallbackContext callbackContext) {
		javascriptCallback(event, null, callbackContext);
	}

	
	private void javascriptErrorback(int errorCode, String errorMessage, CallbackContext callbackContext) {
		JSONObject object = new JSONObject();
		try {
			object.putOpt("message", errorMessage);
		} catch (JSONException e) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.JSON_EXCEPTION));
			return;
		}
		PluginResult result = new PluginResult(Status.ERROR, object);
		result.setKeepCallback(true);
		callbackContext.sendPluginResult(result);
	}

	private void fireDocumentEvent(String eventName) {
		if (eventName != null) {
			javascriptCallback(eventName,mInitCallbackContext);
		}
	}

	@Override
	public void onDestroy() {
		//lifecycle events
        SoundPoolManager.getInstance(cordova.getActivity()).release();
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(cordova
				.getActivity());
		lbm.unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
	}


	public void onRequestPermissionResult(int requestCode, String[] permissions,
										  int[] grantResults) throws JSONException
	{
		for(int r:grantResults)
		{
			if(r == PackageManager.PERMISSION_DENIED)
			{
				mInitCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "Permission denied"));
				return;
			}
		}
		switch(requestCode)
		{
			case RECORD_AUDIO_REQ_CODE:
                startGCMRegistration();
				break;
		}
	}

    /*
     * Register your GCM token with Twilio to enable receiving incoming calls via GCM
     */
    private void register() {
        VoiceClient.register(cordova.getActivity().getApplicationContext(), mAccessToken, mGCMToken, mRegistrationListener);
    }

    // Process incoming call invites
    private void handleIncomingCallIntent(Intent intent) {
        Log.d(TAG, "handleIncomingCallIntent()");
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_INCOMING_CALL)) {
            mCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
            Log.d(TAG, "Call Invite: " + mCallInvite.toString());
            if (!mCallInvite.isCancelled()) {
                SoundPoolManager.getInstance(cordova.getActivity()).playRinging();
                NotificationManager mNotifyMgr = 
		        (NotificationManager) cordova.getActivity().getSystemService(Activity.NOTIFICATION_SERVICE);
                mNotifyMgr.cancel(intent.getIntExtra(INCOMING_CALL_NOTIFICATION_ID, 0));
                JSONObject callInviteProperties = new JSONObject();
                try {
                    callInviteProperties.putOpt("from", mCallInvite.getFrom());
                    callInviteProperties.putOpt("to", mCallInvite.getTo());
                    callInviteProperties.putOpt("callSid", mCallInvite.getCallSid());
                    String callInviteState = getCallInviteState(mCallInvite.getState());
                    callInviteProperties.putOpt("state",callInviteState);
                } catch (JSONException e) {
                    Log.e(TAG,e.getMessage(),e);
                }
                javascriptCallback("oncallinvitereceived", callInviteProperties, mInitCallbackContext); 
            } else {
                SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
                javascriptCallback("oncallinvitecanceled",mInitCallbackContext); 
            }
        }
    }


	// Twilio Voice Registration Listener
	private RegistrationListener mRegistrationListener = new RegistrationListener() {
		@Override
		public void onRegistered(String accessToken, String gcmToken) {
            Log.d(TAG, "Registered Voice Client");
		}

		@Override
		public void onError(RegistrationException exception, String accessToken, String gcmToken) {
            Log.e(TAG, "Error registering Voice Client: " + exception.getMessage(), exception);
		}
	};

	// Twilio Voice Call Listener
	private Call.Listener mCallListener = new Call.Listener() {
		@Override
		public void onConnected(Call call) {
			mCall = call;

			JSONObject callProperties = new JSONObject();
			try {
				callProperties.putOpt("from", call.getFrom());
				callProperties.putOpt("to", call.getTo());
				callProperties.putOpt("callSid", call.getCallSid());
				callProperties.putOpt("isMuted", call.isMuted());
				String callState = getCallState(call.getState());
				callProperties.putOpt("state",callState);
			} catch (JSONException e) {
				Log.e(TAG,e.getMessage(),e);
			}
			javascriptCallback("oncalldidconnect",callProperties,mInitCallbackContext);
		}

		@Override
		public void onDisconnected(Call call) {
			mCall = null;
			javascriptCallback("oncalldiddisconnect",mInitCallbackContext);
		}

		@Override
		public void onDisconnected(Call call, CallException exception) {
			mCall = null;
			javascriptErrorback(exception.getErrorCode(), exception.getMessage(), mInitCallbackContext);
		}
	};

	private String getCallState(CallState callState) {
		if (callState == CallState.CONNECTED) {
			return "connected";
		} else if (callState == CallState.CONNECTING) {
			return "connecting";
		} else if (callState == CallState.DISCONNECTED) {
			return "disconnected";
		}
		return null;
	}

	private String getCallInviteState(CallInvite.State state) {
		if (state == CallInvite.State.PENDING) {
			return "pending";
		} else if (state == CallInvite.State.ACCEPTED) {
			return "accepted";
		} else if (state == CallInvite.State.REJECTED) {
			return "rejected";
		} else if (state == CallInvite.State.CANCELLED) {
			return "cancelled";
		}

		return null;
	}


    private void startGCMRegistration() {
        Log.d(TAG, "Starting GCM Registration");
        if (checkPlayServices()) {
            Log.d(TAG, "Found Google Play Services");
            Intent intent = new Intent(cordova.getActivity(), GCMRegistrationService.class);
            cordova.getActivity().startService(intent);
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(cordova.getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(cordova.getActivity(), resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.e(TAG, "This device is not supported for Google Play Services.");
                javascriptErrorback(0, "This device is not supported for Google Play Services.", mInitCallbackContext);

            }
            return false;
        }
        return true;
    }

}
