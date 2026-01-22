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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tvDate = findViewById(R.id.tvDate);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        rvInterventions = findViewById(R.id.rvInterventions);

        rvInterventions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new InterventionAdapter();
        rvInterventions.setAdapter(adapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            currentDate = LocalDate.now(); // Ou une date fixe pour tester: LocalDate.of(2026, 1, 21);
            updateUI();

            btnPrev.setOnClickListener(v -> {
                currentDate = currentDate.minusDays(1);
                updateUI();
            });

            btnNext.setOnClickListener(v -> {
                currentDate = currentDate.plusDays(1);
                updateUI();
            });

            tvDate.setOnClickListener(v -> {
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            currentDate = LocalDate.of(year, month + 1, dayOfMonth);
                            updateUI();
                        },
                        currentDate.getYear(), currentDate.getMonthValue() - 1, currentDate.getDayOfMonth());
                dialog.show();
            });
        }
    }

    // IMPORTANT : Se déclenche quand on revient de "DetailActivity"
    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && currentDate != null) {
            updateUI();
        }
    }

    private void updateUI() {
        tvDate.setText(currentDate.format(formatter));
        reloadInterventionsForDay();
    }

    private void reloadInterventionsForDay() {
        List<Intervention> allData = DataRepository.getAllInterventions(); // On prend les données partagées
        List<Intervention> filteredList = new ArrayList<>();

        for (Intervention i : allData) {
            if (i != null && i.date != null && i.date.equals(currentDate)) {
                filteredList.add(i);
            }
        }
        adapter.setData(filteredList);
    }
}