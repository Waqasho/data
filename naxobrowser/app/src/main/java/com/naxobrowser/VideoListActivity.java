package com.naxobrowser;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoListActivity extends AppCompatActivity {

    private ListView videoListView;
    private VideoAdapter videoAdapter;
    private Button clearButton;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        initializeViews();
        setupAdapter();
        loadVideos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadVideos();
    }

    private void initializeViews() {
        videoListView = findViewById(R.id.videoListView);
        clearButton = findViewById(R.id.clearButton);
        emptyStateText = findViewById(R.id.emptyStateText);

        clearButton.setOnClickListener(v -> {
            VideoManager.getInstance().clearAllVideos();
            loadVideos();
            Toast.makeText(this, "All videos cleared", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupAdapter() {
        videoAdapter = new VideoAdapter();
        videoListView.setAdapter(videoAdapter);
    }

    private void loadVideos() {
        List<VideoDetector.VideoInfo> videos = VideoManager.getInstance().getDetectedVideos();
        if (videos.isEmpty()) {
            videoListView.setVisibility(View.GONE);
            clearButton.setVisibility(View.GONE);
            emptyStateText.setVisibility(View.VISIBLE);
        } else {
            videoListView.setVisibility(View.VISIBLE);
            clearButton.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
            videoAdapter.setVideos(videos);
        }
    }

    private class VideoAdapter extends BaseAdapter {

        private List<VideoDetector.VideoInfo> videos = new ArrayList<>();

        public void setVideos(List<VideoDetector.VideoInfo> videos) {
            this.videos.clear();
            this.videos.addAll(videos);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() { return videos.size(); }
        @Override
        public Object getItem(int pos) { return videos.get(pos); }
        @Override
        public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(VideoListActivity.this)
                        .inflate(R.layout.video_item, parent, false);
            }

            VideoDetector.VideoInfo video = videos.get(position);
            bindVideoItem(convertView, video);

            return convertView;
        }

        private void bindVideoItem(View itemView, VideoDetector.VideoInfo video) {
            TextView titleText = itemView.findViewById(R.id.videoTitle);
            TextView urlText = itemView.findViewById(R.id.videoUrl);
            TextView formatText = itemView.findViewById(R.id.videoFormat);
            TextView qualityText = itemView.findViewById(R.id.videoQuality);
            TextView drmText = itemView.findViewById(R.id.videoDrm);
            Button copyButton = itemView.findViewById(R.id.copyButton);
            Button downloadButton = itemView.findViewById(R.id.downloadButton);
            // Remove playButton logic since it does not exist
            boolean isBlob = video.url != null && video.url.startsWith("blob:");
            if (isBlob) {
                downloadButton.setEnabled(false);
                downloadButton.setAlpha(0.5f);
                downloadButton.setOnClickListener(v -> Toast.makeText(VideoListActivity.this, "Cannot download/play blob videos. Try a different video.", Toast.LENGTH_SHORT).show());
            } else {
                downloadButton.setEnabled(true);
                downloadButton.setAlpha(1.0f);
                downloadButton.setOnClickListener(v -> {
                    if (!video.isDrm) {
                        showVideoPlaybackOptions(video);
                    } else {
                        Toast.makeText(VideoListActivity.this, "Cannot download DRM protected content", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private void showVideoPlaybackOptions(VideoDetector.VideoInfo video) {
        // Headers tayyar karein
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 12; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36");
        if (video.pageUrl != null && !video.pageUrl.isEmpty()) {
            headers.put("Referer", video.pageUrl);
        } else {
            headers.put("Referer", "https://www.facebook.com/" );
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Play or Download");
        builder.setItems(new CharSequence[]{"Play In-App", "Download", "Copy URL"}, (dialog, which) -> {
            switch (which) {
                case 0: // Play In-App
                    Intent playIntent = new Intent(VideoListActivity.this, VideoPlayerActivity.class);
                    playIntent.putExtra("video_url", Html.fromHtml(video.url).toString());
                    // Headers ko Intent ke saath bhejein
                    playIntent.putExtra("video_headers", headers);
                    startActivity(playIntent);
                    break;
                case 1: // Download
                    startVideoDownload(video, headers);
                    break;
                case 2: // Copy URL
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("Video URL", video.url);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(VideoListActivity.this, "URL copied!", Toast.LENGTH_SHORT).show();
                    break;
            }
        });
        builder.show();
    }

    private void startVideoDownload(VideoDetector.VideoInfo video, Map<String, String> headers) {
        String cleanUrl = Html.fromHtml(video.url).toString();
        String fileName = (video.title != null && !video.title.isEmpty() ? video.title : "video_" + System.currentTimeMillis()) + ".mp4";

        OkHttpDownloader.downloadWithHeaders(this, cleanUrl, fileName, headers);
        Toast.makeText(this, "Download started...", Toast.LENGTH_LONG).show();
    }
}
