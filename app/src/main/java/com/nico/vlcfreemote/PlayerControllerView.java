package com.nico.vlcfreemote;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nico.vlcfreemote.vlc_connector.Cmd_CycleAudioTrack;
import com.nico.vlcfreemote.vlc_connector.Cmd_CycleSubtitle;
import com.nico.vlcfreemote.vlc_connector.Cmd_JumpRelativePercent;
import com.nico.vlcfreemote.vlc_connector.Cmd_JumpToPositionPercent;
import com.nico.vlcfreemote.vlc_connector.Cmd_Next;
import com.nico.vlcfreemote.vlc_connector.Cmd_Prev;
import com.nico.vlcfreemote.vlc_connector.Cmd_SetVolume;
import com.nico.vlcfreemote.vlc_connector.Cmd_ToggleFullscreen;
import com.nico.vlcfreemote.vlc_connector.Cmd_TogglePlay;
import com.nico.vlcfreemote.vlc_connector.VlcStatus;

import java.util.Locale;

public class PlayerControllerView extends VlcFragment
                                  implements View.OnClickListener,
                                             SeekBar.OnSeekBarChangeListener  {
    private Activity activity;

    /************************************************************/
    /* Android stuff                                            */
    /************************************************************/
    public PlayerControllerView() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_player_controller_view, container, false);

        v.findViewById(R.id.wPlayer_ToggleMoreOptions).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_PlayPosition_JumpBack).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_PlayPosition).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_PlayPosition_JumpForward).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_BtnPrev).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_BtnNext).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_Volume).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_BtnPlayPause).setOnClickListener(this);

        v.findViewById(R.id.wPlayer_ToggleFullscreen).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_CycleAudioTrack).setOnClickListener(this);
        v.findViewById(R.id.wPlayer_CycleSubtitleTrack).setOnClickListener(this);

        ((SeekBar) v.findViewById(R.id.wPlayer_Volume)).setOnSeekBarChangeListener(this);
        ((SeekBar) v.findViewById(R.id.wPlayer_PlayPosition)).setOnSeekBarChangeListener(this);

        return v;
    }

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.activity = null;
    }

    /************************************************************/
    /* Event handlers                                           */
    /************************************************************/
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.wPlayer_ToggleMoreOptions: onToggleMoreOptionsClicked(); break;
            case R.id.wPlayer_PlayPosition_JumpBack: onPlayPosition_JumpBackClicked(); break;
            case R.id.wPlayer_PlayPosition_JumpForward: onPlayPosition_JumpForwardClicked(); break;
            case R.id.wPlayer_BtnPrev: onBtnPrevClicked(); break;
            case R.id.wPlayer_BtnNext: onBtnNextClicked(); break;
            case R.id.wPlayer_BtnPlayPause: onBtnPlayPauseClicked(); break;
            case R.id.wPlayer_ToggleFullscreen: onToggleFullscreen(); break;
            case R.id.wPlayer_CycleAudioTrack: onCycleAudioTrack(); break;
            case R.id.wPlayer_CycleSubtitleTrack: onCycleSubtitleTrack(); break;
            default:
                throw new RuntimeException(getClass().getName() + " received an event it doesn't know how to handle.");
        }
    }

    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) return;
        switch (seekBar.getId()) {
            case R.id.wPlayer_PlayPosition: onPlayPositionClicked(progress); break;
            case R.id.wPlayer_Volume: onVolumeClicked(progress); break;
        }
    }

    private void onToggleMoreOptionsClicked() {
        View panel = this.activity.findViewById(R.id.wPlayer_ExtraOptions);
        if (panel.getVisibility() == View.GONE) {
            panel.setVisibility(View.VISIBLE);
        } else {
            panel.setVisibility(View.GONE);
        }
    }

    /************************************************************/
    /* Vlc interaction                                          */
    /************************************************************/

    private void onPlayPosition_JumpBackClicked() {
        getVlc().exec(new Cmd_JumpRelativePercent(-0.5f, getVlc()));
    }

    private void onPlayPositionClicked(int progress) {
        getVlc().exec(new Cmd_JumpToPositionPercent(progress, getVlc()));
    }

    private void onPlayPosition_JumpForwardClicked() {
        getVlc().exec(new Cmd_JumpRelativePercent(+0.5f, getVlc()));
    }

    private void onBtnPrevClicked() {
        getVlc().exec(new Cmd_Prev(getVlc()));
    }

    private void onBtnNextClicked() {
        getVlc().exec(new Cmd_Next(getVlc()));
    }

    private void onVolumeClicked(int progress) {
        getVlc().exec(new Cmd_SetVolume(progress, getVlc()));
    }

    private void onBtnPlayPauseClicked() {
        getVlc().exec(new Cmd_TogglePlay(getVlc()));
    }

    public void onStatusUpdated(final Activity activity, VlcStatus status) {
        // TODO: Current play file...

        final SeekBar volumeCtrl = ((SeekBar) activity.findViewById(R.id.wPlayer_Volume));
        volumeCtrl.setProgress(status.volume);

        final SeekBar posCtrl = ((SeekBar) activity.findViewById(R.id.wPlayer_PlayPosition));
        posCtrl.setProgress((int) (status.position));

        final String currPos = String.format(Locale.getDefault(), "%d:%02d", status.time / 60, status.time % 60);
        final TextView currentPosTxt = ((TextView) activity.findViewById(R.id.wPlayer_PlayPosition_CurrentPositionText));
        currentPosTxt.setText(currPos);

        final String length = String.format(Locale.getDefault(), "%d:%02d", status.length / 60, status.length % 60);
        final TextView lengthTxt = ((TextView) activity.findViewById(R.id.wPlayer_PlayPosition_Length));
        lengthTxt.setText(length);
    }


    private void onToggleFullscreen() {
        getVlc().exec(new Cmd_ToggleFullscreen(getVlc()));
    }

    private void onCycleSubtitleTrack() {
        getVlc().exec(new Cmd_CycleSubtitle(getVlc()));
    }

    private void onCycleAudioTrack() {
        getVlc().exec(new Cmd_CycleAudioTrack(getVlc()));
    }
}
