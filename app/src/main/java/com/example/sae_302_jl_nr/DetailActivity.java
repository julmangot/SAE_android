package com.example.sae_302_jl_nr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private static final String API_BASE = "http://51.38.176.17";
    private static final String API_STATUS = API_BASE + "/interventions_status.php";
    private static final String API_DELETE = API_BASE + "/interventions_delete.php";

    private RequestQueue requestQueue;

    private TextView tvDetails;
    private View vPriority;

    private String reference;
    private String selectedDate;

    // ✅ valeurs DB
    private String statut = "Planifiée";
    private String priorite = "Basse";

    // champs optionnels (si pas de endpoint détail)
    private String type = "";
    private String technicien = "";
    private String adresse = "";
    private String ville = "";
    private String action = "";
    private String duree = "";
    private String materiel = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        tvDetails = findViewById(R.id.tvDetails);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnChangeStatus = findViewById(R.id.btnModifier);
        vPriority = findViewById(R.id.vLeft); // garde ton id vLeft dans activity_detail

        requestQueue = Volley.newRequestQueue(this);

        reference = getIntent().getStringExtra("reference");
        selectedDate = getIntent().getStringExtra("selectedDate");

        // ✅ récup depuis la liste pour cohérence d’affichage
        String statutFromList = getIntent().getStringExtra("statut");
        if (statutFromList != null && !statutFromList.trim().isEmpty()) {
            statut = normalizeStatut(statutFromList.trim());
        }

        String prioriteFromList = getIntent().getStringExtra("priorite");
        if (prioriteFromList != null && !prioriteFromList.trim().isEmpty()) {
            priorite = prioriteFromList.trim();
        }

        // (optionnel) si tu passes aussi type/ville etc depuis la liste
        String typeFromList = getIntent().getStringExtra("type");
        if (typeFromList != null) type = typeFromList;

        String villeFromList = getIntent().getStringExtra("ville");
        if (villeFromList != null) ville = villeFromList;

        if (reference == null || reference.trim().isEmpty()) {
            Toast.makeText(this, "Erreur : référence manquante", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        refreshDisplay();

        btnChangeStatus.setText("Changer le statut");
        btnChangeStatus.setOnClickListener(v -> showStatusDialog());

        btnBack.setOnClickListener(v -> finish());

        View deleteBtn = findViewById(R.id.btnDelete);
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> confirmDelete());
        }
    }

    private void refreshDisplay() {
        // Couleur barre gauche selon priorité DB (Basse/Moyenne/Haute/Critique...)
        String p = (priorite == null) ? "" : priorite.trim().toLowerCase(Locale.ROOT);
        int color = Color.parseColor("#4CAF50"); // basse
        if (p.contains("critique")) color = Color.parseColor("#D32F2F");
        else if (p.contains("haute")) color = Color.parseColor("#F05A5A");
        else if (p.contains("moy")) color = Color.parseColor("#F5A623");
        vPriority.setBackgroundColor(color);

        String dateTxt = (selectedDate != null && !selectedDate.isEmpty()) ? selectedDate : "";

        String details =
                "• Référence : " + reference + "\n" +
                        (dateTxt.isEmpty() ? "" : "• Date : " + dateTxt + "\n") +
                        (type.isEmpty() ? "" : "• Type : " + type + "\n") +
                        "• Statut : " + statut + "\n" +
                        "• Priorité : " + priorite + "\n\n" +
                        (technicien.isEmpty() ? "" : "• Technicien : " + technicien + "\n") +
                        ((adresse.isEmpty() && ville.isEmpty()) ? "" : "• Lieu : " + adresse + (ville.isEmpty() ? "" : " - " + ville) + "\n") +
                        (action.isEmpty() ? "" : "\n• Action : " + action + "\n") +
                        (duree.isEmpty() ? "" : "• Durée : " + duree + "\n") +
                        (materiel.isEmpty() ? "" : "• Matériel : " + materiel + "\n");

        tvDetails.setText(details.trim());
    }

    private void showStatusDialog() {
        // ✅ EXACTEMENT valeurs DB
        final String[] options = {"Planifiée", "En cours", "Terminée"};

        int checked = 0;
        if ("En cours".equalsIgnoreCase(statut)) checked = 1;
        else if ("Terminée".equalsIgnoreCase(statut) || "Terminee".equalsIgnoreCase(statut)) checked = 2;

        final int[] chosen = {checked};

        new AlertDialog.Builder(this)
                .setTitle("Changer le statut")
                .setSingleChoiceItems(options, checked, (dialog, which) -> chosen[0] = which)
                .setPositiveButton("Valider", (dialog, which) -> updateStatusOnServer(options[chosen[0]]))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateStatusOnServer(String newStatusRaw) {
        String newStatus = normalizeStatut(newStatusRaw);

        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);
            body.put("statut", newStatus);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_STATUS,
                    body,
                    response -> {
                        statut = newStatus;
                        refreshDisplay();
                        Toast.makeText(this, "Statut mis à jour ✅", Toast.LENGTH_SHORT).show();

                        // ✅ prévenir MainActivity + fermer pour recharger la liste
                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);
                        finish();
                    },
                    error -> showVolleyError("statut", error)
            );

            requestQueue.add(req);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer l'intervention")
                .setMessage("Cette action est définitive. Continuer ?")
                .setPositiveButton("Supprimer", (d, w) -> deleteOnServer())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteOnServer() {
        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_DELETE,
                    body,
                    response -> {
                        Toast.makeText(this, "Supprimé 🗑️", Toast.LENGTH_SHORT).show();
                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);
                        finish();
                    },
                    error -> showVolleyError("delete", error)
            );

            requestQueue.add(req);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }

    // ✅ Sécurise les anciennes valeurs envoyées (Planifié -> Planifiée, Terminé -> Terminée)
    private String normalizeStatut(String s) {
        if (s == null) return "Planifiée";
        String t = s.trim();

        if (t.equalsIgnoreCase("Planifié")) return "Planifiée";
        if (t.equalsIgnoreCase("Terminé") || t.equalsIgnoreCase("Termine")) return "Terminée";
        if (t.equalsIgnoreCase("Planifiée")) return "Planifiée";
        if (t.equalsIgnoreCase("Terminée") || t.equalsIgnoreCase("Terminee")) return "Terminée";
        if (t.equalsIgnoreCase("En cours")) return "En cours";

        // fallback
        return t;
    }

    private void showVolleyError(String action, com.android.volley.VolleyError error) {
        int code = (error.networkResponse != null) ? error.networkResponse.statusCode : -1;
        String body = "";

        if (error.networkResponse != null && error.networkResponse.data != null) {
            try {
                body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }

        String msg = "Erreur API " + action + " (" + code + ")";
        if (!body.isEmpty()) msg += " : " + body;

        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        error.printStackTrace();
    }
}
