package com.example.sae_302_jl_nr;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_TITRE = "extra_titre";
    public static final String EXTRA_SOUS_TITRE = "extra_sous_titre";
    public static final String EXTRA_DATE = "extra_date";
    public static final String EXTRA_PRIORITE = "extra_priorite"; // ✅ ajouté

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        TextView tvDetails = findViewById(R.id.tvDetails);
        ImageButton btnBack = findViewById(R.id.btnBack);
        View vLeft = findViewById(R.id.vLeft);

        // Données reçues
        String titre = getIntent().getStringExtra(EXTRA_TITRE);
        String sousTitre = getIntent().getStringExtra(EXTRA_SOUS_TITRE);
        String date = getIntent().getStringExtra(EXTRA_DATE);
        int priorite = getIntent().getIntExtra(EXTRA_PRIORITE, 1); // défaut = basse

        // Texte priorité
        String prioriteTxt;
        if (priorite == 3) prioriteTxt = "Haute";
        else if (priorite == 2) prioriteTxt = "Moyenne";
        else prioriteTxt = "Basse";

        // 🎨 Couleur barre gauche (synchro avec la liste)
        switch (priorite) {
            case 3:
                vLeft.setBackgroundColor(Color.parseColor("#F05A5A")); // rouge
                break;
            case 2:
                vLeft.setBackgroundColor(Color.parseColor("#F5A623")); // orange
                break;
            default:
                vLeft.setBackgroundColor(Color.parseColor("#4CAF50")); // vert
                break;
        }

        // Contenu détails
        String details =
                "• Date : " + (date != null ? date : "-") + "\n" +
                        "• Mission : " + (titre != null ? titre : "-") + "\n" +
                        "• Statut / Lieu : " + (sousTitre != null ? sousTitre : "-") + "\n" +
                        "• Priorité : " + prioriteTxt + "\n\n" +
                        "• Actions à mener : Test de continuité\n" +
                        "• Temps estimé : 1h30\n" +
                        "• Matériel : Soudeuse optique, jarretière fibre";

        tvDetails.setText(details);

        // Retour agenda
        btnBack.setOnClickListener(v -> finish());
    }
}
