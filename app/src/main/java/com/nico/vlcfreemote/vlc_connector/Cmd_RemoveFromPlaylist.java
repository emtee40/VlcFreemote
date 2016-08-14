package com.nico.vlcfreemote.vlc_connector;

import com.nico.vlcfreemote.model.VlcStatus;

public class Cmd_RemoveFromPlaylist extends VlcCommand_ReturnsVlcStatus {

    private final int id;

    public Cmd_RemoveFromPlaylist(int id, VlcStatus.ObserverRegister cb) {
        super(cb);
        this.id = id;
    }
    
    @Override
    public String getCommandPath() {
        return "requests/status.xml?command=pl_delete&id=" + String.valueOf(id);
    }

    @Override
    public Priority getPriority() { return Priority.MustExecute; }
}