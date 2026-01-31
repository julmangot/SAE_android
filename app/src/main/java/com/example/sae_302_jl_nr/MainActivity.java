package com.example.sae_302_jl_nr;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONObject;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;

/**
 * MainActivity
 * ------------
 * Écran principal de l’application.
 *
 * Rôles :
 * - Afficher la date sélectionnée (jour courant)
 * - Lister les interventions de ce jour (RecyclerView)
 * - Naviguer entre les jours (boutons + swipe) en sautant les week-ends
 * - Ouvrir :
 *   → DetailActivity au clic sur une intervention
 *   → AddInterventionActivity via le bouton "+"
 * - Recharger la liste au retour d’un écran si "changed=true"
 *
 * Données :
 * - Récupérées via l’API PHP /interventions.php?date=YYYY-MM-DD (Volley)
 */
public class MainActivity extends AppCompatActivity {

    // ===== UI =====
    private TextView tvDate;                     // Affiche la date au format "31 janvier 2026"
    private ImageButton btnPrev, btnNext;        // Navigation jour précédent / suivant
    private FloatingActionButton fabAdd;         // Bouton + pour ajouter

    private RecyclerView rvInterventions;        // Liste d’interventions
    private InterventionAdapter adapter;         // Adaptateur de la RecyclerView

    // ===== Date courante affichée =====
    private LocalDate currentDate;

    // Format d’affichage de la date (texte)
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    // ===== API =====
    private static final String API_BASE = "http://51.38.176.17";
    private static final String API_LIST = API_BASE + "/interventions.php?date=";

    // Volley queue : gère les requêtes HTTP (GET, POST, etc.)
    private RequestQueue requestQueue;

    // Lanceur d’activité (sert pour DetailActivity et AddInterventionActivity)
    // -> permet de récupérer "changed=true" au retour
    private ActivityResultLauncher<Intent> detailLauncher;

    // ===== Swipe (gauche/droite) pour changer de jour =====
    private GestureDetector gestureDetector;

    // Seuil minimum pour considérer un swipe
    private static final int SWIPE_THRESHOLD = 120;

    // Vitesse minimum pour valider le swipe
    private static final int SWIPE_VELOCITY_THRESHOLD = 120;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Mode EdgeToEdge : l’app peut aller sous la barre de statut / navigation
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Applique des marges (padding) pour éviter que l’UI soit cachée par les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialise le détecteur de swipe (gauche/droite)
        setupSwipeDetector();

