# twilio-voice-phonegap-plugin
PhoneGap/Cordova Plugin for Twilio Programmable Voice SDK

on NPM:
https://www.npmjs.com/package/twilio-voice-phonegap-plugin

There are three preferences you will need to configure:

INCOMING_CALL_APP_NAME

GCM_SENDER_ID

ENABLE_CALL_KIT

If you aren't using Android, you can set GCM_SENDER_ID to a dummy value, like 123. ENABLE_CALL_KIT should be "true" or "false"

The GCM Sender ID has to match the one you already use inside your application - you can only register with one at a time.
