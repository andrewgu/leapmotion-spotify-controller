require([
    '$api/models'
], function(models) {
    'use strict';
    window.hooks = {
        player : models.player,
        play : function() {
            window.hooks.player.load("playing", "track").done(function(p) {
                if (p.track != null && !p.playing) {
                    console.log("Resuming");
                    window.hooks.player.play();
                }
                else {
                    console.log("Already playing (or no track queued).");
                }
            });
        },
        pause : function() {
            window.hooks.player.load("playing", "track").done(function(p) {
                if (p.track != null && p.playing) {
                    console.log("Pausing");
                    window.hooks.player.pause();
                }
                else {
                    console.log("Already paused (or no track queued).");
                }
            });
        }
    };
    
    var serverUpdateCallback = function() {
        // Use most up to date information available about player status.
        window.hooks.player.load("playing", "track").done(function(p) {
            $.ajax("http://localhost:15678/getcommand", {
                    async: true,
                    crossDomain: true,
                    type: "POST",
                    data: {playing: (p.playing ? 1 : 0), hastrack:((p.track != null) ? 1 : 0)},
                    dataType: "json"
            })
            .done(function(data) {
                console.log("Successful server response.");
                /* Expected data format:
                    {command: "play"}
                    {command: "pause"}
                    or {command: "none"}
                */
                
                if (data.command == "play") {
                    console.log("Got play command.");
                    window.hooks.play();
                }
                else if (data.command == "pause") {
                    console.log("Got pause command.");
                    window.hooks.pause();
                }
                else if (data.command == "volume") {
                    var targetVolume = data.volume;
                    console.log("Got volume command: " + targetVolume.toString());
                    window.hooks.player.setVolume(targetVolume);
                }
            })
            .fail(function(jqXHR, textStatus, errorThrown) {
                console.log("Failed server update request.");
                console.log(textStatus);
                console.log(errorThrown);
            })
            .always(function() {
                console.log("Pinging server for update.");
                setTimeout(serverUpdateCallback, 300);
            });
        });
    };
    
    // 100 ms delay between pings to the server. Rely on server to control response spacing to keep frequency of pings down.
    setTimeout(serverUpdateCallback, 300);
});

$(window).load(function() {
    $("#lps-debug").text("Connecting...");
});
