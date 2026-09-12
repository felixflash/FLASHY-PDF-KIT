package com.flashypdfkit;

import android.content.Intent;
import android.Manifest;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.content.pm.PackageManager;
import android.provider.OpenableColumns;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.view.WindowCompat;
import com.getcapacitor.BridgeActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import androidx.activity.OnBackPressedCallback;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {
    private static final int LEGACY_STORAGE_REQUEST = 1001;
    private static final int NOTIFICATION_REQUEST = 1002;
    public static JSONObject pendingPdf = null;
    public static boolean webReady = false;
    public static boolean isIntentLaunch = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("FlashyPDF", "onCreate: Starting");
        WindowCompat.enableEdgeToEdge(getWindow());
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        Log.d("FlashyPDF", "onCreate: SplashScreen installed");
        
        deleteOldCacheFiles();

        // Capacitor builds its bridge during super.onCreate(), so custom plugins
        // must be registered first to be available to the WebView.
        registerPlugin(SaveToDownloadsPlugin.class);
        registerPlugin(SystemUIPlugin.class);
        
        Log.d("FlashyPDF", "onCreate: calling super.onCreate");
        super.onCreate(savedInstanceState);
        Log.d("FlashyPDF", "onCreate: super.onCreate finished");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                String js = "window.dispatchEvent(new CustomEvent('nativeBack'));";
                if (getBridge() != null && getBridge().getWebView() != null) {
                    getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(js, null));
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                    setEnabled(true);
                }
            }
        });

        splashScreen.setOnExitAnimationListener(splashScreenView -> {
            Log.d("FlashyPDF", "SplashScreen: Exit animation listener triggered");
            View iconView = splashScreenView.getIconView();

            ObjectAnimator alpha = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 2f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 2f);

            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.setInterpolator(new AccelerateInterpolator());
            animatorSet.setDuration(isIntentLaunch ? 150L : 500L);
            animatorSet.playTogether(alpha, scaleX, scaleY);

            animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(android.animation.Animator animation) {
                    Log.d("FlashyPDF", "SplashScreen: Animation finished, removing view");
                    splashScreenView.remove();
                }
            });

            animatorSet.start();
        });

        requestLegacyStoragePermission();
        requestNotificationPermission();
        handlePdfIntent(getIntent());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().getDecorView().setImportantForContentCapture(View.IMPORTANT_FOR_CONTENT_CAPTURE_NO);
        }
        Log.d("FlashyPDF", "onCreate: Finished");
    }

    @Override
    public void onStart() {
        Log.d("FlashyPDF", "onStart");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.d("FlashyPDF", "onResume");
        super.onResume();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_REQUEST
                );
            }
        }
    }

    private void requestLegacyStoragePermission() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    LEGACY_STORAGE_REQUEST
            );
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePdfIntent(intent);
    }

    private void handlePdfIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            Log.d("FlashyPDF", "handlePdfIntent: No data in intent");
            return;
        }
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            isIntentLaunch = true;
            Uri uri = intent.getData();
            Log.d("FlashyPDF", "handlePdfIntent: Processing URI: " + uri);
            processPdfUri(uri);
        } else {
            Log.d("FlashyPDF", "handlePdfIntent: Unhandled action: " + intent.getAction());
        }
    }

    private void processPdfUri(Uri uri) {
        new Thread(() -> {
            try {
                String name = getFileName(uri);
                // Use unique filename to avoid browser caching issues
                String fileName = "intent_" + System.currentTimeMillis() + ".pdf";
                File cacheFile = new File(getCacheDir(), fileName);
                try (InputStream in = getContentResolver().openInputStream(uri);
                     FileOutputStream out = new FileOutputStream(cacheFile)) {
                    if (in == null) return;
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }

                JSONObject pdfInfo = new JSONObject();
                pdfInfo.put("name", name);
                pdfInfo.put("path", cacheFile.getAbsolutePath());

                runOnUiThread(() -> {
                    if (webReady && getBridge() != null && getBridge().getWebView() != null) {
                        String safeName = JSONObject.quote(name);
                        String path = cacheFile.getAbsolutePath();
                        String js = "window.dispatchEvent(new CustomEvent('pdfOpen', {detail:{name:" + safeName + ", path:'" + path + "'}}));";
                        getBridge().getWebView().post(() -> getBridge().getWebView().evaluateJavascript(js, null));
                    } else {
                        pendingPdf = pdfInfo;
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void deleteOldCacheFiles() {
        try {
            File[] files = getCacheDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("intent_") && file.getName().endsWith(".pdf")) {
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFileName(Uri uri) {
        String name = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) {
                        name = cursor.getString(index);
                    }
                }
            }
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        if (name == null || name.isEmpty()) {
            name = "opened.pdf";
        }
        return name;
    }
}
