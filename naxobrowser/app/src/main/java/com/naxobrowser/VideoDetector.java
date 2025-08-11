package com.naxobrowser;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoDetector {
    private static final String TAG = "VideoDetector";
    private Context context;
    private WebView webView;
    private VideoDetectionListener listener;
    private Handler mainHandler;

    public interface VideoDetectionListener {
        void onVideoDetected(VideoInfo videoInfo);
        void onDrmDetected(String drmType, String licenseUrl);
    }

    public static class VideoInfo {
        public String url;
        public String format;
        public String quality;
        public String title;
        public boolean isDrm;
        public String drmType;
        public String pageUrl;
        public List<QualityOption> qualities = new ArrayList<>();
        public long timestamp = System.currentTimeMillis();
    }

    public static class QualityOption {
        public String resolution, bandwidth, url;
        public QualityOption(String res, String band, String u) {
            this.resolution = res; this.bandwidth = band; this.url = u;
        }
    }

    public VideoDetector(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void setVideoDetectionListener(VideoDetectionListener listener) {
        this.listener = listener;
    }

    public void setupVideoDetection() {
        webView.addJavascriptInterface(new VideoJSInterface(), "VideoDetectorAndroid");
        String script = getAdvancedVideoDetectionScript();
        webView.evaluateJavascript(script, null);
        
        // Additional detection after some delay
        mainHandler.postDelayed(() -> {
            webView.evaluateJavascript(getDOMVideoDetectionScript(), null);
            webView.evaluateJavascript(getNetworkMonitorScript(), null);
        }, 2000);
    }

    private String getAdvancedVideoDetectionScript() {
        return "javascript:(function() {" +
                "if (window.videoDetectorAttached) return;" +
                "window.videoDetectorAttached = true;" +
                "console.log('🎥 Advanced Video Detector Attached');" +

                // Monitor XMLHttpRequest
                "const originalXhrOpen = XMLHttpRequest.prototype.open;" +
                "const originalXhrSend = XMLHttpRequest.prototype.send;" +
                "XMLHttpRequest.prototype.open = function(method, url) {" +
                "  this._url = url;" +
                "  this._method = method;" +
                "  return originalXhrOpen.apply(this, arguments);" +
                "};" +
                "XMLHttpRequest.prototype.send = function(data) {" +
                "  const self = this;" +
                "  this.addEventListener('readystatechange', function() {" +
                "    if (self.readyState === 4 && self.status === 200) {" +
                "      if (isPotentialVideo(self._url)) {" +
                "        console.log('📡 XHR Video URL:', self._url);" +
                "        VideoDetectorAndroid.onVideoUrlDetected(self._url, 'XHR', self.responseURL || self._url);" +
                "      }" +
                "    }" +
                "  });" +
                "  return originalXhrSend.apply(this, arguments);" +
                "};" +

                // Monitor Fetch API
                "const originalFetch = window.fetch;" +
                "window.fetch = function(input, init) {" +
                "  const url = (typeof input === 'string') ? input : input.url;" +
                "  const promise = originalFetch.apply(this, arguments);" +
                "  if (isPotentialVideo(url)) {" +
                "    console.log('🌐 Fetch Video URL:', url);" +
                "    VideoDetectorAndroid.onVideoUrlDetected(url, 'FETCH', url);" +
                "  }" +
                "  return promise;" +
                "};" +

                // Monitor media source extensions
                "if (window.MediaSource) {" +
                "  const originalAddSourceBuffer = MediaSource.prototype.addSourceBuffer;" +
                "  MediaSource.prototype.addSourceBuffer = function(mimeType) {" +
                "    console.log('📺 MediaSource buffer added:', mimeType);" +
                "    VideoDetectorAndroid.onMediaSourceDetected(mimeType, window.location.href);" +
                "    return originalAddSourceBuffer.apply(this, arguments);" +
                "  };" +
                "}" +

                // Enhanced video URL detection
                "function isPotentialVideo(url) {" +
                "  if (!url || typeof url !== 'string') return false;" +
                "  if (url.startsWith('blob:') && url.length > 20) return true;" +
                "  const videoPatterns = [" +
                "    /\\.mp4/i, /\\.m3u8/i, /\\.mpd/i, /\\.webm/i, /\\.mkv/i, /\\.avi/i," +
                "    /manifest/i, /playlist/i, /googlevideo\\.com/i, /fbcdn\\.net.*\\.mp4/i," +
                "    /video.*\\.facebook\\.com/i, /scontent.*\\.mp4/i, /instagram.*\\.mp4/i," +
                "    /tiktok.*\\.mp4/i, /youtube.*videoplayback/i, /ytimg\\.com.*\\.mp4/i," +
                "    /dailymotion.*\\.mp4/i, /vimeo.*\\.mp4/i, /twitch\\.tv.*\\.m3u8/i" +
                "  ];" +
                "  return videoPatterns.some(pattern => pattern.test(url));" +
                "}" +

                "console.log('✅ Video detection script loaded successfully');" +
                "})();";
    }

    private String getDOMVideoDetectionScript() {
        return "javascript:(function() {" +
                "console.log('🔍 Starting DOM video scan...');" +
                "const videoElements = document.querySelectorAll('video');" +
                "videoElements.forEach((video, index) => {" +
                "  if (video.src && video.src !== '') {" +
                "    console.log('📹 DOM Video found:', video.src);" +
                "    VideoDetectorAndroid.onVideoUrlDetected(video.src, 'DOM_VIDEO', video.src);" +
                "  }" +
                "  if (video.currentSrc && video.currentSrc !== '') {" +
                "    console.log('📹 DOM Video currentSrc:', video.currentSrc);" +
                "    VideoDetectorAndroid.onVideoUrlDetected(video.currentSrc, 'DOM_CURRENT', video.currentSrc);" +
                "  }" +
                "});" +

                // Check for source elements
                "const sourceElements = document.querySelectorAll('source');" +
                "sourceElements.forEach(source => {" +
                "  if (source.src && source.src !== '') {" +
                "    console.log('📼 Source element found:', source.src);" +
                "    VideoDetectorAndroid.onVideoUrlDetected(source.src, 'DOM_SOURCE', source.src);" +
                "  }" +
                "});" +

                // Facebook specific detection
                "const fbVideoSelectors = [" +
                "  '[data-video-id]'," +
                "  '[data-video-url]'," +
                "  '.scaledImageFitWidth'," +
                "  '[role=\"presentation\"] video'," +
                "  '.fbStoryAttachmentImage'," +
                "  '[data-sigil=\"inlineVideo\"]'" +
                "];" +
                "fbVideoSelectors.forEach(selector => {" +
                "  try {" +
                "    const elements = document.querySelectorAll(selector);" +
                "    elements.forEach(el => {" +
                "      const attrs = ['data-video-url', 'data-src', 'src', 'data-video-id'];" +
                "      attrs.forEach(attr => {" +
                "        const value = el.getAttribute(attr);" +
                "        if (value && isPotentialVideo(value)) {" +
                "          console.log('📱 Facebook video found:', value);" +
                "          VideoDetectorAndroid.onVideoUrlDetected(value, 'FB_DOM', value);" +
                "        }" +
                "      });" +
                "    });" +
                "  } catch(e) { console.log('FB selector error:', e); }" +
                "});" +

                "console.log('✅ DOM scan completed');" +
                "})();";
    }

    private String getNetworkMonitorScript() {
        return "javascript:(function() {" +
                "if (window.networkMonitorAttached) return;" +
                "window.networkMonitorAttached = true;" +
                
                // Monitor all network requests via Performance Observer
                "if ('PerformanceObserver' in window) {" +
                "  const observer = new PerformanceObserver((list) => {" +
                "    list.getEntries().forEach((entry) => {" +
                "      if (entry.name && isPotentialVideo(entry.name)) {" +
                "        console.log('⚡ Performance API Video:', entry.name);" +
                "        VideoDetectorAndroid.onVideoUrlDetected(entry.name, 'PERFORMANCE', entry.name);" +
                "      }" +
                "    });" +
                "  });" +
                "  observer.observe({entryTypes: ['resource']});" +
                "}" +

                // Enhanced video URL detection function
                "function isPotentialVideo(url) {" +
                "  if (!url || typeof url !== 'string') return false;" +
                "  if (url.startsWith('blob:') && url.length > 20) return true;" +
                "  const patterns = [" +
                "    'mp4', 'm3u8', 'mpd', 'webm', 'mkv', 'avi', 'mov', 'wmv', 'flv'," +
                "    'manifest', 'playlist', 'videoplayback', 'googlevideo.com'," +
                "    'fbcdn.net', 'facebook.com/video', 'scontent', 'instagram.com'," +
                "    'tiktokcdn.com', 'youtube.com', 'ytimg.com', 'dailymotion.com'," +
                "    'vimeo.com', 'twitch.tv', 'twitter.com/video'" +
                "  ];" +
                "  return patterns.some(p => url.toLowerCase().includes(p));" +
                "}" +

                "console.log('🌐 Network monitor attached');" +
                "})();";
    }

    public class VideoJSInterface {
        @JavascriptInterface
        public void onVideoUrlDetected(String url, String source, String finalUrl) {
            if (url == null || url.trim().isEmpty()) return;
            
            mainHandler.post(() -> {
                Log.d(TAG, "🎥 Video Detected (" + source + "): " + url);
                
                VideoInfo videoInfo = new VideoInfo();
                videoInfo.url = finalUrl != null ? finalUrl : url;
                videoInfo.format = detectFormat(videoInfo.url);
                videoInfo.title = generateTitle(source, videoInfo.format);
                videoInfo.pageUrl = webView.getUrl();
                
                analyzeVideoUrl(videoInfo.url, videoInfo);
                detectDRM(videoInfo);

                if (listener != null) {
                    listener.onVideoDetected(videoInfo);
                }
            });
        }

        @JavascriptInterface
        public void onMediaSourceDetected(String mimeType, String pageUrl) {
            mainHandler.post(() -> {
                Log.d(TAG, "📺 MediaSource detected: " + mimeType + " on " + pageUrl);
                
                VideoInfo videoInfo = new VideoInfo();
                videoInfo.url = "mediasource://" + mimeType + "@" + pageUrl;
                videoInfo.format = "MSE";
                videoInfo.title = "Media Source Extension";
                videoInfo.pageUrl = pageUrl;
                videoInfo.isDrm = mimeType.contains("encrypted");

                if (listener != null) {
                    listener.onVideoDetected(videoInfo);
                }
            });
        }
    }

    private String detectFormat(String url) {
        if (url == null) return "Unknown";
        
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.contains(".m3u8") || lowerUrl.contains("playlist")) return "HLS";
        if (lowerUrl.contains(".mpd") || lowerUrl.contains("manifest")) return "DASH";
        if (lowerUrl.contains(".mp4") || lowerUrl.contains("videoplayback")) return "MP4";
        if (lowerUrl.contains(".webm")) return "WebM";
        if (lowerUrl.contains(".mkv")) return "MKV";
        if (lowerUrl.contains("blob:")) return "Blob/MSE";
        if (lowerUrl.contains("mediasource://")) return "MSE";
        
        return "Unknown";
    }

    private String generateTitle(String source, String format) {
        String pageTitle = "Video";
        try {
            webView.evaluateJavascript("document.title", title -> {
                if (title != null && !title.equals("null")) {
                    // Clean up the title
                    String cleanTitle = title.replace("\"", "").trim();
                    if (cleanTitle.length() > 50) {
                        cleanTitle = cleanTitle.substring(0, 47) + "...";
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting page title", e);
        }
        
        return pageTitle + " (" + source + "/" + format + ")";
    }

    private void analyzeVideoUrl(String url, VideoInfo videoInfo) {
        if (url == null) return;
        
        // Extract quality information
        Pattern qualityPattern = Pattern.compile("(\\d{3,4}p|\\d+x\\d+|hd|sd|720|1080|480|360|240)");
        Matcher matcher = qualityPattern.matcher(url);
        if (matcher.find()) {
            videoInfo.quality = matcher.group(1);
        } else {
            videoInfo.quality = "Unknown";
        }
        
        // Extract additional quality options from URL parameters
        if (url.contains("itag=") || url.contains("quality=")) {
            Pattern itagPattern = Pattern.compile("itag=(\\d+)");
            Matcher itagMatcher = itagPattern.matcher(url);
            if (itagMatcher.find()) {
                String itag = itagMatcher.group(1);
                videoInfo.quality = mapItagToQuality(itag);
            }
        }
    }

    private String mapItagToQuality(String itag) {
        // YouTube itag mapping (simplified)
        switch (itag) {
            case "18": return "360p";
            case "22": return "720p";
            case "37": return "1080p";
            case "38": return "3072p";
            case "136": return "720p";
            case "137": return "1080p";
            default: return "Unknown (" + itag + ")";
        }
    }

    private void detectDRM(VideoInfo videoInfo) {
        if (videoInfo.url == null) return;
        
        String lowerUrl = videoInfo.url.toLowerCase();
        if (lowerUrl.contains("drm") || lowerUrl.contains("widevine") || 
            lowerUrl.contains("playready") || lowerUrl.contains("fairplay") ||
            lowerUrl.contains("encrypted")) {
            
            videoInfo.isDrm = true;
            if (lowerUrl.contains("widevine")) videoInfo.drmType = "Widevine";
            else if (lowerUrl.contains("playready")) videoInfo.drmType = "PlayReady";
            else if (lowerUrl.contains("fairplay")) videoInfo.drmType = "FairPlay";
            else videoInfo.drmType = "Unknown DRM";
            
            if (listener != null) {
                listener.onDrmDetected(videoInfo.drmType, videoInfo.url);
            }
        }
    }
}