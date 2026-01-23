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

public class DetailActivity extends AppCompatActivity {

    // ===== API =====
    private static final String API_BASE = "http://51.38.176.17";
    // ✅ Mets ici TES vrais noms de fichiers PHP
    private static final String API_UPDATE_STATUT = API_BASE + "/interventions_status.php";
    private static final String API_DELETE        = API_BASE + "/interventions_delete.php";

    private RequestQueue queue;

    // ===== UI =====
    private View root;
    private View bottomActionContainer;

    private ImageButton btnBack;
    private View vStatusColor;

    private TextView tvTypeTitle, tvReference, tvStatutBadge, tvPrioriteBadge;
    private TextView tvDate, tvTechnicien, tvAdresse;
    private TextView tvAction, tvDuree, tvMateriel;

    private Button btnModifier, btnDelete;

    // ===== Data =====
    private String reference = "";
    private LocalDate dateIntervention;

    private String currentStatut = "Planifiée";
    private String currentPriorite = "Basse";

    // ===== Swipe back =====
    private float downX = 0f, downY = 0f;
    private boolean trackingBack = false;
    private boolean ignoreSwipeThisGesture = false;

    private int finishThresholdPx;
    private int touchSlopPx;

    // ✅ empêche le swipe de te faire quitter pendant une requête réseau
    private boolean networkBusy = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

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

        // Seuils swipe
        DisplayMetrics dm = getResources().getDisplayMetrics();
        finishThresholdPx = (int) (110 * dm.density);
        touchSlopPx = ViewConfiguration.get(this).getScaledTouchSlop();

        // Back button
        btnBack.setOnClickListener(v -> finish());

        // Remplir UI
        readExtrasAndFillUI();

        // Boutons -> API
        setupButtons();

