package com.reactordevelopment.simplex;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
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

public class ShareActivity extends AppCompatActivity {
    private static String[] sharedFile = {};
    private Context context;
    private LinearLayout saveHolder;
    private ActivityResultLauncher<String> openFileResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        context = this;
        saveHolder = findViewById(R.id.saveHolder);
        openFileResult = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if(uri == null) return;
                        String src = uri.getPath();
                        String[] split = src.split(":");
                        String location = Environment.getExternalStorageDirectory().toString();
                        if(!split[0].equals("/document/primary")){
                            Toast.makeText(ShareActivity.this, "Pick a file from local storage", Toast.LENGTH_LONG).show();
                            return;
                        }
                        String contents = MainActivity.openContents(context, location + "/" + split[1]);
                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                        DatabaseReference dbPath = database.getReference("user-code/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"-"+ format(sharedFile[0]));
                        dbPath.setValue(Arrays.asList(1, sharedFile[1]));
                    }
                });
        findViewById(R.id.sendSignOut).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
        findViewById(R.id.sendToMain).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ShareActivity.this, MainActivity.class));
                finish();
            }
        });
        findViewById(R.id.uploadFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFile();
            }
        });
        findViewById(R.id.getFiles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFiles();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        checkCurrentUser();
    }
    public static void shareFile(String[] fileInfo){ sharedFile = fileInfo; }

    private void getFiles(){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("user-code");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NotNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String name = postSnapshot.getKey();
                    ArrayList<Object> value = (ArrayList<Object>) postSnapshot.getValue();

                    if(value.get(0).equals(0)) continue;
                    Log.i("Post", value.toString());
                    TextView save = new TextView(context);
                    save.setText("Name: "+unFormat(name)+", Content: "+value.get(0)+", "+value.get(1));
                    saveHolder.addView(save);
                }
            }

            @Override
            public void onCancelled(@NotNull DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }
    private void uploadFile(){
        if (Arrays.equals(sharedFile, new Object[]{}))
            openFileResult.launch("*/*");
        else{
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference dbPath = database.getReference("user-code/"+FirebaseAuth.getInstance().getCurrentUser().getUid()+"-"+ format(sharedFile[0]));
            dbPath.setValue(Arrays.asList(1, sharedFile[1]));
        }
    }
    private String format(String str){
        return str.replace("~", "~0").replace(".", "~1").replace("#", "~2").replace("$", "~3").replace("[", "~4").replace("]", "~5");
    }
    private String unFormat(String str){
        return str.replace("~0", "~").replace("~1", ".").replace("~2", "#").replace("~3", "$").replace("~4", "[").replace("~5", "]");
    }
    private void signOut(){
        FirebaseAuth.getInstance().signOut();
        startActivity(new Intent(ShareActivity.this, MainActivity.class));
        finish();
    }
    private void checkCurrentUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            TextView title = findViewById(R.id.title);
            title.setText("Welcome\n"+user.getEmail());
        } else {
            Intent myIntent = new Intent(ShareActivity.this, LoginActivity.class);
            startActivity(myIntent);
        }
    }
    public static class Post {
        public int visibility;
        public String content;

        public Post(){
            visibility = -1;
            content = null;
        }
        public Post(int vis, String cont) {
            visibility = vis;
            content = cont;
        }

        @NotNull
        public String toString(){
            return visibility+", "+(content != null ? content : "");
        }

    }
}