        // Bind des vues (récupération depuis activity_main.xml)
        tvDate = findViewById(R.id.tvDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        fabAdd = findViewById(R.id.fabAdd);

        // RecyclerView : liste + layout vertical
        rvInterventions = findViewById(R.id.rvInterventions);
        rvInterventions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InterventionAdapter();
        rvInterventions.setAdapter(adapter);

        // Volley : initialisation
        requestQueue = Volley.newRequestQueue(this);

        // Lanceur pour récupérer les retours d’activités (Detail/Add)
        setupActivityResultLauncher();

        // Clic sur un item -> ouvre DetailActivity avec tous les champs en extra
        setupItemClickToOpenDetail();

        // Si Android < 8 (API 26), LocalDate n’existe pas -> fallback simple
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Date initiale : aujourd’hui
            currentDate = LocalDate.now();

            // Si on lance l’app un week-end, on avance jusqu’au lundi
            skipWeekendIfNeeded(+1);

            updateDateLabel();
            reloadInterventionsForDay();

            // Boutons précédent / suivant
            btnPrev.setOnClickListener(v -> changeDay(-1));
            btnNext.setOnClickListener(v -> changeDay(+1));

            // Clic sur la date -> ouvre un DatePicker
            tvDate.setOnClickListener(v -> openDatePicker());

            // Bouton + -> ouvre AddInterventionActivity (date pré-remplie)
            fabAdd.setOnClickListener(v -> openAddIntervention());
        } else {
            // Mode compat : pas de LocalDate
            tvDate.setText("Date");
            adapter.setData(new ArrayList<>());
        }
    }

    /**
     * Configure le détecteur de swipe horizontal.
     * - Swipe droite : jour précédent
     * - Swipe gauche : jour suivant
     */
    private void setupSwipeDetector() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // On garde uniquement les swipes horizontaux (plus grands que les verticaux)
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    // Seuil distance + vitesse
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) changeDay(-1); // vers la droite -> passé
                        else changeDay(+1);           // vers la gauche -> futur
                        return true;
                    }
                }
                return false;
            }
        });
    }

    /**
     * Lanceur d’activité :
     * quand on revient de Detail/Add, on regarde "changed".
     * - si changed=true -> on recharge la liste du jour.
     */
    private void setupActivityResultLauncher() {
        detailLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        boolean changed = result.getData().getBooleanExtra("changed", false);
                        if (changed) reloadInterventionsForDay();
                    }
                }
        );
    }

    /**
     * Au clic sur une intervention, on ouvre DetailActivity.
     * On passe tous les champs nécessaires via Intent extras.
     */
    private void setupItemClickToOpenDetail() {
        adapter.setOnInterventionClickListener(intervention -> {
            Intent intent = new Intent(MainActivity.this, DetailActivity.class);

            // Infos clés
            intent.putExtra("reference", intervention.idMission);
            intent.putExtra("date", intervention.dateIntervention.toString());

            // Statut / priorité (affichage + couleurs)
            intent.putExtra("statut", intervention.statut);
            intent.putExtra("priorite", intervention.prioriteStr);

            // Infos détaillées
            intent.putExtra("type", intervention.type);
            intent.putExtra("technicien", intervention.technicien);
            intent.putExtra("adresse", intervention.adresse);
            intent.putExtra("ville", intervention.ville);
            intent.putExtra("action", intervention.action);
            intent.putExtra("duree", intervention.duree);
            intent.putExtra("materiel", intervention.materiel);

            detailLauncher.launch(intent);
        });
    }

    /**
     * Ouvre le DatePicker pour choisir une date.
     * Après sélection :
     * - on met à jour currentDate
     * - on met à jour le label
     * - on recharge les interventions
     */
    private void openDatePicker() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        int year = currentDate.getYear();
        int month = currentDate.getMonthValue() - 1; // DatePicker = mois 0..11
        int day = currentDate.getDayOfMonth();

        DatePickerDialog dialog = new DatePickerDialog(
                MainActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    currentDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay);

                    // Si l’utilisateur choisit un week-end, on bascule automatiquement
                    // Ici on choisit d'aller vers le lundi (sens +1)
                    skipWeekendIfNeeded(+1);

                    updateDateLabel();
                    reloadInterventionsForDay();
                },
                year, month, day
        );

        dialog.show();
    }

    /**
     * Ouvre AddInterventionActivity avec la date pré-remplie.
     */
    private void openAddIntervention() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        Intent i = new Intent(MainActivity.this, AddInterventionActivity.class);
        i.putExtra("date", currentDate.toString());
        detailLauncher.launch(i);
    }

    /**
     * Met à jour le texte de la date (ex: "31 janvier 2026").
     */
    private void updateDateLabel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
            tvDate.setText(currentDate.format(formatter));
        }
    }

    /**
     * Change de jour (+1 ou -1) en sautant le week-end.
     * - deltaDays = +1 -> jour suivant
     * - deltaDays = -1 -> jour précédent
     */
    private void changeDay(int deltaDays) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        // 1) applique le changement
        currentDate = currentDate.plusDays(deltaDays);

        // 2) saute samedi/dimanche dans le bon sens
        skipWeekendIfNeeded(deltaDays);

        updateDateLabel();
        reloadInterventionsForDay();
    }

    /**
     * Si currentDate tombe sur samedi/dimanche, on saute :
     * - si deltaDays > 0 -> on avance vers lundi
     * - si deltaDays < 0 -> on recule vers vendredi
     */
    private void skipWeekendIfNeeded(int deltaDays) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        while (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY ||
                currentDate.getDayOfWeek() == DayOfWeek.SUNDAY) {

            if (deltaDays > 0) currentDate = currentDate.plusDays(1);
            else currentDate = currentDate.minusDays(1);
        }
    }

    /**
     * Appel API :
     * - GET /interventions.php?date=YYYY-MM-DD
     * - Parse le JSON en objets Intervention
     * - Calcule une priorité int (pour trier)
     * - Trie la liste (priorité décroissante)
     * - Donne la liste à l'adapter
     */
    private void reloadInterventionsForDay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        String url = API_LIST + currentDate;

        JsonArrayRequest request = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    ArrayList<Intervention> list = new ArrayList<>();

                    // Parse chaque objet JSON de la réponse
                    for (int idx = 0; idx < response.length(); idx++) {
                        JSONObject o = response.optJSONObject(idx);
                        if (o == null) continue;

                        // Champs principaux (les noms doivent matcher ton PHP)
                        String reference = o.optString("reference", "");

                        String dateStr = o.optString("date_intervention", currentDate.toString());
                        if (dateStr.contains(" ")) dateStr = dateStr.split(" ")[0]; // garde YYYY-MM-DD

                        String type = o.optString("type_intervention", "");
                        String prioriteStr = o.optString("priorite", "Basse");
                        String technicien = o.optString("technicien", "");
                        String adresse = o.optString("adresse", "");
                        String ville = o.optString("ville", "");
                        String action = o.optString("action_realisee", "");
                        String duree = o.optString("duree", "");
                        String materiel = o.optString("materiel", "");
                        String statut = o.optString("statut", "Planifiée");

                        // Conversion date JSON -> LocalDate
                        LocalDate d;
                        try { d = LocalDate.parse(dateStr); }
                        catch (Exception e) { d = currentDate; }

                        // Petit label court (ex: "31 janvier")
                        String libelleCourt = d.getDayOfMonth() + " " +
                                d.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.FRENCH);

                        // Création de l’objet Intervention
                        Intervention in = new Intervention(
                                reference,
                                libelleCourt,
                                d,
                                type,
                                prioriteStr,
                                technicien,
                                adresse,
                                ville,
                                action,
                                duree,
                                materiel,
                                statut
                        );

                        // Priorité en int pour trier facilement (4=critique, 1=basse)
                        int prioriteInt = 1;
                        String p = (prioriteStr == null) ? "" : prioriteStr.toLowerCase(Locale.ROOT);
                        if (p.contains("critique")) prioriteInt = 4;
                        else if (p.contains("haut")) prioriteInt = 3;
                        else if (p.contains("moy")) prioriteInt = 2;

                        in.priorite = prioriteInt;
                        list.add(in);
                    }

                    // Tri : priorité décroissante (critique en haut)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        list.sort((i1, i2) -> Integer.compare(i2.priorite, i1.priorite));
                    }

                    // Envoi à l’adapter pour affichage
                    adapter.setData(list);
                },
                error -> {
                    // On récupère le code HTTP + message si possible (debug)
                    int code = -1;
                    String bodyErr = "";

                    if (error.networkResponse != null) {
                        code = error.networkResponse.statusCode;
                        try {
                            bodyErr = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception ignored) {}
                    }

                    error.printStackTrace();
                    adapter.setData(new ArrayList<>());
                    Toast.makeText(this, "Erreur API (" + code + "): " + bodyErr, Toast.LENGTH_LONG).show();
                }
        );

        // Ajoute la requête dans la file Volley (déclenche l’appel)
        requestQueue.add(request);
    }

    /**
     * Intercepte les touch events pour alimenter le GestureDetector (swipe).
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }
}
