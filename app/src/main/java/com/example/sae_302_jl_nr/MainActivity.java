package com.example.sae_302_jl_nr;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
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

    private static final String API_BASE   = "http://51.38.176.17";
    private static final String API_LIST   = API_BASE + "/interventions.php?date=";
    private static final String API_CREATE = API_BASE + "/interventions_create.php";

    private RequestQueue requestQueue;

    private ActivityResultLauncher<Intent> detailLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvDate = findViewById(R.id.tvDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        fabAdd = findViewById(R.id.fabAdd);

        rvInterventions = findViewById(R.id.rvInterventions);
        rvInterventions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InterventionAdapter();
        rvInterventions.setAdapter(adapter);

        requestQueue = Volley.newRequestQueue(this);

        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        boolean changed = result.getData().getBooleanExtra("changed", false);
                        if (changed) reloadInterventionsForDay();
                    }
                }
        );

        adapter.setOnInterventionClickListener(intervention -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);

            intent.putExtra("reference", intervention.idMission);  // référence DB
            intent.putExtra("statut", intervention.statut);        // ✅ pour afficher pareil
            intent.putExtra("priorite", intervention.prioriteStr); // ✅ si tu as ce champ, sinon intervention.prioriteText

            intent.putExtra("type", intervention.type);
            intent.putExtra("ville", intervention.ville);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
                intent.putExtra("selectedDate", currentDate.toString());
            }

            detailLauncher.launch(intent);
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now();
            updateDateLabel();
            reloadInterventionsForDay();

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

            fabAdd.setOnClickListener(v -> createInterventionQuick(currentDate));

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
                        String statut = o.optString("statut", "Planifié");

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

                        // calcul priorite int (pour couleur)
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
                    error.printStackTrace();
                    adapter.setData(new ArrayList<>());
                    Toast.makeText(this, "Erreur API : " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
        );

        requestQueue.add(request);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createInterventionQuick(LocalDate date) {
        try {
            String ref = "REF" + System.currentTimeMillis();

            JSONObject body = new JSONObject();
            body.put("reference", ref);
            body.put("date_intervention", date.toString());
            body.put("type_intervention", "SAV");
            body.put("priorite", "Basse");
            body.put("technicien", "");
            body.put("adresse", "");
            body.put("ville", "");
            body.put("action_realisee", "");
            body.put("duree", "");
            body.put("materiel", "");

            // ✅ IMPORTANT: valeur identique DB + DetailActivity
            body.put("statut", "Planifié");

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_CREATE,
                    body,
                    response -> {
                        Toast.makeText(this, "Intervention ajoutée ✅", Toast.LENGTH_SHORT).show();
                        reloadInterventionsForDay();
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Erreur ajout : " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
            );

            requestQueue.add(req);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur création", Toast.LENGTH_LONG).show();
        }
    }
}
