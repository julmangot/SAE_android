package com.example.sae_302_jl_nr;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITRE = "extra_titre";
    public static final String EXTRA_SOUS_TITRE = "extra_sous_titre";
    public static final String EXTRA_DATE = "extra_date";

    // Dans DetailActivity.java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Intervention i = (Intervention) getIntent().getSerializableExtra("EXTRA_INTER");
        if (i == null) return;

        TextView tvDetails = findViewById(R.id.tvDetails);

        // On construit l'affichage exact de ta Fenêtre 2 du PDF
        String content = "Mission n°" + i.idMission + "\n\n" +
                "Date : " + i.dateTexte + "\n" +
                "Type : " + i.type + "\n" +
                "Priorité : " + i.priority + "\n" +
                "Technicien : " + i.technician + "\n" +
                "Localisation : " + i.address + ", " + i.city + "\n\n" +
                "Actions : " + i.actions + "\n" +
                "Temps : " + i.time + "\n" +
                "Matériel : " + i.material + "\n\n" +
                "Statut actuel : " + i.status;

        tvDetails.setText(content);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}