package com.peacockweb.billsplitter;

import android.content.Intent;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.peacockweb.billsplitter.util.TinyDB;
import com.tokenautocomplete.TokenCompleteTextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddGroup extends AppCompatActivity implements AddGroupMemberDialog.NoticeDialogListener, TokenCompleteTextView.TokenListener {

    EditText groupName;
    ArrayList<GroupMember> people;
    ArrayList<GroupMember> addedMembers = new ArrayList();
    ArrayList knownMembers;
    ArrayAdapter<GroupMember> adapter;
    MembersCompletionView completionView;
    AddGroupMemberDialog dialog;
    FragmentManager manager;
    TinyDB tinyDB;
    String groupId;
    ArrayList<String> usernames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group);

        dialog = new AddGroupMemberDialog();
        manager = getSupportFragmentManager();

        tinyDB = new TinyDB(this);
        people = new ArrayList();
        knownMembers = tinyDB.getListObject("knownMembers", GroupMember.class);
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Users");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List<ParseObject> users, ParseException e) {
                if (e == null) {
                    for (int i = 0; i < users.size(); i++) {
                        people.add(new GroupMember(
                                users.get(i).getString("name"),
                                users.get(i).getString("username"),
                                users.get(i).getObjectId()
                        ));
                    }
                } else {
                    Log.d("Users", "Error: " + e.getMessage());
                }
            }
        });

        groupName = (EditText) findViewById(R.id.newGroupName);

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, people);

        completionView = (MembersCompletionView)findViewById(R.id.addGroupMembers);
        completionView.setTokenListener(this);
        completionView.setTokenClickStyle(TokenCompleteTextView.TokenClickStyle.Delete);
        completionView.setDeletionStyle(TokenCompleteTextView.TokenDeleteStyle.ToString);
        completionView.setAdapter(adapter);
    }

    @Override
    public void onTokenAdded(Object token) {
        GroupMember mem = (GroupMember) token;
        addedMembers.add(mem);
        System.out.println("Added: " + mem.getUsername());
    }

    @Override
    public void onTokenRemoved(Object token) {
        addedMembers.remove(token);
        System.out.println("Removed: " + token);
    }

    public void onCreateGroupClick(View view) {
        String str = groupName.getText().toString();
        if (str != null && !str.isEmpty()) {
            HashMap<String, String> params = new HashMap<>();
            params.put("name", groupName.getText().toString());
            ParseCloud.callFunctionInBackground("createGroup", params, new FunctionCallback<String>() {
                public void done(String id, ParseException e) {
                    if (e == null) {

                        groupId = id;
                        usernames = new ArrayList();

                        for (GroupMember groupMember : addedMembers) {
                            usernames.add(groupMember.getUsername());
                        }

                        ParseQuery<ParseObject> query = ParseQuery.getQuery("Users");
                        query.whereContainedIn("username", usernames);
                        query.findInBackground(new FindCallback<ParseObject>() {
                            public void done(List<ParseObject> users, ParseException e) {
                                if (e == null) {
                                    if (!users.isEmpty()) {
                                        for (GroupMember mem : addedMembers) {
                                            if (!usernames.contains(mem.getUsername())) {
                                                knownMembers.add(mem);
                                            }
                                        }
                                        tinyDB.putListObject("knownMembers", knownMembers);

                                        ParseObject user = users.get(0);

                                        HashMap<String, String> params1 = new HashMap<>();
                                        params1.put("userId", user.getObjectId());
                                        params1.put("groupId", groupId);
                                        ParseCloud.callFunctionInBackground("addUserToGroup", params1, new FunctionCallback<Object>() {
                                            public void done(Object id, ParseException e) {
                                                if (e == null) {
                                                    System.out.println("User should have been added here...");
                                                } else {
                                                    System.out.println("Error: " + e.getMessage());
                                                }
                                            }
                                        });
                                    }
                                    Group group = new Group(addedMembers, groupName.getText().toString(), groupId);
                                    ArrayList temp = tinyDB.getListObject("groupList", Group.class);
                                    temp.add(group);
                                    tinyDB.putListObject("groupList", temp);
                                    ArrayList arr = tinyDB.getListObject("groupList", Group.class);
                                    GroupFragment.groupsData.add(group);
                                    GroupFragment.groupAdapter.notifyDataSetChanged();
                                } else {
                                    System.out.println("Error: " + e.getMessage());
                                }
                            }
                        });
                        finish();
                    } else {
                        e.printStackTrace();
                    }
                }
            });
        }
        else {
            finish();
        }
    }

    @Override
    public void onDialogPositiveClick(String username) {
        for (Object obj : knownMembers) {
            GroupMember mem = (GroupMember) obj;
            String _username = mem.getUsername();
            if (_username.equals(username)) {
                completionView.addObject(new GroupMember(mem.getName(), username, mem.getUserId()));
            }
        }
    }

    public void addMemberClick(View view) {
        dialog.show(manager, "addGroupMember");
    }
}
