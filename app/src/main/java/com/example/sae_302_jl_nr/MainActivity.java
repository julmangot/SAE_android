package com.example.sae_302_jl_nr;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Padding auto pour les barres système (status bar / nav bar)
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

        // Date initiale + listeners
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now();
            updateDateLabel();
            reloadInterventionsForDay();

            btnPrev.setOnClickListener(v -> {
                currentDate = currentDate.minusDays(1);
                updateDateLabel();
                reloadInterventionsForDay();
            });

            btnNext.setOnClickListener(v -> {
                currentDate = currentDate.plusDays(1);
                updateDateLabel();
                reloadInterventionsForDay();
            });

        } else {
            // Si minSdk < 26 : LocalDate non dispo
            tvDate.setText("Date");
            adapter.setData(new ArrayList<>());
        }
    }

    private void updateDateLabel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
            tvDate.setText(currentDate.format(formatter));
        }
    }

    private void reloadInterventionsForDay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || currentDate == null) return;

        List<Intervention> list = new ArrayList<>();

        // Exemple: on affiche 1 item seulement le 21 janvier 2026
        LocalDate testDate = LocalDate.of(2026, 1, 21);

        if (currentDate.equals(testDate)) {
            list.add(new Intervention(
                    "Raccordement fibre | Client Test",
                    "Planifiée | Saint-Malo",
                    currentDate
            ));
        }

        adapter.setData(list);
    }
}
