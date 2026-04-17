package com.example.spacecolony.model;

/**
 * Scientist crew member specialization.
 * Excels at research, anomaly, solar, and radiation-related missions.
 * Has a chance to deal critical damage on attacks.
 */
public class Scientist extends CrewMember {

    /**
     * Create a new Scientist with default stats
     * @param name Display name of the scientist
     */
    public Scientist(String name) {
        super(name, 8, 1, 17);
        this.specialization = "Scientist";
    }

    @Override
    public String getColor() {
        return "Purple";
    }

    @Override
    public int getImageResource() {
        return android.R.drawable.ic_menu_search;
    }

    /**
     * Check if this scientist has a bonus on the given mission type
     * @param missionType Type of mission to check
     * @return true if scientist gets bonus on this mission
     */
    @Override
    public boolean hasBonusOnMission(String missionType) {
        return missionType.toLowerCase().contains("research") ||
                missionType.toLowerCase().contains("anomaly") ||
                missionType.toLowerCase().contains("solar") ||
                missionType.toLowerCase().contains("radiation");
    }

    /**
     * Get bonus amount for applicable missions
     * @param missionType Type of mission
     * @return Bonus value (2 if applicable, 0 otherwise)
     */
    @Override
    public int getMissionBonus(String missionType) {
        return hasBonusOnMission(missionType) ? 2 : 0;
    }

    /**
     * Perform attack with 20% chance of critical hit (+3 damage)
     * @return Damage amount to deal
     */
    @Override
    public int act() {
        int baseDamage = super.act();
        if (Math.random() < 0.2) {
            baseDamage += 3;
        }
        return baseDamage;
    }
}
