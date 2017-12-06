package com.aefyr.apheleia.fragments;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.aefyr.apheleia.LoginActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.adapters.MessageReceiversAdapter;
import com.aefyr.apheleia.helpers.Helper;
import com.aefyr.journalism.EljurApiClient;
import com.aefyr.journalism.exceptions.JournalismException;
import com.aefyr.journalism.objects.major.MessageReceiversInfo;
import com.android.volley.Request;

import java.util.HashSet;

/**
 * A simple {@link Fragment} subclass.
 */
public class MCReceiversFragment extends Fragment {
    private Request receiversGetRequest;

    private RecyclerView receiversRecycler;
    private MessageReceiversAdapter receiversAdapter;
    private ProgressDialog progressDialog;

    public MCReceiversFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mcreceivers, container, false);
        receiversRecycler = (RecyclerView) view.findViewById(R.id.receiversRecycler);
        receiversRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL);
        dividerItemDecoration.setDrawable(getResources().getDrawable(R.drawable.messages_divider));
        receiversRecycler.addItemDecoration(dividerItemDecoration);
        receiversRecycler.setItemViewCacheSize(4);

        progressDialog = new ProgressDialog(getActivity(), ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.fetching_receivers));
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);

        loadReceivers();

        return view;
    }

    private String forcedReceiver;

    public void forceSetReceiver(String id) {
        forcedReceiver = id;
    }

    public HashSet<String> getReceiversIds() {
        return receiversAdapter.getCheckedReceiversIds();
    }

    private void setReceiversInfoToAdapter(MessageReceiversInfo receiversInfo) {
        if (receiversAdapter == null) {
            receiversAdapter = new MessageReceiversAdapter(receiversInfo);
            receiversAdapter.setHasStableIds(true);
            receiversRecycler.setAdapter(receiversAdapter);
        } else
            receiversAdapter.setReceiversInfo(receiversInfo);
        if (forcedReceiver != null)
            receiversAdapter.checkReceiver(forcedReceiver);
    }

    private void loadReceivers() {
        progressDialog.show();
        receiversGetRequest = EljurApiClient.getInstance(getActivity()).getMessagesReceivers(Helper.getInstance(getActivity()).getPersona(), new EljurApiClient.JournalismListener<MessageReceiversInfo>() {
            @Override
            public void onSuccess(MessageReceiversInfo result) {
                setReceiversInfoToAdapter(result);
                progressDialog.dismiss();
            }

            @Override
            public void onNetworkError(boolean tokenIsWrong) {
                if (tokenIsWrong) {
                    LoginActivity.tokenExpired(getActivity());
                    return;
                }
                progressDialog.dismiss();
                new AlertDialog.Builder(getActivity()).setMessage(getString(R.string.network_error_tip)).setTitle(getString(R.string.cant_get_receivers)).setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getActivity().finish();
                    }
                }).setPositiveButton(getString(R.string.retry), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        loadReceivers();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        getActivity().finish();
                    }
                }).create().show();
            }

            @Override
            public void onApiError(JournalismException e) {
                //Well, apparently this will never be called, maybe I should make the parser in Utility throw EljurApiErrors
            }
        });
    }

    @Override
    public void onDetach() {
        if (!receiversGetRequest.hasHadResponseDelivered())
            receiversGetRequest.cancel();
        super.onDetach();
    }
}
