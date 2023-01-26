[![npm version](https://badge.fury.io/js/cordova-plugin-twiliovoicesdk.svg)](https://badge.fury.io/js/cordova-plugin-twiliovoicesdk)

# Cordova Plugin for Twilio Voice
Cordova Plugin for Twilio Programmable Voice SDK

## Looking for Maintainer
This project does not get as much of my attention as it deserves, as I'm not a user of the plugin. I would be happy to put a link here to an active fork or fork(s) of this project, or to retire it if need be. 

# Roadmap (October 30, 2022)
Here is what is on the road map as of the current date for this project:
* Bump supported Twilio iOS Voice SDK to 6.4.2
* Bump supported Twilio Android Voice SDK to 6.1.2
* Bump supported Cordova version to 11.0
* Refactor iOS and Android plugin code - both were based on Twilio sample code, which probably isn't the best base for this project
* Add test coverage for JS, Obj-C, and Java - this will be an interesting project, as the code in this plugin largely acts as an integration layer

# Changelog
* 7.0.0 - February 20, 2022 - Send call properties with canceled call invites, show Android dialog on incoming calls
* 6.0.3 - October 31, 2021 - Support all parameters being passed to the Android plugin, not just "To"
* 6.0.2 - October 2, 2021 - Added error code to error message for debugging in Android
* 6.0.1 - October 1, 2021 - Updated to support Twilio Voice SDK 5.8.0 for Android
* 6.0.0 - April 18, 2021 - Updated to support Twilio Voice SDK 6.2.2 for iOS

# Using the plugin

As a very first step to using this plugin, you need to make sure that you can actually use the Twilio Voice SDK for Android or iOS with your project. You also need to have a working Cordova or Capacitor app to add this plugin.

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
