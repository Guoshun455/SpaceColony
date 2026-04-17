package com.example.spacecolony.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages crew member training in the simulator facility.
 * Allows crew members to gain experience through training sessions.
 */
public class Simulator {
    private Storage storage;

    /**
     * Constructor
     * @param storage Reference to the storage system for crew management
     */
    public Simulator(Storage storage) {
        this.storage = storage;
    }

    /**
     * Train a crew member to gain experience
     * Crew member must be located in Simulator to train successfully
     * @param cm Crew member to train
     */
    public void train(CrewMember cm) {
        cm.train();
    }

    /**
     * Get list of all crew members currently in the simulator
     * @return List of crew members in training location
     */
    public List<CrewMember> getTrainingCrew() {
        return storage.getCrewByLocation("Simulator");
    }
}
