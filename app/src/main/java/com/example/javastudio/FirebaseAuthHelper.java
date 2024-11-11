package com.example.javastudio;

import android.content.Context;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;

public class FirebaseAuthHelper {

    private final FirebaseAuth auth;

    public FirebaseAuthHelper() {
        this.auth = FirebaseAuth.getInstance();
    }

    public void logout(Context context, Class<?> targetActivity) {
        auth.signOut();
        Intent intent = new Intent(context, targetActivity);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }
}
