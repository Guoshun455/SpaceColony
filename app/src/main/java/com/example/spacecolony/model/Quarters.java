package com.example.spacecolony.model;

/**
 * Manages crew member creation and basic location-based operations.
 * Handles creating new crew members and managing their energy/medbay status.
 */
public class Quarters {
    private Storage storage;

    /**
     * Constructor
     * @param storage Reference to the storage system for crew management
     */
    public Quarters(Storage storage) {
        this.storage = storage;
    }

    /**
     * Create a new crew member with the specified specialization
     * @param name Display name for the crew member
     * @param specialization Job type (Pilot, Engineer, Medic, Scientist, Soldier)
     * @return The newly created crew member
     */
    public CrewMember createCrewMember(String name, String specialization) {
        CrewMember cm;
        switch (specialization) {
            case "Pilot":
                cm = new Pilot(name);
                break;
            case "Engineer":
                cm = new Engineer(name);
                break;
            case "Medic":
                cm = new Medic(name);
                break;
            case "Scientist":
                cm = new Scientist(name);
                break;
            case "Soldier":
                cm = new Soldier(name);
                break;
            default:
                cm = new Pilot(name);
        }

        cm.setCurrentLocation("Quarters");
        storage.addCrewMember(cm);
        return cm;
    }

    /**
     * Fully restore crew member's energy and remove from medbay
     * @param cm Crew member to restore
     */
    public void restoreEnergy(CrewMember cm) {
        cm.restoreEnergy();
        cm.setInMedbay(false);
    }

    /**
     * Send defeated crew member to medbay with half energy
     * @param cm Defeated crew member to recover
     */
    public void sendToMedbay(CrewMember cm) {
        cm.setInMedbay(true);
        cm.setCurrentLocation("Medbay");
        cm.energy = cm.getMaxEnergy() / 2;
        cm.incrementTimesDefeated();
    }
}
