package com.aefyr.apheleia;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.aefyr.apheleia.helpers.Chief;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.apheleia.helpers.TimeLord;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.objects.major.MessagesList;
import com.aefyr.journalism.objects.minor.Attachment;
import com.aefyr.journalism.objects.minor.MessageInfo;
import com.aefyr.journalism.objects.minor.MessagePerson;
import com.android.volley.toolbox.StringRequest;
import com.google.firebase.analytics.FirebaseAnalytics;

public class MessageViewActivity extends AppCompatActivity {

    private StringRequest messageGetRequest;
    private boolean inbox;
    private String messageId;

    private View messageLayout;
    private TextView subject;
    private TextView date;
    private TextView sender;
    private TextView receivers;
    private TextView text;
    private LinearLayout attachmentsContainer;
    private FloatingActionButton replyFab;

    private ProgressDialog loadingDialog;
    private boolean gotMessage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_view);

        getSupportActionBar().setTitle(getString(R.string.viewing_message));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        messageId = getIntent().getStringExtra("messageId");
        inbox = getIntent().getBooleanExtra("inbox", false);

        messageLayout = findViewById(R.id.messageLayout);
        subject = (TextView) findViewById(R.id.subject);
        date = (TextView) findViewById(R.id.date);
        sender = (TextView) findViewById(R.id.sender);
        receivers = (TextView) findViewById(R.id.receivers);
        text = (TextView) findViewById(R.id.text);
        attachmentsContainer = (LinearLayout) findViewById(R.id.attachmentsContainer);
        replyFab = (FloatingActionButton) findViewById(R.id.replyFab);
        if (!inbox)
            replyFab.hide();

        loadingDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        loadingDialog.setCancelable(false);
        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setMessage(getString(R.string.fetching_message));

        getMessage();
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

    private void setMessage(final MessageInfo message) {
        messageLayout.setVisibility(View.VISIBLE);

        subject.setText(message.getSubject());
        date.setText(TimeLord.getInstance().getFullMessageDate(message.getDate()));
        text.setText(message.getText());


        int receiversCount = message.getReceivers().size();
        String receiversInfo;
        if (inbox) {
            sender.setText(String.format(getString(R.string.from), message.getSender().getCompositeName(true, true, true)));
            if (receiversCount > 3) {
                receiversInfo = String.format(getString(R.string.to_many), getString(R.string.you), receiversCount - 1);
            } else {
                StringBuilder receiversBuilder = new StringBuilder();
                for (MessagePerson receiver : message.getReceivers()) {
                    receiversBuilder.append(receiver.getCompositeName(true, false, true)).append(", ");
                }
                receiversInfo = String.format(getString(R.string.to), receiversBuilder.toString().substring(0, receiversBuilder.length() - 2));
            }

            replyFab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent replyIntent = new Intent(MessageViewActivity.this, MessageComposeActivity.class);
                    replyIntent.putExtra("receiver", message.getSender().getId());
                    replyIntent.putExtra("subject", message.getSubject());
                    startActivity(replyIntent);
                }
            });

        } else {
            sender.setText(String.format(getString(R.string.from), getString(R.string.you)));
            if (receiversCount > 3) {
                receiversInfo = String.format(getString(R.string.to_many), message.getReceivers().get(0).getCompositeName(true, false, true), receiversCount - 1);
            } else {
                StringBuilder receiversBuilder = new StringBuilder();
                for (MessagePerson receiver : message.getReceivers()) {
                    receiversBuilder.append(receiver.getCompositeName(true, false, true)).append(", ");
                }
                receiversInfo = String.format(getString(R.string.to), receiversBuilder.toString().substring(0, receiversBuilder.length() - 2));
            }
        }

        receivers.setText(receiversInfo);

        if (message.hasAttachments()) {
            LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
            for (final Attachment attachment : message.getAttachments()) {
                View attachmentView = inflater.inflate(R.layout.attachment, attachmentsContainer);
                Button attachmentButton = (Button) attachmentView.findViewById(R.id.attachmentButton);
                attachmentButton.setText(attachment.getName());

                View.OnClickListener linkOpenRequestListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openLink(attachment.getUri());
                    }
                };
                attachmentButton.setOnClickListener(linkOpenRequestListener);
                attachmentView.findViewById(R.id.attachmentImageButton).setOnClickListener(linkOpenRequestListener);
            }
        }

        loadingDialog.dismiss();
    }

    private void getMessage() {
        loadingDialog.show();

        messageGetRequest = EljurApiClient.getInstance(this).getMessageInfo(Helper.getInstance(this).getPersona(), inbox ? MessagesList.Folder.INBOX : MessagesList.Folder.SENT, messageId, new EljurApiClient.JournalismListener<MessageInfo>() {
            @Override
            public void onSuccess(MessageInfo result) {
                setMessage(result);
                gotMessage = true;
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                if(tokenIsWrong){
                    LoginActivity.tokenExpired(MessageViewActivity.this);
                    return;
                }
                loadingDialog.dismiss();
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
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                }).create().show();
            }

            @Override
            public void onApiError(String message, String json) {
                loadingDialog.dismiss();
                Chief.makeReportApiErrorDialog(MessageViewActivity.this, getString(R.string.message), message, json, false).setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (!messageGetRequest.hasHadResponseDelivered())
            messageGetRequest.cancel();
        super.onDestroy();
    }

    @Override
    public void finish() {
        setResult(gotMessage ? RESULT_OK : RESULT_CANCELED);
        super.finish();
    }

    private void openLink(String uri) {
        Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(linkIntent);
    }
}
