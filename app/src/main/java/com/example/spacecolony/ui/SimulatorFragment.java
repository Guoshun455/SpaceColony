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
 * Simulator fragment for training crew members.
 * Allows crew to gain experience through training sessions.
 */
public class SimulatorFragment extends Fragment implements CrewAdapter.OnCrewClickListener {
    private RecyclerView recyclerView;
    private CrewAdapter adapter;
    private TextView emptyText;
    private Button trainButton;
    private Button restButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_simulator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_simulator);
        emptyText = view.findViewById(R.id.empty_text);
        trainButton = view.findViewById(R.id.btn_train);
        restButton = view.findViewById(R.id.btn_rest);

        adapter = new CrewAdapter(null, true);
        adapter.setOnCrewClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        trainButton.setOnClickListener(v -> trainSelected());
        restButton.setOnClickListener(v -> restSelected());

        refreshCrewList();
    }

    private Storage getStorage() {
        return Storage.getInstance();
    }

    /**
     * Refresh the list of crew members in simulator
     */
    private void refreshCrewList() {
        List<CrewMember> crew = getStorage().getCrewByLocation("Simulator");

        System.out.println("[SimulatorFragment] Found " + crew.size() + " crew in Simulator");

        adapter.setCrewList(crew);

        if (crew.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            trainButton.setEnabled(false);
            restButton.setEnabled(false);
        } else {
            emptyText.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Train selected crew members to gain experience
     */
    private void trainSelected() {
        Set<Integer> selected = adapter.getSelectedIds();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "Select crew to train", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder trained = new StringBuilder("Trained: ");
        for (int id : selected) {
            CrewMember cm = getStorage().getCrewMember(id);
            if (cm != null) {
                cm.train();
                trained.append(cm.getName()).append(" ");
            }
        }

        Toast.makeText(requireContext(), trained.toString(), Toast.LENGTH_SHORT).show();
        adapter.clearSelection();
        refreshCrewList();
    }

    /**
     * Return selected crew members to quarters
     */
    private void restSelected() {
        Set<Integer> selected = adapter.getSelectedIds();
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "Select crew to send to quarters", Toast.LENGTH_SHORT).show();
            return;
        }

        for (int id : selected) {
            getStorage().moveCrewMember(id, "Quarters");
        }

        Toast.makeText(requireContext(),
                "Sent " + selected.size() + " crew to Quarters", Toast.LENGTH_SHORT).show();

        adapter.clearSelection();
        refreshCrewList();
    }

    @Override
    public void onCrewClick(CrewMember crew, int position) {}

    @Override
    public void onCrewLongClick(CrewMember crew, int position) {}

    @Override
    public void onSelectionChanged(Set<Integer> selectedIds) {
        trainButton.setEnabled(!selectedIds.isEmpty());
        restButton.setEnabled(!selectedIds.isEmpty());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshCrewList();
    }
}