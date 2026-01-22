package com.example.sae_302_jl_nr;

import android.app.DatePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvDate;
    private ImageButton btnPrev, btnNext;

    private RecyclerView rvInterventions;
    private InterventionAdapter adapter;

    private LocalDate currentDate;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    // ✅ Mets ton IP ici (HTTPS via Nginx)
    private static final String API_BASE = "http://51.38.176.17"; // ex: https://203.0.113.10
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Padding auto pour les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Vues du header date
        tvDate = findViewById(R.id.tvDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        // RecyclerView
        rvInterventions = findViewById(R.id.rvInterventions);
        rvInterventions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InterventionAdapter();
        rvInterventions.setAdapter(adapter);

        // Volley
        requestQueue = Volley.newRequestQueue(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            currentDate = LocalDate.now();
            updateDateLabel();
            reloadInterventionsForDay(); // ✅ charge depuis l'API

            btnPrev.setOnClickListener(v -> {
                currentDate = currentDate.minusDays(1);
                updateDateLabel();
                reloadInterventionsForDay();
            });

            btnNext.setOnClickListener(v -> {
                currentDate = currentDate.plusDays(1);
                updateDateLabel();
                reloadInterventionsForDay();
            });

            // Clic sur la date => DatePicker
            tvDate.setOnClickListener(v -> {
                int year = currentDate.getYear();
                int month = currentDate.getMonthValue() - 1; // DatePicker: 0-11
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

    /**
     * ✅ Remplace ton filtre local : ici on récupère depuis l'API par date
     * Endpoint attendu : GET /interventions?date=YYYY-MM-DD
     * Retour attendu : JSON array [...]
     */
    private void reloadInterventionsForDay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        String url = API_BASE + "/interventions?date=" + currentDate.toString();

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    ArrayList<Intervention> list = new ArrayList<>();

                    for (int idx = 0; idx < response.length(); idx++) {
                        JSONObject o = response.optJSONObject(idx);
                        if (o == null) continue;

                        // Champs JSON (depuis ta DB)
                        String reference = o.optString("reference", "");
                        String dateStr = o.optString("date_intervention", currentDate.toString());

                        String type = o.optString("type_intervention", "");
                        String priorite = o.optString("priorite", "Basse");
                        String technicien = o.optString("technicien", "");

                        String adresse = o.optString("adresse", "");
                        String ville = o.optString("ville", "");

                        String action = o.optString("action_realisee", "");
                        String duree = o.optString("duree", "");
                        String materiel = o.optString("materiel", "");
                        String statut = o.optString("statut", "");

                        LocalDate d = LocalDate.parse(dateStr);

                        // libellé court (optionnel, tu peux laisser vide)
                        String libelleCourt = d.getDayOfMonth() + " " + d.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH);

                        Intervention in = new Intervention(
                                reference,
                                libelleCourt,
                                d,
                                type,
                                priorite,
                                technicien,
                                adresse,
                                ville,
                                action,
                                duree,
                                materiel,
                                statut
                        );

                        list.add(in);
                    }

                    adapter.setData(list);
                },
                error -> {
                    error.printStackTrace();
                    adapter.setData(new ArrayList<>());
                    Toast.makeText(this, "Erreur API (SSL/URL ?) : " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(request);
    }
}
