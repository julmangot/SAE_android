package com.example.sae_302_jl_nr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * DetailActivity
 * -------------
 * Écran de détail d’une intervention.
 *
 * Rôles :
 * - Afficher toutes les infos d’une intervention (données reçues via Intent)
 * - Modifier le statut (appel API)
 * - Supprimer l’intervention (appel API)
 *
 * Important :
 * - Cette Activity ne modifie pas la base directement : tout passe par l’API PHP.
 * - Quand une modif/suppression réussit, on renvoie "changed=true" à l’écran précédent
 *   pour qu’il recharge la liste.
 */
public class DetailActivity extends AppCompatActivity {

    // ================= API =================
    private static final String API_BASE = "http://51.38.176.17";
    private static final String API_UPDATE_STATUT = API_BASE + "/interventions_status.php";
    private static final String API_DELETE        = API_BASE + "/interventions_delete.php";

    // File Volley : gère les requêtes HTTP
    private RequestQueue queue;

    // ================= UI =================
    private View root;                    // Vue racine (si tu veux gérer swipe plus tard)
    private View bottomActionContainer;   // Zone boutons

    private ImageButton btnBack;
    private View vStatusColor;

    private TextView tvTypeTitle, tvReference, tvStatutBadge, tvPrioriteBadge;
    private TextView tvDate, tvTechnicien, tvAdresse;
    private TextView tvAction, tvDuree, tvMateriel;

    private Button btnModifier, btnDelete;

    // ================= DATA =================
    private String reference = "";
    private LocalDate dateIntervention;

    private String currentStatut   = "Planifiée";
    private String currentPriorite = "Basse";

    // Bloque les actions/retour pendant une requête réseau
    private boolean networkBusy = false;

    // Format date lisible (ex: 31 janvier 2026)
    @RequiresApi(api = Build.VERSION_CODES.O)
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialise Volley
        queue = Volley.newRequestQueue(this);

        // Root + zone boutons (si ids existent dans ton XML)
        root = findViewById(R.id.rootDetail);
        bottomActionContainer = findViewById(R.id.bottomActionContainer);

        // Bind UI
        btnBack = findViewById(R.id.btnBack);

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

        btnModifier = findViewById(R.id.btnModifier);
        btnDelete = findViewById(R.id.btnDelete);

        // Bouton retour simple
        btnBack.setOnClickListener(v -> finish());

