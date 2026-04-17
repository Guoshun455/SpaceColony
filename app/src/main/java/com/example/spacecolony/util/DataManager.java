package com.example.spacecolony.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.example.spacecolony.model.CrewMember;
import com.example.spacecolony.model.Storage;
import com.example.spacecolony.model.Pilot;
import com.example.spacecolony.model.Engineer;
import com.example.spacecolony.model.Medic;
import com.example.spacecolony.model.Scientist;
import com.example.spacecolony.model.Soldier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager for saving and loading game data using SharedPreferences.
 * Handles serialization of crew members and colony statistics.
 */
public class DataManager {
    private static final String PREFS_NAME = "SpaceColonyData";
    private static final String KEY_CREW_DATA = "crew_data";
    private static final String KEY_MISSION_COUNT = "mission_count";
    private static final String KEY_FAILED_MISSION_COUNT = "failed_mission_count";
    private static final String KEY_RECRUITED_COUNT = "recruited_count";
    private static final String KEY_STORAGE_NAME = "storage_name";

    private final SharedPreferences prefs;
    private final Gson gson;

    /**
     * Constructor
     * @param context Application context for accessing SharedPreferences
     */
    public DataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Save all game data including crew members and statistics
     */
    public void saveGameData() {
        Storage storage = Storage.getInstance();
        SharedPreferences.Editor editor = prefs.edit();

        List<CrewMemberData> crewDataList = new ArrayList<>();
        for (CrewMember cm : storage.listAllCrewMembers()) {
            crewDataList.add(new CrewMemberData(cm));
        }

        editor.putString(KEY_CREW_DATA, gson.toJson(crewDataList));
        editor.putInt(KEY_MISSION_COUNT, storage.getCompletedMissions());
        editor.putInt(KEY_FAILED_MISSION_COUNT, storage.getFailedMissions());
        editor.putInt(KEY_RECRUITED_COUNT, storage.getTotalCrewRecruited());
        editor.putString(KEY_STORAGE_NAME, storage.getName());

        editor.apply();
    }

    /**
     * Load saved game data and restore game state
     * @return true if data was successfully loaded, false if no save exists
     */
    public boolean loadGameData() {
        if (!hasSavedData()) return false;

        String crewJson = prefs.getString(KEY_CREW_DATA, null);
        int completedMissions = prefs.getInt(KEY_MISSION_COUNT, 0);
        int failedMissions = prefs.getInt(KEY_FAILED_MISSION_COUNT, 0);
        int totalRecruited = prefs.getInt(KEY_RECRUITED_COUNT, 0);
        String storageName = prefs.getString(KEY_STORAGE_NAME, "Main Colony");

        Storage.resetInstance();
        Storage newStorage = Storage.getInstance();

        if (crewJson != null) {
            Type listType = new TypeToken<List<CrewMemberData>>(){}.getType();
            List<CrewMemberData> crewDataList = gson.fromJson(crewJson, listType);

            if (crewDataList != null) {
                int maxId = 0;
                for (CrewMemberData data : crewDataList) {
                    if (data.id > maxId) maxId = data.id;
                }

                CrewMember.resetIdCounter();
                while (CrewMember.getNumberOfCreated() < maxId) {
                    try {
                        java.lang.reflect.Field field = CrewMember.class.getDeclaredField("idCounter");
                        field.setAccessible(true);
                        field.setInt(null, maxId);
                        break;
                    } catch (Exception e) {
                        new Pilot("Temp").setId(0);
                    }
                }

                for (CrewMemberData data : crewDataList) {
                    CrewMember cm = createCrewMemberFromData(data);
                    if (cm != null) {
                        cm.setId(data.id);
                        restoreCrewStats(cm, data);
                        newStorage.getCrewMap().put(cm.getId(), cm);
                    }
                }
            }
        }

        try {
            java.lang.reflect.Field completedField = Storage.class.getDeclaredField("completedMissions");
            completedField.setAccessible(true);
            completedField.setInt(newStorage, completedMissions);

            java.lang.reflect.Field failedField = Storage.class.getDeclaredField("failedMissions");
            failedField.setAccessible(true);
            failedField.setInt(newStorage, failedMissions);

            java.lang.reflect.Field recruitedField = Storage.class.getDeclaredField("totalCrewRecruited");
            recruitedField.setAccessible(true);
            recruitedField.setInt(newStorage, totalRecruited);
        } catch (Exception e) {
            e.printStackTrace();
            for (int i = 0; i < completedMissions; i++) {
                newStorage.incrementCompletedMissions();
            }
            for (int i = 0; i < failedMissions; i++) {
                newStorage.incrementFailedMissions();
            }
        }

        return true;
    }

