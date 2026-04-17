package com.example.spacecolony.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Singleton storage system for managing colony data.
 * Handles crew members, mission statistics, and game state persistence.
 */
public class Storage implements Serializable {
    private static final long serialVersionUID = 2L;  // Updated version number

    private static volatile Storage instance;
    private static final Object lock = new Object();

    private String name;
    private HashMap<Integer, CrewMember> crewMembers;
    private int completedMissions;  // Wins
    private int failedMissions;     // Losses
    private int totalCrewRecruited;

    private Storage(String name) {
        this.name = name;
        this.crewMembers = new HashMap<>();
        this.completedMissions = 0;
        this.failedMissions = 0;
        this.totalCrewRecruited = 0;
    }

    /**
     * Get singleton instance (thread-safe)
     */
    public static Storage getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Storage("SpaceColony");
                }
            }
        }
        return instance;
    }

    /**
     * Set singleton instance (for save/load)
     */
    public static void setInstance(Storage storage) {
        synchronized (lock) {
            instance = storage;
        }
    }

    /**
     * Reset singleton instance (for new games)
     */
    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    /**
     * Custom serialization to ensure all fields are saved
     */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

    /**
     * Custom deserialization to ensure all fields are restored
     * Handles backward compatibility with older save files
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Ensure backward compatibility with older save files
        if (crewMembers == null) {
            crewMembers = new HashMap<>();
        }
    }

    /**
     * Add a new crew member to storage and initialize their state
     */
    public void addCrewMember(CrewMember cm) {
        if (cm != null) {
            cm.setCurrentLocation("Quarters");
            cm.setInMedbay(false);
            cm.restoreEnergy();
            crewMembers.put(cm.getId(), cm);
            totalCrewRecruited++;
        }
    }

    public CrewMember getCrewMember(int id) {
        return crewMembers.get(id);
    }

    public void removeCrewMember(int id) {
        crewMembers.remove(id);
    }

    /**
     * Get list of all crew members
     */
    public List<CrewMember> listAllCrewMembers() {
        return new ArrayList<>(crewMembers.values());
    }

    /**
     * Get crew members filtered by current location
     * @param location Location name to filter by
     */
    public List<CrewMember> getCrewByLocation(String location) {
        List<CrewMember> result = new ArrayList<>();
        for (CrewMember cm : crewMembers.values()) {
            if (location.equals(cm.getCurrentLocation())) {
                result.add(cm);
            }
        }
        return result;
    }

    /**
     * Move crew member to a new location and update their state
     * @param id Crew member ID
     * @param newLocation Target location (Quarters, Medbay, Mission, Simulator)
     */
    public void moveCrewMember(int id, String newLocation) {
        CrewMember cm = crewMembers.get(id);
        if (cm == null) {
            System.out.println("[ERROR] CrewMember with id " + id + " not found!");
            return;
        }

        String oldLocation = cm.getCurrentLocation();

        cm.setCurrentLocation(newLocation);

        if ("Quarters".equals(newLocation)) {
            cm.restoreEnergy();
            cm.setInMedbay(false);
        } else if ("Medbay".equals(newLocation)) {
            cm.setInMedbay(true);
        } else {
            cm.setInMedbay(false);
        }

        System.out.println("[MOVE] " + cm.getName() + ": " + oldLocation + " -> " + newLocation
                + (cm.isInMedbay() ? " [MEDBAY]" : ""));
    }

    /**
     * Increment wins counter (called on mission success)
     */
    public void incrementCompletedMissions() {
        completedMissions++;
        System.out.println("[STORAGE] Wins incremented to: " + completedMissions);
    }

    /**
     * Increment losses counter (called on mission failure)
     */
    public void incrementFailedMissions() {
        failedMissions++;
        System.out.println("[STORAGE] Losses incremented to: " + failedMissions);
    }

    /**
     * Calculate and return comprehensive colony statistics
     */
    public ColonyStats getColonyStats() {
        ColonyStats stats = new ColonyStats();
        stats.totalCrew = crewMembers.size();
        stats.totalRecruited = totalCrewRecruited;

        int wins = completedMissions;
        int losses = failedMissions;
        int totalMissions = wins + losses;

        stats.totalMissions = totalMissions;
        stats.completedMissions = wins;
        stats.failedMissions = losses;

        int totalWinsFromCrew = 0;
        int totalTraining = 0;
        int defeated = 0;
        int active = 0;

        for (CrewMember cm : crewMembers.values()) {
            totalWinsFromCrew += cm.getMissionsWon();
            totalTraining += cm.getTrainingSessions();
            defeated += cm.getTimesDefeated();
            if (!cm.isInMedbay() && !cm.isDefeated()) active++;
        }

        stats.totalVictories = totalWinsFromCrew;
        stats.totalTrainingSessions = totalTraining;
        stats.totalDefeated = defeated;
        stats.activeCrew = active;

        if (totalMissions > 0) {
            stats.successRate = (wins * 100.0 / totalMissions);
        } else {
            stats.successRate = 0.0;
        }

        return stats;
    }

    // Getters
    public String getName() { return name; }
    public int getCompletedMissions() { return completedMissions; }
    public int getFailedMissions() { return failedMissions; }
    public int getTotalCrewRecruited() { return totalCrewRecruited; }
    public HashMap<Integer, CrewMember> getCrewMap() { return crewMembers; }

    /**
     * Print debug information to console
     */
    public void printDebugInfo() {
        System.out.println("=== STORAGE DEBUG ===");
        System.out.println("Name: " + name);
        System.out.println("Wins (completedMissions): " + completedMissions);
        System.out.println("Losses (failedMissions): " + failedMissions);
        System.out.println("Total Recruited: " + totalCrewRecruited);
        System.out.println("Crew Count: " + crewMembers.size());
        System.out.println("===================");
    }

    /**
     * Data class for colony statistics
     */
    public static class ColonyStats {
        public int totalCrew;
        public int activeCrew;
        public int totalRecruited;
        public int totalMissions;
        public int completedMissions;
        public int failedMissions;
        public int totalVictories;
        public int totalTrainingSessions;
        public int totalDefeated;
        public double successRate;
    }
}