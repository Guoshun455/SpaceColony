package com.example.spacecolony.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spacecolony.R;
import com.example.spacecolony.adapter.CrewAdapter;
import com.example.spacecolony.model.CrewMember;
import com.example.spacecolony.model.Storage;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Stats fragment displaying colony statistics and mission results.
 * Shows overall statistics and a pie chart of mission wins/losses.
 */
public class StatsFragment extends Fragment {

    private TextView colonyStatsText;
    private RecyclerView recyclerView;
    private CrewAdapter adapter;
    private PieChart pieChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        colonyStatsText = view.findViewById(R.id.colony_stats);
        recyclerView = view.findViewById(R.id.recycler_stats);
        pieChart = view.findViewById(R.id.chart_view);

        adapter = new CrewAdapter(null, false);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        refreshStats();
    }

    private Storage getStorage() {
        return Storage.getInstance();
    }

    /**
     * Refresh all statistics displays
     */
    private void refreshStats() {
        Storage.ColonyStats stats = getStorage().getColonyStats();

        int wins = stats.completedMissions;
        int losses = stats.failedMissions;
        int totalMissions = stats.totalMissions;

        String statsText = String.format(
                "🚀 COLONY STATISTICS 🚀\n\n" +
                        "Total Crew Active: %d\n" +
                        "Total Recruited: %d\n" +
                        "Missions Completed: %d (%d Wins / %d Losses)\n" +
                        "Total Victories: %d\n" +
                        "Training Sessions: %d\n" +
                        "Defeated (Medbay): %d\n" +
                        "Success Rate: %.1f%%",
                stats.activeCrew,
                stats.totalRecruited,
                totalMissions,
                wins,
                losses,
                stats.totalVictories,
                stats.totalTrainingSessions,
                stats.totalDefeated,
                stats.successRate
        );

        colonyStatsText.setText(statsText);

        setupPieChart(wins, losses, totalMissions);

        List<CrewMember> allCrew = getStorage().listAllCrewMembers();
        adapter.setCrewList(allCrew);
    }

    /**
     * Configure and display the mission results pie chart
     * @param wins Number of successful missions
     * @param losses Number of failed missions
     * @param totalMissions Total number of missions completed
     */
    private void setupPieChart(int wins, int losses, int totalMissions) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        if (totalMissions > 0) {
            entries.add(new PieEntry(wins, "Wins"));
            if (losses > 0) {
                entries.add(new PieEntry(losses, "Losses"));
            }
        } else {
            entries.add(new PieEntry(1, "No Missions"));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Mission Results");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        dataSet.setValueTextSize(12f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.setCenterText("Missions");
        pieChart.setCenterTextColor(android.graphics.Color.WHITE);
        pieChart.getLegend().setTextColor(android.graphics.Color.WHITE);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
    }
}
