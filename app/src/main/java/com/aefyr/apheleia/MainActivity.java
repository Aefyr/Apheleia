package com.aefyr.apheleia;


import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import com.aefyr.apheleia.gcm.RegistrationIntentService;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.EljurPersona;
import com.aefyr.journalism.objects.major.PersonaInfo;
import com.aefyr.journalism.objects.major.Token;

public class MainActivity extends AppCompatActivity {

    Helper helper;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Intent intent = new Intent(this, RegistrationIntentService.class);
        //startService(intent);

        helper = Helper.getInstance(this);
        if(!helper.isLoggedIn()||helper.isTokenExpired()){
            LoginActivity.startFromActivity(this);
            return;
        }

        testRequest();
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
}
