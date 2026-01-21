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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        // Vues
        TextView tvDetails = findViewById(R.id.tvDetails);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Données reçues depuis l’agenda
        String titre = getIntent().getStringExtra(EXTRA_TITRE);
        String sousTitre = getIntent().getStringExtra(EXTRA_SOUS_TITRE);
        String date = getIntent().getStringExtra(EXTRA_DATE);

        // Texte de détail
        String details =
                "• Date : " + (date != null ? date : "-") + "\n" +
                        "• Mission : " + (titre != null ? titre : "-") + "\n" +
                        "• Statut / Lieu : " + (sousTitre != null ? sousTitre : "-") + "\n\n" +
                        "• Actions à mener : Test de continuité\n" +
                        "• Temps estimé : 1h30\n" +
                        "• Matériel : Soudeuse optique, jarretière fibre";

        tvDetails.setText(details);

        // Retour vers l’agenda
        btnBack.setOnClickListener(v -> finish());
    }
}