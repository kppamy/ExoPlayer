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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.LinearLayout;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.C.ContentType;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackPreparer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;

import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.RandomTrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;
import com.google.android.exoplayer2.upstream.RawResourceDataSource;
import com.google.android.exoplayer2.upstream.cache.Cache;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory;
import com.google.android.exoplayer2.util.EventLogger;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.util.UUID;

/**
 * Manages the {@link ExoPlayer}, the IMA plugin and all video playback.
 */
/* package */ final class PlayerManager implements PlaybackPreparer{

    private static ExtractorMediaSource.Factory dataSourceFactory;
    private  static MediaSource source;
    public final String TAG = "localplayer";
    private SimpleExoPlayer player;
    private long contentPosition;
    private LinearLayout debugRootView;
    private Context mContext;
    private PlayerView playerView;

    public PlayerManager(Context context) {
        mContext = context;
    }

    public void init(Context context, PlayerView playerView, LinearLayout debugRootView) {
//    // Create a default track selector.
//    TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
//    TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        this.playerView = playerView;
        this.debugRootView = debugRootView;
        initializePlayer(buildRawDataSource(R.raw.audio1));
    }

    public void reset() {
        if (player != null) {
            contentPosition = player.getContentPosition();
            player.release();
            player = null;
        }
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    public void preparePlayback() {

    }

    public void initializePlayer(MediaSource source) {
        if (player == null) {
            // Create a player instance.
            player = ExoPlayerFactory.newSimpleInstance(mContext);
            // Bind the player to the view.
            playerView.setPlayer(player);
            player.addListener(new PlayerEventListener());
            // This is the MediaSource representing the content media (i.e. not the ad).
//            String contentUrl = mContext.getString(R.string.content_url);
            // Prepare the player with the source.
            player.seekTo(contentPosition);
        }
        player.prepare(source);
        player.setPlayWhenReady(true);
    }

    public MediaSource buildLocalFileDataSource(Uri uri){
        dataSourceFactory = new ExtractorMediaSource.Factory(new FileDataSourceFactory());
        return dataSourceFactory.createMediaSource(uri);
    }

    public MediaSource buildRawDataSource(int id){
        final RawResourceDataSource rawResourceDataSource = new RawResourceDataSource(mContext);
        DataSpec dp = new DataSpec(RawResourceDataSource.buildRawResourceUri(id));

        try {
            rawResourceDataSource.open(dp);
        } catch (RawResourceDataSource.RawResourceDataSourceException ex) {
            ex.printStackTrace();
            return null;
        }
        dataSourceFactory =
                new ExtractorMediaSource.Factory(new DataSource.Factory() {
                    @Override
                    public DataSource createDataSource() {
                        return rawResourceDataSource;
                    }
                });
        source = dataSourceFactory.createMediaSource(rawResourceDataSource.getUri());
        return source;
    }

    private static CacheDataSourceFactory buildReadOnlyCacheDataSource(
            DefaultDataSourceFactory upstreamFactory, Cache cache) {
        return new CacheDataSourceFactory(
                cache,
                upstreamFactory,
                new FileDataSourceFactory(),
                /* cacheWriteDataSinkFactory= */ null,
                CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
                /* eventListener= */ null);
    }


    private class PlayerEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String state = "";
            switch (playbackState) {
                case Player.STATE_IDLE:
                    state = "STATE_IDLE";
                    break;
                case Player.STATE_BUFFERING:
                    state = "STATE_BUFFERING";
                    break;
                case Player.STATE_READY:
                    state = "STATE_READY";
                    break;
                case Player.STATE_ENDED:
                    state = "STATE_ENDED";
                    debugRootView.setVisibility(View.VISIBLE);
                    break;
                default:
                    break;
            }
            Log.i(TAG, "onPlayerStateChanged " + state);

        }

        @Override
        public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason) {
            Log.i(TAG, "onPositionDiscontinuity reason " + reason);

        }

        @Override
        public void onPlayerError(ExoPlaybackException e) {
            e.printStackTrace();
            Log.i(TAG, "onPlayerError ");
        }

        @Override
        @SuppressWarnings("ReferenceEquality")
        public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {
            Log.i(TAG, "onTracksChanged ");
        }
    }


}
