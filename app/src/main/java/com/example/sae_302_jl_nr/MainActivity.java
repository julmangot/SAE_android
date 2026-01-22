package com.example.sae_302_jl_nr;

import android.app.DatePickerDialog;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvDate;
    private ImageButton btnPrev, btnNext;

    private RecyclerView rvInterventions;
    private InterventionAdapter adapter;

    private LocalDate currentDate;

    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

    private final List<Intervention> allInterventions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Padding auto pour les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Vues du header date
        tvDate = findViewById(R.id.tvDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        // RecyclerView
        rvInterventions = findViewById(R.id.rvInterventions);
        rvInterventions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InterventionAdapter();
        rvInterventions.setAdapter(adapter);

        // Date initiale + data
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            currentDate = LocalDate.now();

            initAllData();               // charge les interventions une seule fois
            updateDateLabel();           // affiche la date
            reloadInterventionsForDay(); // affiche la liste du jour

            // Bouton jour précédent
            btnPrev.setOnClickListener(v -> {
                currentDate = currentDate.minusDays(1);
                updateDateLabel();
                reloadInterventionsForDay();
            });

            // Bouton jour suivant
            btnNext.setOnClickListener(v -> {
                currentDate = currentDate.plusDays(1);
                updateDateLabel();
                reloadInterventionsForDay();
            });

            // ✅ Clic sur la date => DatePicker
            tvDate.setOnClickListener(v -> {
                int year = currentDate.getYear();
                int month = currentDate.getMonthValue() - 1; // DatePicker: 0-11
                int day = currentDate.getDayOfMonth();

                DatePickerDialog dialog = new DatePickerDialog(
                        MainActivity.this,
                        (view, selectedYear, selectedMonth, selectedDay) -> {
                            currentDate = LocalDate.of(selectedYear, selectedMonth + 1, selectedDay);
                            updateDateLabel();
                            reloadInterventionsForDay();
                        },
                        year, month, day
                );


                dialog.show();
            });

        } else {
            tvDate.setText("Date");
            adapter.setData(new ArrayList<>());
        }
    }

    private void updateDateLabel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
            tvDate.setText(currentDate.format(formatter));
        }
    }

    private void initAllData() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        allInterventions.clear();

        // Semaine du 19 au 25 Janvier
        allInterventions.add(new Intervention("15480", "19 Janvier",
                LocalDate.of(2026, 1, 19),
                "SAV Fibre", "Critique", "Medhi Ralouf",
                "10 rue Paris", "Rennes", "Soudure", "1h", "Soudeuse", "Planifiée"));

        allInterventions.add(new Intervention("15481", "20 Janvier",
                LocalDate.of(2026, 1, 20),
                "Installation", "Basse", "Julie Bois",
                "2 av. Briand", "Rennes", "Pose Box", "45min", "Modem", "Terminée"));

        // JOUR TEST : 21 Janvier (3 interventions)
        allInterventions.add(new Intervention("15485", "21 Janvier",
                LocalDate.of(2026, 1, 21),
                "SAV Problème fibre", "Critique Haute", "Medhi Ralouf",
                "10 rue de Paris", "Rennes", "Test de continuité", "1h30", "Soudeuse optique", "Planifiée"));

        allInterventions.add(new Intervention("15486", "21 Janvier",
                LocalDate.of(2026, 1, 21),
                "Raccordement", "Moyenne", "Martin Delavega",
                "Zone Sud", "Rennes", "Tirage", "2h", "Echelle", "En cours"));

        allInterventions.add(new Intervention("15487", "21 Janvier",
                LocalDate.of(2026, 1, 21),
                "SAV Fibre", "Haute", "Julie Bois",
                "Rue de Fougères", "Rennes", "Soudure", "1h", "Soudeuse", "Planifiée"));

        // Autres jours
        allInterventions.add(new Intervention("15490", "22 Janvier",
                LocalDate.of(2026, 1, 22),
                "Audit", "Basse", "Thomas Le Gall",
                "Mairie", "Dinan", "Mesures", "3h", "Tablette", "Planifiée"));
    }

    private void reloadInterventionsForDay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        List<Intervention> filteredList = new ArrayList<>();
        for (Intervention i : allInterventions) {
            // ⚠️ adapte si ton champ date n'est pas public
            if (i != null && i.date != null && i.date.equals(currentDate)) {
                filteredList.add(i);
            }
        }

        adapter.setData(filteredList);
    }
}
