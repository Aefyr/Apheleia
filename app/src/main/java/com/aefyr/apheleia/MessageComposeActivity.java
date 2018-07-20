package com.aefyr.apheleia;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.aefyr.apheleia.fragments.MCMessageFragment;
import com.aefyr.apheleia.fragments.MCReceiversFragment;
import com.aefyr.apheleia.helpers.AnalyticsHelper;
import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.SentMessageResponse;
import com.android.volley.Request;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;

public class MessageComposeActivity extends AppCompatActivity {

    private Request messageSendRequest;

    private ProgressDialog sendingDialog;

    private MCReceiversFragment receiversFragment;
    private MCMessageFragment messageFragment;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_compose);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            setTaskDescription(new ActivityManager.TaskDescription(getString(R.string.app_name), BitmapFactory.decodeResource(getResources(), R.mipmap.icon), getResources().getColor(R.color.colorRecentsTab)));

        boolean replyIntent = getIntent().getStringExtra("receiver") != null;

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.compose_message));

        viewPager = (ViewPager) findViewById(R.id.composeMessageViewPager);

        if (savedInstanceState != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            receiversFragment = (MCReceiversFragment) fragmentManager.getFragment(savedInstanceState, "receivers");
            messageFragment = (MCMessageFragment) fragmentManager.getFragment(savedInstanceState, "message");
        } else {
            receiversFragment = new MCReceiversFragment();
            messageFragment = new MCMessageFragment();
        }

        final ArrayList<Fragment> fragments = new ArrayList<>();
        fragments.add(receiversFragment);
        fragments.add(messageFragment);

        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return fragments.get(position);
            }

            @Override
            public int getCount() {
                return fragments.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return getString(position == 0 ? R.string.receivers : R.string.message);
            }
        });

        if (savedInstanceState == null) {
            if (replyIntent) {
                receiversFragment.forceSetReceiver(getIntent().getStringExtra("receiver"));
                messageFragment.setForcedMessageSubject(String.format(getString(R.string.reply_subject_prefix), getIntent().getStringExtra("subject")));
                scrollToPage(1);
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.compose, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.menu_send:
                sendMessage();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sendMessage() {
        if (receiversFragment.getReceiversIds().size() == 0) {
            Chief.makeAnAlert(this, getString(R.string.error_no_receivers));
            scrollToPage(0);
            return;
        }
        if (!messageFragment.checkFields()) {
            scrollToPage(1);
            return;
        }

        new AlertDialog.Builder(this).setMessage(R.string.send_confirmation).setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendingDialog = new ProgressDialog(MessageComposeActivity.this);
                sendingDialog.setMessage(getString(R.string.sending_message));
                sendingDialog.setCancelable(false);
                sendingDialog.setCanceledOnTouchOutside(false);
                sendingDialog.show();

                messageSendRequest = EljurApiClient.getInstance(MessageComposeActivity.this).sendMessage(Helper.getInstance(MessageComposeActivity.this).getPersona(), messageFragment.getMessageSubject(), messageFragment.getMessageText(), receiversFragment.getReceiversIds(), new EljurApiClient.JournalismListener<SentMessageResponse>() {
                    @Override
                    public void onSuccess(SentMessageResponse result) {
                        AnalyticsHelper.sentMessage(FirebaseAnalytics.getInstance(MessageComposeActivity.this));
                        sendingDialog.dismiss();
                        Chief.makeAToast(MessageComposeActivity.this, result.wasSent() ? getString(R.string.message_sent) : getString(R.string.sending_error));
                        finish();
                    }

                    @Override
                    public void onNetworkError(boolean tokenIsWrong) {
                        if (tokenIsWrong) {
                            LoginActivity.tokenExpired(MessageComposeActivity.this);
                            return;
                        }
                        AlertDialog networkErrorDialog = new AlertDialog.Builder(MessageComposeActivity.this).setMessage(getString(R.string.network_error_tip)).setTitle(getString(R.string.cant_send_message)).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }).setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                sendMessage();
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        }).create();
                        networkErrorDialog.setCanceledOnTouchOutside(false);
                        networkErrorDialog.show();
                    }

                    @Override
                    public void onApiError(JournalismException e) {
                        AlertDialog apiErrorDialog = new AlertDialog.Builder(MessageComposeActivity.this).setMessage(getString(R.string.error_api)).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                finish();
                            }
                        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                finish();
                            }
                        }).create();
                        apiErrorDialog.setCanceledOnTouchOutside(false);
                        apiErrorDialog.show();
                    }
                });
            }
        }).create().show();
    }

    @Override
    protected void onDestroy() {
        if (messageSendRequest != null && !messageSendRequest.hasHadResponseDelivered())
            messageSendRequest.cancel();
        super.onDestroy();
    }

    private void scrollToPage(int page) {
        if (viewPager.getCurrentItem() != page)
            viewPager.setCurrentItem(page, true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.putFragment(outState, "receivers", receiversFragment);
        fragmentManager.putFragment(outState, "message", messageFragment);

    }
}
