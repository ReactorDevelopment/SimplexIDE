package com.reactordevelopment.simplex;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
/**Holds all main tabs (edit, build, execute) and the ability to manipulate files*/
public class MainActivity extends AppCompatActivity {
    /**The tag for an editor fragment*/
    private static final String EDITOR = "editor";
    /**The tag for a build fragment*/
    private static final String BUILD = "build";
    /**The tag for an execute fragment*/
    private static final String EXECUTE = "execute";
    /**The tag of the tab that is currently open*/
    private static String currentTab = EDITOR;
    /**The tag of the tab that is currently open*/
    private static String fromDB = null;
    /**The context for the activity*/
    private Context context;
    /**The request code for storage permissions*/
    private final int STORAGE_PERMISSION_CODE = 1;
    /**Popup menu for saving a file as*/
    private ConstraintLayout saveAsHolder;
    /**The save as input for the file name*/
    private EditText newFileName;
    /**Saves the file as*/
    private Button submitSaveAs;
    /**Cancels the save as operation*/
    private Button cancelSaveAs;
    /**Launcher for choosing a file*/
    private ActivityResultLauncher<String> openFileResult;
    /**Launcher for choosing a directory*/
    private ActivityResultLauncher<Uri> chooseDirectory;
    /**The directory that the file will be saved to*/
    private String saveAsPath;
    /**The height of the device in pixels*/
    private int screenHeight;
    /**The editor fragment*/
    private Editor editorFrag;
    /**The build fragment*/
    private Build buildFrag;
    /**The execute fragment*/
    private Execute executeFrag;
    /**Code saved to be executed later*/
    private static Wrapper waiting = null;
    /**The file that is currently open*/
    private static final File[] openFile = new File[1];

