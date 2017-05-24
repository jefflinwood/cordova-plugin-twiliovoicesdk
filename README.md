# PhoneGap Plugin for Twilio Voice
PhoneGap/Cordova Plugin for Twilio Programmable Voice SDK

## Example Application
Looking for a simple PhoneGap starter application to show how this is used?
https://github.com/jefflinwood/twilio-voice-phonegap-example

## Twilio Voice
This plugin is a wrapper for the Twilio Voice SDK for iOS and Android:
https://www.twilio.com/docs/api/voice-sdk

If you are using the Twilio Client SDK for iOS and Android, see the earlier Twilio Client Plugin
https://github.com/jefflinwood/twilio_client_phonegap

## Available on NPM

```
cordova plugin add twilio-voice-phonegap-plugin
```

The NPM Page for this plugin: https://www.npmjs.com/package/twilio-voice-phonegap-plugin

## Preferences

There are three preferences you will need to configure:

Preference | Example | Description
---------- | ------- | -----------
INCOMING_CALL_APP_NAME | PhoneApp | Users will get a notification that they have an inbound call (either a standard Push notification, or a CallKit screen) - this name is shown to the users.
GCM_SENDER_ID | 12345 | This is the Google Cloud Messaging Sender ID used for sending push notifications. If you aren't using Android, you can set GCM_SENDER_ID to a dummy value, like 123. The GCM Sender ID has to match the one you already use inside your application - you can only register with one at a time. 
ENABLE_CALL_KIT | true | This plugin has optional CallKit support for iOS 10 and above. ENABLE_CALL_KIT should be "true" or "false"
