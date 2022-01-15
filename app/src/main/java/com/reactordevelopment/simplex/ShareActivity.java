package com.reactordevelopment.simplex;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
/**Activity to allow user to share file to server*/
public class ShareActivity extends AppCompatActivity {
    /**The file that is to be shared*/
    private static String[] sharedFile = {};
    /**The file that is selected from the database*/
    private static String[] selectedFile = {};
    /**The activity contents*/
    private Context context;
    /**Where the saves from the server are saved*/
    private LinearLayout saveHolder;
    /**File chooser to choose which file to upload*/
    private ActivityResultLauncher<String> openFileResult;
    /**Creates the views and initializes listeners*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        context = this;
        saveHolder = findViewById(R.id.saveHolder);
        //Opens file to be shared
        openFileResult = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if(uri == null) return;
                        //Parse URI into file path
                        String src = uri.getPath();
                        String[] split = src.split(":");
                        String location = Environment.getExternalStorageDirectory().toString();
                        if(!split[0].equals("/document/primary")){
                            Toast.makeText(ShareActivity.this, "Pick a file from local storage", Toast.LENGTH_LONG).show();
                            return;
                        }
                        //The contents of the file
                        String contents = MainActivity.openContents(context, location + "/" + split[1]);
                        sharedFile = new String[]{location + "/" + split[1], contents.replace("\n", "`n")};
                        //Get database instance
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference dbPath = database.getReference("user-code/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"-"+ format(sharedFile[0]));
                        //Update file in database
                        dbPath.setValue(Arrays.asList(1, sharedFile[1]));
                    }
                });
        //Signs the user out of their account
        findViewById(R.id.sendSignOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        //Returns the user to the main activity
        findViewById(R.id.sendToMain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShareActivity.this, MainActivity.class));
                finish();
            }
        });
        //Uploads the selected file to the database
        findViewById(R.id.uploadFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });
        //Opens the selected file from the database into the main activity
        findViewById(R.id.openSelected).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedFile.length == 2){
                    MainActivity.setDbText(selectedFile[1]);
                    onBackPressed();
                }
            }
        });
        //Gets all files from database
        findViewById(R.id.getFiles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFiles();
            }
        });
    }
    /**Checks current user on startup*/
    @Override
    public void onStart() {
        super.onStart();
        checkCurrentUser();
    }
    /**Handles back button action*/
    @Override
    public void onBackPressed() {
        //Finish activity and return to main
        Intent myIntent = new Intent(ShareActivity.this, MainActivity.class);
        startActivity(myIntent);
        finish();
    }
    /**Updates sharedFile with the given item*/
    public static void shareFile(String[] fileInfo){ sharedFile = fileInfo; }
    /**Fetches all files from database and adds them to saveHolder layout*/
    private void getFiles(){
        //Database instance
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("user-code");
        //Listener for when server data is changed
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                //For each save on the server
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String name = postSnapshot.getKey();
                    //Gets save title and content
                    ArrayList<Object> value = (ArrayList<Object>) postSnapshot.getValue();
                    //If the file is private
                    if(value.get(0).equals(0)) continue;
                    Log.i("Post", value.toString());
                    TextView save = new TextView(context);
                    String fileName = unFormat(name);
                    fileName = fileName.substring(fileName.indexOf("-")+1);
                    save.setText("Name: "+fileName+", Content: "+(""+value.get(1)).replace("`n", "\n"));
                    //Sets click listener for save
                    String finalFileName = fileName;
                    save.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            selectedFile = new String[]{finalFileName, (""+value.get(1)).replace("`n", "\n") };
                            for(int i=0; i<saveHolder.getChildCount(); i++)
                                saveHolder.getChildAt(i).setBackgroundColor(Color.parseColor("#00000000"));
                            save.setBackgroundColor(Color.parseColor("#AA00FF00"));
                        }
                    });
                    //Adds save to holder
                    saveHolder.addView(save);
                }
            }
            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
                Log.i("The read failed", ""+databaseError.getCode());
            }
        });
    }
    /**Uploads the shared tile to the database*/
    private void uploadFile(){
        //If there is no selected file
        if (sharedFile.length == 0)
            openFileResult.launch("*/*");
        else{
            //Get database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbPath = database.getReference("user-code/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"-"+ format(sharedFile[0]));
            //Upload shared file
            dbPath.setValue(Arrays.asList(1, sharedFile[1].replace("\n", "`n")));
        }
    }
    /**Formats the given text so it fits with firebase character constraints*/
    private String format(String str){
        return str.replace("~", "~0").replace(".", "~1").replace("#", "~2").replace("$", "~3").replace("[", "~4").replace("]", "~5");
    }
    /**Restores a formatted string to its original*/
    private String unFormat(String str){
        return str.replace("~0", "~").replace("~1", ".").replace("~2", "#").replace("~3", "$").replace("~4", "[").replace("~5", "]");
    }
    /**Signs the user ouf of their account and returns to the menu*/
    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(ShareActivity.this, MainActivity.class));
        finish();
    }
    /**Checks if a user is currently logged in*/
    private void checkCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //If there is a user logged in
        if (user != null) {
            //Display welcome message
            TextView title = findViewById(R.id.title);
            title.setText("Welcome\n"+user.getEmail());
        } else {
            //Launches login window
            Intent myIntent = new Intent(ShareActivity.this, LoginActivity.class);
            startActivity(myIntent);
        }
    }
}