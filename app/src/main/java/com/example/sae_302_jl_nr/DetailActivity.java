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

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DetailActivity extends AppCompatActivity {

    // ===== UI =====
    private View root;
    private ImageButton btnBack;

    private View vStatusColor;
    private TextView tvTypeTitle, tvReference, tvStatutBadge, tvPrioriteBadge;
    private TextView tvDate, tvTechnicien, tvAdresse;
    private TextView tvAction, tvDuree, tvMateriel;

    private Button btnModifier, btnDelete;

    // ===== Data =====
    private String reference = "";
    private LocalDate dateIntervention = null;

    // ===== Swipe-back "partout" (gauche -> droite) =====
    private float downX = 0f, downY = 0f;
    private boolean trackingBack = false;
    private int finishThresholdPx;     // distance pour valider le retour
    private int touchSlopPx;           // évite déclenchement par petits mouvements

    @RequiresApi(api = Build.VERSION_CODES.O)
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Root (tu as ajouté android:id="@+id/rootDetail" dans ton XML)
        root = findViewById(R.id.rootDetail);

        // Bind views
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

        // Back button
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        // Setup swipe "partout"
        setupGlobalSwipeBack();

        // Fill UI from intent
        readExtrasAndFillUI();

        // Bottom buttons (placeholder -> à brancher sur tes APIs)
        setupBottomButtons();
    }

    private void setupGlobalSwipeBack() {
        if (root == null) return;

        DisplayMetrics dm = getResources().getDisplayMetrics();
        finishThresholdPx = (int) (110 * dm.density); // 110dp
        touchSlopPx = ViewConfiguration.get(this).getScaledTouchSlop();

        root.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {

                case MotionEvent.ACTION_DOWN: {
                    downX = event.getX();
                    downY = event.getY();
                    trackingBack = false;

                    // IMPORTANT : on ne consomme pas encore, sinon scroll vertical cassé
                    return false;
                }

                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getX() - downX;
                    float dy = event.getY() - downY;

                    // tant que c'est un petit mouvement -> laisse scroller
                    if (Math.abs(dx) < touchSlopPx && Math.abs(dy) < touchSlopPx) {
                        return false;
                    }

                    // Si on n'a pas encore "pris" le gesture :
                    if (!trackingBack) {
                        // si l'utilisateur part vertical -> laisser le ScrollView gérer
                        if (Math.abs(dy) > Math.abs(dx)) {
                            return false;
                        }
                        // c'est horizontal -> on prend le contrôle
                        trackingBack = true;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }

                    // trackingBack = true : on anime l'écran avec le doigt
                    if (dx < 0) dx = 0; // on ignore swipe droite->gauche
                    v.setTranslationX(dx);
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    if (!trackingBack) return false;

                    float dx = event.getX() - downX;

                    if (dx > finishThresholdPx) {
                        // retour validé
                        v.animate()
                                .translationX(v.getWidth())
                                .setDuration(180)
                                .withEndAction(() -> {
                                    finish();
                                    overridePendingTransition(0, 0);
                                })
                                .start();
                    } else {
                        // annule -> revient en place
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

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void readExtrasAndFillUI() {
        Intent intent = getIntent();

        reference = safe(intent.getStringExtra("reference"));
        String dateStr = intent.getStringExtra("date");

        String statut = safeOrDefault(intent.getStringExtra("statut"), "Planifiée");
        String priorite = safeOrDefault(intent.getStringExtra("priorite"), "Basse");

        String type = safeOrDefault(intent.getStringExtra("type"), "Intervention");
        String technicien = safe(intent.getStringExtra("technicien"));
        String adresse = safe(intent.getStringExtra("adresse"));
        String ville = safe(intent.getStringExtra("ville"));
        String action = safe(intent.getStringExtra("action"));
        String duree = safe(intent.getStringExtra("duree"));
        String materiel = safe(intent.getStringExtra("materiel"));

        dateIntervention = parseDateOrNow(dateStr);

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

        applyPriorityColor(priorite);
        applyStatutBadgeStyle(statut);
    }

    private void setupBottomButtons() {
        // ⚠️ placeholder : remplace par tes appels API statut/delete
        if (btnModifier != null) {
            btnModifier.setOnClickListener(v -> {
                Intent data = new Intent();
                data.putExtra("changed", true);
                setResult(RESULT_OK, data);
                // tu peux rester sur la page si tu veux
            });
        }

        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                Intent data = new Intent();
                data.putExtra("changed", true);
                setResult(RESULT_OK, data);
                finish();
            });
        }
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
