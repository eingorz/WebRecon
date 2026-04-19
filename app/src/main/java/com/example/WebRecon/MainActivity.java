package com.example.WebRecon;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.WebRecon.databinding.ActivityMainBinding;
import com.example.WebRecon.fragment.HistoryFragment;
import com.example.WebRecon.fragment.ReconFragment;
import com.example.WebRecon.fragment.ToolsFragment;
import com.example.WebRecon.util.Prefs;
import com.example.WebRecon.util.WordlistManager;

public class MainActivity extends AppCompatActivity {

    private static final String KEY_SELECTED_TAB = "selected_tab";

    private ActivityMainBinding binding;
    private int selectedTabId = R.id.nav_recon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme();
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        WordlistManager.copyAssetsToInternalStorage(this);

        if (savedInstanceState != null) {
            selectedTabId = savedInstanceState.getInt(KEY_SELECTED_TAB, R.id.nav_recon);
        } else {
            showFragment(selectedTabId);
        }

        binding.bottomNav.setSelectedItemId(selectedTabId);
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == selectedTabId) return true;
            selectedTabId = id;
            showFragment(id);
            return true;
        });
    }

    private void applyTheme() {
        String theme = Prefs.getTheme(this);
        if ("dark".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if ("light".equals(theme)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
    }

    private void showFragment(int tabId) {
        Fragment fragment;
        String tag;
        if (tabId == R.id.nav_tools) {
            fragment = new ToolsFragment();
            tag = "tools";
        } else if (tabId == R.id.nav_history) {
            fragment = new HistoryFragment();
            tag = "history";
        } else {
            fragment = new ReconFragment();
            tag = "recon";
        }

        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        tx.replace(R.id.fragment_container, fragment, tag);
        tx.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_overflow, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_SELECTED_TAB, selectedTabId);
    }
}
