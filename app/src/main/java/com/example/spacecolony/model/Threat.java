package com.example.spacecolony.model;

import java.io.Serializable;
import java.util.Random;

/**
 * Represents a threat/enemy that the crew must face during missions.
 * Threats scale in difficulty based on completed missions.
 */
public class Threat implements Serializable {
    private String name;
    private String type;
    private int skill;
    private int resilience;
    private int energy;
    private int maxEnergy;
    private int missionDifficulty;

    private static final String[] THREAT_TYPES = {
            "Asteroid Storm", "Solar Flare", "Alien Attack", "System Failure",
            "Fire in Kitchen", "Fuel Leakage", "Heating Malfunction",
            "Biological Hazard", "Radiation Spike", "Mechanical Failure"
    };

    private static final Random random = new Random();

    /**
     * Create a new threat with difficulty based on completed missions
     * @param completedMissions Number of missions completed (affects difficulty scaling)
     */
    public Threat(int completedMissions) {
        this.missionDifficulty = completedMissions;
        generateThreat();
    }

    /**
     * Generate random threat properties based on difficulty
     */
    private void generateThreat() {
        this.type = THREAT_TYPES[random.nextInt(THREAT_TYPES.length)];
        this.name = generateName();
        this.skill = 4 + (missionDifficulty / 2);
        this.resilience = 2 + (missionDifficulty / 5);
        this.maxEnergy = 25 + (missionDifficulty * 3);
        this.energy = this.maxEnergy;
    }

    /**
     * Generate a descriptive name with random prefix
     */
    private String generateName() {
        String[] prefixes = {"Intense", "Severe", "Moderate", "Critical", "Minor", "Major"};
        String prefix = prefixes[random.nextInt(prefixes.length)];
        return prefix + " " + type;
    }

    /**
     * Attack a crew member
     * @param target Crew member being attacked
     * @return Damage amount to deal (minimum 1)
     */
    public int attack(CrewMember target) {
        int randomBonus = random.nextInt(3);
        int damage = skill + randomBonus;
        if (target.hasBonusOnMission(type)) {
            damage -= 1;
        }
        return Math.max(1, damage);
    }

    /**
     * Take damage from an attack
     * @param incomingDamage Raw damage amount
     * @return Actual damage taken after resilience reduction
     */
    public int defend(int incomingDamage) {
        int actualDamage = Math.max(0, incomingDamage - resilience);
        energy -= actualDamage;
        if (energy < 0) energy = 0;
        return actualDamage;
    }

    /**
     * Check if threat is defeated (energy depleted)
     */
    public boolean isDefeated() {
        return energy <= 0;
    }

    // Getters
    public String getType() {
        return type;
    }

    public String getName() { return name; }
    public int getSkill() { return skill; }
    public int getResilience() { return resilience; }
    public int getEnergy() { return energy; }
    public int getMaxEnergy() { return maxEnergy; }

    @Override
    public String toString() {
        return String.format("%s (Skill:%d, Res:%d, Energy:%d/%d)",
                name, skill, resilience, energy, maxEnergy);
    }
}
