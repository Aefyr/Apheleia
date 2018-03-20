package com.aefyr.apheleia;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aefyr.apheleia.adapters.LandscapeModeSideMenuListViewAdapter;
import com.aefyr.apheleia.fcm.NotificationsHelper;
import com.aefyr.apheleia.fragments.DiaryFragment;
import com.aefyr.apheleia.fragments.FinalsFragment;
import com.aefyr.apheleia.fragments.MarksFragment;
import com.aefyr.apheleia.fragments.MessagesFragment;
import com.aefyr.apheleia.fragments.ScheduleFragment;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Destroyer;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.PeriodsHelper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.TheInitializer;
import com.aefyr.apheleia.helpers.Tutorial;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.watcher.WatcherHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{


    private Helper helper;
    private FragmentManager fragmentManager;
    private DrawerLayout drawer;
    private static final String TAG = "Apheleia/Main";
    private MessagesBroadcastReceiver messagesBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name), BitmapFactory.decodeResource(getResources(), R.mipmap.icon), getResources().getColor(R.color.colorRecentsTab)));

        Intent intent = getIntent();
        if (intent.getExtras() != null) {
            if(intent.getStringExtra("message_dup")!=null){
                String notificationMessage = intent.getStringExtra("message_dup");
                Log.d(TAG, "Showing notification from FCM: " + notificationMessage);
                NotificationsHelper.showDevAlert(this, notificationMessage);
            }
        }
        messagesBroadcastReceiver = new MessagesBroadcastReceiver();
        registerReceiver(messagesBroadcastReceiver, new IntentFilter(FirebaseConstants.INTENT_ACTION_GOT_FCM_MESSAGE));

        helper = Helper.getInstance(this);
        if (!helper.isLoggedIn() || helper.isTokenExpired()) {
            LoginActivity.startFromActivity(this, helper.isTokenExpired() && helper.isLoggedIn() ? LoginActivity.Reason.TOKEN_EXPIRED : null);
            return;
        }

        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("first_launch", true)) {
            WatcherHelper.showPrompt(this);
            Tutorial.showTutorial(this);
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("first_launch", false).apply();
        }


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Preparing UI
        NavigationView navigationView = null;
        LandscapeModeSideMenuListViewAdapter sideMenuListViewAdapter = null;
        if(!lidlIsTabletMode()) {

            drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close).syncState();

            navigationView = (NavigationView) findViewById(R.id.nav_view);
            navigationView.setNavigationItemSelectedListener(this);

            initializeNavHeader(navigationView.getHeaderView(0));
        }else {
            ListView listView = findViewById(R.id.landscapeSideMenu);
            sideMenuListViewAdapter = new LandscapeModeSideMenuListViewAdapter(listView, new LandscapeModeSideMenuListViewAdapter.OnSideMenuInteractionListener() {
                @Override
                public void onApheleiaFragmentSelected(String fragment) {
                    setFragment(fragment);
                }

                @Override
                public void onSettingsClick() {
                    startActivity(new Intent(MainActivity.this, PreferencesActivity.class));
                }

                @Override
                public void onLogoutClick() {
                    logout();
                }
            });
            listView.setAdapter(sideMenuListViewAdapter);
            View header = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.nav_header_main, listView, false);
            listView.addHeaderView(header);
            initializeNavHeader(header);
        }

        fragmentManager = getSupportFragmentManager();

        //Dealing with instance state
        if(savedInstanceState==null) {
            if (getIntent().getStringExtra("requested_fragment") != null) {
                if (getIntent().getStringExtra("requested_fragment").equals("messages")) {
                    currentFragment = new MessagesFragment();
                    currentApheleiaFragment = FRAGMENT_MESSAGES;
                    fragmentManager.beginTransaction().add(R.id.fragmentContainer, currentFragment, FRAGMENT_MESSAGES).commit();

                    if (navigationView != null)
                        navigationView.setCheckedItem(R.id.nav_messages);
                    else
                        sideMenuListViewAdapter.lidlSelect(currentApheleiaFragment);
                }
            } else {
                currentFragment = new DiaryFragment();
                currentApheleiaFragment = FRAGMENT_DIARY;
                fragmentManager.beginTransaction().add(R.id.fragmentContainer, currentFragment, FRAGMENT_DIARY).commit();

                if (navigationView != null)
                    navigationView.setCheckedItem(R.id.nav_diary);
                else
                    sideMenuListViewAdapter.lidlSelect(currentApheleiaFragment);
            }

            checkPeriods(false);
        }else {
            currentApheleiaFragment = savedInstanceState.getString("currentFragment", FRAGMENT_DIARY);
            currentFragment = fragmentManager.findFragmentByTag(currentApheleiaFragment);
            if(!lidlIsTabletMode()){
                switch (currentApheleiaFragment){
                    case FRAGMENT_DIARY:
                        navigationView.setCheckedItem(R.id.nav_diary);
                        break;
                    case FRAGMENT_MARKS:
                        navigationView.setCheckedItem(R.id.nav_marks);
                        break;
                    case FRAGMENT_FINALS:
                        navigationView.setCheckedItem(R.id.nav_finals);
                        break;
                    case FRAGMENT_SCHEDULE:
                        navigationView.setCheckedItem(R.id.nav_schedule);
                        break;
                    case FRAGMENT_MESSAGES:
                        navigationView.setCheckedItem(R.id.nav_messages);
                        break;
                }
            }else {
                sideMenuListViewAdapter.lidlSelect(currentApheleiaFragment);
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private MenuItem timePeriodSwitchButton;
    private MenuItem mailFolderSwitchButton;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        timePeriodSwitchButton = menu.findItem(R.id.action_time_period_switcher);
        mailFolderSwitchButton = menu.findItem(R.id.action_mail_folder_switch);

        mailFolderSwitchButton.setVisible(currentApheleiaFragment.equals(FRAGMENT_MESSAGES));
        timePeriodSwitchButton.setVisible(currentApheleiaFragment.equals(FRAGMENT_DIARY)||currentApheleiaFragment.equals(FRAGMENT_MARKS)||currentApheleiaFragment.equals(FRAGMENT_SCHEDULE));

        if(currentApheleiaFragment.equals(FRAGMENT_MESSAGES)){
            if (((MessagesFragment) currentFragment).isInboxSelected())
                mailFolderSwitchButton.setIcon(R.drawable.ic_send_white_36dp);
            else
                mailFolderSwitchButton.setIcon(R.drawable.ic_inbox_white_36dp);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_refresh:

                ((ActionListener) currentFragment).onAction(ActionListener.Action.UPDATE_REQUESTED);
                return true;
            case R.id.action_check_periods:

                checkPeriods(true);
                return true;
            case R.id.action_mail_folder_switch:

                if (currentApheleiaFragment.equals(FRAGMENT_MESSAGES)) {
                    ((MessagesFragment) currentFragment).toggleFolder();
                    if (((MessagesFragment) currentFragment).isInboxSelected())
                        mailFolderSwitchButton.setIcon(R.drawable.ic_send_white_36dp);
                    else
                        mailFolderSwitchButton.setIcon(R.drawable.ic_inbox_white_36dp);

                    return true;
                }
                return false;
            case R.id.action_time_period_switcher:

                switch (currentApheleiaFragment) {
                    case FRAGMENT_DIARY:
                        ((DiaryFragment) currentFragment).showTimePeriodSwitcherDialog();
                        break;
                    case FRAGMENT_MARKS:
                        ((MarksFragment) currentFragment).showTimePeriodSwitcherDialog();
                        break;
                    case FRAGMENT_SCHEDULE:
                        ((ScheduleFragment) currentFragment).showTimePeriodSwitcherDialog();
                        break;
                }
                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean doNotClose = false;

        if (id == R.id.nav_diary) {
            setFragment(FRAGMENT_DIARY);
        } else if (id == R.id.nav_marks) {
            setFragment(FRAGMENT_MARKS);

        } else if (id == R.id.nav_finals) {
            setFragment(FRAGMENT_FINALS);
        } else if (id == R.id.nav_schedule) {
            setFragment(FRAGMENT_SCHEDULE);

        } else if (id == R.id.nav_messages) {
            setFragment(FRAGMENT_MESSAGES);

        } else if (id == R.id.nav_logout) {
            logout();
            doNotClose = true;
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, PreferencesActivity.class));
        }

        if (!doNotClose) {
            drawer.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private boolean lidlIsTabletMode(){
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && (((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) || ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE));
    }

    public static final String FRAGMENT_DIARY = "diary";
    public static final String FRAGMENT_MARKS = "marks";
    public static final String FRAGMENT_SCHEDULE = "schedule";
    public static final String FRAGMENT_MESSAGES = "messages";
    public static final String FRAGMENT_FINALS = "finals";


    private Fragment currentFragment;
    private static String currentApheleiaFragment = FRAGMENT_DIARY;

    private void setFragment(String fragment) {
        if (fragment.equals(currentApheleiaFragment))
            return;

        currentApheleiaFragment = fragment;

        FragmentTransaction transaction = fragmentManager.beginTransaction().hide(currentFragment).setCustomAnimations(R.anim.frag_enter, R.anim.frag_exit);
        switch (fragment) {
            case FRAGMENT_DIARY:
                Fragment diary = fragmentManager.findFragmentByTag(FRAGMENT_DIARY);
                if (diary == null) {
                    diary = new DiaryFragment();
                    transaction.add(R.id.fragmentContainer, diary, FRAGMENT_DIARY);
                } else {
                    transaction.show(diary);
                }
                currentFragment = diary;
                timePeriodSwitchButton.setVisible(true);
                mailFolderSwitchButton.setVisible(false);
                break;
            case FRAGMENT_MARKS:
                Fragment marks = fragmentManager.findFragmentByTag(FRAGMENT_MARKS);
                if (marks == null) {
                    marks = new MarksFragment();
                    transaction.add(R.id.fragmentContainer, marks, FRAGMENT_MARKS);
                } else {
                    transaction.show(marks);
                }
                currentFragment = marks;
                timePeriodSwitchButton.setVisible(true);
                mailFolderSwitchButton.setVisible(false);
                break;
            case FRAGMENT_SCHEDULE:
                Fragment schedule = fragmentManager.findFragmentByTag(FRAGMENT_SCHEDULE);
                if (schedule == null) {
                    schedule = new ScheduleFragment();
                    transaction.add(R.id.fragmentContainer, schedule, FRAGMENT_SCHEDULE);
                } else {
                    transaction.show(schedule);
                }
                currentFragment = schedule;
                timePeriodSwitchButton.setVisible(true);
                mailFolderSwitchButton.setVisible(false);
                break;
            case FRAGMENT_MESSAGES:
                Fragment messages = fragmentManager.findFragmentByTag(FRAGMENT_MESSAGES);
                if (messages == null) {
                    messages = new MessagesFragment();
                    transaction.add(R.id.fragmentContainer, messages, FRAGMENT_MESSAGES);
                } else {
                    transaction.show(messages);
                }
                currentFragment = messages;
                timePeriodSwitchButton.setVisible(false);
                mailFolderSwitchButton.setVisible(true);
                break;
            case FRAGMENT_FINALS:
                Fragment finals = fragmentManager.findFragmentByTag(FRAGMENT_FINALS);
                if (finals == null) {
                    finals = new FinalsFragment();
                    transaction.add(R.id.fragmentContainer, finals, FRAGMENT_FINALS);
                } else {
                    transaction.show(finals);
                }
                currentFragment = finals;
                timePeriodSwitchButton.setVisible(false);
                mailFolderSwitchButton.setVisible(false);
                break;
        }

        transaction.commit();


    }

    private TextView studentName;
    private AlertDialog studentPickerDialog;
    private int debugModeCounter = 0;

    private void initializeNavHeader(View navHeader) {
        final ProfileHelper profileHelper = ProfileHelper.getInstance(this);
        boolean parent = profileHelper.getRole().equals(ProfileHelper.Role.PARENT);

        ((TextView) navHeader.findViewById(R.id.usernameText)).setText(profileHelper.getName());
        ((TextView) navHeader.findViewById(R.id.emailText)).setText(profileHelper.getEmail());
        ImageView roleIcon = (ImageView) navHeader.findViewById(R.id.roleIcon);
        roleIcon.setImageResource(parent ? R.drawable.ic_business_center_white_24dp : R.drawable.ic_school_white_24dp);
        ((TextView) navHeader.findViewById(R.id.domainText)).setText(helper.getDomain());

        studentName = (TextView) navHeader.findViewById(R.id.studentNameText);

        if (profileHelper.getStudentsCount() == 1) {
            studentName.setVisibility(View.GONE);
            navHeader.findViewById(R.id.switchStudentButton).setVisibility(View.GONE);
            navHeader.findViewById(R.id.currentStudentStaticText).setVisibility(View.GONE);
        } else {
            studentName.setText(profileHelper.getStudentName(profileHelper.getCurrentStudentId()) + " (" + profileHelper.getStudentClass(profileHelper.getCurrentStudentId()) + ")");

            final String[] studentsIds = profileHelper.getStudentsIds().toArray(new String[]{});
            final String[] studentsNames = new String[studentsIds.length];
            int i = 0;
            for (String id : studentsIds) {
                studentsNames[i++] = profileHelper.getStudentName(id) + " (" + profileHelper.getStudentClass(id) + ")";
            }

            studentPickerDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.pick_student)).setSingleChoiceItems(studentsNames, Arrays.asList(studentsIds).indexOf(profileHelper.getCurrentStudentId()), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    studentPickerDialog.dismiss();
                    if (studentsIds[i].equals(profileHelper.getCurrentStudentId())) {
                        if(drawer!=null)
                            drawer.closeDrawer(GravityCompat.START);
                        return;
                    }
                    studentName.setText(studentsNames[i]);
                    profileHelper.setCurrentStudent(studentsIds[i]);
                    studentSwitched();

                    if(drawer!=null)
                        drawer.closeDrawer(GravityCompat.START);
                }
            }).setNegativeButton(getString(R.string.cancel), null).create();

            navHeader.findViewById(R.id.switchStudentButton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    studentPickerDialog.show();
                }
            });
        }

        //Debug mode switching
        roleIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                debugModeCounter++;
            }
        });
        roleIcon.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(debugModeCounter<7)
                    return true;

                new AlertDialog.Builder(MainActivity.this).setMessage("Выберите состояние дебаг-режима\n\nНе рекомендуется трогать это меню, если вы не разработчик данной программы, так как это нарушит нормальную работу приложения :/").setNegativeButton("Выключен", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("debug_mode", false).apply();
                        Chief.makeAToast(MainActivity.this,  getString(R.string.quick_picker_warn));
                    }
                }).setPositiveButton("Включен", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("debug_mode", true).apply();
                    }
                }).create().show();
                return true;
            }
        });
    }

    private void studentSwitched() {
        ((ActionListener) currentFragment).onAction(ActionListener.Action.STUDENT_SWITCHED);
    }

    private void checkPeriods(final boolean requestedByUser) {
        PeriodsHelper.getInstance(this).checkPeriods(new PeriodsHelper.OnPeriodsChangeDetectedListener() {
            @Override
            public void OnFoundMoreWeeks() {
                Chief.makeAToast(MainActivity.this, getString(R.string.check_periods_updated));
                studentSwitched();
                destroyAllFragmentsExceptCurrent();
            }

            @Override
            public void OnFoundMorePeriods() {
                Chief.makeAToast(MainActivity.this, getString(R.string.check_periods_updated));
                studentSwitched();
                destroyAllFragmentsExceptCurrent();
            }

            @Override
            public void OnFoundLessWeeks() {
                showReinitializePrompt();
            }

            @Override
            public void OnFoundLessPeriods() {
                showReinitializePrompt();
            }

            @Override
            public void onNothingChanged() {
                if (requestedByUser)
                    Chief.makeAToast(MainActivity.this, getString(R.string.check_periods_no_change));
            }

            @Override
            public void onNetworkError() {
                if (requestedByUser)
                    Chief.makeAToast(MainActivity.this, getString(R.string.check_periods_error));
            }
        });
    }

    private void destroyAllFragmentsExceptCurrent(){
        List<Fragment> fragments = fragmentManager.getFragments();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        for(Fragment fragment: fragments){
            if(fragment!=currentFragment)
                transaction.remove(fragment);
        }

        //TODO Prolly change this to commitAllowingStateLoss
        transaction.commit();
    }

    private void showReinitializePrompt() {
        new AlertDialog.Builder(this).setTitle(getString(R.string.reinitialization_title)).setMessage(getString(R.string.reinitialization_prompt)).setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.reinitialization_title)).setMessage(getString(R.string.reinitialization_warning)).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        reInitialize();
                    }
                }).setNegativeButton(getString(R.string.cancel), null).create().show();
            }
        }).setNegativeButton(getString(R.string.no), null).create().show();
    }

    private void reInitialize() {
        helper.setLoggedIn(false);
        TheInitializer initializer = new TheInitializer(this, new TheInitializer.OnInitializationListener() {
            @Override
            public void OnSuccess() {
                studentSwitched();
                helper.setLoggedIn(true);
                destroyAllFragmentsExceptCurrent();
            }

            @Override
            public void OnError(String m) {
                AlertDialog d = new AlertDialog.Builder(MainActivity.this).setTitle(getString(R.string.network_error)).setMessage(getString(R.string.reinitialize_failed)).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).setCancelable(false).create();
                d.setCanceledOnTouchOutside(false);
                d.show();
            }

        });
        initializer.initialize();
    }

    private void logout() {
        new AlertDialog.Builder(this).setMessage(getString(R.string.logout_prompt)).setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final Bundle domain = new Bundle();
                domain.putString(FirebaseConstants.SCHOOL_DOMAIN, helper.getDomain());
                new Destroyer(MainActivity.this).destroy(false, new Destroyer.OnDestructionListener() {
                    @Override
                    public void onDestroyed() {
                        WatcherHelper.setWatcherEnabled(MainActivity.this, false);
                        FirebaseAnalytics.getInstance(MainActivity.this).logEvent(FirebaseConstants.LOGOUT, domain);
                        LoginActivity.startFromActivity(MainActivity.this, null);
                    }
                });
            }
        }).setNegativeButton(getString(R.string.cancel), null).create().show();

    }

    private class MessagesBroadcastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction()==null)
                return;

            if(intent.getAction().equals(FirebaseConstants.INTENT_ACTION_GOT_FCM_MESSAGE)){
                NotificationsHelper.showDevAlert(MainActivity.this, intent.getStringExtra("message"));
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentFragment", currentApheleiaFragment);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(messagesBroadcastReceiver);
    }
}
