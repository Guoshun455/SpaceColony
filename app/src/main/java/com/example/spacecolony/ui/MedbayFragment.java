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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spacecolony.R;
import com.example.spacecolony.adapter.CrewAdapter;
import com.example.spacecolony.model.CrewMember;
import com.example.spacecolony.model.Storage;

import java.util.List;
import java.util.Set;

/**
 * Medbay fragment for managing injured crew members.
 * Allows healing selected crew members with experience penalty.
 */
public class MedbayFragment extends Fragment implements CrewAdapter.OnCrewClickListener {

    private RecyclerView recyclerView;
    private CrewAdapter adapter;
    private TextView emptyText;
    private Button healButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_medbay, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_medbay);
        emptyText = view.findViewById(R.id.empty_text);
        healButton = view.findViewById(R.id.btn_heal);

        adapter = new CrewAdapter(null, true);
        adapter.setOnCrewClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        healButton.setOnClickListener(v -> healSelected());

        refreshCrewList();
    }

    private Storage getStorage() {
        return Storage.getInstance();
    }

    /**
     * Refresh the list of crew members currently in medbay
     */
    private void refreshCrewList() {
        List<CrewMember> crew = getStorage().getCrewByLocation("Medbay");
        adapter.setCrewList(crew);

        if (crew.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            healButton.setEnabled(false);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Heal all selected crew members and move them to quarters
     * Applies experience penalty (half of current experience is lost)
     */
    private void healSelected() {
        Set<Integer> selected = adapter.getSelectedIds();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "Select crew to heal", Toast.LENGTH_SHORT).show();
            return;
        }

        int healedCount = 0;
        for (int id : selected) {
            CrewMember cm = getStorage().getCrewMember(id);
            if (cm != null && cm.isInMedbay()) {
                int currentExp = cm.getExperience();
                int penaltyExp = currentExp / 2;
                for (int i = 0; i < penaltyExp; i++) {
                    cm.gainExperience(-1);
                }
                cm.setInMedbay(false);
                cm.restoreEnergy();
                getStorage().moveCrewMember(id, "Quarters");
                healedCount++;
            }
        }

        Toast.makeText(requireContext(),
                "Healed " + healedCount + " crew (exp penalty applied)",
                Toast.LENGTH_SHORT).show();

        adapter.clearSelection();
        refreshCrewList();
    }

    @Override
    public void onCrewClick(CrewMember crew, int position) {
    }

    @Override
    public void onCrewLongClick(CrewMember crew, int position) {
        Toast.makeText(requireContext(),
                crew.getName() + " - " + crew.getSpecialization() +
                        "\nEnergy: " + crew.getEnergy() + "/" + crew.getMaxEnergy() +
                        "\nExp: " + crew.getExperience(),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSelectionChanged(Set<Integer> selectedIds) {
        healButton.setEnabled(!selectedIds.isEmpty());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCrewList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        adapter = null;
    }
}