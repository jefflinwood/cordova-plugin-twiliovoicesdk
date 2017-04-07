//
//  TwilioVoicePlugin.m
//  TwilioVoiceExample
//
//  Created by Jeffrey Linwood on 3/11/17.
//
//  Based on https://github.com/twilio/voice-callkit-quickstart-objc
//

#import "TwilioVoicePlugin.h"

@import AVFoundation;
@import CallKit;
@import PushKit;
@import TwilioVoiceClient;

@interface TwilioVoicePlugin () <PKPushRegistryDelegate, TVOCallDelegate, TVONotificationDelegate, CXProviderDelegate>

// Callback for the Javascript plugin delegate, used for events
@property(nonatomic, strong) NSString *callback;

// Push registry for APNS VOIP
@property (nonatomic, strong) PKPushRegistry *voipPushRegistry;

// Current call (can be nil)
@property (nonatomic, strong) TVOCall *call;

// Current call invite (can be nil)
@property (nonatomic, strong) TVOCallInvite *callInvite;

// Device Token from Apple Push Notification Service for VOIP
@property (nonatomic, strong) NSString *pushDeviceToken;

// Access Token from Twilio
@property (nonatomic, strong) NSString *accessToken;

// Call Kit member variables
@property (nonatomic, strong) CXProvider *callKitProvider;
@property (nonatomic, strong) CXCallController *callKitCallController;

@end

@implementation TwilioVoicePlugin

- (void) pluginInitialize {
    [super pluginInitialize];
    
    NSLog(@"Initializing plugin");

    // set log level for development
    [[VoiceClient sharedInstance] setLogLevel:TVOLogLevelOff];
    
}

- (void) initializeWithAccessToken:(CDVInvokedUrlCommand*)command  {
    NSLog(@"Initializing with an access token");
    
    // retain this command as the callback to use for raising Twilio events
    self.callback = command.callbackId;
    
    self.accessToken = [command.arguments objectAtIndex:0];
    if (self.accessToken) {
        
        // initialize VOIP Push Registry
        self.voipPushRegistry = [[PKPushRegistry alloc] initWithQueue:dispatch_get_main_queue()];
        self.voipPushRegistry.delegate = self;
        self.voipPushRegistry.desiredPushTypes = [NSSet setWithObject:PKPushTypeVoIP];
        
        // initialize CallKit (based on Twilio ObjCVoiceCallKitQuickstart)
        NSString *incomingCallAppName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"TVPIncomingCallAppName"];
        CXProviderConfiguration *configuration = [[CXProviderConfiguration alloc] initWithLocalizedName:incomingCallAppName];
        configuration.maximumCallGroups = 1;
        configuration.maximumCallsPerCallGroup = 1;
        UIImage *callkitIcon = [UIImage imageNamed:@"logo.png"];
        configuration.iconTemplateImageData = UIImagePNGRepresentation(callkitIcon);
        configuration.ringtoneSound = @"ringing.wav";
        
        self.callKitProvider = [[CXProvider alloc] initWithConfiguration:configuration];
        [self.callKitProvider setDelegate:self queue:nil];
        
        self.callKitCallController = [[CXCallController alloc] init];

        [self javascriptCallback:@"onclientinitialized"];
    }
    
}

- (void) call:(CDVInvokedUrlCommand*)command {
    if ([command.arguments count] > 0) {
        self.accessToken = command.arguments[0];
        if ([command.arguments count] > 1) {
            NSDictionary *params = command.arguments[1];
            NSLog(@"Making call to with params %@", params);
            self.call = [[VoiceClient sharedInstance] call:self.accessToken
                                                params:params
                                              delegate:self];
        } else {
            NSLog(@"Making call with no params");
            self.call = [[VoiceClient sharedInstance] call:self.accessToken
                                                    params:@{}
                                                  delegate:self];

        }
    }
}

- (void) sendDigits:(CDVInvokedUrlCommand*)command {
    if ([command.arguments count] > 0) {
        [self.call sendDigits:command.arguments[0]];
    }
}

- (void) disconnect:(CDVInvokedUrlCommand*)command {
    if (self.call && self.call.state == TVOCallStateConnected) {
        [self.call disconnect];
    }
}

