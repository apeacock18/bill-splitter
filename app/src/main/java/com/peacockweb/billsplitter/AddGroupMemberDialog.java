package com.peacockweb.billsplitter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

public class AddGroupMemberDialog extends DialogFragment {
    String username;

    public interface NoticeDialogListener {
        public void onDialogPositiveClick(String username);
    }

    // Use this instance of the interface to deliver action events
    NoticeDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (NoticeDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement NoticeDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        final LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        final View v = inflater.inflate(R.layout.fragment_add_group_member_dialog, null);
        builder.setView(v)
                // Add action buttons
                .setPositiveButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                        EditText editText = (EditText) v.findViewById(R.id.username);
                        final String name = editText.getText().toString().toLowerCase();

                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Users");
                        query.whereEqualTo("username", name);
                        query.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> obj, ParseException e) {
                                if (e == null) {
                                    Log.d("username", "Found " + obj.size() + " username for " + name);
                                    if (obj.size() != 0) {
                                        username = ((EditText) v.findViewById(R.id.username)).getText().toString();
                                        mListener.onDialogPositiveClick(username);
                                    }
                                } else {
                                    Log.d("username", "Error: " + e.getMessage());
                                }
                            }
                        });
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddGroupMemberDialog.this.getDialog().cancel();
                    }
                });
        return builder.create();
    }
}
