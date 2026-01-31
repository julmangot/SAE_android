package com.example.sae_302_jl_nr;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * InterventionAdapter
 * -------------------
 * Adaptateur RecyclerView qui affiche une liste d'interventions.
 *
 * Rôle :
 * - Créer les lignes (ViewHolder) à partir du layout item_intervention.xml
 * - Remplir chaque ligne avec les données (type, technicien, statut, ville)
 * - Afficher une barre de couleur selon la priorité
 * - Remonter le clic sur une ligne vers l'Activity (via un listener)
 */
public class InterventionAdapter extends RecyclerView.Adapter<InterventionAdapter.VH> {

    /**
     * Listener de clic : l'Activity l'implémente pour savoir quel item a été cliqué.
     */
    public interface OnInterventionClickListener {
        void onClick(Intervention intervention);
    }

    // Stocke le listener fourni par l'Activity (peut être null si non défini)
    private OnInterventionClickListener listener;

    // Liste de données affichées par le RecyclerView
    private List<Intervention> data = new ArrayList<>();

    /**
     * Permet à l'Activity de définir quoi faire lors d'un clic sur un item.
     */
    public void setOnInterventionClickListener(OnInterventionClickListener l) {
        this.listener = l;
    }

    /**
     * Remplace la liste affichée puis force le RecyclerView à se rafraîchir.
     * (si newData == null, on met une liste vide pour éviter NullPointerException)
     */
    public void setData(List<Intervention> newData) {
        data = (newData == null) ? new ArrayList<>() : newData;
        notifyDataSetChanged(); // indique au RecyclerView : "tout a changé, redraw"
    }

    /**
     * Création d'une "ligne" (ViewHolder) :
     * Android inflate le XML item_intervention.xml.
     * Appelé quand RecyclerView a besoin de nouvelles vues.
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_intervention, parent, false);
        return new VH(v);
    }

    /**
     * Remplissage d'une ligne avec les données de l'intervention.
     * Appelé à chaque fois qu'un item doit être affiché à l'écran.
     */
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Intervention i = data.get(position);

        // ===== 1) Couleur selon la priorité =====
        // Priorité DB (ex: "Basse", "Moyenne", "Haute", "Critique"...)
        String pr = (i.prioriteStr == null) ? "" : i.prioriteStr.trim();
        String prLower = pr.toLowerCase(Locale.ROOT);

        // Couleur par défaut : basse (vert)
        int color = Color.parseColor("#4CAF50");
        if (prLower.contains("critique")) color = Color.parseColor("#D32F2F");          // rouge
        else if (prLower.contains("haute") || prLower.contains("haut")) color = Color.parseColor("#F05A5A"); // rouge clair
        else if (prLower.contains("moy")) color = Color.parseColor("#F5A623");         // orange

        // Applique la couleur sur la petite barre (vPriority) dans item_intervention.xml
        h.vPriority.setBackgroundColor(color);

        // ===== 2) Texte ligne 1 : "Type | Technicien" =====
        String type = (i.type == null) ? "" : i.type;
        String tech = (i.technicien == null) ? "" : i.technicien;

        // Construit un texte propre (on ajoute " | " seulement si tech existe)
        String line1 = type;
        if (!tech.isEmpty()) line1 += " | " + tech;
        h.tvLine1.setText(line1.trim());

        // ===== 3) Texte ligne 2 : "Statut | Ville" =====
        String statut = (i.statut == null) ? "" : i.statut;
        String ville = (i.ville == null) ? "" : i.ville;

        String line2 = statut;
        if (!ville.isEmpty()) line2 += " | " + ville;
        h.tvLine2.setText(line2.trim());

        // ===== 4) Clic sur la ligne =====
        // Quand l'utilisateur clique, on appelle le listener défini par l'Activity
        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(i);
        });
    }

    /**
     * Retourne le nombre d'items à afficher.
     */
    @Override
    public int getItemCount() {
        return data.size();
    }

    /**
     * ViewHolder = "cache" des vues d'une ligne.
     * Avantage : findViewById est fait une seule fois par ligne créée (plus rapide).
     */
    static class VH extends RecyclerView.ViewHolder {
        View vPriority;       // barre couleur priorité
        TextView tvLine1;     // "Type | Technicien"
        TextView tvLine2;     // "Statut | Ville"

        VH(@NonNull View itemView) {
            super(itemView);

            // Bind des vues du layout item_intervention.xml
            vPriority = itemView.findViewById(R.id.vPriority);
            tvLine1 = itemView.findViewById(R.id.tvLine1);
            tvLine2 = itemView.findViewById(R.id.tvLine2);
        }
    }
}
