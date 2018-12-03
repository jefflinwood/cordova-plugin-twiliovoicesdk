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
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CallState;
import com.twilio.voice.RegistrationException;
import com.twilio.voice.RegistrationListener;
import com.twilio.voice.Voice;
import com.twilio.voice.MessageListener;
import com.twilio.voice.MessageException;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.cordova.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.phonegap.plugins.twiliovoice.fcm.VoiceFirebaseMessagingService;

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

    // FCM Token
    private String mFCMToken;

	// Has the plugin been initialized
	private boolean mInitialized = false;

	// An incoming call intent to process (can be null)
	private Intent mIncomingCallIntent;

    // Google Play Services Request Magic Number
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    // Constants for Intents and Broadcast Receivers
    public static final String ACTION_SET_FCM_TOKEN = "SET_FCM_TOKEN";
    public static final String INCOMING_CALL_INVITE = "INCOMING_CALL_INVITE";
    public static final String INCOMING_CALL_NOTIFICATION_ID = "INCOMING_CALL_NOTIFICATION_ID";
    public static final String ACTION_INCOMING_CALL = "INCOMING_CALL";

    public static final String KEY_FCM_TOKEN = "FCM_TOKEN";

	private AudioManager audioManager;
	private int savedAudioMode = AudioManager.MODE_INVALID;

	private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ACTION_SET_FCM_TOKEN)) {
                String fcmToken = intent.getStringExtra(KEY_FCM_TOKEN);
                Log.i(TAG, "FCM Token : " + fcmToken);
                mFCMToken = fcmToken;
                if(fcmToken == null) {
                    javascriptErrorback(0, "Did not receive GCM Token - unable to receive calls", mInitCallbackContext);
                }
                if (mFCMToken != null) {
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

	// Twilio Voice Registration Listener
	private RegistrationListener mRegistrationListener = new RegistrationListener() {
		@Override
		public void onRegistered(String accessToken, String fcmToken) {
            Log.d(TAG, "Registered Voice Client");
		}

		@Override
		public void onError(RegistrationException exception, String accessToken, String fcmToken) {
            Log.e(TAG, "Error registering Voice Client: " + exception.getMessage(), exception);
		}
	};

	// Twilio Voice Call Listener
	private Call.Listener mCallListener = new Call.Listener() {
		@Override
		public void onConnected(Call call) {
			SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
			mCall = call;

			JSONObject callProperties = new JSONObject();
			try {
				callProperties.putOpt("from", call.getFrom());
				callProperties.putOpt("to", call.getTo());
				callProperties.putOpt("callSid", call.getSid());
				callProperties.putOpt("isMuted", call.isMuted());
				String callState = getCallState(call.getState());
				callProperties.putOpt("state", callState);
				setAudioFocus(true);
			} catch (JSONException e) {
				Log.e(TAG,e.getMessage(),e);
			}
			javascriptCallback("oncalldidconnect", callProperties,mInitCallbackContext);
		}

		@Override
		public void onDisconnected(Call call, CallException exception) {
			mCall = null;
			setAudioFocus(false);
			javascriptCallback("oncalldiddisconnect", mInitCallbackContext);
		}

		@Override
		public void onConnectFailure(Call call, CallException exception) {
			mCall = null;
			setAudioFocus(false);
			javascriptErrorback(exception.getErrorCode(), exception.getMessage(), mInitCallbackContext);
		}
	};

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Log.d(TAG, "initialize()");

        // initialize sound SoundPoolManager
        SoundPoolManager.getInstance(cordova.getActivity());

		Context context = cordova.getActivity().getApplicationContext();
		audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

		// Handle an incoming call intent if launched from a notification
		Intent intent = cordova.getActivity().getIntent();
		if (intent.getAction().equals(ACTION_INCOMING_CALL)) {
			mIncomingCallIntent = intent;
		}
	}

	@Override
	public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
		super.onRestoreStateForActivityResult(state, callbackContext);
		Log.d(TAG, "onRestoreStateForActivityResult()");
		mInitCallbackContext = callbackContext;
	}


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
            intentFilter.addAction(ACTION_SET_FCM_TOKEN);
            intentFilter.addAction(ACTION_INCOMING_CALL);
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(cordova.getActivity());
            lbm.registerReceiver(mBroadcastReceiver, intentFilter);
			String newToken = FirebaseInstanceId.getInstance().getToken();
    Log.e("newToken", newToken);
   mFCMToken = newToken;
				if (newToken != null) {
					register();
				}

			if (mIncomingCallIntent != null) {
				Log.d(TAG, "initialize(): Handle an incoming call");
			 	handleIncomingCallIntent(mIncomingCallIntent);
				mIncomingCallIntent = null;
			}

			javascriptCallback("onclientinitialized",mInitCallbackContext);

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
		} else if ("turnRingerOn".equals(action)) {
			SoundPoolManager.getInstance(cordova.getActivity()).playRinging();
			return true;
		}  else if ("turnRingerOff".equals(action)) {
			SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
			return true;
		} else if ("isCallMuted".equals(action)) {
			isCallMuted(callbackContext);
			return true;
		} else if ("callStatus".equals(action)) {
			callStatus(callbackContext);
			return true;
		}else if ("rejectCallInvite".equals(action)) {
			Log.d(TAG, "rejectCallInvite!");
			rejectCallInvite(args, callbackContext);
			return true;
		} else if ("showNotification".equals(action)) {
			showNotification(args,callbackContext);
			return true;
		} else if ("cancelNotification".equals(action)) {
			cancelNotification(args, callbackContext);
			return true;
		} else if ("setSpeaker".equals(action)) {
			setSpeaker(args, callbackContext);
			return true;
		}