- (void) acceptCallInvite:(CDVInvokedUrlCommand*)command {
    if (self.callInvite) {
        [self.callInvite acceptWithDelegate:self];
    }
}

- (void) rejectCallInvite: (CDVInvokedUrlCommand*)command {
    if (self.callInvite) {
        [self.callInvite reject];
    }
}

-(void)setSpeaker:(CDVInvokedUrlCommand*)command {
    NSString *mode = [command.arguments objectAtIndex:0];
    if([mode isEqual: @"on"]) {
        UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_Speaker;
        AudioSessionSetProperty (
            kAudioSessionProperty_OverrideAudioRoute,
            sizeof (audioRouteOverride),
            &audioRouteOverride
        );
    }
    else {
        UInt32 audioRouteOverride = kAudioSessionOverrideAudioRoute_None;
        AudioSessionSetProperty (
            kAudioSessionProperty_OverrideAudioRoute,
            sizeof (audioRouteOverride),
            &audioRouteOverride
        );
    }
}

- (void) muteCall: (CDVInvokedUrlCommand*)command {
    if (self.call) {
        self.call.muted = YES;
    }
}

- (void) unmuteCall: (CDVInvokedUrlCommand*)command {
    if (self.call) {
        self.call.muted = NO;
    }
}

- (void) isCallMuted: (CDVInvokedUrlCommand*)command {
    if (self.call) {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:self.call.muted];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:NO];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    }
}

#pragma mark PKPushRegistryDelegate methods
- (void)pushRegistry:(PKPushRegistry *)registry didUpdatePushCredentials:(PKPushCredentials *)credentials forType:(PKPushType)type {
    if ([type isEqualToString:PKPushTypeVoIP]) {
        self.pushDeviceToken = [credentials.token description];
        NSLog(@"Updating push device token for VOIP: %@",self.pushDeviceToken);
        [[VoiceClient sharedInstance] registerWithAccessToken:self.accessToken
                                                  deviceToken:self.pushDeviceToken completion:^(NSError * _Nullable error) {
            if (error) {
                NSLog(@"Error registering Voice Client for VOIP Push: %@", [error localizedDescription]);
            } else {
                NSLog(@"Registered Voice Client for VOIP Push");
            }
        }];
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry didInvalidatePushTokenForType:(PKPushType)type {
    if ([type isEqualToString:PKPushTypeVoIP]) {
        NSLog(@"Invalidating push device token for VOIP: %@",self.pushDeviceToken);
        [[VoiceClient sharedInstance] unregisterWithAccessToken:self.accessToken
                                                    deviceToken:self.pushDeviceToken completion:^(NSError * _Nullable error) {
            if (error) {
                NSLog(@"Error unregistering Voice Client for VOIP Push: %@", [error localizedDescription]);
            } else {
                NSLog(@"Unegistered Voice Client for VOIP Push");
            }
            self.pushDeviceToken = nil;
        }];
    }
}

- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(PKPushType)type {
    if ([type isEqualToString:PKPushTypeVoIP]) {
        NSLog(@"Received Incoming Push Payload for VOIP: %@",payload.dictionaryPayload);
        [[VoiceClient sharedInstance] handleNotification:payload.dictionaryPayload delegate:self];
    }
}

#pragma mark TVONotificationDelegate
- (void)callInviteReceived:(TVOCallInvite *)callInvite {
    NSLog(@"Call Invite Received: %@", [callInvite description]);
    self.callInvite = callInvite;
    NSDictionary *callInviteProperties = @{
                                           @"from":callInvite.from,
                                           @"to":callInvite.to,
                                           @"callSid":callInvite.callSid,
                                           @"state":[self stringFromCallInviteState:callInvite.state]
                                           };
    
    [self reportIncomingCallFrom:callInvite.from withUUID:callInvite.uuid];

    [self javascriptCallback:@"oncallinvitereceived" withArguments:callInviteProperties];
}

- (void)callInviteCancelled:(TVOCallInvite *)callInvite {
    NSLog(@"Call Invite Cancelled: %@", [callInvite description]);
    self.callInvite = nil;
    [self javascriptCallback:@"oncallinvitecanceled"];

}

- (void)notificationError:(NSError *)error {
    NSLog(@"Twilio Voice Notification Error: %@", [error localizedDescription]);
    [self javascriptErrorback:error];
}

#pragma mark TVOCallDelegate
- (void)callDidConnect:(TVOCall *)call {
    NSLog(@"Call Did Connect: %@", [call description]);
    self.call = call;
    
    NSMutableDictionary *callProperties = [NSMutableDictionary new];
    if (call.from) {
        callProperties[@"from"] = call.from;
    }
    if (call.to) {
        callProperties[@"to"] = call.to;
    }
    if (call.callSid) {
        callProperties[@"callSid"] = call.callSid;
    }
    callProperties[@"isMuted"] = [NSNumber numberWithBool:call.isMuted];
    NSString *callState = [self stringFromCallState:call.state];
    if (callState) {
        callProperties[@"state"] = callState;
    }
    [self javascriptCallback:@"oncalldidconnect" withArguments:callProperties];
    
}

- (void)callDidDisconnect:(TVOCall *)call {
    NSLog(@"Call Did Disconnect: %@", [call description]);
    
    // Call Kit Integration
    [self performEndCallActionWithUUID:call.uuid];
    
    self.call = nil;
    [self javascriptCallback:@"oncalldiddisconnect"];
}

- (void)call:(TVOCall *)call didFailWithError:(NSError *)error {
    NSLog(@"Call Did Fail with Error: %@, %@", [call description], [error localizedDescription]);
    self.call = nil;
    [self javascriptErrorback:error];
}

#pragma mark Conversion methods for the plugin

- (NSString*) stringFromCallInviteState:(TVOCallInviteState)state {
    if (state == TVOCallInviteStatePending) {
        return @"pending";
    } else if (state == TVOCallInviteStateAccepted) {
        return @"accepted";
    } else if (state == TVOCallInviteStateRejected) {
        return @"rejected";
    } else if (state == TVOCallInviteStateCancelled) {
        return @"cancelled";
    }
    
    return nil;
}

- (NSString*) stringFromCallState:(TVOCallState)state {
    if (state == TVOCallStateConnected) {
        return @"connected";
    } else if (state == TVOCallStateConnecting) {
        return @"connecting";
    } else if (state == TVOCallStateDisconnected) {
        return @"disconnected";
    }
    return nil;
}

#pragma mark Cordova Integration methods for the plugin Delegate - from TCPlugin.m/Stevie Graham

- (void) javascriptCallback:(NSString *)event withArguments:(NSDictionary *)arguments {
    NSDictionary *options   = [NSDictionary dictionaryWithObjectsAndKeys:event, @"callback", arguments, @"arguments", nil];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:options];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:self.callback];
}

