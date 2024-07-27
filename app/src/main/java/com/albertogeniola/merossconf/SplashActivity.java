package com.albertogeniola.merossconf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import com.albertogeniola.merossconf.model.AndroidNetworkProxy;
import com.albertogeniola.merossconf.model.HttpClientManager;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.button.MaterialButton;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends Activity {
    private void gotoMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        if (!AndroidPreferencesManager.didAcceptTerms(getApplicationContext())) {
            gotoMainActivity();
            return;
        }

        setContentView(R.layout.activity_splash);

        MaterialButton agreeButton = findViewById(R.id.acceptButton);
        agreeButton.setOnClickListener(view -> {
            AndroidPreferencesManager.setAcceptedTerms(getApplicationContext());
            gotoMainActivity();
        });
        MaterialButton quitButton = findViewById(R.id.quitButton);
        quitButton.setOnClickListener(view -> finish());

        hideSystemUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        init();
    }

    private void init() {
        // Configure logging
        if (BuildConfig.DEBUG) {
            ConsoleHandler handler = new ConsoleHandler();
            handler.setLevel(Level.ALL);
            Logger logger = Logger.getLogger("com.albertogeniola.merosslib");
            logger.setLevel(Level.ALL);
            logger.addHandler(handler);
        }

        // Load credentials
        HttpClientManager instance = HttpClientManager.getInstance();
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(this);
        if (creds != null) {
            instance.loadFromCredentials(creds, new AndroidNetworkProxy(null));
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        android.app.ActionBar bar = getActionBar();
        if (bar!=null) {
            bar.setDisplayHomeAsUpEnabled(false);
        }

        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}
