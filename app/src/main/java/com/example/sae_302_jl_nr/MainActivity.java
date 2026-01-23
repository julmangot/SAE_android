package com.example.sae_302_jl_nr;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvDate;
    private ImageButton btnPrev, btnNext;
    private FloatingActionButton fabAdd;

    private RecyclerView rvInterventions;
    private InterventionAdapter adapter;

    private LocalDate currentDate;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private static final String API_BASE = "http://51.38.176.17";
    private static final String API_LIST = API_BASE + "/interventions.php?date=";

    private RequestQueue requestQueue;

    private ActivityResultLauncher<Intent> detailLauncher;

    // Swipe
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 120;
    private static final int SWIPE_VELOCITY_THRESHOLD = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Swipe detector (gauche/droite)
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // On ne traite que les swipes horizontaux
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            // Swipe droite = jour précédent
                            changeDay(-1);
                        } else {
                            // Swipe gauche = jour suivant
                            changeDay(+1);
                        }
                        return true;
                    }
                }
                return false;
            }
        });

        // Views
        tvDate = findViewById(R.id.tvDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        fabAdd = findViewById(R.id.fabAdd);

        rvInterventions = findViewById(R.id.rvInterventions);
        rvInterventions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InterventionAdapter();
        rvInterventions.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        // Result launcher (Detail + Add)
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        boolean changed = result.getData().getBooleanExtra("changed", false);
                        if (changed) reloadInterventionsForDay();
                    }
                }
        );

        // Click item -> Detail
        adapter.setOnInterventionClickListener(intervention -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);

            intent.putExtra("reference", intervention.idMission);
            intent.putExtra("date", intervention.dateIntervention.toString());

            intent.putExtra("statut", intervention.statut);
            intent.putExtra("priorite", intervention.prioriteStr);

            intent.putExtra("type", intervention.type);
            intent.putExtra("technicien", intervention.technicien);
            intent.putExtra("adresse", intervention.adresse);
            intent.putExtra("ville", intervention.ville);
            intent.putExtra("action", intervention.action);
            intent.putExtra("duree", intervention.duree);
            intent.putExtra("materiel", intervention.materiel);

            detailLauncher.launch(intent);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now();
            updateDateLabel();
            reloadInterventionsForDay();

            // Boutons prev/next
            btnPrev.setOnClickListener(v -> changeDay(-1));
            btnNext.setOnClickListener(v -> changeDay(+1));

            // DatePicker au clic sur la date
            tvDate.setOnClickListener(v -> {
                int year = currentDate.getYear();
                int month = currentDate.getMonthValue() - 1;
                int day = currentDate.getDayOfMonth();

                DatePickerDialog dialog = new DatePickerDialog(
                        MainActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            currentDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay);
                            updateDateLabel();
                            reloadInterventionsForDay();
                        },
                        year, month, day
                );
                dialog.show();
            });

            // FAB -> AddInterventionActivity
            fabAdd.setOnClickListener(v -> {
                Intent i = new Intent(MainActivity.this, AddInterventionActivity.class);
                i.putExtra("date", currentDate.toString()); // pré-remplir la date
                detailLauncher.launch(i);
            });

        } else {
            tvDate.setText("Date");
            adapter.setData(new ArrayList<>());
        }
    }

    private void updateDateLabel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
            tvDate.setText(currentDate.format(formatter));
        }
    }

    private void changeDay(int deltaDays) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
            currentDate = currentDate.plusDays(deltaDays);
            updateDateLabel();
            reloadInterventionsForDay();
        }
    }

    private void reloadInterventionsForDay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        String url = API_LIST + currentDate;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    ArrayList<Intervention> list = new ArrayList<>();

                    for (int idx = 0; idx < response.length(); idx++) {
                        JSONObject o = response.optJSONObject(idx);
                        if (o == null) continue;

                        String reference = o.optString("reference", "");
                        String dateStr = o.optString("date_intervention", currentDate.toString());
                        if (dateStr.contains(" ")) dateStr = dateStr.split(" ")[0];

                        String type = o.optString("type_intervention", "");
                        String prioriteStr = o.optString("priorite", "Basse");
                        String technicien = o.optString("technicien", "");
                        String adresse = o.optString("adresse", "");
                        String ville = o.optString("ville", "");
                        String action = o.optString("action_realisee", "");
                        String duree = o.optString("duree", "");
                        String materiel = o.optString("materiel", "");
                        String statut = o.optString("statut", "Planifiée");

                        LocalDate d;
                        try { d = LocalDate.parse(dateStr); }
                        catch (Exception e) { d = currentDate; }

                        String libelleCourt = d.getDayOfMonth() + " " +
                                d.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH);

                        Intervention in = new Intervention(
                                reference,
                                libelleCourt,
                                d,
                                type,
                                prioriteStr,
                                technicien,
                                adresse,
                                ville,
                                action,
                                duree,
                                materiel,
                                statut
                        );

                        int prioriteInt = 1;
                        String p = (prioriteStr == null) ? "" : prioriteStr.toLowerCase(Locale.ROOT);
                        if (p.contains("haut")) prioriteInt = 3;
                        else if (p.contains("moy")) prioriteInt = 2;
                        in.priorite = prioriteInt;

                        list.add(in);
                    }

                    adapter.setData(list);
                },
                error -> {
                    int code = -1;
                    String bodyErr = "";

                    if (error.networkResponse != null) {
                        code = error.networkResponse.statusCode;
                        try {
                            bodyErr = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception ignored) {}
                    }

                    error.printStackTrace();
                    adapter.setData(new ArrayList<>());
                    Toast.makeText(this, "Erreur API (" + code + "): " + bodyErr, Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(request);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }
}
