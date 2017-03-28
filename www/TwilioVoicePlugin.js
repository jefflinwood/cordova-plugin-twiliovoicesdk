(function() {
    var delegate = {}
    var TwilioPlugin = {

        TwilioVoiceClient: function() {
            return this;
        }
    }

    TwilioPlugin.TwilioVoiceClient.prototype.call = function(token, params) {
        Cordova.exec(null,null,"TwilioVoicePlugin","call",[token, params]);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.sendDigits = function(digits) {
        Cordova.exec(null,null,"TwilioVoicePlugin","sendDigits",[digits]);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.disconnect = function() {
        Cordova.exec(null,null,"TwilioVoicePlugin","disconnect",null);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.rejectCallInvite = function() {
        Cordova.exec(null,null,"TwilioVoicePlugin","rejectCallInvite",null);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.acceptCallInvite = function() {
        Cordova.exec(null,null,"TwilioVoicePlugin","acceptCallInvite",null);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.setSpeaker = function(mode) {
        // "on" or "off"        
        Cordova.exec(null, null, "TwilioVoicePlugin", "setSpeaker", [mode]);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.muteCall = function() {
        Cordova.exec(null, null, "TwilioVoicePlugin", "muteCall", null);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.unmuteCall = function() {
        Cordova.exec(null, null, "TwilioVoicePlugin", "unmuteCall", null);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.isCallMuted = function(fn) {
        Cordova.exec(fn, null, "TwilioVoicePlugin", "isCallMuted", null);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.initialize = function(token) {

        var error = function(error) {
            //TODO: Handle errors here
            if(delegate['onerror']) delegate['onerror'](error)
        }

        var success = function(callback) {
            var argument = callback['arguments'];
            if (delegate[callback['callback']]) delegate[callback['callback']](argument);
        }


        Cordova.exec(success,error,"TwilioVoicePlugin","initializeWithAccessToken",[token]);
    }

    TwilioPlugin.TwilioVoiceClient.prototype.error = function(fn) {
        delegate['onerror'] = fn;
    }

    TwilioPlugin.TwilioVoiceClient.prototype.clientinitialized = function(fn) {
        delegate['onclientinitialized'] = fn;
    }


    TwilioPlugin.TwilioVoiceClient.prototype.callinvitereceived = function(fn) {
        delegate['oncallinvitereceived'] = fn;
    }

    TwilioPlugin.TwilioVoiceClient.prototype.callinvitecanceled = function(fn) {
        delegate['oncallinvitecanceled'] = fn;
    }

    TwilioPlugin.TwilioVoiceClient.prototype.calldidconnect = function(fn) {
        delegate['oncalldidconnect'] = fn;
    }

    TwilioPlugin.TwilioVoiceClient.prototype.calldiddisconnect = function(fn) {
        delegate['oncalldiddisconnect'] = fn;
    }

    TwilioPlugin.install = function() {
        if (!window.Twilio) window.Twilio = {};
        if (!window.Twilio.TwilioVoiceClient) window.Twilio.TwilioVoiceClient = new TwilioPlugin.TwilioVoiceClient();
    }
 TwilioPlugin.install();

})()