//		} else if ("callInvite".equals(action)) {
//			JSONObject data = args.optJSONObject(0);
//			Map<String, String> map = getMap(data);
//			Log.d(TAG, "Call Invite");
//			for (Map.Entry<String, String> entry: map.entrySet()) {
//				Log.d(TAG, entry.getKey() + " : " + entry.getValue());
//			}
//			final int notificationId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
//			Voice.handleMessage(this.cordova.getActivity().getApplicationContext(), map, new MessageListener() {
//					@Override
//					public void onCallInvite(CallInvite callInvite) {
//						SoundPoolManager.getInstance(cordova.getActivity()).playRinging();
//							Log.d(TAG, "Call Invite Created");
//							mCallInvite = callInvite;
//							if (mCallInvite != null && (mCallInvite.getState() == CallInvite.State.PENDING)) {
//								//VoiceFirebaseMessagingService.this.notify(callInvite, notificationId);
//								JSONObject callInviteProperties = new JSONObject();
//								try {
//										callInviteProperties.putOpt("from", mCallInvite.getFrom());
//										callInviteProperties.putOpt("to", mCallInvite.getTo());
//										callInviteProperties.putOpt("callSid", mCallInvite.getCallSid());
//										String callInviteState = getCallInviteState(mCallInvite.getState());
//										callInviteProperties.putOpt("state", callInviteState);
//								} catch (JSONException e) {
//										Log.e(TAG,e.getMessage(),e);
//								}
//								Log.d(TAG,"oncallinvitereceived");
//								javascriptCallback("oncallinvitereceived", callInviteProperties, mInitCallbackContext);
//						} else {
//							SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
//							Log.d(TAG,"oncallinvitecanceled");
//							javascriptCallback("oncallinvitecanceled",mInitCallbackContext);
//						}
//					}
//
//					@Override
//					public void onError(MessageException messageException) {
//							Log.e(TAG, messageException.getLocalizedMessage());
//					}
//			});
//			return true;
//		}

		return false;
	}

	private void call(final JSONArray arguments, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				String accessToken = arguments.optString(0, mAccessToken);
				JSONObject options = arguments.optJSONObject(1);
				Map<String, String> map = getMap(options);
				if (mCall != null && mCall.getState().equals(CallState.CONNECTED)) {
					mCall.disconnect();
				}
				mCall = Voice.call(cordova.getActivity(), accessToken, map, mCallListener);
				Log.d(TAG, "Placing call with params: " + map.toString());
			}
		});

	}

	private void acceptCallInvite(JSONArray arguments, final CallbackContext callbackContext) {
		Log.d(TAG, "acceptCallInvite()");
		SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
		if (mCallInvite == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				mCallInvite.accept(cordova.getActivity(),mCallListener);
				callbackContext.success();
			}
		});

	}

	private void rejectCallInvite(JSONArray arguments, final CallbackContext callbackContext) {
		Log.d(TAG, "rejectCallInvite()");
		if (mCallInvite == null) {
			Log.d(TAG, "mCallInvite == nul");
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}

		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				// OF - for ringing to stop if rejected
				SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
				mCallInvite.reject(cordova.getActivity());
				callbackContext.success();
			}
		});
	}

	private void disconnect(JSONArray arguments, final CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				mCall.disconnect();
//				mCall = null;
//				SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
				callbackContext.success();
			}
		});
	}

	private void sendDigits(final JSONArray arguments,
			final CallbackContext callbackContext) {
		if (arguments == null || arguments.length() < 1 || mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				mCall.sendDigits(arguments.optString(0));
				callbackContext.success();
			}
		});

	}

	private void muteCall(final CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				mCall.mute(true);
				callbackContext.success();
			}
		});
	}

    private void unmuteCall(final CallbackContext callbackContext) {
		if (mCall == null) {
			callbackContext.sendPluginResult(new PluginResult(
					PluginResult.Status.ERROR));
			return;
		}
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				mCall.mute(false);
				callbackContext.success();
			}
		});
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
		Log.d(TAG, "showNotification() Twilio");
		Context acontext = TwilioVoicePlugin.this.webView.getContext();
		NotificationManager mNotifyMgr = (NotificationManager) acontext.getSystemService(Activity.NOTIFICATION_SERVICE);
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
		NotificationManager mNotifyMgr = (NotificationManager) TwilioVoicePlugin.this.webView.getContext().getSystemService(Activity.NOTIFICATION_SERVICE);
		mNotifyMgr.cancel(mCurrentNotificationId);
		context.success();
	}

	public void setSpeaker(final JSONArray arguments, final CallbackContext callbackContext) {
		cordova.getThreadPool().execute(new Runnable(){
			public void run() {
				String mode = arguments.optString(0);
				if(mode.equals("on")) {
					Log.d(TAG, "SPEAKER");
					audioManager.setMode(AudioManager.MODE_NORMAL);
					audioManager.setSpeakerphoneOn(true);
				}
				else {
					Log.d(TAG, "EARPIECE");
					audioManager.setMode(AudioManager.MODE_IN_CALL);
					audioManager.setSpeakerphoneOn(false);
				}
			}
		});
	}

	private void setAudioFocus(boolean setFocus) {
        if (audioManager != null) {
            if (setFocus) {
                savedAudioMode = audioManager.getMode();
                // Request audio focus before making any device switch.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
						AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build();
                    AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                            .setAudioAttributes(playbackAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(new AudioManager.OnAudioFocusChangeListener() {
                                @Override
                                public void onAudioFocusChange(int i) {
                                }
                            })
                            .build();
                    audioManager.requestAudioFocus(focusRequest);
                } else {
                    audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                }
                /*
                 * Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
                 * required to be in this mode when playout and/or recording starts for
                 * best possible VoIP performance. Some devices have difficulties with speaker mode
                 * if this is not set.
                 */
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(savedAudioMode);
                audioManager.abandonAudioFocus(null);
            }
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
		LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(cordova.getActivity());
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
	}

    /*
     * Register your FCM token with Twilio to enable receiving incoming calls via FCM
     */
    private void register() {
        Voice.register(cordova.getActivity().getApplicationContext(), mAccessToken,  Voice.RegistrationChannel.FCM, mFCMToken, mRegistrationListener);
	}

    // Process incoming call invites
    private void handleIncomingCallIntent(Intent intent) {
        Log.d(TAG, "handleIncomingCallIntent()");
        if (intent != null && intent.getAction() != null && intent.getAction().equals(ACTION_INCOMING_CALL)) {
            mCallInvite = intent.getParcelableExtra(INCOMING_CALL_INVITE);
            if (mCallInvite != null && (mCallInvite.getState() == CallInvite.State.PENDING)) {
            	Log.d(TAG, "CallInvite.State == PENDING");
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
                    callInviteProperties.putOpt("state", callInviteState);
                } catch (JSONException e) {
                    Log.e(TAG,e.getMessage(),e);
                }
				Log.d(TAG,"oncallinvitereceived");
                javascriptCallback("oncallinvitereceived", callInviteProperties, mInitCallbackContext);
            } else {
                SoundPoolManager.getInstance(cordova.getActivity()).stopRinging();
				Log.d(TAG,"oncallinvitecanceled");
                javascriptCallback("oncallinvitecanceled",mInitCallbackContext);
            }
        }
    }

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
		} else if (state == CallInvite.State.CANCELED) {
			return "cancelled";
		}

		return null;
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

}
