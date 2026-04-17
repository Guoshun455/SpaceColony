package com.example.spacecolony.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.spacecolony.R;
import com.example.spacecolony.model.Storage;

/**
 * Home screen fragment displaying colony overview and navigation options.
 * Shows crew counts by location and provides access to all game areas.
 */
public class HomeFragment extends Fragment {

    private TextView quartersCountText;
    private TextView simulatorCountText;
    private TextView missionCountText;
    private TextView medbayCountText;
    private TextView totalStatsText;
    private Button recruitButton;
    private Button loadButton;
    private Button saveButton;
    private CardView cardQuarters;
    private CardView cardSimulator;
    private CardView cardMission;
    private CardView cardMedbay;

    private OnHomeActionListener listener;

    /**
     * Interface for handling navigation and action events from home screen
     */
    public interface OnHomeActionListener {
        void onRecruitClick();
        void onNavigateToQuarters();
        void onNavigateToSimulator();
        void onNavigateToMissionControl();
        void onNavigateToMedbay();
        void onSaveGame();
        void onLoadGame();
    }

    public void setOnHomeActionListener(OnHomeActionListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            quartersCountText = view.findViewById(R.id.quarters_count);
            simulatorCountText = view.findViewById(R.id.simulator_count);
            missionCountText = view.findViewById(R.id.mission_count);
            medbayCountText = view.findViewById(R.id.medbay_count);
            totalStatsText = view.findViewById(R.id.total_stats);
            recruitButton = view.findViewById(R.id.btn_recruit);
            loadButton = view.findViewById(R.id.btn_load);
            saveButton = view.findViewById(R.id.btn_save);

            cardQuarters = view.findViewById(R.id.card_quarters);
            cardSimulator = view.findViewById(R.id.card_simulator);
            cardMission = view.findViewById(R.id.card_mission);
            cardMedbay = view.findViewById(R.id.card_medbay);

            if (cardQuarters != null) {
                cardQuarters.setOnClickListener(v -> {
                    if (listener != null) listener.onNavigateToQuarters();
                });
            }

            if (cardSimulator != null) {
                cardSimulator.setOnClickListener(v -> {
                    if (listener != null) listener.onNavigateToSimulator();
                });
            }

            if (cardMission != null) {
                cardMission.setOnClickListener(v -> {
                    if (listener != null) listener.onNavigateToMissionControl();
                });
            }

            if (cardMedbay != null) {
                cardMedbay.setOnClickListener(v -> {
                    if (listener != null) listener.onNavigateToMedbay();
                });
            }

            if (recruitButton != null) {
                recruitButton.setOnClickListener(v -> {
                    if (listener != null) listener.onRecruitClick();
                });
            }

            if (saveButton != null) {
                saveButton.setOnClickListener(v -> {
                    if (listener != null) listener.onSaveGame();
                });
            }

            if (loadButton != null) {
                loadButton.setOnClickListener(v -> {
                    if (listener != null) listener.onLoadGame();
                });
            }

            updateStats();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error initializing view: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Update all statistics displayed on the home screen
     */
    public void updateStats() {
        try {
            Storage storage = Storage.getInstance();

            int quarters = storage.getCrewByLocation("Quarters").size();
            int simulator = storage.getCrewByLocation("Simulator").size();
            int mission = storage.getCrewByLocation("Mission").size();
            int medbay = storage.getCrewByLocation("Medbay").size();

            if (quartersCountText != null) quartersCountText.setText(String.valueOf(quarters));
            if (simulatorCountText != null) simulatorCountText.setText(String.valueOf(simulator));
            if (missionCountText != null) missionCountText.setText(String.valueOf(mission));
            if (medbayCountText != null) medbayCountText.setText(String.valueOf(medbay));

            Storage.ColonyStats stats = storage.getColonyStats();
            String statsText = String.format(
                    "Total Crew: %d | Missions: %d | Victories: %d | Training: %d",
                    stats.totalCrew, stats.totalMissions, stats.totalVictories, stats.totalTrainingSessions
            );
            if (totalStatsText != null) totalStatsText.setText(statsText);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStats();
    }
}
