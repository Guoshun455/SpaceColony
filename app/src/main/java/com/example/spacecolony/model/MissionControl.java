package com.example.spacecolony.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton controller for managing mission gameplay.
 * Handles tactical combat, turn management, and mission outcomes.
 */
public class MissionControl {

    private static volatile MissionControl instance;
    private static final Object lock = new Object();

    private List<MissionListener> listeners;
    private Threat currentThreat;
    private List<CrewMember> currentSquad;
    private boolean missionInProgress;
    private StringBuilder missionLog;
    private int currentRound;

    /**
     * Action types available during tactical combat
     */
    public enum ActionType {
        ATTACK, DEFEND, SPECIAL
    }

    /**
     * Interface for receiving mission event callbacks
     */
    public interface MissionListener {
        void onMissionUpdate(String message);
        void onCrewTurn(CrewMember crew, Threat threat);
        void onThreatTurn(CrewMember target, Threat threat);
        void onMissionComplete(boolean success, List<CrewMember> survivors);
        void onCrewDefeated(CrewMember crew);
        void onRoundStart(int round);
    }

    private MissionControl() {
        this.listeners = new ArrayList<>();
        this.currentSquad = new ArrayList<>();
        this.missionLog = new StringBuilder();
        this.currentRound = 0;
    }

