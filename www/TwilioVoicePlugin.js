(function(module, require) {
    var exec = require("cordova/exec")
    var delegate = {}

    function TwilioVoiceClient() {
        return this;
    }

    TwilioVoiceClient.prototype.call = function(token, params) {
        exec(null, null, "TwilioVoicePlugin", "call", [token, params]);
    }

    TwilioVoiceClient.prototype.sendDigits = function(digits) {
        exec(null, null, "TwilioVoicePlugin", "sendDigits", [digits]);
    }

    TwilioVoiceClient.prototype.disconnect = function() {
        exec(null, null, "TwilioVoicePlugin", "disconnect", null);
    }

    TwilioVoiceClient.prototype.rejectCallInvite = function() {
        exec(null, null, "TwilioVoicePlugin", "rejectCallInvite", null);
    }

    TwilioVoiceClient.prototype.acceptCallInvite = function() {
        exec(null, null, "TwilioVoicePlugin", "acceptCallInvite", null);
    }

    TwilioVoiceClient.prototype.setSpeaker = function(mode) {
        // "on" or "off"
        exec(null, null, "TwilioVoicePlugin", "setSpeaker", [mode]);
    }

    TwilioVoiceClient.prototype.muteCall = function() {
        exec(null, null, "TwilioVoicePlugin", "muteCall", null);
    }

    TwilioVoiceClient.prototype.unmuteCall = function() {
        exec(null, null, "TwilioVoicePlugin", "unmuteCall", null);
    }

    TwilioVoiceClient.prototype.isCallMuted = function(fn) {
        exec(fn, null, "TwilioVoicePlugin", "isCallMuted", null);
    }

    TwilioVoiceClient.prototype.initialize = function(token) {

        var error = function(error) {
            //TODO: Handle errors here
            if(delegate['onerror']) delegate['onerror'](error)
        }

        var success = function(callback) {
            var argument = callback['arguments'];
            if (delegate[callback['callback']]) delegate[callback['callback']](argument);
        }


        exec(success, error, "TwilioVoicePlugin", "initializeWithAccessToken", [token]);
    }

    TwilioVoiceClient.prototype.error = function(fn) {
        delegate['onerror'] = fn;
    }

    TwilioVoiceClient.prototype.clientinitialized = function(fn) {
        delegate['onclientinitialized'] = fn;
    }

    TwilioVoiceClient.prototype.callinvitereceived = function(fn) {
        delegate['oncallinvitereceived'] = fn;
    }

    TwilioVoiceClient.prototype.callinvitecanceled = function(fn) {
        delegate['oncallinvitecanceled'] = fn;
    }

    TwilioVoiceClient.prototype.calldidconnect = function(fn) {
        delegate['oncalldidconnect'] = fn;
    }

    TwilioVoiceClient.prototype.calldiddisconnect = function(fn) {
        delegate['oncalldiddisconnect'] = fn;
    }

    module.exports = new TwilioVoiceClient()

})(module, require)
