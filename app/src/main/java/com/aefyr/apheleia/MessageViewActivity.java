package com.aefyr.apheleia;

import android.content.DialogInterface;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.android.volley.toolbox.StringRequest;

public class MessageViewActivity extends AppCompatActivity {

    private StringRequest messageGetRequest;
    private boolean inbox;
    private String messageId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getString(R.string.viewing_message));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        messageId = getIntent().getStringExtra("messageId");
        inbox = getIntent().getBooleanExtra("inbox", false);




    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getMessage(){
        messageGetRequest = EljurApiClient.getInstance(this).getMessageInfo(Helper.getInstance(this).getPersona(), inbox ? MessagesList.Folder.INBOX : MessagesList.Folder.SENT, messageId, new EljurApiClient.JournalismListener<MessageInfo>() {
            @Override
            public void onSuccess(MessageInfo result) {

            }

            @Override
            public void onNetworkError() {
                new AlertDialog.Builder(MessageViewActivity.this).setMessage(getString(R.string.network_error_tip)).setTitle(getString(R.string.cant_get_message)).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getMessage();
                    }
                });
            }

            @Override
            public void onApiError(String message, String json) {
                Chief.makeReportApiErrorDialog(MessageViewActivity.this, getString(R.string.message), message, json, false).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                });
            }
        });
    }
}
