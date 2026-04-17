package com.example.spacecolony.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spacecolony.R;
import com.example.spacecolony.model.CrewMember;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter for displaying crew members in a RecyclerView.
 * Supports multiple selection modes for different screens (Quarters, Mission, etc.)
 * Also displays personal statistics for each crew member (missions, victories, training)
 */
public class CrewAdapter extends RecyclerView.Adapter<CrewAdapter.CrewViewHolder> {
    // Selection mode constants
    public static final int SELECTION_MODE_NONE = 0;      // No selection allowed
    public static final int SELECTION_MODE_MULTIPLE = 1;  // Multi-select with checkboxes (for mission selection)
    public static final int SELECTION_MODE_SINGLE = 2;    // Single selection mode
    public static final int SELECTION_MODE_ACTOR = 3;     // Select active crew member during combat

    private List<CrewMember> crewList;           // List of crew members to display
    private Set<Integer> selectedIds;            // Set of selected crew member IDs
    private OnCrewClickListener listener;        // Callback for click events
    private int selectionMode;                   // Current selection mode
    private int selectedActorId = -1;            // ID of currently selected actor in combat
    private boolean missionActive = false;       // Whether a mission is currently active

    /**
     * Interface for handling crew member click events
     */
    public interface OnCrewClickListener {
        void onCrewClick(CrewMember crew, int position);
        void onCrewLongClick(CrewMember crew, int position);
        void onSelectionChanged(Set<Integer> selectedIds);
    }

    /**
     * Constructor
     * @param crewList Initial list of crew members
     * @param showCheckbox Whether to show checkboxes (enables multiple selection mode)
     */
    public CrewAdapter(List<CrewMember> crewList, boolean showCheckbox) {
        this.crewList = new ArrayList<>();
        if (crewList != null) {
            this.crewList.addAll(crewList);
        }
        this.selectedIds = new HashSet<>();
        this.selectionMode = showCheckbox ? SELECTION_MODE_MULTIPLE : SELECTION_MODE_NONE;
    }

    /**
     * Set the click listener for crew member interactions
     */
    public void setOnCrewClickListener(OnCrewClickListener listener) {
        this.listener = listener;
    }

    /**
     * Update the crew list and refresh the view
     * Clears invalid selections (crew members that no longer exist)
     */
    public void setCrewList(List<CrewMember> crewList) {
        this.crewList.clear();
        if (crewList != null) {
            this.crewList.addAll(crewList);
        }
        // Remove selected IDs that no longer exist in the new list
        Set<Integer> validIds = new HashSet<>();
        for (CrewMember cm : this.crewList) {
            validIds.add(cm.getId());
        }
        selectedIds.retainAll(validIds);
        if (!validIds.contains(selectedActorId)) {
            selectedActorId = -1;
        }

        notifyDataSetChanged();

        if (listener != null && selectionMode == SELECTION_MODE_MULTIPLE) {
            listener.onSelectionChanged(selectedIds);
        }
    }

