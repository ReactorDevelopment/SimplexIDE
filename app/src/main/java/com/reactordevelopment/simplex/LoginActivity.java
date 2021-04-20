package com.reactordevelopment.simplex;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginActivity extends Activity {
    private static final String TAG = "EmailPassword";
    private FirebaseAuth mAuth;
    private EditText emailField;
    private EditText passWordField;
    private Button sendLogin;
    private Button createAccount;
    private Button backSignIn;
    private EditText confirmPassword;
    private boolean isCreating;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        isCreating = false;
        emailField = findViewById(R.id.loginEmail);
        passWordField = findViewById(R.id.loginPassword);
        confirmPassword = findViewById(R.id.confirmPassword);

        backSignIn = findViewById(R.id.backSignIn);
        sendLogin = findViewById(R.id.sendLogin);
        createAccount = findViewById(R.id.createAccount);
        sendLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailRegEx = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,4}$";
                Pattern pattern = Pattern.compile(emailRegEx);
                Matcher matcher = pattern.matcher(emailField.getText().toString());
                if (emailField.getText().toString().isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email", Toast.LENGTH_LONG).show();
                } else if (!matcher.find()) {
                    Toast.makeText(LoginActivity.this, "Not an email", Toast.LENGTH_LONG).show();
                } else
                    signIn(emailField.getText().toString(), passWordField.getText().toString());

            }
        });
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isCreating){
                    createAccountView();
                    return;
                }
                String emailRegEx = "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,4}$";
                Pattern pattern = Pattern.compile(emailRegEx);
                Matcher matcher = pattern.matcher(emailField.getText().toString());
                if (emailField.getText().toString().isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email", Toast.LENGTH_LONG).show();
                } else if (!matcher.find()) {
                    Toast.makeText(LoginActivity.this, "Not an email", Toast.LENGTH_LONG).show();
                } else {
                    if(passWordField.getText().toString().equals(confirmPassword.getText().toString()))
                        if(passWordField.getText().toString().length() >= 6)
                            createAccount(emailField.getText().toString(), passWordField.getText().toString());
                        else
                            Toast.makeText(LoginActivity.this, "Password must be at least 6 characters long", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(LoginActivity.this, "Password fields must match", Toast.LENGTH_LONG).show();
                }
            }
        });
        backSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCreating) signInView();
                else
                    onBackPressed();
            }
        });
    }

    /*@Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            reload();
        }
    }*/
    @Override
    public void onBackPressed() {
        if(mAuth.getCurrentUser() == null){
            Intent myIntent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(myIntent);
            finish();
        }
        else
            super.onBackPressed();
    }
    private void signInView(){
        isCreating = false;
        sendLogin.setVisibility(View.VISIBLE);
        confirmPassword.setVisibility(View.INVISIBLE);
    }
    private void createAccountView(){
        isCreating = true;
        sendLogin.setVisibility(View.INVISIBLE);
        confirmPassword.setVisibility(View.VISIBLE);
    }

    private void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "createUserWithEmail:success");
                            Toast.makeText(LoginActivity.this, "Account Created.",
                                    Toast.LENGTH_SHORT).show();
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            if(task.getException() != null)
                                Toast.makeText(LoginActivity.this, "Account Creation failed.\n"+task.getException().getMessage(),
                                        Toast.LENGTH_LONG).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    }
                });
    }

    private void sendEmailVerification() {
        // Send verification email
        final FirebaseUser user = mAuth.getCurrentUser();
        user.sendEmailVerification()
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        // Email sent
                    }
                });
    }

    private void reload() { }

    private void updateUI(FirebaseUser user) {
        if(user == null) {
            emailField.setText("");
            passWordField.setText("");
            confirmPassword.setText("");
            if(!isCreating) signInView();
        }
        else
            onBackPressed();
    }
}