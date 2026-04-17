package com.example.spacecolony.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spacecolony.R;
import com.example.spacecolony.adapter.CrewAdapter;
import com.example.spacecolony.model.CrewMember;
import com.example.spacecolony.model.MissionControl;
import com.example.spacecolony.model.Storage;
import com.example.spacecolony.model.Threat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Mission fragment for managing tactical combat missions.
 * Handles crew selection, turn-based combat, and mission execution.
 */
public class MissionFragment extends Fragment implements CrewAdapter.OnCrewClickListener,
        MissionControl.MissionListener {

    /**
     * Interface for notifying mission status changes to MainActivity
     */
    public interface OnMissionStatusListener {
        void onMissionStatusChanged(boolean inProgress);
    }

    private RecyclerView recyclerView;
    private CrewAdapter adapter;
    private TextView emptyText;
    private Button launchButton;
    private Button autoMissionButton;
    private Button returnButton;

    private CardView missionCard;
    private TextView threatNameText;
    private TextView threatStatsText;
    private ProgressBar threatHealthBar;
    private TextView missionLogText;
    private ScrollView logScrollView;
    private LinearLayout actionButtonsLayout;
    private Button attackButton;
    private Button defendButton;
    private Button specialButton;
    private TextView turnIndicatorText;

    private List<CrewMember> selectedSquad;
    private boolean missionActive = false;
    private boolean isAutoMode = false;

    private CrewMember selectedActor = null;
    private boolean waitingForCrewSelection = false;

    private StringBuilder battleLog = new StringBuilder();
    private static final String KEY_BATTLE_LOG = "battle_log";

    private OnMissionStatusListener missionStatusListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnMissionStatusListener) {
            missionStatusListener = (OnMissionStatusListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_mission_crew);
        emptyText = view.findViewById(R.id.empty_text);
        launchButton = view.findViewById(R.id.btn_launch_mission);
        autoMissionButton = view.findViewById(R.id.btn_auto_mission);
        returnButton = view.findViewById(R.id.btn_return);
        missionCard = view.findViewById(R.id.mission_card);
        threatNameText = view.findViewById(R.id.threat_name);
        threatStatsText = view.findViewById(R.id.threat_stats);
        threatHealthBar = view.findViewById(R.id.threat_health_bar);
        missionLogText = view.findViewById(R.id.mission_log);
        logScrollView = view.findViewById(R.id.log_scroll);
        actionButtonsLayout = view.findViewById(R.id.action_buttons);
        attackButton = view.findViewById(R.id.btn_attack);
        defendButton = view.findViewById(R.id.btn_defend);
        specialButton = view.findViewById(R.id.btn_special);
        turnIndicatorText = view.findViewById(R.id.turn_indicator);

        adapter = new CrewAdapter(null, true);
        adapter.setOnCrewClickListener(this);
        adapter.setSelectionMode(CrewAdapter.SELECTION_MODE_MULTIPLE);
        adapter.setMissionActive(false);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        launchButton.setOnClickListener(v -> launchMission());
        autoMissionButton.setOnClickListener(v -> launchAutoMission());
        returnButton.setOnClickListener(v -> returnSelectedToQuarters());

        attackButton.setOnClickListener(v -> executeAction(MissionControl.ActionType.ATTACK));
        defendButton.setOnClickListener(v -> executeAction(MissionControl.ActionType.DEFEND));
        specialButton.setOnClickListener(v -> executeAction(MissionControl.ActionType.SPECIAL));

        selectedSquad = new ArrayList<>();

        getMissionControl().addMissionListener(this);

        if (savedInstanceState != null) {
            String savedLog = savedInstanceState.getString(KEY_BATTLE_LOG, "");
            battleLog.append(savedLog);
            missionLogText.setText(battleLog.toString());
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }

        if (getMissionControl().isMissionInProgress()) {
            missionActive = true;
            notifyMissionStatusChanged(true);
            selectedSquad = getMissionControl().getCurrentSquad();
            showMissionUI();
            Threat threat = getMissionControl().getCurrentThreat();
            if (threat != null) {
                updateThreatStats(threat);
            }
            if (!isAutoMode && selectedActor == null && waitingForCrewSelection) {
                startCrewSelectionPhase();
            }
        } else {
            refreshCrewList();
            hideMissionUI();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_BATTLE_LOG, battleLog.toString());
    }

    private Storage getStorage() {
        return Storage.getInstance();
    }

    private MissionControl getMissionControl() {
        return MissionControl.getInstance();
    }

    /**
     * Set the listener for mission status changes
     */
    public void setOnMissionStatusListener(OnMissionStatusListener listener) {
        this.missionStatusListener = listener;
    }

    /**
     * Notify MainActivity about mission status changes
     */
    private void notifyMissionStatusChanged(boolean inProgress) {
        if (missionStatusListener != null) {
            missionStatusListener.onMissionStatusChanged(inProgress);
        }
    }

    /**
     * Return selected crew members to quarters
     */
    private void returnSelectedToQuarters() {
        Set<Integer> selected = adapter.getSelectedIds();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "Select crew to return", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int id : selected) {
            getStorage().moveCrewMember(id, "Quarters");
        }

        Toast.makeText(requireContext(),
                "Returned " + selected.size() + " crew to Quarters", Toast.LENGTH_SHORT).show();

        adapter.clearSelection();
        refreshCrewList();
    }

    /**
     * Refresh the list of available crew members in mission control
     */
    private void refreshCrewList() {
        if (missionActive) return;

        List<CrewMember> allMissionCrew = getStorage().getCrewByLocation("Mission");
        List<CrewMember> availableCrew = new ArrayList<>();

        for (CrewMember cm : allMissionCrew) {
            if (!cm.isDefeated() && !cm.isInMedbay()) {
                availableCrew.add(cm);
            }
        }

        adapter.setSelectionMode(CrewAdapter.SELECTION_MODE_MULTIPLE);
        adapter.setMissionActive(false);
        adapter.setCrewList(availableCrew);

        if (availableCrew.isEmpty()) {
            emptyText.setText("No crew in Mission Control.\nSend crew from Quarters first!");
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            launchButton.setEnabled(false);
            autoMissionButton.setEnabled(false);
            returnButton.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            returnButton.setVisibility(View.VISIBLE);
            launchButton.setEnabled(false);
            autoMissionButton.setEnabled(false);
            returnButton.setEnabled(false);
        }
    }

    /**
     * Launch a manual mission with selected squad
     */
    private void launchMission() {
        selectedSquad = adapter.getSelectedCrew();

        if (selectedSquad.size() < 2 || selectedSquad.size() > 3) {
            Toast.makeText(requireContext(), "Select 2-3 crew members", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CrewMember cm : selectedSquad) {
            if (!"Mission".equals(cm.getCurrentLocation())) {
                Toast.makeText(requireContext(),
                        cm.getName() + " is not in Mission Control!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        isAutoMode = false;
        getMissionControl().launchMission(selectedSquad);
        missionActive = true;
        notifyMissionStatusChanged(true);

        showMissionUI();
        adapter.clearSelection();

        startCrewSelectionPhase();
    }

    /**
     * Start the crew selection phase for turn-based combat
     */
    private void startCrewSelectionPhase() {
        waitingForCrewSelection = true;
        selectedActor = null;
        adapter.clearActorSelection();

        setActionButtonsEnabled(false);

        List<CrewMember> availableActors = getAvailableActors();

        if (availableActors.isEmpty()) {
            getMissionControl().endRound();
            return;
        }

        adapter.setMissionActive(true);
        adapter.setSelectionMode(CrewAdapter.SELECTION_MODE_ACTOR);
        adapter.setCrewList(availableActors);

        updateTurnIndicator("Select a crew member to act");
    }

    /**
     * Get list of crew members who can still act this turn
     */
    private List<CrewMember> getAvailableActors() {
        List<CrewMember> available = new ArrayList<>();
        List<CrewMember> squad = getMissionControl().getCurrentSquad();

        for (CrewMember cm : squad) {
            if (!cm.isDefeated() && !cm.isInMedbay()) {
                available.add(cm);
            }
        }
        return available;
    }

    @Override
    public void onCrewClick(CrewMember crew, int position) {
        if (!missionActive || !waitingForCrewSelection) {
            if (!missionActive) {
                adapter.toggleSelection(crew.getId());
            }
            return;
        }

        if (crew.isDefeated()) {
            Toast.makeText(requireContext(),
                    crew.getName() + " is defeated", Toast.LENGTH_SHORT).show();
            return;
        }

        if (crew.hasActedThisTurn()) {
            Toast.makeText(requireContext(),
                    crew.getName() + " already acted", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedActor != null && selectedActor.getId() == crew.getId()) {
            selectedActor = null;
            adapter.clearActorSelection();
            setActionButtonsEnabled(false);
            updateTurnIndicator("Select a crew member to act");
            Toast.makeText(requireContext(), "Cancelled", Toast.LENGTH_SHORT).show();
        } else {
            selectedActor = crew;
            adapter.setSelectedActor(crew.getId());
            setActionButtonsEnabled(true);
            updateTurnIndicator(">>> " + crew.getName() + "'s turn <<<");
        }
    }

    /**
     * Execute the selected action for the current actor
     */
    private void executeAction(MissionControl.ActionType action) {
        if (!missionActive || !getMissionControl().isMissionInProgress()) {
            Toast.makeText(requireContext(), "No active mission", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedActor == null) {
            Toast.makeText(requireContext(), "Select a crew member first!", Toast.LENGTH_SHORT).show();
            return;
        }

        final CrewMember actor = selectedActor;
        selectedActor = null;
        adapter.clearActorSelection();
        setActionButtonsEnabled(false);

        if (actor.isDefeated()) {
            Toast.makeText(requireContext(), actor.getName() + " is defeated!", Toast.LENGTH_SHORT).show();
            startCrewSelectionPhase();
            return;
        }

        if (actor.hasActedThisTurn()) {
            Toast.makeText(requireContext(), actor.getName() + " already acted!", Toast.LENGTH_SHORT).show();
            startCrewSelectionPhase();
            return;
        }
        getMissionControl().executeTacticalTurn(actor, action);

        List<CrewMember> availableActors = getAvailableActors();
        if (!availableActors.isEmpty()) {
            startCrewSelectionPhase();
        }
    }

    /**
     * Enable or disable action buttons and update visual state
     */
    private void setActionButtonsEnabled(boolean enabled) {
        attackButton.setEnabled(enabled);
        defendButton.setEnabled(enabled);
        specialButton.setEnabled(enabled);

        float alpha = enabled ? 1.0f : 0.5f;
        attackButton.setAlpha(alpha);
        defendButton.setAlpha(alpha);
        specialButton.setAlpha(alpha);
    }

    /**
     * Update the turn indicator text display
     */
    private void updateTurnIndicator(String text) {
        if (turnIndicatorText != null) {
            turnIndicatorText.setText(text);
            turnIndicatorText.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Show mission UI elements during active mission
     */
    private void showMissionUI() {
        missionCard.setVisibility(View.VISIBLE);

        if (isAutoMode) {
            actionButtonsLayout.setVisibility(View.GONE);
        } else {
            actionButtonsLayout.setVisibility(View.VISIBLE);
        }

        launchButton.setVisibility(View.GONE);
        autoMissionButton.setVisibility(View.GONE);
        returnButton.setVisibility(View.GONE);

        recyclerView.setEnabled(true);

        Threat threat = getMissionControl().getCurrentThreat();
        if (threat != null) {
            threatNameText.setText(threat.getName());
            threatHealthBar.setMax(threat.getMaxEnergy());
            threatHealthBar.setProgress(threat.getEnergy());
            updateThreatStats(threat);
        }
    }

    /**
     * Hide mission UI elements when mission ends
     */
    private void hideMissionUI() {
        missionCard.setVisibility(View.GONE);
        actionButtonsLayout.setVisibility(View.GONE);
        launchButton.setVisibility(View.VISIBLE);
        autoMissionButton.setVisibility(View.VISIBLE);
        returnButton.setVisibility(View.VISIBLE);
        launchButton.setEnabled(true);
        autoMissionButton.setEnabled(true);
        recyclerView.setEnabled(true);
        missionLogText.setText("");
        battleLog.setLength(0);
        if (turnIndicatorText != null) {
            turnIndicatorText.setVisibility(View.GONE);
        }
    }

    /**
     * Update threat statistics display
     */
    private void updateThreatStats(Threat threat) {
        if (threat == null) return;
        threatStatsText.setText(String.format("Skill: %d | Res: %d | Energy: %d/%d",
                threat.getSkill(), threat.getResilience(), threat.getEnergy(), threat.getMaxEnergy()));
        threatHealthBar.setProgress(threat.getEnergy());
    }

    @Override
    public void onMissionUpdate(String message) {
        if (getContext() == null) return;
        requireActivity().runOnUiThread(() -> {
            battleLog.append(message).append("\n");
            missionLogText.append(message + "\n");
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    @Override
    public void onCrewTurn(CrewMember crew, Threat threat) {
        if (getContext() == null) return;
    }

    @Override
    public void onThreatTurn(CrewMember target, Threat threat) {
        if (getContext() == null) return;
        requireActivity().runOnUiThread(() -> {
            updateThreatStats(threat);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onMissionComplete(boolean success, List<CrewMember> survivors) {
        if (getContext() == null) return;
        requireActivity().runOnUiThread(() -> {
            missionActive = false;
            notifyMissionStatusChanged(false);
            waitingForCrewSelection = false;
            selectedActor = null;
            isAutoMode = false;

            hideMissionUI();
            refreshCrewList();

            String message = success ?
                    "Mission Successful!" : "Mission Failed!";
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onCrewDefeated(CrewMember crew) {
        if (getContext() == null) return;
        requireActivity().runOnUiThread(() -> {
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onRoundStart(int round) {
        if (getContext() == null) return;
        requireActivity().runOnUiThread(() -> {
            updateTurnIndicator("Round " + round + " - Select crew to act");

            selectedActor = null;
            waitingForCrewSelection = true;
            setActionButtonsEnabled(false);

            List<CrewMember> availableActors = getAvailableActors();
            adapter.setMissionActive(true);
            adapter.setSelectionMode(CrewAdapter.SELECTION_MODE_ACTOR);
            adapter.setCrewList(availableActors);
        });
    }

    @Override
    public void onCrewLongClick(CrewMember crew, int position) {
    }

    @Override
    public void onSelectionChanged(Set<Integer> selectedIds) {
        if (!missionActive) {
            boolean hasSelection = !selectedIds.isEmpty();
            boolean validSize = selectedIds.size() >= 2 && selectedIds.size() <= 3;

            returnButton.setEnabled(hasSelection);
            launchButton.setEnabled(validSize);
            autoMissionButton.setEnabled(validSize);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getMissionControl().removeMissionListener(this);
        getMissionControl().addMissionListener(this);

        if (getMissionControl().isMissionInProgress()) {
            missionActive = true;
            notifyMissionStatusChanged(true);
            selectedSquad = getMissionControl().getCurrentSquad();

            showMissionUI();

            Threat threat = getMissionControl().getCurrentThreat();
            if (threat != null) {
                updateThreatStats(threat);
            }

            if (battleLog.length() > 0) {
                missionLogText.setText(battleLog.toString());
                logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
            }

            List<CrewMember> availableActors = getAvailableActors();
            if (!availableActors.isEmpty()) {
                adapter.setMissionActive(true);
                adapter.setSelectionMode(CrewAdapter.SELECTION_MODE_ACTOR);
                adapter.setCrewList(availableActors);
                recyclerView.setVisibility(View.VISIBLE);
            }

            if (!isAutoMode) {
                waitingForCrewSelection = true;
                selectedActor = null;
                setActionButtonsEnabled(false);
                updateTurnIndicator("Select a crew member to act");
            }

        } else {
            missionActive = false;
            notifyMissionStatusChanged(false);
            isAutoMode = false;
            waitingForCrewSelection = false;
            selectedActor = null;
            hideMissionUI();
            refreshCrewList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        adapter = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getMissionControl().removeMissionListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        missionStatusListener = null;
    }

    /**
     * Launch an automatic mission with AI-controlled combat
     */
    private void launchAutoMission() {
        selectedSquad = adapter.getSelectedCrew();

        if (selectedSquad.size() < 2 || selectedSquad.size() > 3) {
            Toast.makeText(requireContext(), "Select 2-3 crew members", Toast.LENGTH_SHORT).show();
            return;
        }

        for (CrewMember cm : selectedSquad) {
            if (!"Mission".equals(cm.getCurrentLocation())) {
                Toast.makeText(requireContext(),
                        cm.getName() + " is not in Mission Control!", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        isAutoMode = true;
        missionActive = true;
        notifyMissionStatusChanged(true);

        showAutoMissionUI();
        adapter.clearSelection();

        // First launch the mission to initialize it properly
        getMissionControl().launchMission(selectedSquad);

        // Then execute auto mission
        getMissionControl().executeAutoMission(selectedSquad, (success, survivors) -> {
            if (getContext() == null || getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                missionActive = false;
                notifyMissionStatusChanged(false);
                isAutoMode = false;

                hideMissionUI();
                refreshCrewList();

                String message = success ?
                        "Mission Successful!" : "Mission Failed!";
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            });
        });
    }

    /**
     * Show UI for automatic mission mode
     */
    private void showAutoMissionUI() {
        missionCard.setVisibility(View.VISIBLE);
        actionButtonsLayout.setVisibility(View.GONE);
        launchButton.setVisibility(View.GONE);
        autoMissionButton.setVisibility(View.GONE);
        returnButton.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("Auto mission in progress...");

        Threat threat = getMissionControl().getCurrentThreat();
        if (threat != null) {
            threatNameText.setText(threat.getName());
            threatHealthBar.setMax(threat.getMaxEnergy());
            threatHealthBar.setProgress(threat.getEnergy());
            updateThreatStats(threat);
        }
    }
}