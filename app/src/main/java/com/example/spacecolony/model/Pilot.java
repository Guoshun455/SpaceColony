package com.example.spacecolony.model;

/**
 * Pilot crew member specialization.
 * Excels at asteroid, navigation, and storm-related missions.
 */
public class Pilot extends CrewMember {

    /**
     * Create a new Pilot with default stats
     * @param name Display name of the pilot
     */
    public Pilot(String name) {
        super(name, 5, 4, 20);
        this.specialization = "Pilot";
    }

    @Override
    public String getColor() {
        return "Blue";
    }

    @Override
    public int getImageResource() {
        return android.R.drawable.ic_menu_compass;
    }

    /**
     * Check if this pilot has a bonus on the given mission type
     * @param missionType Type of mission to check
     * @return true if pilot gets bonus on this mission
     */
    @Override
    public boolean hasBonusOnMission(String missionType) {
        return missionType.toLowerCase().contains("asteroid") ||
                missionType.toLowerCase().contains("navigation") ||
                missionType.toLowerCase().contains("storm");
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
}