- (void) javascriptCallback:(NSString *)event {
    [self javascriptCallback:event withArguments:nil];
}

- (void) javascriptErrorback:(NSError *)error {
    NSDictionary *object    = [NSDictionary dictionaryWithObjectsAndKeys:[error localizedDescription], @"message", nil];
    CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:object];
    [result setKeepCallbackAsBool:YES];
    
    [self.commandDelegate sendPluginResult:result callbackId:self.callback];
}


#pragma mark - CXProviderDelegate - based on Twilio Voice with CallKit Quickstart ObjC

- (void)provider:(CXProvider *)provider performPlayDTMFCallAction:(CXPlayDTMFCallAction *)action {
    if (self.call) {
        NSLog(@"Sending Digits: %@", action.digits);
        [self.call sendDigits:action.digits];
    } else {
        NSLog(@"No current call");
    }
    
}

// All CallKit Integration Code comes from https://github.com/twilio/voice-callkit-quickstart-objc/blob/master/ObjCVoiceCallKitQuickstart/ViewController.m

- (void)providerDidReset:(CXProvider *)provider {
    // No implementation
}

- (void)providerDidBegin:(CXProvider *)provider {
    // No implementation
}

- (void)provider:(CXProvider *)provider didActivateAudioSession:(AVAudioSession *)audioSession {
    [[VoiceClient sharedInstance] startAudioDevice];
}

- (void)provider:(CXProvider *)provider didDeactivateAudioSession:(AVAudioSession *)audioSession {
    // No implementation
}

- (void)provider:(CXProvider *)provider timedOutPerformingAction:(CXAction *)action {
    // No implementation
}

