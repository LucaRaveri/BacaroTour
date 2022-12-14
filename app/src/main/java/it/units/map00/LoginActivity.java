package it.units.map00;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText mMail;
    private EditText mPassword;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
    }


    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }

    private void updateUI(FirebaseUser currentUser) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {

                Intent intent3 = new Intent(this, MainActivity.class);
                finish();
                startActivity(intent3);

        }

    }


    public void btnLoginClick(View view) {

        mMail = findViewById(R.id.etRegMail);
        mPassword = findViewById(R.id.etRegPass);

        String mail = mMail.getText().toString();
        String password = mPassword.getText().toString();

        if(!(mail.length()>7)||!(mail.contains("@"))) {
            Toast.makeText(this, "email non valida", Toast.LENGTH_LONG).show();
            return;
        }else if(!(password.length()>5)){
            Toast.makeText(this, "password troppo corta", Toast.LENGTH_LONG).show();
            return;

        }else {
            loginUser(mail, password);
        }

    }

    private void loginUser(String email, String password) {

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(getApplicationContext(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }
                });
    }

    public void tvRegistratiClick(View view) {

        Intent intent1 = new Intent(this, RegisterActivity.class);
        finish();
        startActivity(intent1);

    }
}