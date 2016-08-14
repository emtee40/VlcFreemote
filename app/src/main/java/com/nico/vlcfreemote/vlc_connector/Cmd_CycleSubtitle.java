package com.nico.vlcfreemote.vlc_connector;

public class Cmd_CycleSubtitle extends VlcCommand_ReturnsVlcStatus {

    public Cmd_CycleSubtitle(VlcStatus.ObserverRegister cb) {
        super(cb);
    }

    @Override
    public String getCommandPath() { return "requests/status.xml?command=key&val=subtitle-track"; }

    @Override
    public Priority getPriority() { return Priority.CanDelay; }
}
