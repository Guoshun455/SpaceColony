package com.example.spacecolony.model;

/**
 * Soldier crew member specialization.
 * Excels at combat, alien, attack, and invasion-related missions.
 * Has consistent high damage output with small random variance.
 */
public class Soldier extends CrewMember {

    /**
     * Create a new Soldier with default stats
     * @param name Display name of the soldier
     */
    public Soldier(String name) {
        super(name, 9, 0, 16);
        this.specialization = "Soldier";
    }

    @Override
    public String getColor() {
        return "Red";
    }

    @Override
    public int getImageResource() {
        return android.R.drawable.ic_menu_close_clear_cancel;
    }

    /**
     * Check if this soldier has a bonus on the given mission type
     * @param missionType Type of mission to check
     * @return true if soldier gets bonus on this mission
     */
    @Override
    public boolean hasBonusOnMission(String missionType) {
        return missionType.toLowerCase().contains("combat") ||
                missionType.toLowerCase().contains("alien") ||
                missionType.toLowerCase().contains("attack") ||
                missionType.toLowerCase().contains("invasion");
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
     * Perform attack with reduced random variance (0-1 bonus instead of 0-2)
     * @return Damage amount to deal
     */
    @Override
    public int act() {
        return getEffectiveSkill() + (int)(Math.random() * 2);
    }
}