    /**Creates the view with all of its buttons and tabs*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        //Gets height of screen
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        //Initialize views
        saveAsHolder = findViewById(R.id.saveAsHolder);
        newFileName = findViewById(R.id.newFileName);
        newFileName.setShowSoftInputOnFocus(false);
        submitSaveAs = findViewById(R.id.submitSaveAs);
        cancelSaveAs = findViewById(R.id.cancelSaveAs);
        TextView saveAsWarning = findViewById(R.id.saveAsWarning);
        //Initialize tabs
        editorFrag = new Editor();
        buildFrag = new Build(editorFrag);
        executeFrag = new Execute(editorFrag);
        //Fragment manager
        getSupportFragmentManager()
                .beginTransaction().setReorderingAllowed(true)
                .add(R.id.fragmentView, editorFrag, EDITOR).commit();
        //Opens file
        openFileResult = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if(uri == null) return;
                        //Parses URI into file path
                        String src = uri.getPath();
                        String[] split = src.split(":");
                        String location = Environment.getExternalStorageDirectory().toString();
                        //If the selected file is not in local storage
                        if(!split[0].equals("/document/primary")){
                            Toast.makeText(MainActivity.this, "Pick a file from local storage", Toast.LENGTH_LONG).show();
                            return;
                        }
                        //Add file contents to editor
                        editorFrag.append(openContents(context, location + "/" + split[1]));
                    }
                });
        //Directory chooser
        chooseDirectory = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri uri) {
                //Decode UIR into directory path
                String src = uri.getPath();
                saveAsPath = src.split(":")[1];
                saveAsWarning.setText("");
                //File name
                if(openFile[0] != null) newFileName.setText(openFile[0].getName());
                //Updates save as button
                submitSaveAs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(newFileName.getText().length() != 0) {
                            saveAsWarning.setText("");
                            //Save file to path selected by directory chooser
                            saveFileAs(saveAsPath, newFileName.getText().toString());
                            //Disappear upwards
                            saveAsHolder.animate().y(-saveAsHolder.getMeasuredHeight()).setDuration(500).start();
                            //Resets keyboard focus t default editor
                            editorFrag.keyboardFocusTo(null);
                            //Execute saved code
                            if(waiting != null) {
                                waiting.execute();
                                waiting = null;
                            }
                        }else saveAsWarning.setText(R.string.fileBlankWarn);
                    }
                });
                //Set keyboard focus to file name input
                newFileName.requestFocus();
                editorFrag.keyboardFocusTo(newFileName);
                //Animate dropworn onto the screen
                saveAsHolder.animate().y(screenHeight/3f).setDuration(500).start();
            }
        });
        //Listener to cancel save as and remove dropdown
        cancelSaveAs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAsHolder.animate().y(-saveAsHolder.getMeasuredHeight()).setDuration(500).start();
                editorFrag.keyboardFocusTo(null);
            }
        });
        //Replaces the currently opened file with a new file
        findViewById(R.id.newFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFile[0] = null;
                editorFrag.clear();
            }
        });
        //Opens a selected file
        findViewById(R.id.openFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorFrag.clear();
                openFile();
            }
        });
        //Saves the currently opened file
        findViewById(R.id.saveFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFile();
            }
        });
        //Saves the currently opened file as a different file
        findViewById(R.id.saveFileAs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseDirectory.launch(null);
            }
        });
        //Opens share menu
        findViewById(R.id.toShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toShare");
                //Saves opened file
                if(openFile[0] != null) {
                    saveFile();
                    ShareActivity.shareFile(new String[]{openFile[0].getName(), editorFrag.getCode()});
                }
                //Opens activity
                Intent myIntent = new Intent(MainActivity.this, ShareActivity.class);
                startActivity(myIntent);
            }
        });
        //Switches the tab to editor
        findViewById(R.id.toEditor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toEdit");
                switchTab(EDITOR);
            }
        });
        //Switches the tab to build
        findViewById(R.id.toBuild).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toBuild");
                switchTab(BUILD);
            }
        });
        //Switches the tab to execute
        findViewById(R.id.toExecute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toExe");
                switchTab(EXECUTE);
            }
        });
        //If permissions are granted
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //Make file dirs
            makeDirs();
        }
        //If not
        else {
            //Request permissions
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_CODE);
        }
    }
    /**Requests storage permissions from system*/
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i("Permissions", "reuwest");
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                Log.i("Perm", "" + (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED));
            } else
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
    }
    /**Opens a file by launching the file select*/
    public void openFile(){
        openFileResult.launch("*/*");
    }
    /**Reads and returns the contents of a text file at the given path*/
    public static String openContents(Context context, String path){
        FileInputStream fis;
        String content = "";
        openFile[0] = new File(path);
        try {
            //Open file
            fis = new FileInputStream(openFile[0]);
            int nextChar;
            //Append characters to content
            while((nextChar = fis.read()) != -1)
                content += (char)nextChar;
            Toast.makeText(context, "Opened"+path, Toast.LENGTH_LONG).show();
        } catch (Exception e) { e.printStackTrace(); }
        //Marks the file to be the one shared by save activity
        ShareActivity.shareFile(new String[]{openFile[0].getName(), content});
        return content;
    }
    /**Saves the currently file to its location
     * Saves the file to a new file if there is no existing file opened currently opened*/
    private void saveFile(){
        //If a file is opened
        if(openFile[0] != null){
            try {
                //Write file contents
                FileOutputStream stream = new FileOutputStream(openFile[0]);
                stream.write(editorFrag.getCode().getBytes());
                stream.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
            //Execute saved code
            if(waiting != null){
                waiting.execute();
                waiting = null;
            }
        }
        //Save as if there is no file opened
        else chooseDirectory.launch(null);

    }
    /**Preforms a fave operation on a new file at the given path with the given name*/
    private void saveFileAs(String path, String fileName){
        openFile[0] = new File(Environment.getExternalStorageDirectory().toString()+"/"+path+"/"+fileName);
        saveFile();
    }
    /**Makes simplex home directories*/
    private void makeDirs() {
        //Makes directories
        String path = Environment.getExternalStorageDirectory().getPath() + "/Simplex";
        File dir = new File(path);
        if (!dir.exists()) if (!dir.mkdir()) Log.i("No", "nomake");
        dir = new File(path+"/Code");
        if(!dir.exists()) if(!dir.mkdir()) Log.i("No2", "nomakeCode");
        FileOutputStream fos;
        //Add example file to code directory
        File save = new File(path+"/Code", "helloWold.spx");
        String contents = "#Hello World";
        try {
            fos = new FileOutputStream(save);
            fos.write(contents.getBytes());
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }
    /**Switches from the current tab to the tab with the given tag*/
    private void switchTab(String tabId) {
        //Get frag manager
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        //Detach old tab
        fragmentTransaction.detach(fragmentManager.findFragmentByTag(currentTab));
        //Add or create tabs
        if(fragmentManager.findFragmentByTag(BUILD) == null && tabId.equals("build"))
            fragmentManager
                    .beginTransaction().setReorderingAllowed(true)
                    .add(R.id.fragmentView, buildFrag, BUILD).commit();
        else if(fragmentManager.findFragmentByTag(EXECUTE) == null && tabId.equals("execute"))
            fragmentManager
                    .beginTransaction().setReorderingAllowed(true)
                    .add(R.id.fragmentView, executeFrag, EXECUTE).commit();
        else fragmentTransaction.attach(fragmentManager.findFragmentByTag(tabId));
        //Switch tabs
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        //Refresh tab id
        currentTab = tabId;
    }
    /**Sets fromDB to the given string*/
    public static void setDbText(String text){ fromDB = text; }
    /**Returns the text that was selected from the database*/
    public static String dbText(){ return fromDB; }
}