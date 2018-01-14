package com.example.nancy.meetminus;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class SignUp extends AppCompatActivity {
    /**
     * Add new USERS to database!!
     * Handles peer update events from the mesh - maintains a list of peers and updates the display.
     *
     * @param e event object from mesh
     */

    User me;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();

        final EditText email = (EditText) findViewById(R.id.email);
        final EditText pass = (EditText) findViewById(R.id.password);
        final EditText name = (EditText) findViewById(R.id.name);
        final EditText username = (EditText) findViewById(R.id.username);
        final EditText number = (EditText) findViewById(R.id.number);

        Button btnSignUp = (Button) findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createUser(view, email.getText().toString(), pass.getText().toString());
                addNewUser(view,name.getText().toString(),username.getText().toString(),email.getText().toString(),pass.getText().toString(), number.getText().toString());


                Intent i = new Intent(SignUp.this, MainActivity.class);
                i.putExtra("MeUser", (Parcelable) me);
                startActivity(i);
            }
        });


    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

    }

    public void createUser(View view, String email, String pass){

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SIGNUP", "createUserWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("SIGNUP", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(SignUp.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }


                    }
                }

                );




    }


    public void addNewUser(View view, String name, String username, String email, String password, String number) {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        User user = new User(name, username, email, password, number);
        FirebaseDatabase.getInstance().getReference().child("Users").child(userID).setValue(user);
        me = user;
        //DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Users").child(userID);
    }

}



