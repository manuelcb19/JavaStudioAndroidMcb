package com.example.javastudio;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseDatabaseHelper {

    private final DatabaseReference parkingRef;

    public FirebaseDatabaseHelper() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://javastudio-36113-default-rtdb.europe-west1.firebasedatabase.app");
        this.parkingRef = database.getReference("parkingSpaces");
    }

    public void checkSpaceAvailability(String spaceId, SpaceAvailabilityCallback callback) {
        parkingRef.child(spaceId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Boolean isAvailable = task.getResult().getValue(Boolean.class);
                callback.onResult(isAvailable != null && isAvailable);
            } else {
                callback.onError("Error al obtener el estado de la plaza");
            }
        });
    }

    public void reserveSpace(String spaceId, boolean isAvailable, DatabaseUpdateCallback callback) {
        parkingRef.child(spaceId).setValue(isAvailable).addOnSuccessListener(aVoid -> {
            callback.onSuccess();
        }).addOnFailureListener(e -> {
            callback.onFailure("Error al actualizar el estado de la plaza");
        });
    }

    public void addParkingSpacesListener(ValueEventListener listener) {
        parkingRef.addValueEventListener(listener);
    }

    public interface SpaceAvailabilityCallback {
        void onResult(boolean isAvailable);
        void onError(String errorMessage);
    }

    public interface DatabaseUpdateCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }
}
