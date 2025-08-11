package com.naxobrowser;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class VideoManager {
    private static final String TAG = "VideoManager";
    private static VideoManager instance;

    private final List<VideoDetector.VideoInfo> detectedVideos;
    private final Set<String> videoUrls; // For duplicate checking

    private VideoManager() {
        detectedVideos = new CopyOnWriteArrayList<>();
        videoUrls = new HashSet<>();
    }

    public static synchronized VideoManager getInstance() {
        if (instance == null) {
            instance = new VideoManager();
        }
        return instance;
    }

    public synchronized void addVideo(VideoDetector.VideoInfo videoInfo) {
        if (videoInfo == null || videoInfo.url == null || videoInfo.url.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Attempted to add null or empty video");
            return;
        }

        // Clean the URL for comparison
        String cleanUrl = cleanVideoUrl(videoInfo.url);

        // Check for duplicates
        if (videoUrls.contains(cleanUrl)) {
            Log.d(TAG, "🔄 Duplicate video ignored: " + cleanUrl);
            return;
        }

        // Add to collections
        videoUrls.add(cleanUrl);
        detectedVideos.add(videoInfo);

        Log.d(TAG, "➕ Video added: " + videoInfo.format + " - " + cleanUrl);
        Log.d(TAG, "📊 Total videos: " + detectedVideos.size());
    }

    public synchronized boolean isVideoAlreadyDetected(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String cleanUrl = cleanVideoUrl(url);
        return videoUrls.contains(cleanUrl);
    }

    public synchronized List<VideoDetector.VideoInfo> getDetectedVideos() {
        // Return a copy to prevent external modification
        return new ArrayList<>(detectedVideos);
    }

    public synchronized void clearAllVideos() {
        detectedVideos.clear();
        videoUrls.clear();
        Log.d(TAG, "🗑️ All videos cleared");
    }

    public synchronized boolean hasVideos() {
        return !detectedVideos.isEmpty();
    }

    public synchronized int getVideoCount() {
        return detectedVideos.size();
    }

    public synchronized void removeVideo(VideoDetector.VideoInfo videoInfo) {
        if (videoInfo != null && videoInfo.url != null) {
            String cleanUrl = cleanVideoUrl(videoInfo.url);
            videoUrls.remove(cleanUrl);
            detectedVideos.remove(videoInfo);
            Log.d(TAG, "➖ Video removed: " + cleanUrl);
        }
    }

    public synchronized void removeVideoByUrl(String url) {
        if (url == null) return;

        String cleanUrl = cleanVideoUrl(url);
        videoUrls.remove(cleanUrl);

        // Remove from list
        detectedVideos.removeIf(video -> {
            String videoCleanUrl = cleanVideoUrl(video.url);
            return videoCleanUrl.equals(cleanUrl);
        });

        Log.d(TAG, "➖ Video removed by URL: " + cleanUrl);
    }

    /**
     * Clean video URL for comparison by removing parameters that don't affect the actual video
     */
    private String cleanVideoUrl(String url) {
        if (url == null) return "";

        // Remove common tracking parameters but keep important ones
        String cleaned = url;

        // For blob URLs, use as-is since they're unique
        if (url.startsWith("blob:")) {
            return url;
        }

        // For regular URLs, remove some common parameters that don't affect video content
        cleaned = cleaned.replaceAll("[&?]utm_[^&]*", "");
        cleaned = cleaned.replaceAll("[&?]fbclid=[^&]*", "");
        cleaned = cleaned.replaceAll("[&?]_nc_[^&]*", "");

        // Clean up any double ampersands or question marks
        cleaned = cleaned.replaceAll("[&]{2,}", "&");
        cleaned = cleaned.replaceAll("[?&]$", "");

        return cleaned.trim();
    }

    /**
     * Get videos by format
     */
    public synchronized List<VideoDetector.VideoInfo> getVideosByFormat(String format) {
        List<VideoDetector.VideoInfo> filtered = new ArrayList<>();
        for (VideoDetector.VideoInfo video : detectedVideos) {
            if (format.equals(video.format)) {
                filtered.add(video);
            }
        }
        return filtered;
    }

    /**
     * Get non-DRM videos only
     */
    public synchronized List<VideoDetector.VideoInfo> getNonDrmVideos() {
        List<VideoDetector.VideoInfo> filtered = new ArrayList<>();
        for (VideoDetector.VideoInfo video : detectedVideos) {
            if (!video.isDrm) {
                filtered.add(video);
            }
        }
        return filtered;
    }

    /**
     * Get downloadable videos (non-blob, non-DRM)
     */
    public synchronized List<VideoDetector.VideoInfo> getDownloadableVideos() {
        List<VideoDetector.VideoInfo> filtered = new ArrayList<>();
        for (VideoDetector.VideoInfo video : detectedVideos) {
            if (!video.isDrm && !isBlob(video.url)) {
                filtered.add(video);
            }
        }
        return filtered;
    }

    private boolean isBlob(String url) {
        return url != null && url.startsWith("blob:");
    }

    /**
     * Get video statistics
     */
    public synchronized VideoStats getVideoStats() {
        VideoStats stats = new VideoStats();

        for (VideoDetector.VideoInfo video : detectedVideos) {
            stats.totalVideos++;

            if (video.isDrm) {
                stats.drmVideos++;
            }

            if (isBlob(video.url)) {
                stats.blobVideos++;
            }

            if ("MP4".equals(video.format)) {
                stats.mp4Videos++;
            } else if ("HLS".equals(video.format)) {
                stats.hlsVideos++;
            } else if ("DASH".equals(video.format)) {
                stats.dashVideos++;
            }
        }

        stats.downloadableVideos = stats.totalVideos - stats.drmVideos - stats.blobVideos;

        return stats;
    }

    public static class VideoStats {
        public int totalVideos = 0;
        public int downloadableVideos = 0;
        public int drmVideos = 0;
        public int blobVideos = 0;
        public int mp4Videos = 0;
        public int hlsVideos = 0;
        public int dashVideos = 0;

        @Override
        public String toString() {
            return String.format("Total: %d, Downloadable: %d, DRM: %d, Blob: %d, MP4: %d, HLS: %d, DASH: %d",
                    totalVideos, downloadableVideos, drmVideos, blobVideos, mp4Videos, hlsVideos, dashVideos);
        }
    }
}