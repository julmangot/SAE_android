package com.example.sae_302_jl_nr;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.time.LocalDate;

public class AddInterventionActivity extends AppCompatActivity {

    private EditText etReference, etDate, etType, etTechnicien, etAdresse, etVille, etAction, etDuree, etMateriel;
    private Spinner spPriorite, spStatut;
    private Button btnSave, btnCancel;

    private static final String API_BASE   = "http://51.38.176.17";
    private static final String API_CREATE = API_BASE + "/interventions_create.php";

    private RequestQueue requestQueue;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_intervention);

        requestQueue = Volley.newRequestQueue(this);

        etReference = findViewById(R.id.etReference);
        etDate = findViewById(R.id.etDate);
        etType = findViewById(R.id.etType);
        etTechnicien = findViewById(R.id.etTechnicien);
        etAdresse = findViewById(R.id.etAdresse);
        etVille = findViewById(R.id.etVille);
        etAction = findViewById(R.id.etAction);
        etDuree = findViewById(R.id.etDuree);
        etMateriel = findViewById(R.id.etMateriel);

        spPriorite = findViewById(R.id.spPriorite);
        spStatut = findViewById(R.id.spStatut);

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Spinners
        spPriorite.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Basse", "Moyenne", "Haute", "Critique"}
        ));

        spStatut.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Planifiée", "En cours", "Terminée"}
        ));

        // Préremplissage date depuis MainActivity
        String dateStr = getIntent().getStringExtra("date");
        if (dateStr == null || dateStr.isEmpty()) dateStr = LocalDate.now().toString();
        etDate.setText(dateStr);

        // Reference auto (modifiable)
        etReference.setText("REF" + System.currentTimeMillis());

        // Date picker au clic
        etDate.setOnClickListener(v -> openDatePicker());

        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> submit());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openDatePicker() {
        LocalDate d;
        try { d = LocalDate.parse(etDate.getText().toString().trim()); }
        catch (Exception e) { d = LocalDate.now(); }

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    LocalDate nd = LocalDate.of(year, month + 1, dayOfMonth);
                    etDate.setText(nd.toString());
                },
                d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()
        );
        dialog.show();
    }

    private void submit() {
        try {
            String reference = etReference.getText().toString().trim();
            String date = etDate.getText().toString().trim();

            if (reference.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Référence et date obligatoires", Toast.LENGTH_LONG).show();
                return;
            }

            JSONObject body = new JSONObject();
            body.put("reference", reference);
            body.put("date_intervention", date);

            body.put("type_intervention", etType.getText().toString().trim());
            body.put("priorite", spPriorite.getSelectedItem().toString());
            body.put("technicien", etTechnicien.getText().toString().trim());
            body.put("adresse", etAdresse.getText().toString().trim());
            body.put("ville", etVille.getText().toString().trim());
            body.put("action_realisee", etAction.getText().toString().trim());
            body.put("duree", etDuree.getText().toString().trim());
            body.put("materiel", etMateriel.getText().toString().trim());
            body.put("statut", spStatut.getSelectedItem().toString());

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_CREATE,
                    body,
                    response -> {
                        Toast.makeText(this, "Ajout OK ✅", Toast.LENGTH_SHORT).show();

                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);
                        finish();
                    },
                    error -> {
                        int code = -1;
                        String bodyErr = "";
                        if (error.networkResponse != null) {
                            code = error.networkResponse.statusCode;
                            try { bodyErr = new String(error.networkResponse.data, "UTF-8"); }
                            catch (Exception ignored) {}
                        }
                        Toast.makeText(this, "Erreur (" + code + "): " + bodyErr, Toast.LENGTH_LONG).show();
                    }
            );

            requestQueue.add(req);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur formulaire", Toast.LENGTH_LONG).show();
        }
    }
}
