package com.albertogeniola.merossconf.ui.fragments.pair;

import static androidx.core.content.ContextCompat.registerReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier.Builder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PatternMatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.albertogeniola.merossconf.ui.views.TaskLine;
import com.albertogeniola.merosslib.MerossDeviceAp;
import com.albertogeniola.merosslib.model.protocol.MessageGetConfigWifiListResponse;
import com.albertogeniola.merosslib.model.protocol.MessageGetSystemAllResponse;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class FetchDeviceInfoFragment extends Fragment {

    private TaskLine wifiConnectTask;
    private TaskLine fetchDeviceInfoTask;
    private TaskLine scanWifiTask;
    private Handler uiThreadHandler;
    private final ScheduledExecutorService worker;

    private State state = State.INIT;
    private TaskLine currentTask = null;
    private String error = null;
    private MerossDeviceAp device;
    private MessageGetSystemAllResponse deviceInfo;
    private MessageGetConfigWifiListResponse deviceAvailableWifis;

    private PairActivityViewModel pairActivityViewModel;
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private ConnectivityManager mConnectivityManager;

    public FetchDeviceInfoFragment() {
        worker = Executors.newSingleThreadScheduledExecutor();
    }

    // Logic methods
    private void stateMachine(Signal signal) {
        switch(state) {
            case INIT:
                if (signal == Signal.RESUMED) {
                    state = State.CONNECTING_AP;
                    updateUi();
                    connectAp();
                }
                break;
            case CONNECTING_AP:
                if (signal == Signal.AP_CONNECTED) {
                    state = State.GATHERING_INFORMATION;
                    updateUi();
                    collectDeviceInfo();
                }
                break;
            case GATHERING_INFORMATION:
                if (signal == Signal.INFO_GATHERED) {
                    state = State.SCANNING_WIFI;
                    updateUi();
                    startDeviceWifiScan();
                }
                break;
            case SCANNING_WIFI:
                if (signal == Signal.WIFI_SCAN_COMPLETED) {
                    state = State.DONE;
                    updateUi();
                    completeActivityFragment();
                }
                break;
        }

        if (signal == Signal.ERROR) {
            state = State.ERROR;
            updateUi();
        }
    }

    private void connectAp() {
        com.albertogeniola.merossconf.model.MerossDeviceAp data = pairActivityViewModel.getMerossPairingAp().getValue();
        if (data == null) {
            stateMachine(Signal.ERROR);
            return;
        }

        String ssid = data.getSsid();
        final NetworkSpecifier specifier =
                new Builder()
                        .setSsidPattern(new PatternMatcher(ssid, PatternMatcher.PATTERN_PREFIX))
                        .build();
        final NetworkRequest request =
                new NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .setNetworkSpecifier(specifier)
                        .build();

        if (mNetworkCallback != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        }
        EnsureNetworkCallback(mConnectivityManager);
        mConnectivityManager.requestNetwork(request, mNetworkCallback);
    }

    private void EnsureNetworkCallback(ConnectivityManager connectivityManager) {
        if (mNetworkCallback != null) {
            return;
        }

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                mainHandler.post(() -> {
                    connectivityManager.bindProcessToNetwork(network);
                    device = new MerossDeviceAp();
                    stateMachine(Signal.AP_CONNECTED);
                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                mainHandler.post(() -> {
                    stateMachine(Signal.ERROR);
                });
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                mainHandler.post(() -> {
                    stateMachine(Signal.ERROR);
                });
            }
        };
    }

    private void startDeviceWifiScan() {
        try {
            deviceAvailableWifis = device.scanWifi();
            stateMachine(Signal.WIFI_SCAN_COMPLETED);
        } catch (IOException e) {
            uiThreadHandler.post(() -> {
                error = "Error occurred while performing device wifi scanning";
                stateMachine(Signal.ERROR);
            });
        }
    }

    private void collectDeviceInfo() {
        mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
        worker.schedule(() -> {
            try {
                deviceInfo = device.getConfig();
                stateMachine(Signal.INFO_GATHERED);
            } catch (IOException e) {
                uiThreadHandler.post(() -> {
                    error = "Error occurred while gathering device info";
                    stateMachine(Signal.ERROR);
                });
            }
        }, 2, TimeUnit.SECONDS);
    }

    private void completeActivityFragment() {
        uiThreadHandler.post(() -> {
            // Set done and proceed with the next fragment
            pairActivityViewModel.setApDevice(device);
            pairActivityViewModel.setDeviceInfo(deviceInfo);
            pairActivityViewModel.setDeviceAvailableWifis(deviceAvailableWifis);
            NavController ctrl = NavHostFragment.findNavController(FetchDeviceInfoFragment.this);
            ctrl.navigate(R.id.action_fetchDeviceInfo_to_showDeviceInfo,null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
        });
    }

    // UI
    private void updateUi() {
        Runnable uiUpdater = () -> {
            switch (state) {
                case INIT:
                    wifiConnectTask.setState(TaskLine.TaskState.not_started);
                    fetchDeviceInfoTask.setState(TaskLine.TaskState.not_started);
                    scanWifiTask.setState(TaskLine.TaskState.not_started);
                    break;
                case CONNECTING_AP:
                    wifiConnectTask.setState(TaskLine.TaskState.running);
                    currentTask = wifiConnectTask;
                    break;
                case GATHERING_INFORMATION:
                    wifiConnectTask.setState(TaskLine.TaskState.completed);
                    fetchDeviceInfoTask.setState(TaskLine.TaskState.running);
                    currentTask = fetchDeviceInfoTask;
                    break;
                case SCANNING_WIFI:
                    fetchDeviceInfoTask.setState(TaskLine.TaskState.completed);
                    scanWifiTask.setState(TaskLine.TaskState.running);
                    currentTask = scanWifiTask;
                    break;
                case DONE:
                    scanWifiTask.setState(TaskLine.TaskState.completed);
                    break;
                case ERROR:
                    Snackbar.make(requireView(), error, Snackbar.LENGTH_LONG).show();
                    if (currentTask != null) {
                        currentTask.setState(TaskLine.TaskState.failed);
                    }
                    break;
            }
        };

        if (Looper.getMainLooper().getThread().getId() != Thread.currentThread().getId()) {
            uiThreadHandler.post(uiUpdater);
        } else {
            uiUpdater.run();
        }
    }

    // Android activity lifecycle
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
        uiThreadHandler = new Handler(Looper.getMainLooper());
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mConnectivityManager = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_connect, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        wifiConnectTask = view.findViewById(R.id.connectWifiTask);
        fetchDeviceInfoTask = view.findViewById(R.id.fetchDeviceInfoTask);
        scanWifiTask = view.findViewById(R.id.scanWifiTask);

        // As soon as we resume, connect to the given WiFi
        stateMachine(Signal.RESUMED);
    }

    enum State {
        INIT,
        CONNECTING_AP,
        GATHERING_INFORMATION,
        SCANNING_WIFI,
        DONE,
        ERROR
    }

    enum Signal {
        RESUMED,
        AP_CONNECTED,
        INFO_GATHERED,
        WIFI_SCAN_COMPLETED,
        ERROR
    }
}
