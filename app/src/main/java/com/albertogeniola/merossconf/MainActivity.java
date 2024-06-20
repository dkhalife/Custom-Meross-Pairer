package com.albertogeniola.merossconf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.albertogeniola.merossconf.ui.MainActivityViewModel;
import com.albertogeniola.merosslib.model.http.ApiCredentials;
import com.google.android.material.navigation.NavigationView;

import static android.location.LocationManager.PROVIDERS_CHANGED_ACTION;
import static android.net.wifi.WifiManager.NETWORK_STATE_CHANGED_ACTION;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private BroadcastReceiver mReceiver;
    private TextView wifiTextView;
    private TextView wifiMerossWarning;
    private TextView locationTextView;
    private LinearLayout wifiLocationStatusLayout;
    private boolean mActiveFragmentsRequireWifiLocationWarn = false;
    private MenuItem mPairMenuItem, mDeviceMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final NavigationView navigationView = findViewById(R.id.nav_view);
        mPairMenuItem = navigationView.getMenu().findItem(R.id.pair_activity);
        mDeviceMenuItem = navigationView.getMenu().findItem(R.id.devices_fragment);
        TextView versionInfo = navigationView.getHeaderView(0).findViewById(R.id.appVersionTextView);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(this.getPackageName(), 0);
            String version = pInfo.versionName;
            versionInfo.setText("v"+version);
        } catch (PackageManager.NameNotFoundException e) {
            versionInfo.setText("");
        }

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        final NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                navController.getGraph())
                .setDrawerLayout(drawer)
                .build();
        final TextView loggedUserTextView = navigationView.getHeaderView(0).findViewById(R.id.navigation_header_logged_email_textview);
        final TextView httpEndpointTextView = navigationView.getHeaderView(0).findViewById(R.id.navigation_header_http_endpoint_textview);

        final MainActivityViewModel mainActivityViewModel = new ViewModelProvider(this).get(MainActivityViewModel.class);
        mainActivityViewModel.getCredentials().observe(this, apiCredentials -> {
            if (apiCredentials==null) {
                loggedUserTextView.setText(R.string.user_not_logged);
                httpEndpointTextView.setText(R.string.api_server_not_specified);
            } else if (apiCredentials.isManuallySet()) {
                loggedUserTextView.setText("UserId: " + apiCredentials.getUserId() + " (manual)");
                httpEndpointTextView.setText(R.string.api_server_not_specified);
            } else {
                loggedUserTextView.setText(apiCredentials.getUserEmail());
                httpEndpointTextView.setText(apiCredentials.getApiServer());
            }
        });

        wifiTextView = findViewById(R.id.wifiOffTextView);
        wifiMerossWarning = findViewById(R.id.wifiMerossWarning);
        locationTextView = findViewById(R.id.locationOffTextView);
        wifiLocationStatusLayout = findViewById(R.id.wifiLocationStatusLayout);
        this.mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction().equals(NETWORK_STATE_CHANGED_ACTION) || intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    String connectedSsid = AndroidUtils.getConnectedWifi(MainActivity.this);
                    updateWifiStatus(connectedSsid!=null, connectedSsid);
                } else if (intent.getAction().equals(PROVIDERS_CHANGED_ACTION)) {
                    updateLocationStatus(AndroidUtils.isLocationEnabled(MainActivity.this));
                }
            }
        };

        // Load the data into the view
        mainActivityViewModel.setCredentials(AndroidPreferencesManager.loadHttpCredentials(this));
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
        navigationView.setNavigationItemSelectedListener(menuItem -> {
            NavigationUI.onNavDestinationSelected(menuItem, navController);
            return true;
        });
    }

    private void updateWifiStatus(boolean wifiEnabled, @Nullable String connctedSsid) {
        wifiTextView.setVisibility(wifiEnabled ? View.GONE : View.VISIBLE);
        wifiMerossWarning.setVisibility(MerossUtils.isMerossAp(connctedSsid) ? View.VISIBLE : View.GONE);
        updateStatusBarVisibility();
    }

    private void updateLocationStatus(boolean locationEnabled) {
        locationTextView.setVisibility(locationEnabled ? View.GONE : View.VISIBLE);
        updateStatusBarVisibility();
    }

    public void setWifiLocationWarnRequired(boolean enabled) {
        mActiveFragmentsRequireWifiLocationWarn = enabled;
        updateStatusBarVisibility();
    }

    public void updateStatusBarVisibility() {
        if (mActiveFragmentsRequireWifiLocationWarn) {
            wifiLocationStatusLayout.setVisibility(
                    wifiTextView.getVisibility() == View.VISIBLE ||
                            locationTextView.getVisibility() == View.VISIBLE ||
                            wifiMerossWarning.getVisibility() == View.VISIBLE ? View.VISIBLE : View.GONE);
        } else {
            wifiLocationStatusLayout.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(PROVIDERS_CHANGED_ACTION);
        this.registerReceiver(this.mReceiver, filter);

        // Update wifi/location status
        String connectedWifi = AndroidUtils.getConnectedWifi(MainActivity.this);
        boolean locationEnabled = AndroidUtils.isLocationEnabled(MainActivity.this);
        updateWifiStatus(connectedWifi!=null, connectedWifi);
        updateLocationStatus(locationEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.unregisterReceiver(this.mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // At the moment, we have no menu to inflate.
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        ApiCredentials creds = AndroidPreferencesManager.loadHttpCredentials(MainActivity.this);

        if (creds == null) {
            mPairMenuItem.setEnabled(false);
            mPairMenuItem.setTitle("Pair (Login required)");
            mDeviceMenuItem.setEnabled(false);
            mDeviceMenuItem.setTitle("Devices (Login required)");
        } else {
            mPairMenuItem.setEnabled(true);
            mPairMenuItem.setTitle("Pair");
            mDeviceMenuItem.setEnabled(true);
            mDeviceMenuItem.setTitle("Devices");
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.onNavDestinationSelected(item, navController)
                || super.onOptionsItemSelected(item);
    }


    private static final int TIME_INTERVAL = 2000; // # milliseconds, desired time passed between two back presses.
    private long mBackPressed;
    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        int backFrags = fragmentManager.findFragmentById(R.id.nav_host_fragment).getChildFragmentManager().getBackStackEntryCount();

        // If there are more fragments, popback (default behaviour)
        if (backFrags>0)
            super.onBackPressed();

        // otherwise ask for confirmation to exit
        else if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), "Tap back button in order to exit", Toast.LENGTH_SHORT).show();
        }

        mBackPressed = System.currentTimeMillis();
    }

}
