package com.example.spacecolony.model;

/**
 * Medic crew member specialization.
 * Excels at medical, biological, and disease-related missions.
 * Can heal other crew members.
 */
public class Medic extends CrewMember {

    /**
     * Create a new Medic with default stats
     * @param name Display name of the medic
     */
    public Medic(String name) {
        super(name, 7, 2, 18);
        this.specialization = "Medic";
    }

    @Override
    public String getColor() {
        return "Green";
    }

    @Override
    public int getImageResource() {
        return android.R.drawable.ic_menu_add;
    }

    /**
     * Check if this medic has a bonus on the given mission type
     * @param missionType Type of mission to check
     * @return true if medic gets bonus on this mission
     */
    @Override
    public boolean hasBonusOnMission(String missionType) {
        return missionType.toLowerCase().contains("medical") ||
                missionType.toLowerCase().contains("biological") ||
                missionType.toLowerCase().contains("plague") ||
                missionType.toLowerCase().contains("disease");
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
     * Heal a target crew member
     * @param target Crew member to heal
     * @return Actual amount healed (may be less than intended if target is near full)
     */
    public int heal(CrewMember target) {
        int healAmount = 5 + experience;
        int actualHeal = Math.min(healAmount, target.maxEnergy - target.energy);
        target.energy = Math.min(target.maxEnergy, target.energy + healAmount);
        return actualHeal;
    }
}
