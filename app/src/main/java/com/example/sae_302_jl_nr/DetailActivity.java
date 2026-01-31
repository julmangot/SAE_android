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
 * - Swipe gauche → droite pour revenir en arrière (hors zone boutons)
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
    private View root;                    // Vue racine (sert à détecter le swipe)
    private View bottomActionContainer;   // Zone boutons (à exclure du swipe)

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

    // ================= Swipe back =================
    private float downX = 0f, downY = 0f;
    private boolean trackingBack = false;
    private boolean ignoreSwipeThisGesture = false;

    private int finishThresholdPx;
    private int touchSlopPx;

    // Bloque les actions/retour pendant une requête réseau
    private boolean networkBusy = false;

    // Format date lisible (ex: 31 janvier 2026)
    @RequiresApi(api = Build.VERSION_CODES.O)
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    /**
     * onCreate :
     * - bind des vues
     * - récupère les infos envoyées par Intent
     * - configure boutons + swipe retour
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Initialise Volley
        queue = Volley.newRequestQueue(this);

        // Root + zone boutons
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

        // Seuils swipe (convertis en pixels selon densité écran)
        DisplayMetrics dm = getResources().getDisplayMetrics();
        finishThresholdPx = (int) (110 * dm.density);
        touchSlopPx = ViewConfiguration.get(this).getScaledTouchSlop();

        // Bouton retour simple
        btnBack.setOnClickListener(v -> finish());

        // Remplit l’UI avec les données de l’Intent
        readExtrasAndFillUI();

        // Associe les actions aux boutons
        setupButtons();

        // Swipe retour (sauf sur la zone des boutons)
        setupSwipeBackExcludingBottom();
    }

    /**
     * Associe les actions aux boutons Modifier et Supprimer
     */
    private void setupButtons() {
        btnModifier.setOnClickListener(v -> showStatutDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirm());
    }

    /**
     * Affiche un dialog pour choisir un statut.
     *
     * ⚠️ Fix Java important :
     * - Une variable utilisée dans une lambda doit être "final" ou "effectively final"
     * - Comme checkedIndex est modifié dans la boucle, on copie sa valeur dans un final.
     */
    private void showStatutDialog() {
        final String[] statuts = new String[]{"Planifiée", "En cours", "Terminée"};

        // Calcul de l'index sélectionné (variable non-final OK ici)
        int checkedIndexTmp = 0;
        for (int i = 0; i < statuts.length; i++) {
            if (statuts[i].equalsIgnoreCase(currentStatut)) {
                checkedIndexTmp = i;
                break;
            }
        }

        // ✅ Copie "final" utilisable dans la lambda
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

    /**
     * Confirmation avant suppression
     */
    private void showDeleteConfirm() {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer")
                .setMessage("Tu veux vraiment supprimer cette intervention ?")
                .setNegativeButton("Annuler", null)
                .setPositiveButton("Supprimer", (dialog, which) -> deleteOnServer())
                .show();
    }

    /**
     * Active / désactive les boutons (pratique pendant réseau)
     */
    private void setButtonsEnabled(boolean enabled) {
        if (btnModifier != null) btnModifier.setEnabled(enabled);
        if (btnDelete != null) btnDelete.setEnabled(enabled);
        if (btnBack != null) btnBack.setEnabled(enabled);
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

                        // Mise à jour locale + UI
                        currentStatut = response.optString("statut_saved", newStatut);
                        tvStatutBadge.setText(currentStatut);
                        applyStatutBadgeStyle(currentStatut);

                        // Informe l’écran précédent qu’il doit recharger
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

    /**
     * Swipe retour sur le reste de la page.
     * - ignoré si on touche la zone des boutons
     * - ignoré si une requête réseau est en cours
     */
    private void setupSwipeBackExcludingBottom() {
        if (root == null) return;

        root.setOnTouchListener((v, event) -> {
            if (networkBusy) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getX();
                    downY = event.getY();
                    trackingBack = false;

                    // Si le doigt est dans la zone boutons -> pas de swipe
                    ignoreSwipeThisGesture =
                            bottomActionContainer != null && isTouchInsideView(event, bottomActionContainer);
                    return false;

                case MotionEvent.ACTION_MOVE:
                    if (ignoreSwipeThisGesture) return false;

                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;

                    // Pas assez bougé -> on laisse click/scroll
                    if (Math.abs(dx) < touchSlopPx && Math.abs(dy) < touchSlopPx) return false;

                    // Si mouvement vertical dominant -> on laisse le scroll
                    if (!trackingBack && Math.abs(dy) > Math.abs(dx)) return false;

                    // On ne prend que le swipe gauche -> droite
                    if (!trackingBack) {
                        if (dx <= 0) return false;
                        trackingBack = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    v.setTranslationX(Math.max(dx, 0));
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!trackingBack || ignoreSwipeThisGesture) return false;

                    float finalDx = event.getX() - downX;

                    if (finalDx > finishThresholdPx) {
                        v.animate().translationX(v.getWidth()).setDuration(180)
                                .withEndAction(() -> {
                                    finish();
                                    overridePendingTransition(0, 0);
                                })
                                .start();
                    } else {
                        v.animate().translationX(0).setDuration(180).start();
                    }

                    trackingBack = false;
                    return true;
            }
            return false;
        });
    }

    /**
     * Vérifie si un toucher est à l’intérieur d’une vue (en coordonnées écran)
     */
    private boolean isTouchInsideView(MotionEvent event, View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);

        float x = event.getRawX();
        float y = event.getRawY();

        return x >= loc[0] && x <= loc[0] + view.getWidth()
                && y >= loc[1] && y <= loc[1] + view.getHeight();
    }

    /**
     * Lit les données envoyées par Intent et remplit l’UI.
     * (ici tu peux ajouter les champs type/technicien/etc si tu veux les afficher)
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readExtrasAndFillUI() {
        Intent intent = getIntent();

        reference = safe(intent.getStringExtra("reference"));
        currentStatut = safeOrDefault(intent.getStringExtra("statut"), "Planifiée");
        currentPriorite = safeOrDefault(intent.getStringExtra("priorite"), "Basse");

        dateIntervention = parseDateOrNow(intent.getStringExtra("date"));

        tvReference.setText(reference.isEmpty() ? "—" : reference);
        tvStatutBadge.setText(currentStatut);
        tvPrioriteBadge.setText("Priorité " + currentPriorite);
        tvDate.setText(dateIntervention.format(dateFormatter));

        applyPriorityColor(currentPriorite);
        applyStatutBadgeStyle(currentStatut);
    }

    /**
     * Barre colorée selon la priorité
     */
    private void applyPriorityColor(String priorite) {
        if (vStatusColor == null) return;

        String p = priorite.toLowerCase(Locale.ROOT);

        int color = Color.parseColor("#4CAF50");          // défaut : vert
        if (p.contains("critique")) color = Color.parseColor("#D32F2F"); // rouge
        else if (p.contains("haute") || p.contains("haut")) color = Color.parseColor("#F05A5A");
        else if (p.contains("moy")) color = Color.parseColor("#F5A623"); // orange

        vStatusColor.setBackgroundColor(color);
    }

    /**
     * Style du badge statut (couleur fond + texte)
     */
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

    // ================= Utils =================

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