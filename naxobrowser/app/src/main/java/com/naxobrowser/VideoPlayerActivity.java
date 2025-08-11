package com.naxobrowser;

import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.HttpDataSource;

import java.util.HashMap;
import java.util.Map;

public class VideoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.player_view);
        progressBar = findViewById(R.id.progress_bar);

        String videoUrl = getIntent().getStringExtra("video_url");
        HashMap<String, String> headers = (HashMap<String, String>) getIntent().getSerializableExtra("video_headers");

        if (videoUrl != null && !videoUrl.isEmpty()) {
            // URL ko saaf karein (HTML encoding hatayein)
            String cleanUrl = Html.fromHtml(videoUrl).toString();
            initializePlayer(cleanUrl, headers);
        } else {
            Toast.makeText(this, "Video URL not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializePlayer(String videoUrl, Map<String, String> headers) {
        try {
            // 1. HttpDataSource.Factory ko behtar tareeqe se banayein
            DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory( )
                    .setAllowCrossProtocolRedirects(true) // Redirects ko allow karein
                    .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                    .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS);

            // 2. Agar headers hain, to unhe set karein
            if (headers != null && !headers.isEmpty()) {
                Log.d("PLAYER_FIX", "Setting headers for player: " + headers.toString());
                httpDataSourceFactory.setDefaultRequestProperties(headers );
            }

            // 3. MediaSource ke liye DataSource.Factory banayein
            DataSource.Factory dataSourceFactory = httpDataSourceFactory;

            // MediaItem banayein
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(videoUrl ));

            // MediaSource banayein (HLS ya normal video ke liye)
            MediaSource mediaSource;
            if (videoUrl.contains(".m3u8")) {
                mediaSource = new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            } else {
                mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem);
            }

            // ExoPlayer instance banayein
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            // Player ke events ko sunein
            player.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    if (playbackState == Player.STATE_BUFFERING) {
                        progressBar.setVisibility(View.VISIBLE);
                    } else {
                        progressBar.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onPlayerError(com.google.android.exoplayer2.PlaybackException error) {
                    // Error ko log karein taaki aap dekh sakein ke masla kya hai
                    Log.e("PLAYER_ERROR", "Playback Error: " + error.getMessage(), error);
                    Toast.makeText(VideoPlayerActivity.this, "Playback Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

            player.setMediaSource(mediaSource);
            player.prepare();
            player.setPlayWhenReady(true);

        } catch (Exception e) {
            Log.e("PLAYER_ERROR", "Error initializing player: ", e);
            Toast.makeText(this, "Error playing video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) {
            player.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
