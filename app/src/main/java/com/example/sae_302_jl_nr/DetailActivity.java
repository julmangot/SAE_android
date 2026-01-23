package com.example.sae_302_jl_nr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    // ===== UI (header) =====
    private View root;
    private ImageButton btnBack;
    private TextView tvHeaderTitle;

    // ===== UI (card principale) =====
    private View vStatusColor;
    private TextView tvTypeTitle, tvReference, tvStatutBadge, tvPrioriteBadge;
    private TextView tvDate, tvTechnicien, tvAdresse;

    // ===== UI (détails techniques) =====
    private TextView tvAction, tvDuree, tvMateriel;

    // ===== UI (bottom bar) =====
    private Button btnModifier, btnDelete;

    // ===== Data =====
    private String reference = "";
    private LocalDate dateIntervention = null;

    // ===== Swipe back fluide (bord gauche -> droite) =====
    private float downX = 0f;
    private boolean trackingBack = false;
    private int edgeSizePx;          // zone sensible bord gauche (px)
    private int finishThresholdPx;   // distance pour valider (px)

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // ===== Bind root + UI =====
        // IMPORTANT: ton root dans activity_detail.xml doit avoir android:id="@+id/rootDetail"
        root = findViewById(R.id.rootDetail);

        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);

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

        // Header
        if (tvHeaderTitle != null) tvHeaderTitle.setText("Détails Mission");

        // Bouton retour
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Swipe back fluide (comme bouton back)
        setupSwipeBack();

        // Remplir l'écran avec les extras envoyés par MainActivity
        readExtrasAndFillUI();

        // Boutons (tu peux brancher tes APIs ici)
        setupBottomButtons();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readExtrasAndFillUI() {
        Intent intent = getIntent();

        reference = safe(intent.getStringExtra("reference"));

        String dateStr = intent.getStringExtra("date");
        dateIntervention = parseDateOrNow(dateStr);

        String statut = safeOrDefault(intent.getStringExtra("statut"), "Planifiée");
        String priorite = safeOrDefault(intent.getStringExtra("priorite"), "Basse");

        String type = safeOrDefault(intent.getStringExtra("type"), "Intervention");
        String technicien = safe(intent.getStringExtra("technicien"));
        String adresse = safe(intent.getStringExtra("adresse"));
        String ville = safe(intent.getStringExtra("ville"));
        String action = safe(intent.getStringExtra("action"));
        String duree = safe(intent.getStringExtra("duree"));
        String materiel = safe(intent.getStringExtra("materiel"));

        // ---- UI ----
        if (tvTypeTitle != null) tvTypeTitle.setText(type.isEmpty() ? "Intervention" : type);
        if (tvReference != null) tvReference.setText(reference.isEmpty() ? "—" : reference);

        if (tvStatutBadge != null) tvStatutBadge.setText(statut);
        if (tvPrioriteBadge != null) tvPrioriteBadge.setText("Priorité " + priorite);

        if (tvDate != null) tvDate.setText(dateIntervention.format(dateFormatter));
        if (tvTechnicien != null) tvTechnicien.setText(technicien.isEmpty() ? "—" : technicien);

        String adrFull = (adresse + (ville.isEmpty() ? "" : ", " + ville)).trim();
        if (tvAdresse != null) tvAdresse.setText(adrFull.isEmpty() ? "—" : adrFull);

        if (tvAction != null) tvAction.setText(action.isEmpty() ? "—" : action);
        if (tvDuree != null) tvDuree.setText(duree.isEmpty() ? "—" : duree);
        if (tvMateriel != null) tvMateriel.setText(materiel.isEmpty() ? "—" : materiel);

        // Styles
        applyPriorityColor(priorite);
        applyStatutBadgeStyle(statut);
    }

    private void setupBottomButtons() {
        // NOTE: ici on met juste un retour "changed=true" en exemple.
        // Branche tes appels API (update statut / delete) à la place.

        if (btnModifier != null) {
            btnModifier.setOnClickListener(v -> {
                // TODO: ouvrir dialog, appeler API update statut, puis:
                Intent data = new Intent();
                data.putExtra("changed", true);
                setResult(RESULT_OK, data);
                // Tu peux laisser l'écran ouvert si tu veux
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                // TODO: appeler API delete, puis:
                Intent data = new Intent();
                data.putExtra("changed", true);
                setResult(RESULT_OK, data);
                finish();
            });
        }
    }

    private void setupSwipeBack() {
        if (root == null) return;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        edgeSizePx = (int) (24 * dm.density);          // 24dp
        finishThresholdPx = (int) (110 * dm.density);  // 110dp

        // IMPORTANT : pour capter les events sur tout l'écran
        root.setClickable(true);
        root.setFocusable(true);

        root.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    trackingBack = downX <= edgeSizePx; // start depuis bord gauche
                    return trackingBack;
                }

                case MotionEvent.ACTION_MOVE: {
                    if (!trackingBack) return false;

                    float dx = event.getX() - downX;
                    if (dx < 0) dx = 0;

                    // fluide : l'écran suit le doigt
                    v.setTranslationX(dx);
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!trackingBack) return false;

                    float dx = event.getX() - downX;

                    if (dx > finishThresholdPx) {
                        // Validé -> on glisse hors écran puis finish
                        v.animate()
                                .translationX(v.getWidth())
                                .setDuration(180)
                                .withEndAction(() -> {
                                    finish();
                                    overridePendingTransition(0, 0);
                                })
                                .start();
                    } else {
                        // Pas assez -> retour à la position 0
                        v.animate()
                                .translationX(0)
                                .setDuration(180)
                                .start();
                    }

                    trackingBack = false;
                    return true;
                }
            }
            return false;
        });
    }

    private void applyPriorityColor(String prioriteStr) {
        if (vStatusColor == null) return;

        String p = (prioriteStr == null) ? "" : prioriteStr.toLowerCase(Locale.ROOT);
        int color = Color.parseColor("#4CAF50"); // basse

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

    // ===== Utils =====

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

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
