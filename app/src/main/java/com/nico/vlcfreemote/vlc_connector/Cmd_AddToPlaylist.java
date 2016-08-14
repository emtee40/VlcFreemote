package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.model.VlcStatus;

public class Cmd_AddToPlaylist extends VlcCommand_ReturnsVlcStatus {

    private final String mediaUri;

    public Cmd_AddToPlaylist(final String mediaUri, VlcStatus.ObserverRegister cb) {
        super(cb);
        this.mediaUri = mediaUri;
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=in_enqueue&input=" + this.mediaUri;
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}
