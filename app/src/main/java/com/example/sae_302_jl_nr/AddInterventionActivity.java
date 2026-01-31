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

/**
 * Écran "Ajouter une intervention"
 * - Récupère les infos saisies par l'utilisateur
 * - Envoie ces infos à l'API PHP (POST JSON)
 * - Retourne à l'écran précédent avec RESULT_OK si l'ajout a réussi
 */
public class AddInterventionActivity extends AppCompatActivity {

    // Champs texte du formulaire
    private EditText etReference, etDate, etType, etTechnicien, etAdresse, etVille, etAction, etDuree, etMateriel;

    // Listes déroulantes
    private Spinner spPriorite, spStatut;

    // Boutons d’action
    private Button btnSave, btnCancel;

    // Base URL de l'API (serveur VPS) + endpoint de création
    private static final String API_BASE   = "http://51.38.176.17";
    private static final String API_CREATE = API_BASE + "/interventions_create.php";

    // File d'attente Volley : gère l'envoi des requêtes réseau (HTTP) de l'app
    private RequestQueue requestQueue;

    /**
     * onCreate : appelé quand l'écran s'ouvre
     * - Charge le layout
     * - Lie les champs (findViewById)
     * - Initialise les spinners
     * - Préremplit la date et la référence
     * - Branche les boutons (back, cancel, save)
     */
    @RequiresApi(api = Build.VERSION_CODES.O) // LocalDate nécessite Android 8+ (API 26)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_intervention);

        // Initialise Volley (une seule fois par Activity)
        requestQueue = Volley.newRequestQueue(this);

        // Récupération des composants UI
        etReference  = findViewById(R.id.etReference);
        etDate       = findViewById(R.id.etDate);
        etType       = findViewById(R.id.etType);
        etTechnicien = findViewById(R.id.etTechnicien);
        etAdresse    = findViewById(R.id.etAdresse);
        etVille      = findViewById(R.id.etVille);
        etAction     = findViewById(R.id.etAction);
        etDuree      = findViewById(R.id.etDuree);
        etMateriel   = findViewById(R.id.etMateriel);

        spPriorite = findViewById(R.id.spPriorite);
        spStatut   = findViewById(R.id.spStatut);

        btnSave   = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // Bouton retour (ex: flèche en haut) -> ferme l'écran
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Remplit les spinners (valeurs fixes côté app)
        setupSpinners();

        // Préremplit la date : reçue depuis MainActivity si elle a été passée, sinon aujourd'hui
        prefillDateFromIntent();

        // Préremplit une référence unique (modifiable par l'utilisateur)
        // System.currentTimeMillis() = nombre de ms depuis 1970 -> quasi unique
        etReference.setText("REF" + System.currentTimeMillis());

        // Au clic sur la date -> ouvre le calendrier
        etDate.setOnClickListener(v -> openDatePicker());

        // Annuler -> ferme l'écran sans rien renvoyer
        btnCancel.setOnClickListener(v -> finish());

        // Enregistrer -> envoie à l'API
        btnSave.setOnClickListener(v -> submit());
    }

    /**
     * Remplit les spinners Priorité et Statut avec des valeurs simples.
     * (Tu peux ajouter/enlever des valeurs ici, ça mettra à jour l'UI.)
     */
    private void setupSpinners() {
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
    }

    /**
     * Récupère une date transmise par l'écran précédent (MainActivity).
     * - clé: "date"
     * - format attendu: "YYYY-MM-DD" (LocalDate.toString())
     * Si rien n'est envoyé -> met la date du jour.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void prefillDateFromIntent() {
        String dateStr = getIntent().getStringExtra("date");
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = LocalDate.now().toString();
        }
        etDate.setText(dateStr);
    }

    /**
     * Ouvre un DatePickerDialog (calendrier Android).
     * On essaie de lire la date actuelle du champ, sinon on prend aujourd’hui.
     * Ensuite on remet le résultat dans le champ date en "YYYY-MM-DD".
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void openDatePicker() {
        LocalDate d;

        // Parse la date du champ (si l'utilisateur l'a modifiée)
        try {
            d = LocalDate.parse(etDate.getText().toString().trim());
        } catch (Exception e) {
            d = LocalDate.now();
        }

        // month dans DatePicker = 0..11 (d'où month + 1 quand on reconstruit LocalDate)
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

    /**
     * submit : envoie le formulaire à l'API.
     * Étapes :
     * 1) Vérifie les champs obligatoires
     * 2) Construit un JSON (clé -> valeur)
     * 3) POST via Volley vers interventions_create.php
     * 4) Si OK -> renvoie "changed=true" à l'écran précédent + finish()
     * 5) Si erreur -> affiche code HTTP + corps de réponse (si dispo)
     */
    private void submit() {
        try {
            // Champs obligatoires minimum
            String reference = etReference.getText().toString().trim();
            String date      = etDate.getText().toString().trim();

            if (reference.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Référence et date obligatoires", Toast.LENGTH_LONG).show();
                return;
            }

            // Construit le JSON envoyé au backend
            // IMPORTANT : les noms des clés doivent correspondre à ce que ton PHP attend.
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

            // Requête Volley JSON :
            // - Method.POST = envoi de données
            // - API_CREATE = URL de ton script PHP
            // - body = JSON envoyé en request body
            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_CREATE,
                    body,

                    // Callback succès : le serveur a répondu 2xx et renvoie du JSON
                    response -> {
                        Toast.makeText(this, "Ajout OK ✅", Toast.LENGTH_SHORT).show();

                        // On renvoie une info à l'écran précédent pour lui dire "rafraîchis"
                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);

                        // Ferme cette Activity
                        finish();
                    },

                    // Callback erreur : 4xx/5xx ou problème réseau
                    error -> {
                        int code = -1;
                        String bodyErr = "";

                        // networkResponse existe si on a une réponse HTTP (même en erreur)
                        if (error.networkResponse != null) {
                            code = error.networkResponse.statusCode;

                            // Récupère le corps (souvent du JSON d’erreur côté PHP)
                            try {
                                bodyErr = new String(error.networkResponse.data, "UTF-8");
                            } catch (Exception ignored) { }
                        }

                        Toast.makeText(this, "Erreur (" + code + "): " + bodyErr, Toast.LENGTH_LONG).show();
                    }
            );

            // Envoie la requête dans la file Volley
            requestQueue.add(req);

        } catch (Exception e) {
            // Erreur inattendue (JSON, UI, etc.)
            e.printStackTrace();
            Toast.makeText(this, "Erreur formulaire", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * (Optionnel mais propre) Libère/stoppe la file Volley de cette Activity.
     * Utile si tu tagges tes requêtes ; ici c’est surtout pédagogique.
     */
    @Override
    protected void onStop() {
        super.onStop();
        // Si tu ajoutes des "tags" à tes requêtes, tu peux faire:
        // requestQueue.cancelAll("ADD_INTERVENTION");
    }
}
