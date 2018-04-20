package com.aefyr.apheleia;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Destroyer;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.TheInitializer;
import com.aefyr.apheleia.utility.FirebaseConstants;
import com.aefyr.apheleia.utility.Utility;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.Token;
import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;

public class LoginActivity extends AppCompatActivity {

    private Helper helper;
    private EditText domainET;
    private EditText usernameET;
    private EditText passwordET;
    private ImageButton passwordVisibilitySwitch;
    private Button signIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name), BitmapFactory.decodeResource(getResources(), R.mipmap.icon), getResources().getColor(R.color.colorRecentsTab)));

        SharedPreferences l = getSharedPreferences("l", 0);
        if (!l.getBoolean("start_warn_shown", false)) {
            Chief.makeWarning(this, getText(R.string.first_launch_warn)).setCancelable(false);
            l.edit().putBoolean("start_warn_shown", true).apply();
        }

        checkReason(getIntent());

        domainET = (EditText) findViewById(R.id.domain);
        domainET.requestFocus();
        usernameET = (EditText) findViewById(R.id.username);
        passwordET = (EditText) findViewById(R.id.password);
        passwordVisibilitySwitch = (ImageButton) findViewById(R.id.passwordVisibilitySwitch);
        signIn = (Button) findViewById(R.id.signIn);

        setupDomainHelpButton();
        setupPasswordVisibilitySwitcher();
        setupLoginSystem();
    }

    private void setupDomainHelpButton() {
        findViewById(R.id.schoolDomainHelp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showAlert(getString(R.string.help), getText(R.string.what_is_school_domain));
            }
        });
    }

    private boolean passwordShown = false;

    private void setupPasswordVisibilitySwitcher() {
        passwordVisibilitySwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int selection = passwordET.getSelectionEnd();
                if (passwordShown) {
                    passwordET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    passwordVisibilitySwitch.setImageResource(R.drawable.ic_visibility_black_24dp);
                } else {
                    passwordET.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    passwordVisibilitySwitch.setImageResource(R.drawable.ic_visibility_off_black_24dp);
                }
                passwordET.setSelection(selection);
                passwordShown = !passwordShown;
            }
        });
    }

    private void setupLoginSystem() {
        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        domainET.setFilters(new InputFilter[]{new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                return (source.toString()).replaceAll("[^a-zA-Z0-9-_]", "");
            }
        }});

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (domainET.getText().length() == 0) {
                    Utility.highLightET(getResources(), domainET);
                    return;
                }
                if (usernameET.getText().length() == 0) {
                    Utility.highLightET(getResources(), usernameET);
                    return;
                }
                if (passwordET.getText().length() == 0) {
                    Utility.highLightET(getResources(), passwordET);
                    return;
                }
                progressDialog.setMessage(getString(R.string.authorization));
                progressDialog.show();

                signIn.setEnabled(false);

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
                            public void OnError(String m) {
                                signIn.setEnabled(true);
                                new Destroyer(LoginActivity.this).destroy(false, null);
                                Chief.makeAnAlert(LoginActivity.this, m);
                            }

                        });

                        t.initialize();

                    }

                    @Override
                    public void onInvalidCredentialsError() {
                        signIn.setEnabled(true);
                        progressDialog.hide();
                        showAlert(getString(R.string.invalid_credentials), getString(R.string.invalid_credentials_desc));
                    }

                    @Override
                    public void onInvalidDomainError() {
                        signIn.setEnabled(true);
                        progressDialog.hide();
                        showAlert(getString(R.string.invalid_domain), getString(R.string.invalid_domain_desc));
                    }

                    @Override
                    public void onNetworkError() {
                        signIn.setEnabled(true);
                        progressDialog.hide();
                        showAlert(getString(R.string.eljur_connection_error), getString(R.string.eljur_connection_error_desc));
                    }

                    @Override
                    public void onApiError(JournalismException e) {
                        signIn.setEnabled(true);
                        progressDialog.hide();
                        Crashlytics.logException(e);
                        Chief.makeApiErrorAlert(LoginActivity.this, false);
                    }

                    @Override
                    public void onApiAccessForbidden() {
                        signIn.setEnabled(true);
                        progressDialog.hide();
                        showAlert(getString(R.string.error), getString(R.string.error_api_access_forbidden));
                    }
                });
            }
        });
    }


    ProgressDialog progressDialog;

    private void showAlert(String title, CharSequence message) {
        if(!isFinishing())
            new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(getString(R.string.ok), null).create().show();
    }

    private void loggedIn() {
        helper.setLoggedIn(true);
        Bundle b = new Bundle();
        b.putString(FirebaseConstants.SCHOOL_DOMAIN, helper.getDomain().toLowerCase());
        FirebaseAnalytics.getInstance(this).logEvent(FirebaseConstants.LOGIN, b);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkReason(Intent i) {
        String reason = i.getStringExtra("reason");
        if (reason != null) {
            switch (reason) {
                case Reason.TOKEN_EXPIRED:
                    Chief.makeWarning(this, getString(R.string.token_expired));
                    break;
            }
        }
    }

    public class Reason {
        public static final String TOKEN_EXPIRED = "token_expired";
    }

    public static void startFromActivity(Activity activity, String reason) {
        Intent i = new Intent(activity, LoginActivity.class);
        if (reason != null)
            i.putExtra("reason", reason);
        activity.startActivity(i);
        activity.finish();
    }

    public static void tokenExpired(final Activity a) {
        Helper.getInstance(a).setLoggedIn(false);
        new Destroyer(a).destroy(true, new Destroyer.OnDestructionListener() {
            @Override
            public void onDestroyed() {
                startFromActivity(a, Reason.TOKEN_EXPIRED);
            }
        });
    }
}
