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

public class MainActivity extends AppCompatActivity {
    private static final String EDITOR = "editor";
    private static final String BUILD = "build";
    private static final String EXECUTE = "execute";
    private static final int OPEN_CODE = 0;
    private static String currentTab = EDITOR;
    private Context context;
    private int STORAGE_PERMISSION_CODE = 1;
    private String editingFile = "";
    private ConstraintLayout saveAsHolder;
    private EditText newFileName;
    private Button submitSaveAs;
    private Button cancelSaveAs;
    private ActivityResultLauncher<String> openFileResult;
    private ActivityResultLauncher<Uri> chooseDirectory;
    private String saveAsPath;
    private int screenHeight;
    private int screenWidth;
    private Editor editorFrag;
    private Build buildFrag;
    private Execute executeFrag;
    private static Wrapper waiting = null;
    private static final File[] openFile = new File[1];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
        saveAsHolder = findViewById(R.id.saveAsHolder);
        newFileName = findViewById(R.id.newFileName);
        newFileName.setShowSoftInputOnFocus(false);
        submitSaveAs = findViewById(R.id.submitSaveAs);
        cancelSaveAs = findViewById(R.id.cancelSaveAs);
        editorFrag = new Editor();
        buildFrag = new Build(editorFrag);
        executeFrag = new Execute(editorFrag);
        TextView saveAsWarning = findViewById(R.id.saveAsWarning);
        getSupportFragmentManager()
                .beginTransaction().setReorderingAllowed(true)
                .add(R.id.fragmentView, editorFrag, EDITOR).commit();

        openFileResult = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if(uri == null) return;
                        String src = uri.getPath();
                        String[] split = src.split(":");
                        String location = Environment.getExternalStorageDirectory().toString();
                        if(!split[0].equals("/document/primary")){
                            Toast.makeText(MainActivity.this, "Pick a file from local storage", Toast.LENGTH_LONG).show();
                            return;
                        }
                        editorFrag.append(openContents(context, location + "/" + split[1]));
                    }
                });
        chooseDirectory = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), new ActivityResultCallback<Uri>() {
            @Override
            public void onActivityResult(Uri uri) {
                String src = uri.getPath();
                saveAsPath = src.split(":")[1];
                saveAsWarning.setText("");
                if(openFile[0] != null) newFileName.setText(openFile[0].getName());

                submitSaveAs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(newFileName.getText().length() != 0) {
                            saveAsWarning.setText("");
                            saveFileAs(saveAsPath, newFileName.getText().toString());
                            saveAsHolder.animate().y(-saveAsHolder.getMeasuredHeight()).setDuration(500).start();
                            editorFrag.keyboardFocusTo(null);
                            if(waiting != null) {
                                waiting.execute();
                                waiting = null;
                            }
                        }else saveAsWarning.setText("File name cannot be blank!");
                    }
                });
                cancelSaveAs.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        saveAsHolder.animate().y(-saveAsHolder.getMeasuredHeight()).setDuration(500).start();
                        editorFrag.keyboardFocusTo(null);
                    }
                });
                newFileName.requestFocus();
                editorFrag.keyboardFocusTo(newFileName);
                saveAsHolder.animate().y(screenHeight/3f).setDuration(500).start();
            }
        });
        findViewById(R.id.newFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFile[0] = null;
                editorFrag.clear();
            }
        });

        findViewById(R.id.openFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editorFrag.clear();
                openFile();
            }
        });
        findViewById(R.id.saveFile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveFile();
            }
        });
        findViewById(R.id.saveFileAs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseDirectory.launch(null);
            }
        });
        findViewById(R.id.toShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toShare");
                /*waiting = new Wrapper() {
                    @Override
                    public void execute() {
                        ShareActivity.shareFile(new String[]{openFile[0].getName(), editorFrag.getCode()});
                        Intent myIntent = new Intent(MainActivity.this, ShareActivity.class);
                        startActivity(myIntent);
                    }
                };*/
                if(openFile[0] != null) {
                    saveFile();
                    ShareActivity.shareFile(new String[]{openFile[0].getName(), editorFrag.getCode()});
                }

                Intent myIntent = new Intent(MainActivity.this, ShareActivity.class);
                startActivity(myIntent);
            }
        });
        findViewById(R.id.toEditor).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toEdit");
                switchTab(EDITOR);
            }
        });
        findViewById(R.id.toBuild).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toBuild");
                switchTab(BUILD);
            }
        });
        findViewById(R.id.toExecute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i("Transfer", "toExe");
                switchTab(EXECUTE);
            }
        });
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            makeDirs();

        } else {
            requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    STORAGE_PERMISSION_CODE);
        }
    }
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
    public void openFile(){
        openFileResult.launch("*/*");
    }
    public static String openContents(Context context, String path){
        FileInputStream fis;
        String content = "";
        openFile[0] = new File(path);
        try {
            fis = new FileInputStream(openFile[0]);
            int nextChar;
            while((nextChar = fis.read()) != -1)
                content += (char)nextChar;
            Toast.makeText(context, "Opened"+path, Toast.LENGTH_LONG).show();
        } catch (Exception e) { e.printStackTrace(); }
        ShareActivity.shareFile(new String[]{openFile[0].getName(), content});

        return content;
    }
    private void saveFile(){
        if(openFile[0] != null){
            try {
                FileOutputStream stream = new FileOutputStream(openFile[0]);
                stream.write(editorFrag.getCode().getBytes());
                stream.close();
            }
            catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }
            if(waiting != null){
                waiting.execute();
                waiting = null;
            }
        }
        else chooseDirectory.launch(null);

    }
    private void saveFileAs(String path, String fileName){
        openFile[0] = new File(Environment.getExternalStorageDirectory().toString()+"/"+path+"/"+fileName);
        saveFile();
    }
    private void makeDirs() {
        String path = Environment.getExternalStorageDirectory().getPath() + "/Simplex";
        File dir = new File(path);
        if (!dir.exists()) if (!dir.mkdir()) Log.i("No", "nomake");
        dir = new File(path+"/Code");
        if(!dir.exists()) if(!dir.mkdir()) Log.i("No2", "nomakeCode");
        FileOutputStream fos;
        File save = new File(path+"/Code", "helloWold.spx");
        String contents = "#Hello World";
        try {
            fos = new FileOutputStream(save);
            fos.write(contents.getBytes());
            fos.close();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void switchTab(String tabId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.detach(fragmentManager.findFragmentByTag(currentTab));
        if(fragmentManager.findFragmentByTag(BUILD) == null && tabId.equals("build"))
            fragmentManager
                    .beginTransaction().setReorderingAllowed(true)
                    .add(R.id.fragmentView, buildFrag, BUILD).commit();
        else if(fragmentManager.findFragmentByTag(EXECUTE) == null && tabId.equals("execute"))
            fragmentManager
                    .beginTransaction().setReorderingAllowed(true)
                    .add(R.id.fragmentView, executeFrag, EXECUTE).commit();
        else fragmentTransaction.attach(fragmentManager.findFragmentByTag(tabId));

        //fragmentTransaction.addToBackStack(null);

        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        currentTab = tabId;
    }

    public static String stringResource(int resId){
        return Resources.getSystem().getString(resId);
    }
}