    /**
     * Clear all selections and refresh
     */
    public void clearSelection() {
        selectedIds.clear();
        selectedActorId = -1;
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedIds);
        }
    }

    /**
     * Get a copy of currently selected crew member IDs
     */
    public Set<Integer> getSelectedIds() {
        return new HashSet<>(selectedIds);
    }

    /**
     * Get list of selected crew member objects
     */
    public List<CrewMember> getSelectedCrew() {
        List<CrewMember> selected = new ArrayList<>();
        for (CrewMember cm : crewList) {
            if (selectedIds.contains(cm.getId())) {
                selected.add(cm);
            }
        }
        return selected;
    }

    /**
     * Change the selection mode and clear incompatible selections
     */
    public void setSelectionMode(int mode) {
        this.selectionMode = mode;
        if (mode != SELECTION_MODE_MULTIPLE) {
            selectedIds.clear();
        }
        if (mode != SELECTION_MODE_ACTOR) {
            selectedActorId = -1;
        }
        notifyDataSetChanged();
    }

    /**
     * Set the active actor for combat turns
     */
    public void setSelectedActor(int id) {
        this.selectedActorId = id;
        notifyDataSetChanged();
    }

    /**
     * Clear the active actor selection
     */
    public void clearActorSelection() {
        this.selectedActorId = -1;
        notifyDataSetChanged();
    }

    /**
     * Get the ID of currently selected actor
     */
    public int getSelectedActorId() {
        return selectedActorId;
    }

    /**
     * Set whether a mission is active (affects UI state)
     */
    public void setMissionActive(boolean active) {
        this.missionActive = active;
        notifyDataSetChanged();
    }

    /**
     * Toggle selection state for a crew member (for multiple selection mode)
     */
    public void toggleSelection(int id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
        notifyDataSetChanged();
        if (listener != null) {
            listener.onSelectionChanged(selectedIds);
        }
    }

    /**
     * Toggle actor selection for combat mode
     */
    public void toggleActorSelection(int id) {
        if (selectedActorId == id) {
            selectedActorId = -1;
        } else {
            selectedActorId = id;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CrewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_crew_member, parent, false);
        return new CrewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CrewViewHolder holder, int position) {
        CrewMember cm = crewList.get(position);
        boolean isSelected = selectedIds.contains(cm.getId());
        boolean isActor = (cm.getId() == selectedActorId);
        boolean canAct = missionActive && !cm.isDefeated() && !cm.hasActedThisTurn();

        holder.bind(cm, isSelected, isActor, canAct);
    }

    @Override
    public int getItemCount() {
        return crewList.size();
    }

    /**
     * ViewHolder for crew member items
     * Displays crew info, personal statistics, and handles selection states
     */
    class CrewViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView crewImage;
        TextView nameText;
        TextView specText;
        TextView statsText;
        TextView locationText;
        TextView statusText;
        CheckBox checkBox;
        View colorIndicator;

        // Personal statistics views (new)
        LinearLayout personalStatsLayout;
        TextView personalMissionsText;
        TextView personalVictoriesText;
        TextView personalTrainingText;

        CrewViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            crewImage = itemView.findViewById(R.id.crew_image);
            nameText = itemView.findViewById(R.id.crew_name);
            specText = itemView.findViewById(R.id.crew_specialization);
            statsText = itemView.findViewById(R.id.crew_stats);
            locationText = itemView.findViewById(R.id.crew_location);
            statusText = itemView.findViewById(R.id.status_text);
            checkBox = itemView.findViewById(R.id.crew_checkbox);
            colorIndicator = itemView.findViewById(R.id.color_indicator);

            // Initialize personal statistics views
            personalStatsLayout = itemView.findViewById(R.id.personal_stats_layout);
            personalMissionsText = itemView.findViewById(R.id.personal_missions);
            personalVictoriesText = itemView.findViewById(R.id.personal_victories);
            personalTrainingText = itemView.findViewById(R.id.personal_training);

            // Fallback if status text view is not found in layout
            if (statusText == null) {
                statusText = new TextView(itemView.getContext());
            }
        }

        /**
         * Bind crew member data to the view
         * @param cm The crew member to display
         * @param isSelected Whether this crew is selected (multi-select mode)
         * @param isActor Whether this crew is the active actor in combat
         * @param canAct Whether this crew can take an action this turn
         */
        void bind(CrewMember cm, boolean isSelected, boolean isActor, boolean canAct) {
            nameText.setText(cm.getName());
            specText.setText(cm.getSpecialization());
            locationText.setText("📍 " + cm.getCurrentLocation());

            // Format main stats string: Skill, Resilience, Energy, Experience
            String stats = String.format("⚔️%d(+%d) 🛡️%d ❤️%d/%d ⭐%d",
                    cm.getEffectiveSkill(), cm.getExperience(),
                    cm.getResilience(), cm.getEnergy(), cm.getMaxEnergy(),
                    cm.getExperience());
            statsText.setText(stats);

            // Display personal statistics for this crew member (bonus feature: Statistics +1 point)
            // Shows: missions completed, victories (missions won), and training sessions
            personalMissionsText.setText(String.format("Missions: %d", cm.getMissionsCompleted()));
            personalVictoriesText.setText(String.format("Wins: %d", cm.getMissionsWon()));
            personalTrainingText.setText(String.format("Training: %d", cm.getTrainingSessions()));

            int color = getColorForSpecialization(cm.getColor());
            colorIndicator.setBackgroundColor(color);
            crewImage.setImageResource(getImageForSpecialization(cm.getSpecialization()));

            // Show status text during missions (defeated, acted, selected, or ready)
            if (missionActive) {
                statusText.setVisibility(View.VISIBLE);
                if (cm.isDefeated()) {
                    statusText.setText("💀 DEFEATED");
                    statusText.setTextColor(Color.parseColor("#FF4444"));
                } else if (cm.hasActedThisTurn()) {
                    statusText.setText("✓ ACTED");
                    statusText.setTextColor(Color.parseColor("#888888"));
                } else if (isActor) {
                    statusText.setText("▶ SELECTED (tap to cancel)");
                    statusText.setTextColor(Color.parseColor("#00FF00"));
                } else {
                    statusText.setText("READY");
                    statusText.setTextColor(Color.parseColor("#44FF44"));
                }
            } else {
                statusText.setVisibility(View.GONE);
            }

            // Show checkbox only in multiple selection mode
            checkBox.setVisibility(selectionMode == SELECTION_MODE_MULTIPLE ? View.VISIBLE : View.GONE);
            checkBox.setChecked(isSelected);
            checkBox.setOnClickListener(v -> {
                if (selectionMode == SELECTION_MODE_MULTIPLE) {
                    toggleSelection(cm.getId());
                }
            });

            // Set card background color based on state
            int bgColor;
            if (isActor) {
                bgColor = Color.parseColor("#2E7D32");  // Green for active actor
            } else if (isSelected) {
                bgColor = Color.parseColor("#E3F2FD");  // Light blue for selected
            } else if (cm.isDefeated()) {
                bgColor = Color.parseColor("#3E2723");  // Dark brown for defeated
            } else if (missionActive && cm.hasActedThisTurn()) {
                bgColor = Color.parseColor("#424242");  // Gray for already acted
            } else {
                bgColor = Color.WHITE;                   // Default white
            }
            cardView.setCardBackgroundColor(bgColor);

            // Handle click events based on selection mode
            itemView.setOnClickListener(v -> {
                if (selectionMode == SELECTION_MODE_ACTOR && missionActive) {
                    // In actor mode during missions, only allow selecting ready crew
                    if (cm.isDefeated()) return;
                    if (cm.hasActedThisTurn()) return;

                    if (listener != null) {
                        listener.onCrewClick(cm, getAdapterPosition());
                    }
                } else if (selectionMode == SELECTION_MODE_MULTIPLE) {
                    toggleSelection(cm.getId());
                } else if (listener != null) {
                    listener.onCrewClick(cm, getAdapterPosition());
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onCrewLongClick(cm, getAdapterPosition());
                    return true;
                }
                return false;
            });

            // Disable interaction for defeated or already-acted crew in actor mode
            if (selectionMode == SELECTION_MODE_ACTOR) {
                boolean clickable = !cm.isDefeated() && !cm.hasActedThisTurn();
                itemView.setAlpha(clickable ? 1.0f : 0.5f);
                itemView.setClickable(clickable);
            } else {
                itemView.setAlpha(1.0f);
                itemView.setClickable(true);
            }
        }

        /**
         * Get color resource for specialization color name
         */
        private int getColorForSpecialization(String colorName) {
            if (colorName == null) return Color.GRAY;
            switch (colorName) {
                case "Blue": return Color.parseColor("#2196F3");
                case "Yellow": return Color.parseColor("#FFEB3B");
                case "Green": return Color.parseColor("#4CAF50");
                case "Purple": return Color.parseColor("#9C27B0");
                case "Red": return Color.parseColor("#F44336");
                default: return Color.GRAY;
            }
        }

        /**
         * Get icon resource for specialization type
         */
        private int getImageForSpecialization(String specialization) {
            if (specialization == null) return android.R.drawable.ic_menu_help;
            switch (specialization) {
                case "Pilot": return android.R.drawable.ic_menu_compass;
                case "Engineer": return android.R.drawable.ic_menu_preferences;
                case "Medic": return android.R.drawable.ic_menu_add;
                case "Scientist": return android.R.drawable.ic_menu_search;
                case "Soldier": return android.R.drawable.ic_menu_close_clear_cancel;
                default: return android.R.drawable.ic_menu_help;
            }
        }
    }
}