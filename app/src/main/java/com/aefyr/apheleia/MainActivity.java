package com.aefyr.apheleia;

import android.os.Bundle;
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

import com.aefyr.apheleia.fragments.DiaryFragment;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.PersonaInfo;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {


    private Helper helper;
    private FragmentManager fragmentManager;
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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        fragmentManager = getSupportFragmentManager();

    }

    private void testRequest(){
        EljurPersona persona = helper.getPersona();
        EljurApiClient.getInstance(this).getRules(persona, new EljurApiClient.JournalismListener<PersonaInfo>() {
            @Override
            public void onSuccess(PersonaInfo result) {
                System.out.println(String.format("Name: %s\nEmail: %s\nStudents count: %d", result.getCompositeName(true, true, true), result.email(), result.getStudents().size()));
                new AlertDialog.Builder(MainActivity.this).setMessage("Yay~"+(result.gender()== PersonaInfo.Gender.FEMALE?"Miss ":"Mister ")+result.getCompositeName(true, false, false)+", you logged in ;3").create().show();
            }

            @Override
            public void onNetworkError() {
                new AlertDialog.Builder(MainActivity.this).setMessage("Network error!").create().show();
            }

            @Override
            public void onApiError(String message) {
                new AlertDialog.Builder(MainActivity.this).setMessage("Api error!").create().show();
            }
        });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            setFragment(ApheleiaFragment.DIARY);
        } else if (id == R.id.nav_gallery) {

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
        DIARY
    }
    private void setFragment(ApheleiaFragment fragment){
        switch (fragment){

            case DIARY:
                fragmentManager.beginTransaction().replace(R.id.fragmentContainer, new DiaryFragment()).commit();
                break;
        }
    }
}
