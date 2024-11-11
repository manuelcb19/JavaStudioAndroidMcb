package com.example.javastudio;

import android.os.Bundle;
import android.os.Handler;

import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Home extends AppCompatActivity {

    private GridLayout gridLayout;
    private FirebaseAuthHelper authHelper;
    private FirebaseDatabaseHelper databaseHelper;

    private Map<String, TextView> timers = new HashMap<>();
    private Map<String, Integer> timeRemaining = new HashMap<>();
    private Handler handler = new Handler();
    private final int TIME_LIMIT = 60;
    private final int TIME_LIMIT_DOWN = 5;
    private final float COST_PER_MINUTE = 0.05f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        authHelper = new FirebaseAuthHelper();
        databaseHelper = new FirebaseDatabaseHelper();


        gridLayout = findViewById(R.id.gridLayout);

        iniciarGrid();
        setupFirebaseListener();


        findViewById(R.id.logoutButton).setOnClickListener(v -> authHelper.logout(this, MainActivity.class));
    }

    private void iniciarGrid() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String spaceId = "space_" + i + "_" + j;
                Button button = createParkingButton(spaceId, i, j);
                gridLayout.addView(button);

                TextView timerTextView = createTimerTextView(spaceId, i, j);
                LinearLayout timerLayout = findViewById(R.id.timerLayout);
                timerLayout.addView(timerTextView);
            }
        }
    }

    private Button createParkingButton(String spaceId, int i, int j) {
        Button button = new Button(this);
        button.setTag(spaceId);
        button.setLayoutParams(new GridLayout.LayoutParams(GridLayout.spec(i), GridLayout.spec(j)));
        button.setBackgroundResource(R.drawable.button_selector);
        button.setText("Plaza " + (i * 3 + j + 1));
        button.setOnClickListener(v -> checkAndShowDialog(spaceId));
        return button;
    }

    private TextView createTimerTextView(String spaceId, int i, int j) {
        TextView timerTextView = new TextView(this);
        int plazaNumber = i * 3 + j + 1;
        timerTextView.setText("Plaza " + plazaNumber + ": 00:00");
        timerTextView.setTextSize(14);
        timerTextView.setGravity(Gravity.CENTER);
        timers.put(spaceId, timerTextView);
        return timerTextView;
    }

    private void checkAndShowDialog(String spaceId) {
        databaseHelper.checkSpaceAvailability(spaceId, new FirebaseDatabaseHelper.SpaceAvailabilityCallback() {
            @Override
            public void onResult(boolean isAvailable) {
                if (isAvailable) {
                    showTimeSelectionDialog(spaceId);
                } else {
                    showDialog(spaceId, "¿Desea liberar esta plaza?", true);
                }
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(Home.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showTimeSelectionDialog(String spaceId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Seleccionar tiempo de reserva");

        NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(TIME_LIMIT_DOWN);
        numberPicker.setMaxValue(TIME_LIMIT);
        numberPicker.setValue(TIME_LIMIT_DOWN);
        builder.setView(numberPicker);

        builder.setPositiveButton("Calcular precio", (dialog, which) -> {
            int selectedTime = numberPicker.getValue();
            float totalCost = selectedTime * COST_PER_MINUTE;
            showConfirmationDialog(spaceId, selectedTime, totalCost);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void showConfirmationDialog(String spaceId, int selectedTime, float totalCost) {
        AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
        confirmBuilder.setTitle("Confirmar reserva");
        confirmBuilder.setMessage("El precio por alquilar esta plaza por " + selectedTime + " segundos es: " + String.format("%.2f", totalCost) + " €");

        confirmBuilder.setPositiveButton("Reservar", (dialog, which) -> reserveSpaceForTime(spaceId, selectedTime));
        confirmBuilder.setNegativeButton("Cancelar", null);
        confirmBuilder.show();
    }

    private void reserveSpaceForTime(String spaceId, int selectedTime) {
        databaseHelper.reserveSpace(spaceId, false, new FirebaseDatabaseHelper.DatabaseUpdateCallback() {
            @Override
            public void onSuccess() {
                Button button = gridLayout.findViewWithTag(spaceId);
                if (button != null) {
                    button.setEnabled(false);
                    button.setBackgroundResource(R.drawable.button_occupied);
                }
                startTimer(spaceId, selectedTime);
                Toast.makeText(Home.this, "Plaza reservada por " + selectedTime + " minutos. Costo: " + String.format("%.2f", selectedTime * COST_PER_MINUTE) + " €", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Home.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDialog(String spaceId, String message, boolean isOccupied) {
        new AlertDialog.Builder(this)
                .setTitle(isOccupied ? "Liberar espacio" : "Reservar espacio")
                .setMessage(message)
                .setPositiveButton("Sí", (dialog, which) -> toggleSpaceReservation(spaceId, isOccupied))
                .setNegativeButton("No", null)
                .show();
    }

    private void toggleSpaceReservation(String spaceId, boolean isOccupied) {
        databaseHelper.reserveSpace(spaceId, !isOccupied, new FirebaseDatabaseHelper.DatabaseUpdateCallback() {
            @Override
            public void onSuccess() {
                Button button = gridLayout.findViewWithTag(spaceId);
                if (button != null) {
                    if (isOccupied) {
                        button.setEnabled(true);
                        button.setBackgroundResource(R.drawable.button_available);
                    } else {
                        button.setEnabled(false);
                        startTimer(spaceId);
                    }
                }
                Toast.makeText(Home.this, isOccupied ? "Plaza liberada" : "Plaza reservada", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(Home.this, errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFirebaseListener() {
        databaseHelper.addParkingSpacesListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot spaceSnapshot : snapshot.getChildren()) {
                    String spaceId = spaceSnapshot.getKey();
                    Boolean isAvailable = spaceSnapshot.getValue(Boolean.class);
                    Button button = gridLayout.findViewWithTag(spaceId);
                    if (button != null) {
                        button.setEnabled(Boolean.TRUE.equals(isAvailable));
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Home.this, "Error de conexión con Firebase", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startTimer(String spaceId, int selectedTime) {
        timeRemaining.put(spaceId, selectedTime);
        updateTimer(spaceId);
    }

    private void startTimer(String spaceId) {
        timeRemaining.put(spaceId, TIME_LIMIT);
        updateTimer(spaceId);
    }

    private void updateTimer(String spaceId) {
        if (timeRemaining.containsKey(spaceId)) {
            int timeLeft = timeRemaining.get(spaceId);

            if (timeLeft <= 0) {
                databaseHelper.reserveSpace(spaceId, true, new FirebaseDatabaseHelper.DatabaseUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Button button = gridLayout.findViewWithTag(spaceId);
                        if (button != null) {
                            button.setEnabled(true);
                            button.setBackgroundResource(R.drawable.button_available);
                        }
                        timers.get(spaceId).setText("Plaza " + getPlazaNumber(spaceId) + ": 00:00");
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Toast.makeText(Home.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
                timeRemaining.remove(spaceId);
                return;
            }

            int minutes = timeLeft / 60;
            int seconds = timeLeft % 60;
            timers.get(spaceId).setText("Plaza " + getPlazaNumber(spaceId) + ": " + String.format("%02d:%02d", minutes, seconds));

            timeRemaining.put(spaceId, timeLeft - 1);
            handler.postDelayed(() -> updateTimer(spaceId), 1000);
        }
    }

    private int getPlazaNumber(String spaceId) {
        String[] parts = spaceId.split("_");
        int i = Integer.parseInt(parts[1]);
        int j = Integer.parseInt(parts[2]);
        return i * 3 + j + 1;
    }
}