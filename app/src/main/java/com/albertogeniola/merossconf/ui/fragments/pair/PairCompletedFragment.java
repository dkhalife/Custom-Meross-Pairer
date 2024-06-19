package com.albertogeniola.merossconf.ui.fragments.pair;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.albertogeniola.merossconf.R;

import java.util.Timer;
import java.util.TimerTask;

import cdflynn.android.library.checkview.CheckView;


public class PairCompletedFragment extends Fragment {
    private CheckView mCheckView;
    private final Timer mTimer;
    private final Handler mHandler;
    private boolean mVerified;

    public PairCompletedFragment() {
        mHandler = new Handler();
        mTimer = new Timer();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVerified = getArguments().getBoolean("verified", false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pair_done, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        TextView mTextView = view.findViewById(R.id.pairing_completed_text_view);
        ImageView mQuestionMarkView = view.findViewById(R.id.questionMarkIcon);
        Button pairDoneButton = view.findViewById(R.id.pairDoneButton);
        pairDoneButton.setOnClickListener(v -> {
            NavController ctrl = NavHostFragment.findNavController(PairCompletedFragment.this);
            ctrl.popBackStack(R.id.ScanDeviceFragment, false);
            //ctrl.navigate(R.id.PairDone);
        });

        mCheckView = view.findViewById(R.id.pairDoneCheckView);
        mCheckView.setVisibility(mVerified ? View.VISIBLE:View.GONE);
        mQuestionMarkView.setVisibility(!mVerified ? View.VISIBLE:View.GONE);
        mTextView.setText(mVerified ? R.string.pairing_done_verified : R.string.pairing_done_unverified);

        if (mVerified) {
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    mHandler.post(() -> mCheckView.check());
                }
            }, 1000);
        }
    }
}
