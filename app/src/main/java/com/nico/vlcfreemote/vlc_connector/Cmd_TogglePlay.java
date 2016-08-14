package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.model.VlcStatus;

public class Cmd_TogglePlay extends VlcCommand_ReturnsVlcStatus {

    public Cmd_TogglePlay(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_pause";
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}
