package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.model.VlcStatus;

public class Cmd_ClearPlaylist extends VlcCommand_ReturnsVlcStatus {

    public Cmd_ClearPlaylist(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_empty";
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}