        // ✅ Remplit l’UI avec TOUTES les données de l’Intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            readExtrasAndFillUI();
        } else {
            // Fallback si API < 26
            tvReference.setText("—");
            tvStatutBadge.setText("—");
            tvPrioriteBadge.setText("—");
            tvDate.setText("—");
        }

        // Associe les actions aux boutons
        setupButtons();
    }

    private void setupButtons() {
        btnModifier.setOnClickListener(v -> showStatutDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirm());
    }

    /**
     * Affiche un dialog pour choisir un statut.
     */
    private void showStatutDialog() {
        final String[] statuts = new String[]{"Planifiée", "En cours", "Terminée"};

        int checkedIndexTmp = 0;
        for (int i = 0; i < statuts.length; i++) {
            if (statuts[i].equalsIgnoreCase(currentStatut)) {
                checkedIndexTmp = i;
                break;
            }
        }
        final int checkedIndexFinal = checkedIndexTmp;

        new AlertDialog.Builder(this)
                .setTitle("Changer le statut")
                .setSingleChoiceItems(statuts, checkedIndexFinal, null)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    AlertDialog ad = (AlertDialog) dialog;

                    int posTmp = ad.getListView().getCheckedItemPosition();
                    int posFinal = (posTmp < 0) ? checkedIndexFinal : posTmp;

                    String newStatut = statuts[posFinal];
                    updateStatutOnServer(newStatut);
                })
                .show();
    }

    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Tu veux vraiment supprimer cette intervention ?")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer", (dialog, which) -> deleteOnServer())
                .show();
    }

    private void setButtonsEnabled(boolean enabled) {
        if (btnModifier != null) btnModifier.setEnabled(enabled);
        if (btnDelete != null) btnDelete.setEnabled(enabled);
        if (btnBack != null) btnBack.setEnabled(enabled);
    }

    /**
     * ✅ CORRECTION PRINCIPALE :
     * Lit TOUS les extras envoyés par MainActivity et remplit TOUTE l'UI.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readExtrasAndFillUI() {
        Intent intent = getIntent();

        // ----- Champs clés -----
        reference = safe(intent.getStringExtra("reference"));
        currentStatut = safeOrDefault(intent.getStringExtra("statut"), "Planifiée");
        currentPriorite = safeOrDefault(intent.getStringExtra("priorite"), "Basse");
        dateIntervention = parseDateOrNow(intent.getStringExtra("date"));

        // ----- Champs détails (envoyés par MainActivity) -----
        String type = safe(intent.getStringExtra("type"));
        String technicien = safe(intent.getStringExtra("technicien"));
        String adresse = safe(intent.getStringExtra("adresse"));
        String ville = safe(intent.getStringExtra("ville"));
        String action = safe(intent.getStringExtra("action"));
        String duree = safe(intent.getStringExtra("duree"));
        String materiel = safe(intent.getStringExtra("materiel"));

        // ----- UI : champs en haut -----
        tvReference.setText(reference.isEmpty() ? "—" : reference);

        tvStatutBadge.setText(currentStatut);
        tvPrioriteBadge.setText("Priorité " + currentPriorite);
        tvDate.setText(dateIntervention.format(dateFormatter));

        // ✅ Type
        if (tvTypeTitle != null) tvTypeTitle.setText(type.isEmpty() ? "—" : type);

        // ✅ Technicien
        if (tvTechnicien != null) tvTechnicien.setText(technicien.isEmpty() ? "—" : technicien);

        // ✅ Adresse + Ville
        String fullAdresse = adresse;
        if (!ville.isEmpty()) {
            fullAdresse = (fullAdresse.isEmpty() ? "" : fullAdresse + ", ") + ville;
        }
        if (tvAdresse != null) tvAdresse.setText(fullAdresse.isEmpty() ? "—" : fullAdresse);

        // ✅ Action / Durée / Matériel
        if (tvAction != null) tvAction.setText(action.isEmpty() ? "—" : action);
        if (tvDuree != null) tvDuree.setText(duree.isEmpty() ? "—" : duree);
        if (tvMateriel != null) tvMateriel.setText(materiel.isEmpty() ? "—" : materiel);

        // ----- Styles -----
        applyPriorityColor(currentPriorite);
        applyStatutBadgeStyle(currentStatut);
    }

    /**
     * Envoie le nouveau statut au serveur
     */
    private void updateStatutOnServer(String newStatut) {
        if (reference.isEmpty()) {
            Toast.makeText(this, "Référence manquante", Toast.LENGTH_LONG).show();
            return;
        }

        networkBusy = true;
        setButtonsEnabled(false);

        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);
            body.put("statut", newStatut);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_UPDATE_STATUT,
                    body,
                    response -> {
                        networkBusy = false;
                        setButtonsEnabled(true);

                        currentStatut = response.optString("statut_saved", newStatut);
                        tvStatutBadge.setText(currentStatut);
                        applyStatutBadgeStyle(currentStatut);

                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);

                        Toast.makeText(this, "Statut sauvegardé ✅", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        networkBusy = false;
                        setButtonsEnabled(true);
                        Toast.makeText(this, "Erreur update statut", Toast.LENGTH_LONG).show();
                    }
            );

            queue.add(req);

        } catch (Exception e) {
            networkBusy = false;
            setButtonsEnabled(true);
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Supprime l’intervention côté serveur
     */
    private void deleteOnServer() {
        if (reference.isEmpty()) {
            Toast.makeText(this, "Référence manquante", Toast.LENGTH_LONG).show();
            return;
        }

        networkBusy = true;
        setButtonsEnabled(false);

        try {
            JSONObject body = new JSONObject();
            body.put("reference", reference);

            JsonObjectRequest req = new JsonObjectRequest(
                    Request.Method.POST,
                    API_DELETE,
                    body,
                    response -> {
                        networkBusy = false;
                        setButtonsEnabled(true);

                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);

                        Toast.makeText(this, "Supprimée ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    },
                    error -> {
                        networkBusy = false;
                        setButtonsEnabled(true);
                        Toast.makeText(this, "Erreur suppression", Toast.LENGTH_LONG).show();
                    }
            );

            queue.add(req);

        } catch (Exception e) {
            networkBusy = false;
            setButtonsEnabled(true);
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }

    // =================== Styles ===================

    private void applyPriorityColor(String priorite) {
        if (vStatusColor == null) return;

        String p = priorite.toLowerCase(Locale.ROOT);

        int color = Color.parseColor("#4CAF50"); // défaut : vert
        if (p.contains("critique")) color = Color.parseColor("#D32F2F"); // rouge
        else if (p.contains("haute") || p.contains("haut")) color = Color.parseColor("#F05A5A");
        else if (p.contains("moy")) color = Color.parseColor("#F5A623"); // orange

        vStatusColor.setBackgroundColor(color);
    }

    private void applyStatutBadgeStyle(String statut) {
        if (tvStatutBadge == null) return;

        String s = statut.toLowerCase(Locale.ROOT);

        if (s.contains("plan")) {
            tvStatutBadge.setBackgroundColor(Color.parseColor("#E3F2FD"));
            tvStatutBadge.setTextColor(Color.parseColor("#1565C0"));
        } else if (s.contains("cours")) {
            tvStatutBadge.setBackgroundColor(Color.parseColor("#FFF3E0"));
            tvStatutBadge.setTextColor(Color.parseColor("#E65100"));
        } else if (s.contains("termin")) {
            tvStatutBadge.setBackgroundColor(Color.parseColor("#E8F5E9"));
            tvStatutBadge.setTextColor(Color.parseColor("#2E7D32"));
        }
    }

    // =================== Utils ===================

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static String safeOrDefault(String s, String def) {
        String v = safe(s);
        return v.isEmpty() ? def : v;
    }

    /**
     * Parse la date reçue (format "YYYY-MM-DD" ou "YYYY-MM-DD ...")
     * Si erreur -> aujourd’hui.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private static LocalDate parseDateOrNow(String dateStr) {
        try {
            if (dateStr == null) return LocalDate.now();
            return LocalDate.parse(dateStr.trim().split(" ")[0]);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}