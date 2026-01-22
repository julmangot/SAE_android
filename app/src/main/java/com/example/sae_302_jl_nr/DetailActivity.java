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
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    // ⚠️ Vérifie bien ton IP
    private static final String API_BASE = "http://51.38.176.17";
    private static final String API_STATUS = API_BASE + "/interventions_status.php";
    private static final String API_DELETE = API_BASE + "/interventions_delete.php";

    private RequestQueue requestQueue;

    // Champs de l'interface
    private TextView tvTypeTitle, tvReference, tvStatutBadge, tvPrioriteBadge;
    private TextView tvDate, tvTechnicien, tvAdresse;
    private TextView tvAction, tvDuree, tvMateriel;
    private View vStatusColor;

    // Données
    private String reference, date, statut, priorite;
    private String type, technicien, adresse, ville, action, duree, materiel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        // Liaison des Vues (Binding)
        vStatusColor = findViewById(R.id.vStatusColor);
        tvTypeTitle = findViewById(R.id.tvTypeTitle);
        tvReference = findViewById(R.id.tvReference);
        tvStatutBadge = findViewById(R.id.tvStatutBadge);
        tvPrioriteBadge = findViewById(R.id.tvPrioriteBadge);

        tvDate = findViewById(R.id.tvDate);
        tvTechnicien = findViewById(R.id.tvTechnicien);
        tvAdresse = findViewById(R.id.tvAdresse);

        tvAction = findViewById(R.id.tvAction);
        tvDuree = findViewById(R.id.tvDuree);
        tvMateriel = findViewById(R.id.tvMateriel);

        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnChangeStatus = findViewById(R.id.btnModifier);
        Button btnDelete = findViewById(R.id.btnDelete);

        requestQueue = Volley.newRequestQueue(this);

        // 1. Récupération des données (identique)
        Intent i = getIntent();
        reference = i.getStringExtra("reference");
        date = i.getStringExtra("date");
        statut = normalizeStatut(i.getStringExtra("statut"));
        priorite = i.getStringExtra("priorite");

        type = i.getStringExtra("type");
        technicien = i.getStringExtra("technicien");
        adresse = i.getStringExtra("adresse");
        ville = i.getStringExtra("ville");
        action = i.getStringExtra("action");
        duree = i.getStringExtra("duree");
        materiel = i.getStringExtra("materiel");

        if (reference == null || reference.isEmpty()) {
            finish();
            return;
        }

        // 2. Affichage
        refreshDisplay();

        // 3. Boutons
        btnChangeStatus.setText("Changer Statut");
        btnChangeStatus.setOnClickListener(v -> showStatusDialog());
        btnBack.setOnClickListener(v -> finish());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void refreshDisplay() {
        // En-tête
        tvTypeTitle.setText((type != null && !type.isEmpty()) ? type : "Intervention");
        tvReference.setText("Réf: " + reference);

        // Badges & Couleurs
        String p = (priorite != null) ? priorite.toLowerCase(Locale.ROOT) : "basse";
        int color = Color.parseColor("#4CAF50"); // Vert

        if (p.contains("critique")) color = Color.parseColor("#D32F2F");
        else if (p.contains("haute") || p.contains("haut")) color = Color.parseColor("#F05A5A");
        else if (p.contains("moy")) color = Color.parseColor("#F5A623");

        vStatusColor.setBackgroundColor(color);
        tvPrioriteBadge.setText("Priorité : " + (priorite != null ? priorite : "Normale"));

        tvStatutBadge.setText(statut.toUpperCase());
        // Couleur dynamique du texte statut si tu veux
        if(statut.equalsIgnoreCase("Terminée")) tvStatutBadge.setTextColor(Color.parseColor("#2E7D32")); // Vert foncé
        else tvStatutBadge.setTextColor(Color.parseColor("#1565C0")); // Bleu

        // Infos Principales
        tvDate.setText(checkNull(date));
        tvTechnicien.setText(technicien != null && !technicien.isEmpty() ? technicien : "Non assigné");

        String loc = "";
        if (adresse != null) loc += adresse;
        if (ville != null && !ville.isEmpty()) loc += (loc.isEmpty() ? "" : ", ") + ville;
        tvAdresse.setText(loc.isEmpty() ? "Adresse non renseignée" : loc);

        // Technique
        tvAction.setText(checkNull(action));
        tvDuree.setText(checkNull(duree));
        tvMateriel.setText(checkNull(materiel));
    }

    private String checkNull(String txt) {
        return (txt == null || txt.isEmpty()) ? "-" : txt;
    }

    // --- Les fonctions API restent identiques ---

    private void showStatusDialog() {
        final String[] options = {"Planifiée", "En cours", "Terminée"};
        int checkedItem = 0;
        if ("En cours".equalsIgnoreCase(statut)) checkedItem = 1;
        else if ("Terminée".equalsIgnoreCase(statut)) checkedItem = 2;
        final int[] selected = {checkedItem};

        new AlertDialog.Builder(this)
                .setTitle("Modifier le statut")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Valider", (dialog, which) -> updateStatusOnServer(options[selected[0]]))
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateStatusOnServer(String newStatusRaw) {
        String newStatus = normalizeStatut(newStatusRaw);
        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);
            body.put("statut", newStatus);

            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, API_STATUS, body,
                    response -> {
                        statut = newStatus;
                        refreshDisplay();
                        Toast.makeText(this, "Mis à jour !", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("changed", true);
                        setResult(RESULT_OK, resultIntent);
                    },
                    error -> Toast.makeText(this, "Erreur API", Toast.LENGTH_SHORT).show()
            );
            requestQueue.add(req);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer ?")
                .setMessage("Action irréversible.")
                .setPositiveButton("Supprimer", (d, w) -> deleteOnServer())
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void deleteOnServer() {
        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);
            JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, API_DELETE, body,
                    response -> {
                        Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("changed", true);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    },
                    error -> Toast.makeText(this, "Erreur suppression", Toast.LENGTH_SHORT).show()
            );
            requestQueue.add(req);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String normalizeStatut(String s) {
        if (s == null) return "Planifiée";
        String t = s.trim();
        if (t.equalsIgnoreCase("Terminé") || t.equalsIgnoreCase("Termine")) return "Terminée";
        if (t.equalsIgnoreCase("Planifié")) return "Planifiée";
        return t;
    }
}