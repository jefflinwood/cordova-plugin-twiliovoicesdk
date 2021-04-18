[![npm version](https://badge.fury.io/js/cordova-plugin-twiliovoicesdk.svg)](https://badge.fury.io/js/cordova-plugin-twiliovoicesdk)

# Cordova Plugin for Twilio Voice
Cordova Plugin for Twilio Programmable Voice SDK

# Changelog
6.0.0 - April 18, 2021 - Updated to support Twilio Voice SDK 6.2.2 for iOS

# Changed Package Name to cordova-plugin-twiliovoicesdk
Note - as of August 21, 2018, this NPM package name will match the Cordova Plugin id.

# Requires AndroidX (Android only)
Note - as of January 15, 2021, this requires AndroidX support, as well as Cordova Android 9 or above. You also need to set up Firebase Cloud Messaging (FCM), and include a `google-services.json` file in your `platforms/android/app` directory

In your Cordova project's `config.xml`, add these options:

```xml
<preference name="AndroidXEnabled" value="true" />
<preference name="GradlePluginGoogleServicesEnabled" value="true"/>
```

If you don't add that, you will see this error:

```
This project uses AndroidX dependencies, but the 'android.useAndroidX' property is not enabled. Set this property to true in the gradle.properties file and retry.
```

## Android X Required

If you use any Android plugins that do not support AndroidX, please add this plugin to your project:

https://github.com/dpa99c/cordova-plugin-androidx-adapter

## Example Application
Looking for a simple Cordova/PhoneGap starter application to show how this is used?
https://github.com/jefflinwood/twilio-voice-phonegap-example

## Twilio Voice
This plugin is a wrapper for the Twilio Voice SDK for iOS and Android:
https://www.twilio.com/docs/api/voice-sdk

Android SDK example:
https://github.com/twilio/voice-quickstart-android/

If you are using the Twilio Client SDK for iOS and Android, see the earlier Twilio Client Plugin (now deprecated and obsolete)
https://github.com/jefflinwood/twilio_client_phonegap


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
