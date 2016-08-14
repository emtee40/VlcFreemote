package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.model.VlcStatus;

public class Cmd_UpdateStatus extends VlcCommand_ReturnsVlcStatus {

    public Cmd_UpdateStatus(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml";
    }

    @Override
    public Priority getPriority() { return Priority.CanIgnore; }
}
