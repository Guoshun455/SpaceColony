package com.example.spacecolony;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.example.spacecolony.model.MissionControl;
import com.example.spacecolony.model.Storage;
import com.example.spacecolony.ui.HomeFragment;
import com.example.spacecolony.ui.MissionFragment;
import com.example.spacecolony.ui.QuartersFragment;
import com.example.spacecolony.ui.MedbayFragment;
import com.example.spacecolony.ui.RecruitFragment;
import com.example.spacecolony.ui.SimulatorFragment;
import com.example.spacecolony.ui.StatsFragment;
import com.example.spacecolony.util.DataManager;

/**
 * Main activity managing all fragments and navigation.
 * Handles bottom navigation, fragment switching, and game save/load.
 */
public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnHomeActionListener,
        RecruitFragment.OnRecruitListener,
        MissionFragment.OnMissionStatusListener {

    private BottomNavigationView bottomNav;
    private FragmentManager fragmentManager;
    private DataManager dataManager;

    private HomeFragment homeFragment;
    private QuartersFragment quartersFragment;
    private SimulatorFragment simulatorFragment;
    private MissionFragment missionFragment;
    private StatsFragment statsFragment;
    private RecruitFragment recruitFragment;

    // Flag to track if mission is in progress
    private boolean isMissionInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dataManager = new DataManager(this);

        // Always create new game on startup, do not auto-load save
        initializeGameComponents();

        bottomNav = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();

        setupBottomNavigation();
        initializeFragments();

        showFragment(homeFragment);
    }

    /**
     * Initialize game components with new empty game state
     */
    private void initializeGameComponents() {
        Storage.resetInstance();
        Storage.getInstance();
        // No longer auto-check hasSavedData or auto-load
    }

    /**
     * Setup bottom navigation menu handlers
     */
    private void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            // Block navigation if mission is in progress
            if (isMissionInProgress) {
                Toast.makeText(this, "Mission in progress! Please complete the mission first.", Toast.LENGTH_SHORT).show();
                return false;
            }

            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                showFragment(homeFragment);
                return true;
            } else if (itemId == R.id.nav_quarters) {
                showFragment(quartersFragment);
                return true;
            } else if (itemId == R.id.nav_simulator) {
                showFragment(simulatorFragment);
                return true;
            } else if (itemId == R.id.nav_mission) {
                showFragment(missionFragment);
                return true;
            } else if (itemId == R.id.nav_stats) {
                showFragment(statsFragment);
                return true;
            }

            return false;
        });
    }

    /**
     * Create and initialize all fragment instances
     */
    private void initializeFragments() {
        homeFragment = new HomeFragment();
        homeFragment.setOnHomeActionListener(this);

        quartersFragment = new QuartersFragment();
        simulatorFragment = new SimulatorFragment();
        missionFragment = new MissionFragment();
        missionFragment.setOnMissionStatusListener(this);
        statsFragment = new StatsFragment();

        recruitFragment = new RecruitFragment();
        recruitFragment.setOnRecruitListener(this);
    }

    /**
     * Display the specified fragment
     */
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    @Override
    public void onRecruitClick() {
        showFragment(recruitFragment);
    }

    @Override
    public void onNavigateToQuarters() {
        bottomNav.setSelectedItemId(R.id.nav_quarters);
        showFragment(quartersFragment);
    }

    @Override
    public void onNavigateToSimulator() {
        bottomNav.setSelectedItemId(R.id.nav_simulator);
        showFragment(simulatorFragment);
    }

    @Override
    public void onNavigateToMissionControl() {
        bottomNav.setSelectedItemId(R.id.nav_mission);
        showFragment(missionFragment);
    }

    @Override
    public void onNavigateToMedbay() {
        bottomNav.setSelectedItemId(R.id.nav_quarters);
        showFragment(quartersFragment);
        if (quartersFragment != null) {
            quartersFragment.switchToMedbayTab();
        }
    }

    @Override
    public void onSaveGame() {
        dataManager.saveGameData();
        Toast.makeText(this, "Game saved!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLoadGame() {
        if (dataManager.hasSavedData()) {
            MissionControl.resetInstance();

            boolean loaded = dataManager.loadGameData();
            if (loaded) {
                Toast.makeText(this, "Game loaded!", Toast.LENGTH_SHORT).show();
                initializeFragments();
                if (homeFragment != null) {
                    homeFragment.updateStats();
                }
                bottomNav.setSelectedItemId(R.id.nav_home);
                showFragment(homeFragment);
            } else {
                Toast.makeText(this, "Failed to load game", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No saved game found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCrewCreated() {
        Toast.makeText(this, "Crew member recruited!", Toast.LENGTH_SHORT).show();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (homeFragment != null) {
                homeFragment.updateStats();
            }
            if (missionFragment != null && missionFragment.isVisible()) {
            }
        }, 100);

        bottomNav.setSelectedItemId(R.id.nav_home);
        showFragment(homeFragment);
    }

    @Override
    public void onCancel() {
        bottomNav.setSelectedItemId(R.id.nav_home);
        showFragment(homeFragment);
    }

    /**
     * Callback when mission status changes
     * Implements MissionFragment.OnMissionStatusListener
     */
    @Override
    public void onMissionStatusChanged(boolean inProgress) {
        this.isMissionInProgress = inProgress;
    }
}