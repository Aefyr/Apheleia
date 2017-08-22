package com.aefyr.apheleia;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Destroyer;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.ProfileHelper;
import com.aefyr.apheleia.helpers.TheInitializer;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.Token;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

public class LoginActivity extends AppCompatActivity {

    private Helper helper;
    private EditText domainET;
    private EditText usernameET;
    private EditText passwordET;
    private ImageButton passwordVisibilitySwitch;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        domainET = (EditText) findViewById(R.id.domain);
        domainET.requestFocus();
        usernameET = (EditText) findViewById(R.id.username);
        passwordET = (EditText) findViewById(R.id.password);
        passwordVisibilitySwitch = (ImageButton) findViewById(R.id.passwordVisibilitySwitch);

        setupDomainHelpButton();
        setupPasswordVisibilitySwitcher();
        setupLoginSystem();
    }

    private void setupDomainHelpButton(){
        findViewById(R.id.schoolDomainHelp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlert(getString(R.string.help), getString(R.string.what_is_school_domain));
            }
        });
    }

    private boolean passwordShown = false;
    private void setupPasswordVisibilitySwitcher(){
        passwordVisibilitySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selection = passwordET.getSelectionEnd();
                if(passwordShown){
                    passwordET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordVisibilitySwitch.setImageResource(R.drawable.ic_visibility_black_24dp);
                }else {
                    passwordET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    passwordVisibilitySwitch.setImageResource(R.drawable.ic_visibility_off_black_24dp);
                }
                passwordET.setSelection(selection);
                passwordShown = !passwordShown;
            }
        });
    }

    private void setupLoginSystem(){
        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);

        findViewById(R.id.singIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(domainET.getText().length()==0) {
                    Utility.highLightET(getResources(), domainET);
                    return;
                }
                if(usernameET.getText().length()==0) {
                    Utility.highLightET(getResources(), usernameET);
                    return;
                }
                if(passwordET.getText().length()==0) {
                    Utility.highLightET(getResources(), passwordET);
                    return;
                }
                progressDialog.setMessage(getString(R.string.authorization));
                progressDialog.show();

                EljurApiClient.getInstance(LoginActivity.this).requestToken(domainET.getText().toString(), usernameET.getText().toString(), passwordET.getText().toString(), new EljurApiClient.LoginRequestListener() {
                    @Override
                    public void onSuccessfulLogin(Token token) {
                        progressDialog.dismiss();
                        helper = Helper.getInstance(LoginActivity.this);
                        helper.saveToken(token);
                        helper.saveDomain(domainET.getText().toString());

                        TheInitializer t = new TheInitializer(LoginActivity.this, new TheInitializer.OnInitializationListener() {
                            @Override
                            public void OnSuccess() {
                                loggedIn();
                            }

                            @Override
                            public void OnError(String m, String json, String failedWhat) {
                                new Destroyer(LoginActivity.this).destroy(null);
                                if(json!=null) {
                                    Chief.makeReportApiErrorDialog(LoginActivity.this,failedWhat, m, json, true);
                                }else {
                                    Chief.makeAnAlert(LoginActivity.this, m);
                                }
                            }

                        });

                        t.initialize();

                    }

                    @Override
                    public void onInvalidCredentialsError() {
                        progressDialog.hide();
                        showAlert(getString(R.string.invalid_credentials), getString(R.string.invalid_credentials_desc));
                    }

                    @Override
                    public void onInvalidDomainError() {
                        progressDialog.hide();
                        showAlert(getString(R.string.invalid_domain), getString(R.string.invalid_domain_desc));
                    }

                    @Override
                    public void onNetworkError() {
                        progressDialog.hide();
                        showAlert(getString(R.string.network_error), getString(R.string.network_error_tip));
                    }

                    @Override
                    public void onApiError(String message, String json) {
                        progressDialog.hide();
                        showAlert(getString(R.string.api_error), message);
                    }
                });
            }
        });
    }


    ProgressDialog progressDialog;

    private void showAlert(String title, String message){
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(getString(R.string.ok), null).create().show();
    }

    private void loggedIn(){
        helper.setLoggedIn(true);

        //Btw, this may be used to identify person in unique cases like only male/female student in class or so. Does that mean this is personal data? Actually, I guess school life doesn't count as personal/family life, right?
        ProfileHelper profileHelper = ProfileHelper.getInstance(this);
        Bundle params = new Bundle();
        params.putString(FirebaseConstants.SCHOOL_DOMAIN, helper.getDomain());
        params.putString(FirebaseConstants.ROLE, profileHelper.getRole());
        params.putString(FirebaseConstants.GENDER, profileHelper.getGender());
        if(profileHelper.getRole().equals(ProfileHelper.Role.PARENT))
            params.putInt(FirebaseConstants.STUDENTS_COUNT, profileHelper.getStudentsCount());
        else {
            String rawClass = profileHelper.getStudentClass(profileHelper.getCurrentStudentId());
            int parsedClass = 0;
            try {
                parsedClass = Integer.parseInt(rawClass.replaceAll("[^0-9]", ""));
            }catch (NumberFormatException e){
                FirebaseCrash.log("Can't parse class name: "+rawClass);
                FirebaseCrash.report(e);
            }
            params.putString(FirebaseConstants.RAW_CLASS, rawClass);
            params.putInt(FirebaseConstants.PARSED_CLASS, parsedClass);
        }
        FirebaseAnalytics.getInstance(this).logEvent(FirebaseAnalytics.Event.LOGIN, params);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public static void startFromActivity(Activity activity){
        Intent i = new Intent(activity, LoginActivity.class);
        activity.startActivity(i);
        activity.finish();
    }
}
