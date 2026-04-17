package com.example.spacecolony.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * Abstract base class representing a crew member in the space colony.
 * Contains core attributes and behaviors for all crew types.
 */
public abstract class CrewMember implements Serializable {
    // Core attributes
    protected String name;
    protected String specialization;
    protected int skill;
    protected int resilience;
    protected int experience;
    protected int energy;
    protected int maxEnergy;
    protected int id;
    private static int idCounter = 0;

    // Statistics tracking
    protected int missionsCompleted = 0;
    protected int missionsWon = 0;
    protected int trainingSessions = 0;
    protected int timesDefeated = 0;
    protected String currentLocation = "Quarters";
    protected boolean isInMedbay = false;

    // Transient state (not saved)
    protected transient boolean defending = false;

    /**
     * Constructor for creating a new crew member
     * @param name Display name of the crew member
     * @param skill Base skill level
     * @param resilience Damage reduction amount
     * @param maxEnergy Maximum health/energy points
     */
    public CrewMember(String name, int skill, int resilience, int maxEnergy) {
        this.id = ++idCounter;
        this.name = name;
        this.skill = skill;
        this.resilience = resilience;
        this.maxEnergy = maxEnergy;
        this.energy = maxEnergy;
        this.experience = 0;
    }

    /**
     * Custom deserialization to reset transient fields
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        defending = false;
    }

    // Turn-based combat state
    private boolean actedThisTurn = false;

    public boolean hasActedThisTurn() { return actedThisTurn; }
    public void setActedThisTurn(boolean acted) { this.actedThisTurn = acted; }

    /**
     * Calculate effective skill including experience bonus
     * @return Total skill value
     */
    public int getEffectiveSkill() {
        return skill + experience;
    }

    /**
     * Perform an action/attack with random bonus
     * @return Damage amount to deal
     */
    public int act() {
        int randomBonus = (int)(Math.random() * 3);
        return getEffectiveSkill() + randomBonus;
    }

    /**
     * Take damage with resilience reduction
     * @param incomingDamage Raw damage amount
     * @return Actual damage taken after resilience
     */
    public int defend(int incomingDamage) {
        int actualDamage = Math.max(0, incomingDamage - resilience);
        energy -= actualDamage;
        if (energy < 0) energy = 0;
        return actualDamage;
    }

    /**
     * Check if crew member is defeated (energy depleted)
     */
    public boolean isDefeated() {
        return energy <= 0;
    }

    /**
     * Restore energy to maximum
     */
    public void restoreEnergy() {
        this.energy = maxEnergy;
    }

    /**
     * Train in simulator to gain experience
     * @return true if training succeeded (must be in Simulator)
     */
    public boolean train() {
        if (!currentLocation.equals("Simulator")) {
            return false;
        }
        experience++;
        trainingSessions++;
        return true;
    }

    /**
     * Add experience points
     * @param amount Experience to add (can be negative)
     */
    public void gainExperience(int amount) {
        this.experience += amount;
        if (this.experience < 0) {
            this.experience = 0;
        }
    }

    /**
     * Check if crew is currently defending
     */
    public boolean isDefending() {
        return defending;
    }

    public void setDefending(boolean defending) {
        this.defending = defending;
    }

    // Abstract methods for specialization-specific behavior
    public abstract String getColor();
    public abstract int getImageResource();
    public abstract boolean hasBonusOnMission(String missionType);
    public abstract int getMissionBonus(String missionType);

    // Getters and setters
    public String getName() { return name; }
    public String getSpecialization() { return specialization; }
    public int getSkill() { return skill; }
    public int getResilience() { return resilience; }
    public int getExperience() { return experience; }
    public int getEnergy() { return energy; }

    /**
     * Set energy with bounds checking (0 to maxEnergy)
     */
    public void setEnergy(int energy) {
        this.energy = energy;
        if (this.energy > maxEnergy) {
            this.energy = maxEnergy;
        }
        if (this.energy < 0) {
            this.energy = 0;
        }
    }

    public int getMaxEnergy() { return maxEnergy; }
    public int getId() { return id; }
    public int getMissionsCompleted() { return missionsCompleted; }
    public int getMissionsWon() { return missionsWon; }
    public int getTrainingSessions() { return trainingSessions; }
    public int getTimesDefeated() { return timesDefeated; }
    public String getCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(String location) { this.currentLocation = location; }
    public boolean isInMedbay() { return isInMedbay; }

    /**
     * Set medbay status and update location accordingly
     */
    public void setInMedbay(boolean inMedbay) {
        this.isInMedbay = inMedbay;
        if (inMedbay) {
            this.currentLocation = "Medbay";
        }
    }

    // Statistics increment methods
    public void incrementMissionsCompleted() { missionsCompleted++; }
    public void incrementMissionsWon() { missionsWon++; }
    public void incrementTimesDefeated() { timesDefeated++; }
    public void incrementTrainingSessions() { trainingSessions++; }
    public static int getNumberOfCreated() { return idCounter; }

    /**
     * Reset ID counter (used when loading saved games)
     */
    public static void resetIdCounter() {
        idCounter = 0;
    }

    /**
     * Set ID with counter update for save/load consistency
     */
    public void setId(int id) {
        this.id = id;
        if (id > idCounter) {
            idCounter = id;
        }
    }

    @Override
    public String toString() {
        return String.format("%s [%s] Skill:%d(+%d) Res:%d Energy:%d/%d",
                name, specialization, skill, experience, resilience, energy, maxEnergy);
    }
}