    /**
     * Check if saved game data exists
     */
    public boolean hasSavedData() {
        return prefs.contains(KEY_CREW_DATA);
    }

    /**
     * Clear all saved game data
     */
    public void clearSavedData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    /**
     * Create a crew member from saved data
     */
    private CrewMember createCrewMemberFromData(CrewMemberData data) {
        switch (data.specialization) {
            case "Pilot": return new Pilot(data.name);
            case "Engineer": return new Engineer(data.name);
            case "Medic": return new Medic(data.name);
            case "Scientist": return new Scientist(data.name);
            case "Soldier": return new Soldier(data.name);
            default: return null;
        }
    }

    /**
     * Restore all crew member statistics from saved data
     */
    private void restoreCrewStats(CrewMember cm, CrewMemberData data) {
        int expDiff = data.experience - cm.getExperience();
        if (expDiff > 0) {
            for (int i = 0; i < expDiff; i++) cm.gainExperience(1);
        } else if (expDiff < 0) {
            for (int i = 0; i < -expDiff; i++) cm.gainExperience(-1);
        }

        try {
            java.lang.reflect.Field energyField = CrewMember.class.getDeclaredField("energy");
            energyField.setAccessible(true);
            energyField.setInt(cm, data.energy);
        } catch (Exception e) {
            e.printStackTrace();
            if (data.energy < cm.getMaxEnergy()) {
                cm.restoreEnergy();
                int damage = cm.getMaxEnergy() - data.energy;
                cm.defend(0);
            }
        }

        cm.setCurrentLocation(data.currentLocation);
        cm.setInMedbay(data.isInMedbay);

        try {
            java.lang.reflect.Field missionsField = CrewMember.class.getDeclaredField("missionsCompleted");
            missionsField.setAccessible(true);
            missionsField.setInt(cm, data.missionsCompleted);

            java.lang.reflect.Field wonField = CrewMember.class.getDeclaredField("missionsWon");
            wonField.setAccessible(true);
            wonField.setInt(cm, data.missionsWon);

            java.lang.reflect.Field trainingField = CrewMember.class.getDeclaredField("trainingSessions");
            trainingField.setAccessible(true);
            trainingField.setInt(cm, data.trainingSessions);

            java.lang.reflect.Field defeatedField = CrewMember.class.getDeclaredField("timesDefeated");
            defeatedField.setAccessible(true);
            defeatedField.setInt(cm, data.timesDefeated);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Data class for serializing crew member state
     */
    private static class CrewMemberData {
        int id;
        String name;
        String specialization;
        int experience;
        int energy;
        int missionsCompleted;
        int missionsWon;
        int trainingSessions;
        int timesDefeated;
        String currentLocation;
        boolean isInMedbay;

        CrewMemberData(CrewMember cm) {
            this.id = cm.getId();
            this.name = cm.getName();
            this.specialization = cm.getSpecialization();
            this.experience = cm.getExperience();
            this.energy = cm.getEnergy();
            this.missionsCompleted = cm.getMissionsCompleted();
            this.missionsWon = cm.getMissionsWon();
            this.trainingSessions = cm.getTrainingSessions();
            this.timesDefeated = cm.getTimesDefeated();
            this.currentLocation = cm.getCurrentLocation();
            this.isInMedbay = cm.isInMedbay();
        }
    }
}