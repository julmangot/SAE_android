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

    // ⚠️ Vérifie bien ton IP ici
    private static final String API_BASE = "http://51.38.176.17";
    private static final String API_STATUS = API_BASE + "/interventions_status.php";
    private static final String API_DELETE = API_BASE + "/interventions_delete.php";

    private RequestQueue requestQueue;
    private TextView tvDetails;
    private View vLeft; // Barre de couleur

    // Variables pour stocker les données reçues
    private String reference, date, statut, priorite;
    private String type, technicien, adresse, ville, action, duree, materiel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        // Liaison Vues
        tvDetails = findViewById(R.id.tvDetails);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnChangeStatus = findViewById(R.id.btnModifier);
        Button btnDelete = findViewById(R.id.btnDelete); // Si présent dans XML
        vLeft = findViewById(R.id.vLeft);

        requestQueue = Volley.newRequestQueue(this);

        // 1. Récupération de TOUTES les données envoyées par MainActivity
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

        // Vérification sécurité
        if (reference == null || reference.isEmpty()) {
            Toast.makeText(this, "Erreur : Référence manquante", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 2. Affichage initial
        refreshDisplay();

        // 3. Actions Boutons
        btnChangeStatus.setText("Changer le statut");
        btnChangeStatus.setOnClickListener(v -> showStatusDialog());
        btnBack.setOnClickListener(v -> finish());

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDelete());
        }
    }

    private void refreshDisplay() {
        // A. Gestion de la couleur (Priorité)
        String p = (priorite != null) ? priorite.toLowerCase(Locale.ROOT) : "basse";
        int color = Color.parseColor("#4CAF50"); // Vert par défaut

        if (p.contains("critique")) color = Color.parseColor("#D32F2F"); // Rouge foncé
        else if (p.contains("haute") || p.contains("haut")) color = Color.parseColor("#F05A5A"); // Rouge
        else if (p.contains("moy")) color = Color.parseColor("#F5A623"); // Orange

        vLeft.setBackgroundColor(color);

        // B. Construction du texte complet
        StringBuilder sb = new StringBuilder();

        // En-tête
        sb.append("• Mission n° : ").append(reference).append("\n");
        sb.append("• Date : ").append(checkNull(date)).append("\n");
        sb.append("• Statut : ").append(statut).append("\n");
        sb.append("• Priorité : ").append(checkNull(priorite)).append("\n\n");

        // Détails Techniques
        sb.append("• Type : ").append(checkNull(type)).append("\n");
        sb.append("• Technicien : ").append(technicien != null && !technicien.isEmpty() ? technicien : "Non assigné").append("\n\n");

        // Localisation
        sb.append("📍 LIEU D'INTERVENTION\n");
        String lieu = "";
        if (adresse != null && !adresse.isEmpty()) lieu += adresse;
        if (ville != null && !ville.isEmpty()) lieu += (lieu.isEmpty() ? "" : ", ") + ville;
        sb.append(lieu.isEmpty() ? "Non renseigné" : lieu).append("\n\n");

        // Actions & Matériel
        sb.append("🔧 DÉTAILS TECHNIQUES\n");
        sb.append("• Action : ").append(checkNull(action)).append("\n");
        sb.append("• Durée estimée : ").append(checkNull(duree)).append("\n");
        sb.append("• Matériel requis : ").append(checkNull(materiel));

        // Affichage dans le TextView unique
        tvDetails.setText(sb.toString());
    }

    // Helper pour éviter d'afficher "null"
    private String checkNull(String txt) {
        return (txt == null || txt.isEmpty()) ? "-" : txt;
    }

    // Popup de modification
    private void showStatusDialog() {
        final String[] options = {"Planifiée", "En cours", "Terminée"};

        int checkedItem = 0;
        if ("En cours".equalsIgnoreCase(statut)) checkedItem = 1;
        else if ("Terminée".equalsIgnoreCase(statut) || "Terminee".equalsIgnoreCase(statut)) checkedItem = 2;

        final int[] selected = {checkedItem};

        new AlertDialog.Builder(this)
                .setTitle("Modifier le statut")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> selected[0] = which)
                .setPositiveButton("Valider", (dialog, which) -> {
                    updateStatusOnServer(options[selected[0]]);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // API Update
    private void updateStatusOnServer(String newStatusRaw) {
        String newStatus = normalizeStatut(newStatusRaw); // Converti "Terminé" -> "Terminée" si besoin

        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);
            body.put("statut", newStatus);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_STATUS,
                    body,
                    response -> {
                        // Succès
                        statut = newStatus;
                        refreshDisplay();
                        Toast.makeText(this, "Statut mis à jour ✅", Toast.LENGTH_SHORT).show();

                        // Force le rechargement dans MainActivity au retour
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("changed", true);
                        setResult(RESULT_OK, resultIntent);

                        // Optionnel : fermer la page pour revenir à la liste directement
                        // finish();
                    },
                    error -> {
                        String err = new String(error.networkResponse != null ? error.networkResponse.data : new byte[0]);
                        Toast.makeText(this, "Erreur API: " + err, Toast.LENGTH_LONG).show();
                    }
            );

            requestQueue.add(req);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // API Delete
    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer ?")
                .setMessage("Cette action est irréversible.")
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
                        Toast.makeText(this, "Mission supprimée 🗑️", Toast.LENGTH_SHORT).show();
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

    // Uniformisation des statuts (base de données vs affichage)
    private String normalizeStatut(String s) {
        if (s == null) return "Planifiée";
        String t = s.trim();
        if (t.equalsIgnoreCase("Terminé") || t.equalsIgnoreCase("Termine")) return "Terminée";
        if (t.equalsIgnoreCase("Planifié")) return "Planifiée";
        return t;
    }
}