    /**
     * Get singleton instance (thread-safe)
     */
    public static MissionControl getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new MissionControl();
                }
            }
        }
        return instance;
    }

    /**
     * Reset singleton instance (for testing or new games)
     */
    public static void resetInstance() {
        synchronized (lock) {
            instance = null;
        }
    }

    /**
     * Set singleton instance (for save/load)
     */
    public static void setInstance(MissionControl mc) {
        synchronized (lock) {
            instance = mc;
        }
    }

    private Storage getStorage() {
        return Storage.getInstance();
    }

    public void addMissionListener(MissionListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeMissionListener(MissionListener listener) {
        listeners.remove(listener);
    }

    public void clearAllListeners() {
        listeners.clear();
    }

    /**
     * Start a new mission with the specified squad
     * @param squad 2-3 crew members for the mission
     */
    public void launchMission(List<CrewMember> squad) {
        if (squad == null || squad.size() < 2 || squad.size() > 3) {
            throw new IllegalArgumentException("Squad must have 2-3 members");
        }

        for (CrewMember cm : squad) {
            if (cm.isDefeated()) {
                throw new IllegalStateException(cm.getName() + " is defeated and cannot join mission");
            }
            if (cm.isInMedbay()) {
                throw new IllegalStateException(cm.getName() + " is in Medbay and cannot join mission");
            }
        }

        this.currentSquad = new ArrayList<>(squad);
        this.missionInProgress = true;
        this.missionLog = new StringBuilder();
        this.currentRound = 1;

        for (CrewMember cm : currentSquad) {
            cm.setDefending(false);
            cm.setActedThisTurn(false);
        }

        int difficulty = getStorage().getCompletedMissions();
        this.currentThreat = new Threat(difficulty);

        for (CrewMember cm : squad) {
            getStorage().moveCrewMember(cm.getId(), "Mission");
        }

        logMessage("=== MISSION START ===");
        logMessage("Threat: " + currentThreat.getName() +
                " (Skill:" + currentThreat.getSkill() +
                " Res:" + currentThreat.getResilience() +
                " Energy:" + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy() + ")");
        logMessage("Squad: " + squad.size() + " members");
        for (CrewMember cm : squad) {
            logMessage("  - " + cm.getName() + " (" + cm.getSpecialization() +
                    ") Energy:" + cm.getEnergy() + "/" + cm.getMaxEnergy() +
                    " Skill:" + cm.getEffectiveSkill());
        }
        logMessage("");

        notifyRoundStart(currentRound);
    }

    /**
     * Execute a crew member's tactical action
     * @param actor The crew member taking action
     * @param action Type of action to perform
     */
    public void executeTacticalTurn(CrewMember actor, ActionType action) {
        if (!missionInProgress || currentThreat == null) {
            return;
        }

        if (currentThreat.isDefeated()) {
            completeMission(true);
            return;
        }

        if (!currentSquad.contains(actor)) {
            logMessage("⚠️ " + actor.getName() + " is not in squad");
            return;
        }

        if (actor.isDefeated()) {
            logMessage("⚠️ " + actor.getName() + " is defeated and cannot act");
            return;
        }

        if (actor.hasActedThisTurn()) {
            logMessage("⚠️ " + actor.getName() + " has already acted this turn");
            return;
        }

        boolean actionCompleted = false;
        switch (action) {
            case ATTACK:
                executeAttack(actor);
                actionCompleted = true;
                break;
            case DEFEND:
                executeDefend(actor);
                actionCompleted = true;
                break;
            case SPECIAL:
                actionCompleted = executeSpecial(actor);
                break;
        }

        if (!actionCompleted) {
            return;
        }

        actor.setActedThisTurn(true);

        if (currentThreat.isDefeated()) {
            completeMission(true);
            return;
        }

        if (!actor.isDefeated()) {
            executeThreatRetaliation(actor);

            if (actor.isDefeated()) {
                handleCrewDefeated(actor);
            }
        }

        logMessage("");

        if (areAllDefeated()) {
            completeMission(false);
            return;
        }

        if (currentThreat.isDefeated()) {
            completeMission(true);
            return;
        }

        if (haveAllActed()) {
            endRound();
        }
    }

    /**
     * Execute threat's counter-attack against a crew member
     */
    private void executeThreatRetaliation(CrewMember target) {
        logMessage("");
        logMessage("--- Threat Retaliates ---");

        int threatDamage = currentThreat.attack(target);

        if (target.isDefending()) {
            threatDamage = Math.max(1, threatDamage - 2);
            logMessage("   🛡️ " + target.getName() + "'s defense reduces damage by 2!");
        }

        int actualDamage = target.defend(threatDamage);
        logMessage("👾 " + currentThreat.getName() + " attacks " + target.getName() + "!");
        logMessage("   Damage: " + threatDamage + " - " + target.getResilience() + " = " + actualDamage);
        logMessage("   " + target.getName() + " Energy: " + target.getEnergy() + "/" + target.getMaxEnergy());

        notifyThreatTurn(target, currentThreat);
    }

    /**
     * Check if all surviving crew members have acted this round
     */
    private boolean haveAllActed() {
        for (CrewMember cm : currentSquad) {
            if (!cm.isDefeated() && !cm.hasActedThisTurn()) {
                return false;
            }
        }
        return true;
    }

    /**
     * End current round and start next round
     */
    public void endRound() {
        if (!missionInProgress) return;

        currentRound++;
        logMessage("");
        logMessage("=== ROUND " + currentRound + " ===");

        boolean hasAvailableCrew = false;
        for (CrewMember cm : currentSquad) {
            if (!cm.isDefeated() && !cm.isInMedbay()) {
                cm.setActedThisTurn(false);
                cm.setDefending(false);
                hasAvailableCrew = true;
            }
        }

        if (!hasAvailableCrew) {
            logMessage("❌ No available crew members!");
            completeMission(false);
            return;
        }

        notifyRoundStart(currentRound);
    }

    /**
     * Execute attack action
     */
    private void executeAttack(CrewMember actor) {
        int attackPower = actor.getEffectiveSkill();

        if (actor.hasBonusOnMission(currentThreat.getType())) {
            int bonus = actor.getMissionBonus(currentThreat.getType());
            attackPower += bonus;
            logMessage("⭐ " + actor.getName() + " uses specialization advantage! +" + bonus + " damage");
        }

        int randomBonus = (int)(Math.random() * 3);
        attackPower += randomBonus;

        int damageDealt = currentThreat.defend(attackPower);
        logMessage("⚔️ " + actor.getName() + " attacks " + currentThreat.getName() + "!");
        if (randomBonus > 0) {
            logMessage("   🎲 Random bonus: +" + randomBonus);
        }
        logMessage("   Damage: " + attackPower + " - " + currentThreat.getResilience() + " = " + damageDealt);
        logMessage("   Threat Energy: " + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy());

        notifyCrewTurn(actor, currentThreat);
    }

    /**
     * Execute defend action
     */
    private void executeDefend(CrewMember actor) {
        actor.setDefending(true);
        logMessage("🛡️ " + actor.getName() + " takes defensive stance!");
        logMessage("   (Damage will be reduced by 2)");

        notifyCrewTurn(actor, currentThreat);
    }

    /**
     * Execute special ability based on crew specialization
     * @return true if special was successfully executed
     */
    private boolean executeSpecial(CrewMember actor) {
        if (actor instanceof Medic) {
            CrewMember lowest = findLowestHealthAlly(actor);
            if (lowest != null) {
                int healAmount = ((Medic) actor).heal(lowest);
                logMessage("💚 " + actor.getName() + " heals " + lowest.getName() + " for " + healAmount + " Energy!");
                logMessage("   " + lowest.getName() + " Energy: " + lowest.getEnergy() + "/" + lowest.getMaxEnergy());
                notifyCrewTurn(actor, currentThreat);
                return true;
            } else {
                logMessage("💚 " + actor.getName() + " finds no one needs healing, attacks instead!");
                executeAttack(actor);
                return true;
            }
        } else if (actor instanceof Engineer) {
            logMessage("🔧 " + actor.getName() + " reinforces team armor!");
            for (CrewMember cm : currentSquad) {
                if (cm != actor && !cm.isDefeated()) {
                    cm.setDefending(true);
                }
            }
            notifyCrewTurn(actor, currentThreat);
            return true;
        } else if (actor instanceof Scientist) {
            int specialDamage = actor.getEffectiveSkill() + 5;
            int damageDealt = currentThreat.defend(specialDamage);
            logMessage("🔬 " + actor.getName() + " analyzes weak point!");
            logMessage("   Critical Damage: " + specialDamage + " - " + currentThreat.getResilience() + " = " + damageDealt);
            logMessage("   Threat Energy: " + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy());
            notifyCrewTurn(actor, currentThreat);
            return true;
        } else if (actor instanceof Pilot) {
            int specialDamage = actor.getEffectiveSkill() + 3;
            int damageDealt = currentThreat.defend(specialDamage);
            logMessage("🚀 " + actor.getName() + " performs precision strike!");
            logMessage("   Damage: " + specialDamage + " - " + currentThreat.getResilience() + " = " + damageDealt);
            logMessage("   Threat Energy: " + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy());
            notifyCrewTurn(actor, currentThreat);
            return true;
        } else if (actor instanceof Soldier) {
            int specialDamage = actor.getEffectiveSkill() + 4;
            int damageDealt = currentThreat.defend(specialDamage);
            logMessage("💥 " + actor.getName() + " unleashes heavy assault!");
            logMessage("   Damage: " + specialDamage + " - " + currentThreat.getResilience() + " = " + damageDealt);
            logMessage("   Threat Energy: " + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy());
            notifyCrewTurn(actor, currentThreat);
            return true;
        } else {
            int specialDamage = actor.getEffectiveSkill() + 3;
            int damageDealt = currentThreat.defend(specialDamage);
            logMessage("💥 " + actor.getName() + " uses SPECIAL ATTACK!");
            logMessage("   Damage: " + specialDamage + " - " + currentThreat.getResilience() + " = " + damageDealt);
            notifyCrewTurn(actor, currentThreat);
            return true;
        }
    }

    /**
     * Find ally with the lowest health percentage for Medic healing
     */
    private CrewMember findLowestHealthAlly(CrewMember exclude) {
        CrewMember lowest = null;
        int minHealthPercent = Integer.MAX_VALUE;
        for (CrewMember cm : currentSquad) {
            if (cm != exclude && !cm.isDefeated()) {
                int healthPercent = (cm.getEnergy() * 100) / cm.getMaxEnergy();
                if (healthPercent < minHealthPercent) {
                    minHealthPercent = healthPercent;
                    lowest = cm;
                }
            }
        }
        return lowest;
    }

    /**
     * Handle crew member defeat - send to medbay with half energy
     */
    private void handleCrewDefeated(CrewMember cm) {
        logMessage("💀 " + cm.getName() + " has been defeated!");
        notifyCrewDefeated(cm);

        getStorage().moveCrewMember(cm.getId(), "Medbay");
        cm.setInMedbay(true);
        cm.incrementTimesDefeated();

        int halfEnergy = cm.getMaxEnergy() / 2;
        try {
            java.lang.reflect.Field energyField = CrewMember.class.getDeclaredField("energy");
            energyField.setAccessible(true);
            energyField.setInt(cm, halfEnergy);
        } catch (Exception e) {
            int currentEnergy = cm.getEnergy();
            if (currentEnergy > halfEnergy) {
                int damage = currentEnergy - halfEnergy;
                for (int i = 0; i < damage; i++) {
                    cm.defend(1);
                }
            }
        }

        logMessage("🏥 " + cm.getName() + " sent to Medbay for recovery");
    }

    /**
     * Check if all squad members are defeated
     */
    private boolean areAllDefeated() {
        for (CrewMember cm : currentSquad) {
            if (!cm.isDefeated()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Complete the mission and distribute rewards/penalties
     */
    private void completeMission(boolean success) {
        missionInProgress = false;

        if (success) {
            logMessage("");
            logMessage("=== MISSION COMPLETE ===");
            logMessage("✅ Threat neutralized!");
            logMessage("");

            List<CrewMember> survivors = getSurvivors();

            for (CrewMember cm : currentSquad) {
                if (cm.isInMedbay() || "Medbay".equals(cm.getCurrentLocation())) {
                    int halfEnergy = cm.getMaxEnergy() / 2;
                    if (cm.getEnergy() != halfEnergy) {
                        cm.setEnergy(halfEnergy);
                    }
                    logMessage("💀 " + cm.getName() + " is in Medbay recovering");
                } else {
                    cm.gainExperience(1);
                    cm.incrementMissionsCompleted();
                    cm.incrementMissionsWon();

                    getStorage().moveCrewMember(cm.getId(), "Mission");
                    cm.setInMedbay(false);
                    logMessage("⭐ " + cm.getName() + " gains 1 XP (Total: " +
                            cm.getExperience() + ", Effective Skill: " + cm.getEffectiveSkill() + ")");
                }
            }

            getStorage().incrementCompletedMissions();
            notifyMissionComplete(true, survivors);

        } else {
            logMessage("");
            logMessage("=== MISSION FAILED ===");
            logMessage("❌ All crew members defeated!");
            logMessage("");

            for (CrewMember cm : currentSquad) {
                cm.incrementMissionsCompleted();

                if (cm.isDefeated() || cm.isInMedbay()) {
                    if (!"Medbay".equals(cm.getCurrentLocation())) {
                        getStorage().moveCrewMember(cm.getId(), "Medbay");
                        cm.setInMedbay(true);
                    }
                    logMessage("💀 " + cm.getName() + " requires medical attention");
                }
            }

            getStorage().incrementFailedMissions();
            notifyMissionComplete(false, new ArrayList<>());
        }

        currentThreat = null;
        currentSquad.clear();
        currentRound = 0;
    }

    /**
     * Execute automatic mission simulation (runs in background thread)
     */
    public void executeAutoMission(List<CrewMember> squad, AutoMissionCallback callback) {
        new Thread(() -> {
            currentSquad = new ArrayList<>(squad);
            missionInProgress = true;
            missionLog = new StringBuilder();
            currentRound = 1;

            for (CrewMember cm : currentSquad) {
                cm.setDefending(false);
                cm.setActedThisTurn(false);
            }

            int difficulty = getStorage().getCompletedMissions();
            currentThreat = new Threat(difficulty);

            for (CrewMember cm : squad) {
                getStorage().moveCrewMember(cm.getId(), "Mission");
            }

            logMessage("=== AUTO MISSION START ===");
            logMessage("Threat: " + currentThreat.getName() +
                    " (Skill:" + currentThreat.getSkill() +
                    " Res:" + currentThreat.getResilience() +
                    " Energy:" + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy() + ")");
            logMessage("Squad: " + squad.size() + " members");
            for (CrewMember cm : squad) {
                logMessage("  - " + cm.getName() + " (" + cm.getSpecialization() +
                        ") Energy:" + cm.getEnergy() + "/" + cm.getMaxEnergy() +
                        " Skill:" + cm.getEffectiveSkill());
            }
            logMessage("");

            while (missionInProgress && currentThreat != null && !currentThreat.isDefeated()) {
                for (CrewMember cm : currentSquad) {
                    if (!missionInProgress || currentThreat == null || currentThreat.isDefeated()) {
                        break;
                    }

                    if (cm.isDefeated() || cm.isInMedbay()) continue;

                    ActionType action;
                    if (cm.getEnergy() < cm.getMaxEnergy() * 0.3) {
                        action = ActionType.DEFEND;
                        cm.setDefending(true);
                        logMessage("🛡️ " + cm.getName() + " defends!");
                    } else {
                        action = ActionType.ATTACK;
                        int attackPower = cm.getEffectiveSkill();
                        int randomBonus = (int)(Math.random() * 3);
                        attackPower += randomBonus;
                        int actualDamage = currentThreat.defend(attackPower);
                        logMessage("⚔️ " + cm.getName() + " attacks for " + actualDamage + " damage!");
                        if (randomBonus > 0) {
                            logMessage("   🎲 Random bonus: +" + randomBonus);
                        }
                        logMessage("   Threat Energy: " + currentThreat.getEnergy() + "/" + currentThreat.getMaxEnergy());
                    }

                    cm.setActedThisTurn(true);

                    if (currentThreat == null || currentThreat.isDefeated()) {
                        break;
                    }

                    if (!cm.isDefeated()) {
                        int threatDamage = currentThreat.attack(cm);

                        if (cm.isDefending()) {
                            threatDamage = Math.max(1, threatDamage - 2);
                            logMessage("   🛡️ " + cm.getName() + "'s defense reduces damage by 2!");
                        }

                        int actualDamage = cm.defend(threatDamage);
                        logMessage("👾 " + currentThreat.getName() + " retaliates against " + cm.getName() + "!");
                        logMessage("   Damage: " + threatDamage + " - " + cm.getResilience() + " = " + actualDamage);
                        logMessage("   " + cm.getName() + " Energy: " + cm.getEnergy() + "/" + cm.getMaxEnergy());

                        if (cm.isDefeated()) {
                            handleCrewDefeated(cm);
                        }
                    }

                    logMessage("");

                    if (!missionInProgress) {
                        break;
                    }

                    boolean allDefeated = true;
                    for (CrewMember check : currentSquad) {
                        if (!check.isDefeated() && !check.isInMedbay()) {
                            allDefeated = false;
                            break;
                        }
                    }

                    if (allDefeated) {
                        completeMission(false);
                        break;
                    }
                }

                if (!missionInProgress || currentThreat == null) {
                    break;
                }

                if (currentThreat.isDefeated()) {
                    completeMission(true);
                    break;
                }

                if (!missionInProgress) {
                    break;
                }

                boolean allDefeated = true;
                for (CrewMember cm : currentSquad) {
                    if (!cm.isDefeated() && !cm.isInMedbay()) {
                        allDefeated = false;
                        break;
                    }
                }

                if (allDefeated) {
                    completeMission(false);
                    break;
                }

                if (!missionInProgress) {
                    break;
                }

                currentRound++;
                logMessage("");
                logMessage("=== ROUND " + currentRound + " ===");
                for (CrewMember cm : currentSquad) {
                    cm.setActedThisTurn(false);
                    cm.setDefending(false);
                }
            }

            if (callback != null) {
                callback.onComplete(!missionInProgress, getSurvivors());
            }
        }).start();
    }

    /**
     * Callback interface for auto mission completion
     */
    public interface AutoMissionCallback {
        void onComplete(boolean success, List<CrewMember> survivors);
    }

    /**
     * Get list of surviving crew members
     */
    private List<CrewMember> getSurvivors() {
        List<CrewMember> survivors = new ArrayList<>();
        for (CrewMember cm : currentSquad) {
            if (!cm.isDefeated()) {
                survivors.add(cm);
            }
        }
        return survivors;
    }

    /**
     * Add message to mission log and notify listeners
     */
    private void logMessage(String message) {
        missionLog.append(message).append("\n");
        notifyMissionUpdate(message);
    }

    /**
     * Notify all listeners of mission update
     */
    private void notifyMissionUpdate(String message) {
        List<MissionListener> listenersCopy = new ArrayList<>(listeners);
        for (MissionListener listener : listenersCopy) {
            try {
                listener.onMissionUpdate(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify all listeners of crew turn
     */
    private void notifyCrewTurn(CrewMember crew, Threat threat) {
        List<MissionListener> listenersCopy = new ArrayList<>(listeners);
        for (MissionListener listener : listenersCopy) {
            try {
                listener.onCrewTurn(crew, threat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify all listeners of threat turn
     */
    private void notifyThreatTurn(CrewMember target, Threat threat) {
        List<MissionListener> listenersCopy = new ArrayList<>(listeners);
        for (MissionListener listener : listenersCopy) {
            try {
                listener.onThreatTurn(target, threat);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify all listeners of mission completion
     */
    private void notifyMissionComplete(boolean success, List<CrewMember> survivors) {
        List<MissionListener> listenersCopy = new ArrayList<>(listeners);
        for (MissionListener listener : listenersCopy) {
            try {
                listener.onMissionComplete(success, survivors);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify all listeners of crew defeat
     */
    private void notifyCrewDefeated(CrewMember crew) {
        List<MissionListener> listenersCopy = new ArrayList<>(listeners);
        for (MissionListener listener : listenersCopy) {
            try {
                listener.onCrewDefeated(crew);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify all listeners of round start
     */
    private void notifyRoundStart(int round) {
        List<MissionListener> listenersCopy = new ArrayList<>(listeners);
        for (MissionListener listener : listenersCopy) {
            try {
                listener.onRoundStart(round);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Getters
    public boolean isMissionInProgress() {
        return missionInProgress;
    }

    public Threat getCurrentThreat() {
        return currentThreat;
    }

    public List<CrewMember> getCurrentSquad() {
        return new ArrayList<>(currentSquad);
    }

    public String getMissionLog() {
        return missionLog.toString();
    }

    public int getCurrentRound() {
        return currentRound;
    }
}