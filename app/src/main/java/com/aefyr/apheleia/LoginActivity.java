package com.aefyr.apheleia;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.Token;

public class LoginActivity extends AppCompatActivity {

    Helper helper;
    EditText domainET;
    EditText usernameET;
    EditText passwordET;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        helper = Helper.getInstance(this);

        domainET = (EditText) findViewById(R.id.domain);
        domainET.requestFocus();
        usernameET = (EditText) findViewById(R.id.username);
        passwordET = (EditText) findViewById(R.id.password);

        setupDomainHelpButton();
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

    private void setupLoginSystem(){
        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);

        findViewById(R.id.singIn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(domainET.getText().length()==0) {
                    highLightET(domainET);
                    return;
                }
                if(usernameET.getText().length()==0) {
                    highLightET(usernameET);
                    return;
                }
                if(passwordET.getText().length()==0) {
                    highLightET(passwordET);
                    return;
                }
                progressDialog.setMessage(getString(R.string.authorization));
                progressDialog.show();

                EljurApiClient.getInstance(LoginActivity.this).requestToken(domainET.getText().toString(), usernameET.getText().toString(), passwordET.getText().toString(), new EljurApiClient.LoginRequestListener() {
                    @Override
                    public void onSuccessfulLogin(Token token) {
                        progressDialog.dismiss();
                        helper.saveToken(token);
                        helper.saveDomain(domainET.getText().toString());
                        loggedIn();
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
                    public void onApiError(String message) {
                        progressDialog.hide();
                        showAlert(getString(R.string.api_error), message);
                    }
                });
            }
        });
    }

    private void highLightET(final EditText et){
        ValueAnimator colorAnimator = new ValueAnimator();
        colorAnimator.setIntValues(Color.RED, getResources().getColor(R.color.colorEditTextHint));
        colorAnimator.setEvaluator(new ArgbEvaluator());
        colorAnimator.setRepeatCount(6);
        colorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        colorAnimator.setDuration(100);
        colorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                et.setHintTextColor((Integer) valueAnimator.getAnimatedValue());
            }
        });
        et.requestFocus();
        colorAnimator.start();
    }

    ProgressDialog progressDialog;

    private void showAlert(String title, String message){
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(getString(R.string.ok), null).create().show();

    }

    private void loggedIn(){
        //TODO Get rules and set default student, days, period
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
