package com.naxobrowser;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;
import java.util.Map;

public class OkHttpDownloader {

    // Yeh naya method add karein
    public static void downloadWithHeaders(Context context, String url, String fileName, Map<String, String> headers) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

            // Sabhi headers ko request mein add karein
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    request.addRequestHeader(entry.getKey(), entry.getValue());
                }
            }

            request.setTitle(fileName);
            request.setDescription("Downloading...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            dm.enqueue(request);

        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Purana method (agar aap use kar rahe hain)
    public static void download(Context context, String url, String fileName, String userAgent, String referer) {
        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("User-Agent", userAgent);
        headers.put("Referer", referer);
        downloadWithHeaders(context, url, fileName, headers);
    }
}