        // Swipe retour sur le reste de l'écran (pas sur zone boutons)
        setupSwipeBackExcludingBottom();
    }

    private void setupButtons() {
        // ✅ Les clics doivent toujours passer : pas de swipe sur bottomActionContainer
        btnModifier.setOnClickListener(v -> showStatutDialog());
        btnDelete.setOnClickListener(v -> showDeleteConfirm());
    }

    private void showStatutDialog() {
        final String[] statuts = new String[]{"Planifiée", "En cours", "Terminée"};

        int tmpChecked = 0;
        for (int i = 0; i < statuts.length; i++) {
            if (statuts[i].equalsIgnoreCase(currentStatut)) {
                tmpChecked = i;
                break;
            }
        }
        final int checkedIndex = tmpChecked;

        new AlertDialog.Builder(this)
                .setTitle("Changer le statut")
                .setSingleChoiceItems(statuts, checkedIndex, null)
                .setNegativeButton("Annuler", null)
                .setPositiveButton("OK", (dialog, which) -> {
                    AlertDialog ad = (AlertDialog) dialog;
                    int pos = ad.getListView().getCheckedItemPosition();
                    if (pos < 0) pos = checkedIndex;

                    String newStatut = statuts[pos];

                    // ✅ appel serveur
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

                        // ✅ Mise à jour UI
                        currentStatut = response.optString("statut_saved", newStatut);
                        tvStatutBadge.setText(currentStatut);
                        applyStatutBadgeStyle(currentStatut);

                        // ✅ Prévenir MainActivity de recharger
                        Intent data = new Intent();
                        data.putExtra("changed", true);
                        setResult(RESULT_OK, data);

                        Toast.makeText(this, "Statut sauvegardé ✅", Toast.LENGTH_SHORT).show();
                    },
                    error -> {
                        networkBusy = false;
                        setButtonsEnabled(true);

                        error.printStackTrace();
                        Toast.makeText(this, "Erreur update statut", Toast.LENGTH_LONG).show();
                    }
            );

            queue.add(req);

        } catch (Exception e) {
            networkBusy = false;
            setButtonsEnabled(true);

            e.printStackTrace();
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }

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

                        error.printStackTrace();
                        Toast.makeText(this, "Erreur suppression", Toast.LENGTH_LONG).show();
                    }
            );

            queue.add(req);

        } catch (Exception e) {
            networkBusy = false;
            setButtonsEnabled(true);

            e.printStackTrace();
            Toast.makeText(this, "Erreur JSON", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Swipe retour sur le reste de la page.
     * IMPORTANT :
     * - si on est dans bottomActionContainer -> on ignore (boutons cliquables)
     * - si networkBusy -> on ignore (évite retour avant synchro)
     */
    private void setupSwipeBackExcludingBottom() {
        if (root == null) return;

        root.setOnTouchListener((v, event) -> {
            if (networkBusy) return false;

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    downY = event.getY();
                    trackingBack = false;

                    ignoreSwipeThisGesture =
                            (bottomActionContainer != null) && isTouchInsideView(event, bottomActionContainer);

                    return false; // laisse passer les clicks
                }

                case MotionEvent.ACTION_MOVE: {
                    if (ignoreSwipeThisGesture) return false;

                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;

                    // pas assez bougé -> click/scroll
                    if (Math.abs(dx) < touchSlopPx && Math.abs(dy) < touchSlopPx) return false;

                    // vertical -> scroll normal
                    if (!trackingBack && Math.abs(dy) > Math.abs(dx)) return false;

                    // prendre seulement swipe gauche->droite
                    if (!trackingBack) {
                        if (dx <= 0) return false;
                        trackingBack = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    if (dx < 0) dx = 0;
                    v.setTranslationX(dx);
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (ignoreSwipeThisGesture) return false;
                    if (!trackingBack) return false;

                    float dx = event.getX() - downX;

                    if (dx > finishThresholdPx) {
                        v.animate()
                                .translationX(v.getWidth())
                                .setDuration(180)
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
            }
            return false;
        });
    }

    private boolean isTouchInsideView(MotionEvent event, View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);

        float x = event.getRawX();
        float y = event.getRawY();

        return x >= loc[0] && x <= (loc[0] + view.getWidth())
                && y >= loc[1] && y <= (loc[1] + view.getHeight());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readExtrasAndFillUI() {
        Intent intent = getIntent();

        reference = safe(intent.getStringExtra("reference"));
        String dateStr = intent.getStringExtra("date");

        currentStatut = safeOrDefault(intent.getStringExtra("statut"), "Planifiée");
        currentPriorite = safeOrDefault(intent.getStringExtra("priorite"), "Basse");

        String type = safeOrDefault(intent.getStringExtra("type"), "Intervention");
        String technicien = safe(intent.getStringExtra("technicien"));
        String adresse = safe(intent.getStringExtra("adresse"));
        String ville = safe(intent.getStringExtra("ville"));
        String action = safe(intent.getStringExtra("action"));
        String duree = safe(intent.getStringExtra("duree"));
        String materiel = safe(intent.getStringExtra("materiel"));

        dateIntervention = parseDateOrNow(dateStr);

        tvTypeTitle.setText(type.isEmpty() ? "Intervention" : type);
        tvReference.setText(reference.isEmpty() ? "—" : reference);

        tvStatutBadge.setText(currentStatut);
        tvPrioriteBadge.setText("Priorité " + currentPriorite);

        tvDate.setText(dateIntervention.format(dateFormatter));
        tvTechnicien.setText(technicien.isEmpty() ? "—" : technicien);

        String adrFull = (adresse + (ville.isEmpty() ? "" : ", " + ville)).trim();
        tvAdresse.setText(adrFull.isEmpty() ? "—" : adrFull);

        tvAction.setText(action.isEmpty() ? "—" : action);
        tvDuree.setText(duree.isEmpty() ? "—" : duree);
        tvMateriel.setText(materiel.isEmpty() ? "—" : materiel);

        applyPriorityColor(currentPriorite);
        applyStatutBadgeStyle(currentStatut);
    }

    private void applyPriorityColor(String prioriteStr) {
        if (vStatusColor == null) return;

        String p = (prioriteStr == null) ? "" : prioriteStr.toLowerCase(Locale.ROOT);

        int color = Color.parseColor("#4CAF50");
        if (p.contains("critique")) color = Color.parseColor("#D32F2F");
        else if (p.contains("haute") || p.contains("haut")) color = Color.parseColor("#F05A5A");
        else if (p.contains("moy")) color = Color.parseColor("#F5A623");

        vStatusColor.setBackgroundColor(color);
    }

    private void applyStatutBadgeStyle(String statut) {
        if (tvStatutBadge == null) return;

        String s = (statut == null) ? "" : statut.toLowerCase(Locale.ROOT);

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

    // Utils
    private static String safe(String s) { return (s == null) ? "" : s.trim(); }
    private static String safeOrDefault(String s, String def) {
        String v = safe(s);
        return v.isEmpty() ? def : v;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static LocalDate parseDateOrNow(String dateStr) {
        try {
            if (dateStr == null) return LocalDate.now();
            String ds = dateStr.trim();
            if (ds.contains(" ")) ds = ds.split(" ")[0];
            return LocalDate.parse(ds);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }
}
