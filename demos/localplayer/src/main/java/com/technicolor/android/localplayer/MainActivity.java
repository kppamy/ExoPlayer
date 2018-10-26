/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.technicolor.android.localplayer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;

/**
 * Main Activity for the IMA plugin demo. {@link ExoPlayer} objects are created by
 * {@link PlayerManager}, which this class instantiates.
 */
public final class MainActivity extends Activity {
    public final String TAG = "localplayer";
    private PlayerView playerView;
    private PlayerManager player;
    private LinearLayout debugRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        playerView = findViewById(R.id.player_view);
        debugRootView = findViewById(R.id.controls_root);

        Button button = new Button(this);
        button.setText("Select files");
        button.setTag(111);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFileSelector();
            }
        });
        debugRootView.addView(button);
        player = new PlayerManager(this);

    }

    private void showFileSelector() {
        ArrayList<String> fws = new ArrayList<>();
        String[] fwFolderPaths = new String[]{
                Environment.getExternalStorageDirectory().getPath() + "/BLEAudioFiles",
                "/data/misc/audioserver"};
        for (String path : fwFolderPaths) {
            File fwFolder = new File(path);
            if (fwFolder.exists()
                    && fwFolder.isDirectory()
                    && fwFolder.canExecute()) {
                File[] fs = fwFolder.listFiles();
                if (fs != null && fs.length > 0) {
                    for (File f : fs) {
                        fws.add(f.getPath());
                    }
                }
            }
        }
        final String[] files = new String[fws.size()];
        for (int i = 0; i < fws.size(); i++) {
            files[i] = fws.get(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select the audio file");
        if (files.length > 0) {
            builder.setSingleChoiceItems(files, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    String path = files[which];
                    MediaSource source = player.buildLocalFileDataSource(Uri.fromFile(new File(path)));
                    if(which == 0){
                        player.initializePlayer(new LoopingMediaSource(source));
                    }else
                    player.initializePlayer(source);
                }
            });
        } else {
            builder.setMessage("no files found");
        }
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    @Override
    public void onResume() {
        super.onResume();
        player.init(this, playerView, debugRootView);
    }

    @Override
    public void onPause() {
        super.onPause();
        player.reset();
    }

    @Override
    public void onDestroy() {
        player.release();
        super.onDestroy();
    }


}