- (void)provider:(CXProvider *)provider performStartCallAction:(CXStartCallAction *)action {
    
    [[VoiceClient sharedInstance] configureAudioSession];
    
    self.call = [[VoiceClient sharedInstance] call:self.accessToken
                                            params:@{}
                                          delegate:self];
    
    if (!self.call) {
        [action fail];
    } else {
        self.call.uuid = action.callUUID;
        [action fulfillWithDateStarted:[NSDate date]];
    }
}

- (void)provider:(CXProvider *)provider performAnswerCallAction:(CXAnswerCallAction *)action {

    // Below comment from: https://github.com/twilio/voice-callkit-quickstart-objc/blob/master/ObjCVoiceCallKitQuickstart/ViewController.m#L298
    
    // Comment below from
    // RCP: Workaround from https://forums.developer.apple.com/message/169511 suggests configuring audio in the
    //      completion block of the `reportNewIncomingCallWithUUID:update:completion:` method instead of in
    //      `provider:performAnswerCallAction:` per the WWDC examples.
    // [[VoiceClient sharedInstance] configureAudioSession];
    
    self.call = [self.callInvite acceptWithDelegate:self];
    if (self.call) {
        self.call.uuid = [action callUUID];
    }
    
    self.callInvite = nil;
    
    [action fulfill];
}

- (void)provider:(CXProvider *)provider performEndCallAction:(CXEndCallAction *)action {
    
    [[VoiceClient sharedInstance] stopAudioDevice];
    
    if (self.callInvite && self.callInvite.state == TVOCallInviteStatePending) {
        [self.callInvite reject];
        self.callInvite = nil;
    } else if (self.call) {
        [self.call disconnect];
    }
    
    [action fulfill];
}

#pragma mark - CallKit Actions
- (void)performStartCallActionWithUUID:(NSUUID *)uuid handle:(NSString *)handle {
    if (uuid == nil || handle == nil) {
        return;
    }
    
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:handle];
    CXStartCallAction *startCallAction = [[CXStartCallAction alloc] initWithCallUUID:uuid handle:callHandle];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:startCallAction];
    
    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
        if (error) {
            NSLog(@"StartCallAction transaction request failed: %@", [error localizedDescription]);
        } else {
            NSLog(@"StartCallAction transaction request successful");
            
            CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
            callUpdate.remoteHandle = callHandle;
            callUpdate.supportsDTMF = YES;
            callUpdate.supportsHolding = NO;
            callUpdate.supportsGrouping = NO;
            callUpdate.supportsUngrouping = NO;
            callUpdate.hasVideo = NO;
            
            [self.callKitProvider reportCallWithUUID:uuid updated:callUpdate];
        }
    }];
}

- (void)reportIncomingCallFrom:(NSString *) from withUUID:(NSUUID *)uuid {
    CXHandle *callHandle = [[CXHandle alloc] initWithType:CXHandleTypeGeneric value:from];
    
    CXCallUpdate *callUpdate = [[CXCallUpdate alloc] init];
    callUpdate.remoteHandle = callHandle;
    callUpdate.supportsDTMF = YES;
    callUpdate.supportsHolding = NO;
    callUpdate.supportsGrouping = NO;
    callUpdate.supportsUngrouping = NO;
    callUpdate.hasVideo = NO;
    
    [self.callKitProvider reportNewIncomingCallWithUUID:uuid update:callUpdate completion:^(NSError *error) {
        if (!error) {
            NSLog(@"Incoming call successfully reported.");
            
            // RCP: Workaround per https://forums.developer.apple.com/message/169511
            [[VoiceClient sharedInstance] configureAudioSession];
        }
        else {
            NSLog(@"Failed to report incoming call successfully: %@.", [error localizedDescription]);
        }
    }];
}

- (void)performEndCallActionWithUUID:(NSUUID *)uuid {
    if (uuid == nil) {
        return;
    }
    
    CXEndCallAction *endCallAction = [[CXEndCallAction alloc] initWithCallUUID:uuid];
    CXTransaction *transaction = [[CXTransaction alloc] initWithAction:endCallAction];
    
    [self.callKitCallController requestTransaction:transaction completion:^(NSError *error) {
        if (error) {
            NSLog(@"EndCallAction transaction request failed: %@", [error localizedDescription]);
        }
        else {
            NSLog(@"EndCallAction transaction request successful");
        }
    }];
}

@end
