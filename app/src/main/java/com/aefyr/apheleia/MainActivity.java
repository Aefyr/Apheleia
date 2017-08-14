package com.aefyr.apheleia;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.aefyr.apheleia.fragments.DiaryFragment;
import com.aefyr.apheleia.fragments.MarksFragment;
import com.aefyr.apheleia.helpers.ProfileHelper;

import java.util.Arrays;

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
        if(!helper.isLoggedIn()||helper.isTokenExpired()){
            LoginActivity.startFromActivity(this);
            return;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        initializeUserSwitcher(navigationView.getHeaderView(0));

        fragmentManager = getSupportFragmentManager();

        currentFragment = new DiaryFragment();
        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, currentFragment).commit();
        navigationView.setCheckedItem(R.id.nav_diary);
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
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        timePeriodSwitchButton = menu.findItem(R.id.action_time_period_switcher);
        timePeriodSwitchButton.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (currentApheleiaFragment){
                    case DIARY:
                        ((DiaryFragment)currentFragment).showTimePeriodSwitcherDialog();
                        break;
                    case MARKS:
                        ((MarksFragment)currentFragment).showTimePeriodSwitcherDialog();
                        break;
                }
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if(id == R.id.action_time_period_switcher){
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_diary) {
            setFragment(ApheleiaFragment.DIARY);
        } else if (id == R.id.nav_marks) {
            setFragment(ApheleiaFragment.MARKS);

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private enum ApheleiaFragment{
        DIARY, MARKS
    }

    private Fragment currentFragment;
    private ApheleiaFragment currentApheleiaFragment = ApheleiaFragment.DIARY;
    private void setFragment(ApheleiaFragment fragment){
        if(fragment == currentApheleiaFragment)
            return;
        currentApheleiaFragment = fragment;
        switch (fragment){
            case DIARY:
                currentFragment = new DiaryFragment();
                timePeriodSwitchButton.setVisible(true);
                break;
            case MARKS:
                currentFragment = new MarksFragment();
                timePeriodSwitchButton.setVisible(true);
                break;
        }
        fragmentManager.beginTransaction().replace(R.id.fragmentContainer, currentFragment).commit();


    }

    private TextView studentName;
    private AlertDialog studentPickerDialog;
    private void initializeUserSwitcher(View navHeader){
        final ProfileHelper profileHelper = ProfileHelper.getInstance(this);

        ((TextView)navHeader.findViewById(R.id.usernameText)).setText(profileHelper.getName());
        ((TextView)navHeader.findViewById(R.id.emailText)).setText(profileHelper.getEmail());
        studentName = (TextView) navHeader.findViewById(R.id.studentNameText);

        if(profileHelper.getStudentsCount()==1){
            studentName.setVisibility(View.GONE);
            navHeader.findViewById(R.id.switchStudentButton).setVisibility(View.GONE);
        }else {
            studentName.setText(profileHelper.getStudentName(profileHelper.getCurrentStudentId())+" ("+profileHelper.getStudentClass(profileHelper.getCurrentStudentId())+")");

            final String[] studentsIds = profileHelper.getStudentsIds().toArray(new String[]{});
            final String[] studentsNames = new String[studentsIds.length];
            int i = 0;
            for(String id: studentsIds){
                studentsNames[i++] = profileHelper.getStudentName(id)+" ("+profileHelper.getStudentClass(id)+")";
            }

            studentPickerDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.pick_student)).setSingleChoiceItems(studentsNames, Arrays.asList(studentsIds).indexOf(profileHelper.getCurrentStudentId()), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    studentPickerDialog.dismiss();
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

    private void studentSwitched(){
        switch (currentApheleiaFragment){
            case DIARY:
                ((DiaryFragment) currentFragment).studentSwitched();
                break;
            case MARKS:
                ((MarksFragment)currentFragment).studentSwitched();
                break;
        }
    }
}
