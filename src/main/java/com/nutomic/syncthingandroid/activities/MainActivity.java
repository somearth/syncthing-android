package com.nutomic.syncthingandroid.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBar.TabListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.fragments.DevicesFragment;
import com.nutomic.syncthingandroid.fragments.FoldersFragment;
import com.nutomic.syncthingandroid.fragments.LocalDeviceInfoFragment;
import com.nutomic.syncthingandroid.syncthing.SyncthingService;

/**
 * Shows {@link com.nutomic.syncthingandroid.fragments.FoldersFragment} and {@link com.nutomic.syncthingandroid.fragments.DevicesFragment} in different tabs, and
 * {@link com.nutomic.syncthingandroid.fragments.LocalDeviceInfoFragment} in the navigation drawer.
 */
public class MainActivity extends SyncthingActivity
        implements SyncthingService.OnApiChangeListener {

    private AlertDialog mLoadingDialog;

    private AlertDialog mDisabledDialog;

    /**
     * Causes population of folder and device lists, unlocks info drawer.
     */
    @Override
    @SuppressLint("InflateParams")
    public void onApiChange(SyncthingService.State currentState) {
        if (currentState != SyncthingService.State.ACTIVE && !isFinishing()) {
            if (currentState == SyncthingService.State.DISABLED) {
                if (mLoadingDialog != null) {
                    mLoadingDialog.dismiss();
                }
                mDisabledDialog = SyncthingService.showDisabledDialog(this);
            } else if (mLoadingDialog == null) {
                final SharedPreferences prefs =
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

                LayoutInflater inflater = getLayoutInflater();
                View dialogLayout = inflater.inflate(R.layout.loading_dialog, null);
                TextView loadingText = (TextView) dialogLayout.findViewById(R.id.loading_text);
                loadingText.setText((getService().isFirstStart())
                        ? R.string.web_gui_creating_key
                        : R.string.api_loading);

                mLoadingDialog = new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setView(dialogLayout)
                        .show();

                // Make sure the first start dialog is shown on top.
                if (prefs.getBoolean("first_start", true)) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.welcome_title)
                            .setMessage(R.string.welcome_text)
                            .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    prefs.edit().putBoolean("first_start", false).commit();
                                }
                            })
                            .show();
                }
            }
            return;
        }

        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
        if (mDisabledDialog != null) {
            mDisabledDialog.dismiss();
        }
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private final FragmentPagerAdapter mSectionsPagerAdapter =
            new FragmentPagerAdapter(getSupportFragmentManager()) {

                @Override
                public Fragment getItem(int position) {
                    switch (position) {
                        case 0:
                            return mFolderFragment;
                        case 1:
                            return mDevicesFragment;
                        default:
                            return null;
                    }
                }

                @Override
                public int getCount() {
                    return 2;
                }

            };

    private FoldersFragment mFolderFragment;

    private DevicesFragment mDevicesFragment;

    private LocalDeviceInfoFragment mLocalDeviceInfoFragment;

    private ViewPager mViewPager;

    private ActionBarDrawerToggle mDrawerToggle;

    private DrawerLayout mDrawerLayout;

    /**
     * Initializes tab navigation.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final ActionBar actionBar = getSupportActionBar();

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        setContentView(R.layout.main_activity);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        TabListener tabListener = new TabListener() {
            public void onTabSelected(Tab tab, FragmentTransaction ft) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(Tab tab, FragmentTransaction ft) {
            }

            @Override
            public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            }
        };

        actionBar.addTab(actionBar.newTab()
                .setText(R.string.folders_fragment_title)
                .setTabListener(tabListener));
        actionBar.addTab(actionBar.newTab()
                .setText(R.string.devices_fragment_title)
                .setTabListener(tabListener));

        if (savedInstanceState != null) {
            FragmentManager fm = getSupportFragmentManager();
            mFolderFragment = (FoldersFragment) fm.getFragment(
                    savedInstanceState, FoldersFragment.class.getName());
            mDevicesFragment = (DevicesFragment) fm.getFragment(
                    savedInstanceState, DevicesFragment.class.getName());
            mLocalDeviceInfoFragment = (LocalDeviceInfoFragment) fm.getFragment(
                    savedInstanceState, LocalDeviceInfoFragment.class.getName());
            mViewPager.setCurrentItem(savedInstanceState.getInt("currentTab"));
        } else {
            mFolderFragment = new FoldersFragment();
            mDevicesFragment = new DevicesFragment();
            mLocalDeviceInfoFragment = new LocalDeviceInfoFragment();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.drawer, mLocalDeviceInfoFragment)
                .commit();
        mDrawerToggle = mLocalDeviceInfoFragment.new Toggle(this, mDrawerLayout,
                R.drawable.ic_drawer);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        super.onServiceConnected(componentName, iBinder);
        getService().registerOnApiChangeListener(this);
        getService().registerOnApiChangeListener(mFolderFragment);
        getService().registerOnApiChangeListener(mDevicesFragment);
    }

    /**
     * Saves fragment states.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Avoid crash if called during startup.
        if (mFolderFragment != null && mDevicesFragment != null &&
                mLocalDeviceInfoFragment != null) {
            FragmentManager fm = getSupportFragmentManager();
            fm.putFragment(outState, FoldersFragment.class.getName(), mFolderFragment);
            fm.putFragment(outState, DevicesFragment.class.getName(), mDevicesFragment);
            fm.putFragment(outState, LocalDeviceInfoFragment.class.getName(),
                    mLocalDeviceInfoFragment);
            outState.putInt("currentTab", mViewPager.getCurrentItem());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Shows menu only once syncthing service is running, and shows "share" option only when
     * drawer is open.
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(findViewById(R.id.drawer));
        menu.findItem(R.id.share_device_id).setVisible(drawerOpen);
        menu.findItem(R.id.exit).setVisible(!SyncthingService.alwaysRunInBackground(this));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mLocalDeviceInfoFragment.onOptionsItemSelected(item) ||
                mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case R.id.add_folder:
                Intent intent = new Intent(this, SettingsActivity.class)
                        .setAction(SettingsActivity.ACTION_REPO_SETTINGS_FRAGMENT)
                        .putExtra(SettingsActivity.EXTRA_IS_CREATE, true);
                startActivity(intent);
                return true;
            case R.id.add_device:
                intent = new Intent(this, SettingsActivity.class)
                        .setAction(SettingsActivity.ACTION_NODE_SETTINGS_FRAGMENT)
                        .putExtra(SettingsActivity.EXTRA_IS_CREATE, true);
                startActivity(intent);
                return true;
            case R.id.web_gui:
                startActivity(new Intent(this, WebGuiActivity.class));
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class)
                        .setAction(SettingsActivity.ACTION_APP_SETTINGS_FRAGMENT));
                return true;
            case R.id.exit:
                stopService(new Intent(this, SyncthingService.class));
                finish();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

}