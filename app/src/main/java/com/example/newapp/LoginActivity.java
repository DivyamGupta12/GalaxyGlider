package com.example.newapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private EditText email;
    private EditText pass;
    private TextView sub;
    private TextView signup;
    private ImageView psw_show;
    private String loginMode;
    private boolean isCorrectLoginMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email_login_et);
        pass = findViewById(R.id.password_login_et);
        sub = findViewById(R.id.submit_login_tv);
        signup = findViewById(R.id.signup_tv);
        psw_show = findViewById(R.id.psd_eye);

        Intent intent = getIntent();
        loginMode = intent.getStringExtra("loginMode");

        if(loginMode.equals("admin")){
            signup.setVisibility(View.GONE);
        }

        // If already logged in then open the specific activity.
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {

            getCurrentUserLoginMode();

            Intent intent1 = null;
            if(isCorrectLoginMode) {
                if (loginMode.equals("owner")) {

                    String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    intent1 = new Intent(LoginActivity.this, SpaceShipList.class);
                    intent1.putExtra("companyID", companyId);

                } else {
                    intent1 = new Intent(LoginActivity.this, CompanyList.class);
                }
            }
            if (intent1 != null) {
                intent1.putExtra("loginMode", loginMode);
                startActivity(intent1);
                finish();
            }
        }

        psw_show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(pass.getTransformationMethod().equals(HideReturnsTransformationMethod.getInstance())){
                    //If password is visible then hide it.
                    pass.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    //Change icon
//                    psw_show.setImageResource(R.drawable.ic_hide_pwd);
                }
                else{
                    //If password is not visible the show it.
                    pass.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    //Change icon
//                    psw_show.setImageResource(R.drawable.ic_show_pwd);
                }
            }
        });

        //Submit button to login user
        sub.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Checking that text entered in the email and password is empty or not.
                if(email.getText().toString().isEmpty() || pass.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(),"Invalid Input",
                            Toast.LENGTH_SHORT).show();

                    return;
                }
                handleLogin();
            }
        });

        //Signup button navigates the user to MainActivity to register.
        signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent1 = new Intent(new Intent(LoginActivity.this, SignUpActivity.class));
                intent1.putExtra("loginMode",loginMode);
                startActivity(intent1);
            }
        });
    }

    //Login using email and password.
    private void handleLogin() {
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email.getText().toString(),pass.getText().toString())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(task.isSuccessful()){ //Checking if the task to sign in user was successful or not.

                            FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                            String currentUserEmail = firebaseUser.getEmail();
                            //If the user's email is verified then send him to allListsActivity
                            if(firebaseUser.isEmailVerified() || currentUserEmail.equals("admin2@gmail.com")){
                                saveLoginMode();
                                Toast.makeText(getApplicationContext(),"Logged in successful",Toast.LENGTH_SHORT).show();
                                Intent intent1 = null;
                                if(loginMode.equals("admin")){
                                    intent1 = new Intent(LoginActivity.this, CompanyList.class);
                                } else if(loginMode.equals("owner")) {
                                    String companyId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    intent1 = new Intent(LoginActivity.this, SpaceShipList.class);
                                    intent1.putExtra("companyID",companyId);
                                } else {
                                    intent1 = new Intent(LoginActivity.this, CompanyList.class);
                                }
                                intent1.putExtra("loginMode",loginMode);
                                intent1.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        | Intent.FLAG_ACTIVITY_NEW_TASK);

                                startActivity(intent1);
                                finish();
                            }
                            else{
                                //If the user's email is not verified then send him another email and sign out him.
                                firebaseUser.sendEmailVerification();
                                FirebaseAuth.getInstance().signOut();
                                //With the help of alertDialogBox user can directly open an app to see his emails.
                                showAlertDialogBox();
                            }
                        }
                        else{
                            //If the task is not successful toast the exception.
                            Toast.makeText(getApplicationContext(), "Slow Internet Connection",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void showAlertDialogBox() {
        //Building the alertDialog box.
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Email not verified")
                .setMessage("Please verify your email.You cannot use the app without email verification.")
                .setPositiveButton("Continue", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(Intent.ACTION_MAIN);

                        intent.addCategory(Intent.CATEGORY_APP_EMAIL); // To open an email app

                        // To open email app in new window not within our app so that on pressing back our app does not close.
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }).show();
    }

    private void saveLoginMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("loginMode",loginMode);
        editor.putString("email",FirebaseAuth.getInstance().getCurrentUser().getEmail());
        editor.apply();
    }

    private void getCurrentUserLoginMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("MySharedPrefs", Context.MODE_PRIVATE);

        String prevLoginMode = sharedPreferences.getString("loginMode", "");
        String prevLoginEmail = sharedPreferences.getString("email", "");
        String currentUserMail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (prevLoginEmail.equals(currentUserMail) && prevLoginMode.equals(loginMode)) {
            isCorrectLoginMode = true;
        } else {
            isCorrectLoginMode = false;
        }
    }
}