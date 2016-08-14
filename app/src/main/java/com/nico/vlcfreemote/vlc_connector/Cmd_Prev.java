package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.model.VlcStatus;

public class Cmd_Prev extends VlcCommand_ReturnsVlcStatus {

    public Cmd_Prev(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_previous";
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}
