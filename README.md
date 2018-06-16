# PhoneGap Plugin for Twilio Voice
PhoneGap/Cordova Plugin for Twilio Programmable Voice SDK

## Example Application
Looking for a simple PhoneGap starter application to show how this is used?
https://github.com/jefflinwood/twilio-voice-phonegap-example

## Twilio Voice
This plugin is a wrapper for the Twilio Voice SDK for iOS and Android:
https://www.twilio.com/docs/api/voice-sdk

Android SDK example:
https://github.com/twilio/voice-quickstart-android/

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
ENABLE_CALL_KIT | true | This plugin has optional CallKit support for iOS 10 and above. ENABLE_CALL_KIT should be "true" or "false"
MASK_INCOMING_PHONE_NUMBER | false | This plugin has optional ability to mask the incoming phone number. MASK_INCOMING_PHONE_NUMBER should be "true" or "false"
