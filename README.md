# Cordova Plugin for Twilio Voice
PhoneGap/Cordova Plugin for Twilio Programmable Voice SDK

# Changed Package Name to cordova-plugin-twiliovoicesdk
Note - as of August 21, 2018, this NPM package name will match the Cordova Plugin id (as version 3.0.x)

## Example Application
Looking for a simple Cordova/PhoneGap starter application to show how this is used?
https://github.com/jefflinwood/twilio-voice-phonegap-example

## Twilio Voice
This plugin is a wrapper for the Twilio Voice SDK for iOS and Android:
https://www.twilio.com/docs/api/voice-sdk

Android SDK example:
https://github.com/twilio/voice-quickstart-android/

If you are using the Twilio Client SDK for iOS and Android, see the earlier Twilio Client Plugin
https://github.com/jefflinwood/twilio_client_phonegap

## Note - the To parameter for dialing out

The most recent pull request merged in to the plug-in only supports the use of `To` as a parameter to be used for dialing out - if your application uses `tocall` or similar, you will need to adjust this plugin's constant, for now).

## Available on NPM

```
cordova plugin add cordova-plugin-twiliovoicesdk
```

The NPM Page for this plugin: https://www.npmjs.com/package/cordova-plugin-twiliovoicesdk

## Preferences

There are three preferences you will need to configure:

Preference | Example | Description
---------- | ------- | -----------
INCOMING_CALL_APP_NAME | PhoneApp | Users will get a notification that they have an inbound call (either a standard Push notification, or a CallKit screen) - this name is shown to the users.
ENABLE_CALL_KIT | true | This plugin has optional CallKit support for iOS 10 and above. ENABLE_CALL_KIT should be "true" or "false"
MASK_INCOMING_PHONE_NUMBER | false | This plugin has optional ability to mask the incoming phone number. MASK_INCOMING_PHONE_NUMBER should be "true" or "false"
DEBUG_TWILIO | false | Optionally enable twilio library debugging. DEBUG_TWILIO should be "true" or "false"
GMS_VERSION | 10.2.1 | Optionally choose `play-services` library version


# Configuring `play-services` Version

Many other plugins require Google Play Services and/or Firebase libraries. This is a common source of Android build-failures, since the `play-services` library version must be aligned to the same version for **all** plugins. For example, when one plugin imports version `11.0.1` and another one imports `11.2.0`, a gradle build failure will occur. Use the `GMS_VERSION` to align the required play-services version with other plugins.

```
cordova plugin add cordova-plugin-google-analytics --variable GMS_VERSION=11.0.1
```
