//
//  TwilioVoicePlugin.m
//  TwilioVoiceExample
//
//  Created by Jeffrey Linwood on 3/11/17.
//
//

#import "TwilioVoicePlugin.h"

@import AVFoundation;
@import PushKit;
@import TwilioVoiceClient;

@interface TwilioVoicePlugin () <PKPushRegistryDelegate, TVOCallDelegate, TVONotificationDelegate>

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
        return @"TVOCallInviteStatePending";
    } else if (state == TVOCallInviteStateAccepted) {
        return @"TVOCallInviteStateAccepted";
    } else if (state == TVOCallInviteStateRejected) {
        return @"TVOCallInviteStateRejected";
    } else if (state == TVOCallInviteStateCancelled) {
        return @"TVOCallInviteStateCancelled";
    }
    
    return nil;
}

- (NSString*) stringFromCallState:(TVOCallState)state {
    if (state == TVOCallStateConnected) {
        return @"TVOCallStateConnected";
    } else if (state == TVOCallStateConnecting) {
        return @"TVOCallStateConnecting";
    } else if (state == TVOCallStateDisconnected) {
        return @"TVOCallStateDisconnected";
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

@end
