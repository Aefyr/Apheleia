package com.aefyr.apheleia;

import android.app.ActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aefyr.apheleia.fragments.DiaryFragment;
import com.aefyr.apheleia.fragments.FinalsFragment;
import com.aefyr.apheleia.fragments.MarksFragment;
import com.aefyr.apheleia.fragments.MessagesFragment;
import com.aefyr.apheleia.fragments.ScheduleFragment;
import com.aefyr.apheleia.fragments.Tags;
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
        implements NavigationView.OnNavigationItemSelectedListener {


    private Helper helper;
    private FragmentManager fragmentManager;
    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name), BitmapFactory.decodeResource(getResources(), R.mipmap.icon), getResources().getColor(R.color.colorRecentsTab)));


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initializeNavHeader(navigationView.getHeaderView(0));

        fragmentManager = getSupportFragmentManager();

        if (getIntent().getStringExtra("requested_fragment") != null) {
            if (getIntent().getStringExtra("requested_fragment").equals("messages")) {
                currentFragment = new MessagesFragment();
                currentApheleiaFragment = ApheleiaFragment.MESSAGES;
                fragmentManager.beginTransaction().add(R.id.fragmentContainer, currentFragment, Tags.MESSAGES).commit();
                navigationView.setCheckedItem(R.id.nav_messages);
            }
        } else {
            currentFragment = new DiaryFragment();
            currentApheleiaFragment = ApheleiaFragment.DIARY;
            fragmentManager.beginTransaction().add(R.id.fragmentContainer, currentFragment, Tags.DIARY).commit();
            navigationView.setCheckedItem(R.id.nav_diary);
        }

        checkPeriods(false);
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

                if (currentApheleiaFragment == ApheleiaFragment.MESSAGES) {
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
                    case DIARY:
                        ((DiaryFragment) currentFragment).showTimePeriodSwitcherDialog();
                        break;
                    case MARKS:
                        ((MarksFragment) currentFragment).showTimePeriodSwitcherDialog();
                        break;
                    case SCHEDULE:
                        ((ScheduleFragment) currentFragment).showTimePeriodSwitcherDialog();
                        break;
                }
                return true;

        }


        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        boolean doNotClose = false;

        if (id == R.id.nav_diary) {
            setFragment(ApheleiaFragment.DIARY);
        } else if (id == R.id.nav_marks) {
            setFragment(ApheleiaFragment.MARKS);

        } else if (id == R.id.nav_finals) {
            setFragment(ApheleiaFragment.FINALS);
        } else if (id == R.id.nav_schedule) {
            setFragment(ApheleiaFragment.SCHEDULE);

        } else if (id == R.id.nav_messages) {
            setFragment(ApheleiaFragment.MESSAGES);

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

    private enum ApheleiaFragment {
        DIARY, MARKS, SCHEDULE, MESSAGES, FINALS
    }

    private Fragment currentFragment;
    private ApheleiaFragment currentApheleiaFragment = ApheleiaFragment.DIARY;

    private void setFragment(ApheleiaFragment fragment) {
        if (fragment == currentApheleiaFragment)
            return;

        currentApheleiaFragment = fragment;

        FragmentTransaction transaction = fragmentManager.beginTransaction().hide(currentFragment).setCustomAnimations(R.anim.frag_enter, R.anim.frag_exit);
        switch (fragment) {
            case DIARY:
                Fragment diary = fragmentManager.findFragmentByTag(Tags.DIARY);
                if (diary == null) {
                    diary = new DiaryFragment();
                    transaction.add(R.id.fragmentContainer, diary, Tags.DIARY);
                } else {
                    transaction.show(diary);
                }
                currentFragment = diary;
                timePeriodSwitchButton.setVisible(true);
                mailFolderSwitchButton.setVisible(false);
                break;
            case MARKS:
                Fragment marks = fragmentManager.findFragmentByTag(Tags.MARKS);
                if (marks == null) {
                    marks = new MarksFragment();
                    transaction.add(R.id.fragmentContainer, marks, Tags.MARKS);
                } else {
                    transaction.show(marks);
                }
                currentFragment = marks;
                timePeriodSwitchButton.setVisible(true);
                mailFolderSwitchButton.setVisible(false);
                break;
            case SCHEDULE:
                Fragment schedule = fragmentManager.findFragmentByTag(Tags.SCHEDULE);
                if (schedule == null) {
                    schedule = new ScheduleFragment();
                    transaction.add(R.id.fragmentContainer, schedule, Tags.SCHEDULE);
                } else {
                    transaction.show(schedule);
                }
                currentFragment = schedule;
                timePeriodSwitchButton.setVisible(true);
                mailFolderSwitchButton.setVisible(false);
                break;
            case MESSAGES:
                Fragment messages = fragmentManager.findFragmentByTag(Tags.MESSAGES);
                if (messages == null) {
                    messages = new MessagesFragment();
                    transaction.add(R.id.fragmentContainer, messages, Tags.MESSAGES);
                } else {
                    transaction.show(messages);
                }
                currentFragment = messages;
                timePeriodSwitchButton.setVisible(false);
                mailFolderSwitchButton.setVisible(true);
                break;
            case FINALS:
                Fragment finals = fragmentManager.findFragmentByTag(Tags.FINALS);
                if (finals == null) {
                    finals = new FinalsFragment();
                    transaction.add(R.id.fragmentContainer, finals, Tags.FINALS);
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

    private void initializeNavHeader(View navHeader) {
        final ProfileHelper profileHelper = ProfileHelper.getInstance(this);
        boolean parent = profileHelper.getRole().equals(ProfileHelper.Role.PARENT);

        ((TextView) navHeader.findViewById(R.id.usernameText)).setText(profileHelper.getName());
        ((TextView) navHeader.findViewById(R.id.emailText)).setText(profileHelper.getEmail());
        ((ImageView) navHeader.findViewById(R.id.roleIcon)).setImageResource(parent ? R.drawable.ic_business_center_white_24dp : R.drawable.ic_school_white_24dp);
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
                        drawer.closeDrawer(GravityCompat.START);
                        return;
                    }
                    studentName.setText(studentsNames[i]);
                    profileHelper.setCurrentStudent(studentsIds[i]);
                    studentSwitched();
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
            }

            @Override
            public void OnFoundMorePeriods() {
                Chief.makeAToast(MainActivity.this, getString(R.string.check_periods_updated));
                studentSwitched();
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
                        FirebaseAnalytics.getInstance(MainActivity.this).logEvent(FirebaseConstants.LOGOUT, domain);
                        LoginActivity.startFromActivity(MainActivity.this, null);
                    }
                });
            }
        }).setNegativeButton(getString(R.string.cancel), null).create().show();

    }
}
