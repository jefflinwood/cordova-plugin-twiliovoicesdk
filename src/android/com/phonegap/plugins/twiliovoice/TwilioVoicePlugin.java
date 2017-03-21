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

	private final static String TAG = "TwilioVoicePlugin";

	private CallbackContext mInitCallbackContext;
	private JSONArray mInitDeviceSetupArgs;
	private int mCurrentNotificationId = 1;
	private String mCurrentNotificationText;

	// Twilio Voice Member Variables
	private Call mCall;
	private CallInvite mCallInvite;

	// Access Token
	private String mAccessToken;

	// Has the plugin been initialized
	private boolean mInitialized = false;

	// Marshmallow Permissions
	public static final String RECORD_AUDIO = Manifest.permission.RECORD_AUDIO;
	public static final int RECORD_AUDIO_REQ_CODE = 0;



	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// mDevice = intent.getParcelableExtra(Device.EXTRA_DEVICE);
			//mConnection = intent.getParcelableExtra(Device.EXTRA_CONNECTION);
			//mCall.set(plugin);
			//Log.d(TAG, "incoming intent received with connection: "+ mCall.getState().name());
			//String constate = mCall.getState().name();
			//if(constate.equals("PENDING")) {
			//	TwilioVoicePlugin.this.javascriptCallback("onincoming", mInitCallbackContext);
			//}
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
			mAccessToken = args.optString(0);

			mInitCallbackContext = callbackContext;
			if(cordova.hasPermission(RECORD_AUDIO))
			{
				initTwilioVoiceClient(callbackContext);
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
		} else if ("muteConnection".equals(action)) {
			muteConnection(callbackContext);
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
		
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(cordova.getActivity());
		lbm.registerReceiver(mBroadcastReceiver, new IntentFilter(IncomingConnectionActivity.ACTION_NAME));
		
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
	
	private void muteConnection(CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		mCall.mute(!mCall.isMuted());
		callbackContext.success();
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
		super.onDestroy();
		//lifecycle events
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(cordova
				.getActivity());
		lbm.unregisterReceiver(mBroadcastReceiver);
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
				initTwilioVoiceClient(mInitCallbackContext);
				break;
		}
	}


	// Twilio Voice Registration Listener
	private RegistrationListener mRegistrationListener = new RegistrationListener() {
		@Override
		public void onRegistered(String accessToken, String gcmToken) {

		}

		@Override
		public void onError(RegistrationException exception, String accessToken, String gcmToken) {

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
			return "TVOCallStateConnected";
		} else if (callState == CallState.CONNECTING) {
			return "TVOCallStateConnecting";
		} else if (callState == CallState.DISCONNECTED) {
			return "TVOCallStateDisconnected";
		}
		return null;
	}

	private String getCallInviteState(CallInvite.State state) {
		if (state == CallInvite.State.PENDING) {
			return "TVOCallInviteStatePending";
		} else if (state == CallInvite.State.ACCEPTED) {
			return "TVOCallInviteStateAccepted";
		} else if (state == CallInvite.State.REJECTED) {
			return "TVOCallInviteStateRejected";
		} else if (state == CallInvite.State.CANCELLED) {
			return "TVOCallInviteStateCancelled";
		}

		return null;
	}


}
