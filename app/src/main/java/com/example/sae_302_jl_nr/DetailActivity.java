package com.example.sae_302_jl_nr;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    // C'est cette ligne qui manquait et causait ton erreur :
    public static final String EXTRA_ID_MISSION = "extra_id_mission";

    private Intervention currentIntervention;
    private TextView tvDetails;
    private View vLeft;
    private String tempSelectedStatus = ""; // pour stocker le choix temporaire du popup

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detail);

        tvDetails = findViewById(R.id.tvDetails);
        ImageButton btnBack = findViewById(R.id.btnBack);
        Button btnModifier = findViewById(R.id.btnModifier);
        vLeft = findViewById(R.id.vLeft);

        // 1. Récupérer l'ID envoyé par l'Adapter
        String id = getIntent().getStringExtra(EXTRA_ID_MISSION);

        // 2. Chercher la VRAIE intervention dans le Repository
        currentIntervention = DataRepository.getInterventionById(id);

        if (currentIntervention == null) {
            Toast.makeText(this, "Erreur : Mission introuvable", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 3. Afficher les infos
        refreshDisplay();

        // 4. Action Bouton Modifier (POPUP)
        btnModifier.setOnClickListener(v -> showStatusDialog());

        // Bouton retour
        btnBack.setOnClickListener(v -> finish());
    }

    private void refreshDisplay() {
        // Texte priorité
        String prioriteTxt;
        if (currentIntervention.priorite == 3) prioriteTxt = "Haute";
        else if (currentIntervention.priorite == 2) prioriteTxt = "Moyenne";
        else prioriteTxt = "Basse";

        // Couleur barre gauche
        switch (currentIntervention.priorite) {
            case 3: vLeft.setBackgroundColor(Color.parseColor("#F05A5A")); break;
            case 2: vLeft.setBackgroundColor(Color.parseColor("#F5A623")); break;
            default: vLeft.setBackgroundColor(Color.parseColor("#4CAF50")); break;
        }

        // Contenu du texte
        String details =
                "• Date : " + currentIntervention.date.toString() + "\n" +
                        "• Mission : " + currentIntervention.getTitreCarte() + "\n" +
                        "• Statut / Lieu : " + currentIntervention.getSousTitreCarte() + "\n" +
                        "• Priorité : " + prioriteTxt + "\n\n" +
                        "• Actions à mener : " + currentIntervention.action + "\n" +
                        "• Temps estimé : " + currentIntervention.tempsEstime + "\n" +
                        "• Matériel : " + currentIntervention.materiel;

        tvDetails.setText(details);
    }

    private void showStatusDialog() {
        // Les options possibles
        final String[] options = {"Planifiée", "En cours", "Terminée"};

        // Trouver l'index actuel pour le pré-cocher
        int checkedItem = 0;
        if ("En cours".equals(currentIntervention.statut)) checkedItem = 1;
        else if ("Terminée".equals(currentIntervention.statut)) checkedItem = 2;

        // Valeur par défaut si on ne change rien
        tempSelectedStatus = options[checkedItem];

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Modifier le statut");

        builder.setSingleChoiceItems(options, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // On enregistre temporairement le choix
                tempSelectedStatus = options[which];
            }
        });

        builder.setPositiveButton("Modifier", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // On applique le changement à l'objet réel
                currentIntervention.statut = tempSelectedStatus;

                // On met à jour l'affichage de la page détail
                refreshDisplay();

                Toast.makeText(DetailActivity.this, "Statut modifié !", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Annuler", null);

        builder.show();
    }
}