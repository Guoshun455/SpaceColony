package com.example.spacecolony.model;

/**
 * Engineer crew member specialization.
 * Excels at repair, mechanical, and station maintenance missions.
 */
public class Engineer extends CrewMember {

    /**
     * Create a new Engineer with default stats
     * @param name Display name of the engineer
     */
    public Engineer(String name) {
        super(name, 6, 3, 19);
        this.specialization = "Engineer";
    }

    @Override
    public String getColor() {
        return "Yellow";
    }

    @Override
    public int getImageResource() {
        return android.R.drawable.ic_menu_preferences;
    }

    /**
     * Check if this engineer has a bonus on the given mission type
     * @param missionType Type of mission to check
     * @return true if engineer gets bonus on this mission
     */
    @Override
    public boolean hasBonusOnMission(String missionType) {
        return missionType.toLowerCase().contains("repair") ||
                missionType.toLowerCase().contains("mechanical") ||
                missionType.toLowerCase().contains("station") ||
                missionType.toLowerCase().contains("heating") ||
                missionType.toLowerCase().contains("leakage");
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
