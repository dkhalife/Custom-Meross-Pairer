package com.albertogeniola.merossconf.ui.fragments.pair;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiSsid;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.albertogeniola.merossconf.AndroidUtils;
import com.albertogeniola.merossconf.MerossUtils;
import com.albertogeniola.merossconf.R;
import com.albertogeniola.merossconf.model.MerossDeviceAp;
import com.albertogeniola.merossconf.ui.PairActivityViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ScanDevicesFragment extends Fragment {
    private PairActivityViewModel pairActivityViewModel;

    private WifiManager wifiManager = null;
    private ProgressBar scanningProgressBar;
    private FloatingActionButton fab;
    private final MerossWifiScanAdapter adapter = new MerossWifiScanAdapter();
    private SwipeRefreshLayout swipeContainer;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.wifiManager = (WifiManager) requireContext().getSystemService(Context.WIFI_SERVICE);
        pairActivityViewModel = new ViewModelProvider(requireActivity()).get(PairActivityViewModel.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        swipeContainer = view.findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(() -> {
            swipeContainer.setRefreshing(false);
            startScan();
        });

        scanningProgressBar = view.findViewById(R.id.scanningProgressSpinner);
        final RecyclerView recyclerView = view.findViewById(R.id.wifiList);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);
        recyclerView.setAdapter(adapter);

        fab = view.findViewById(R.id.scan_button);
        fab.setOnClickListener(v -> startScan());

        final TextView enableWifiAndLocationTextView = view.findViewById(R.id.enableWifiAndLocationTextView);
        pairActivityViewModel.getWifiLocationStatus().observe(requireActivity(), wifiLocationStatus -> {
            boolean wifiOk = wifiLocationStatus.getWifiEnabledOrEnabling() != null && wifiLocationStatus.getWifiEnabledOrEnabling();
            boolean locationOk = wifiLocationStatus.getLocationEnabledOrEnabling() != null && wifiLocationStatus.getLocationEnabledOrEnabling();
            enableWifiAndLocationTextView.setVisibility(wifiOk && locationOk ? View.GONE : View.VISIBLE);
            recyclerView.setVisibility(wifiOk && locationOk ? View.VISIBLE : View.GONE);
            fab.setEnabled(wifiOk && locationOk);
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        startScan();
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startScan();
            } else {
                AlertDialog.Builder permissionAlert = new AlertDialog.Builder(requireContext());
                permissionAlert.setTitle("Permission required");
                permissionAlert.setMessage("Wifi scanning requires location and nearby wifi permission to manipulate wifi adapter.");
                permissionAlert.show();
            }
        });

    private void startScan() {
        if (scanningProgressBar.getVisibility() == View.VISIBLE) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CHANGE_WIFI_STATE);
            return;
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }

        fab.hide();
        scanningProgressBar.setVisibility(View.VISIBLE);

        wifiManager.registerScanResultsCallback(requireContext().getMainExecutor(), new WifiManager.ScanResultsCallback() {
            @Override
            public void onScanResultsAvailable() {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            updateScanData(wifiManager.getScanResults());
            scanningProgressBar.setVisibility(View.GONE);
            fab.show();
            }
        });
        //noinspection deprecation
        wifiManager.startScan();
    }

    private void updateScanData(List<ScanResult> wifiNetworks) {
        // Only filter access points that match the Meross
        ArrayList<ScanResult> data = new ArrayList<>();
        for (ScanResult r : wifiNetworks) {
            WifiSsid ssid = r.getWifiSsid();
            if (MerossUtils.isMerossAp(AndroidUtils.getWifiNameFromSsid(ssid))) {
                data.add(r);
            }
        }

        adapter.updateData(data);
    }

    class MerossWifiScanAdapter extends RecyclerView.Adapter<MerossWifiScanAdapter.MyViewHolder>{
        private final ArrayList<ScanResult> scanResult;

        MerossWifiScanAdapter(){
            this.scanResult = new ArrayList<>();
        }

        @SuppressLint("NotifyDataSetChanged")
        public void updateData(List<ScanResult> data) {
            this.scanResult.clear();
            this.scanResult.addAll(data);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.meross_wifi_scan_result, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyViewHolder holder, int position) {
            final ScanResult sr = this.scanResult.get(position);
            WifiSsid ssid = sr.getWifiSsid();
            holder.updateScanResult(sr);
            holder.itemView.setOnClickListener(v -> {
                MerossDeviceAp targetAp = new MerossDeviceAp(AndroidUtils.getWifiNameFromSsid(ssid),sr.BSSID);
                pairActivityViewModel.setMerossPairingAp(targetAp);
                NavHostFragment
                        .findNavController(ScanDevicesFragment.this)
                        .navigate(R.id.action_scan_to_fetchDeviceInfo,null, new NavOptions.Builder().setEnterAnim(android.R.animator.fade_in).setExitAnim(android.R.animator.fade_out).build());
            });
        }

        @Override
        public int getItemCount() {
            return scanResult.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder{
            private final TextView wifiName;
            private final TextView bssidName;
            private final ImageView signalStrength;

            MyViewHolder(View itemView) {
                super(itemView);
                wifiName = itemView.findViewById(R.id.ssid);
                bssidName = itemView.findViewById(R.id.bssid);
                signalStrength = itemView.findViewById(R.id.wifiSignalStrength);
            }

            public void updateScanResult(ScanResult sr) {
                WifiSsid ssid = sr.getWifiSsid();
                this.wifiName.setText(AndroidUtils.getWifiNameFromSsid(ssid));
                this.bssidName.setText(sr.BSSID);
                int signalLevel = sr.level;
                if (signalLevel>=-40)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_4_bar_black_24dp);
                else if (signalLevel>=-50)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_3_bar_black_24dp);
                else if (signalLevel>=-60)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_2_bar_black_24dp);
                else if (signalLevel>=-70)
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_1_bar_black_24dp);
                else
                    this.signalStrength.setImageResource(R.drawable.ic_signal_wifi_0_bar_black_24dp);
            }
        }
    }
}
