package com.example.sae_302_jl_nr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.time.LocalDate;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    private static final String API_BASE = "http://51.38.176.17";

    // ✅ Mets ici selon ton routing réel :
    // Si tu utilises les fichiers direct :
    private static final String API_STATUS = API_BASE + "/interventions_status.php";
    private static final String API_DELETE = API_BASE + "/interventions_delete.php";
    // Si tu utilises des routes nginx propres, ce serait plutôt :
    // private static final String API_STATUS = API_BASE + "/interventions/status";
    // private static final String API_DELETE = API_BASE + "/interventions/delete";

    private RequestQueue requestQueue;

    private TextView tvDetails;
    private View vLeft;

    private String reference;
    private String selectedDate; // optionnel (juste pour affichage si tu veux)

    // Données affichées (minimales)
    private String statut = "Planifié";   // DB: "Planifié" / "En cours" / "Terminé"
    private String priorite = "Basse";    // "Haute" / "Moyenne" / "Basse"
    private String type = "";
    private String technicien = "";
    private String adresse = "";
    private String ville = "";
    private String action = "";
    private String duree = "";
    private String materiel = "";
    private LocalDate dateIntervention = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        tvDetails = findViewById(R.id.tvDetails);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnChangeStatus = findViewById(R.id.btnModifier); // ton bouton existant
        vLeft = findViewById(R.id.vLeft);

        requestQueue = Volley.newRequestQueue(this);

        // ✅ On récupère la référence envoyée par MainActivity
        reference = getIntent().getStringExtra("reference");
        selectedDate = getIntent().getStringExtra("selectedDate");

        if (reference == null || reference.trim().isEmpty()) {
            Toast.makeText(this, "Erreur : référence manquante", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ✅ OPTION 1 (rapide) : si tu n'as pas encore endpoint GET /intervention?ref=...
        // -> on affiche au moins la référence + date choisie
        // OPTION 2 (propre) : ajouter un endpoint GET détail par référence.
        // Pour l’instant on affiche minimal, et le statut sera synchro après update.

        // Affichage initial minimal
        refreshDisplay();

        // Changer statut (popup + API)
        btnChangeStatus.setText("Changer le statut");
        btnChangeStatus.setOnClickListener(v -> showStatusDialog());

        // Bouton retour
        btnBack.setOnClickListener(v -> finish());

        // ✅ Delete (si le bouton existe)
        View deleteBtn = findViewById(R.id.btnDelete);
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> confirmDelete());
        }
    }

    private void refreshDisplay() {
        // Couleur barre gauche selon priorité (string)
        String p = (priorite == null) ? "" : priorite.trim().toLowerCase(Locale.ROOT);
        if (p.contains("haute")) vLeft.setBackgroundColor(Color.parseColor("#F05A5A"));
        else if (p.contains("moy")) vLeft.setBackgroundColor(Color.parseColor("#F5A623"));
        else vLeft.setBackgroundColor(Color.parseColor("#4CAF50"));

        String dateTxt = (selectedDate != null && !selectedDate.isEmpty())
                ? selectedDate
                : (dateIntervention != null ? dateIntervention.toString() : "");

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
        // ✅ valeurs DB
        final String[] options = {"Planifié", "En cours", "Terminé"};

        int checked = 0;
        if ("En cours".equalsIgnoreCase(statut)) checked = 1;
        else if ("Terminé".equalsIgnoreCase(statut) || "Terminee".equalsIgnoreCase(statut)) checked = 2;

        final int[] chosen = {checked};

        new AlertDialog.Builder(this)
                .setTitle("Changer le statut")
                .setSingleChoiceItems(options, checked, (dialog, which) -> chosen[0] = which)
                .setPositiveButton("Valider", (dialog, which) -> {
                    String newStatus = options[chosen[0]];
                    updateStatusOnServer(newStatus);
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateStatusOnServer(String newStatus) {
        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);
            body.put("statut", newStatus);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_STATUS,
                    body,
                    response -> {
                        // ✅ on met à jour l’affichage local
                        statut = newStatus;
                        refreshDisplay();

                        Toast.makeText(this, "Statut mis à jour ✅", Toast.LENGTH_SHORT).show();

                        // ✅ on prévient MainActivity qu'il faut resync la liste
                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Erreur API statut : " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
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

                        // ✅ prévenir MainActivity + fermer
                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);
                        finish();
                    },
                    error -> {
                        error.printStackTrace();
                        Toast.makeText(this, "Erreur API delete : " + error.getMessage(), Toast.LENGTH_LONG).show();
                    }
            );

            requestQueue.add(req);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }
}
