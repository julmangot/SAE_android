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

public class InterventionAdapter extends RecyclerView.Adapter<InterventionAdapter.VH> {

    public interface OnInterventionClickListener {
        void onClick(Intervention intervention);
    }

    private OnInterventionClickListener listener;
    private List<Intervention> data = new ArrayList<>();

    public void setOnInterventionClickListener(OnInterventionClickListener l) {
        this.listener = l;
    }

    public void setData(List<Intervention> newData) {
        data = (newData == null) ? new ArrayList<>() : newData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_intervention, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Intervention i = data.get(position);

        // Priorité DB (Basse/Moyenne/Haute/Critique...)
        String pr = (i.prioriteStr == null) ? "" : i.prioriteStr.trim();
        String prLower = pr.toLowerCase(Locale.ROOT);

        int color = Color.parseColor("#4CAF50"); // basse
        if (prLower.contains("critique")) color = Color.parseColor("#D32F2F");
        else if (prLower.contains("haute") || prLower.contains("haut")) color = Color.parseColor("#F05A5A");
        else if (prLower.contains("moy")) color = Color.parseColor("#F5A623");

        h.vPriority.setBackgroundColor(color);

        // Ligne 1 : "Type | Technicien"
        String type = (i.type == null) ? "" : i.type;
        String tech = (i.technicien == null) ? "" : i.technicien;
        String line1 = type;
        if (!tech.isEmpty()) line1 += " | " + tech;
        h.tvLine1.setText(line1.trim());

        // Ligne 2 : "Statut | Ville"
        String statut = (i.statut == null) ? "" : i.statut;
        String ville = (i.ville == null) ? "" : i.ville;
        String line2 = statut;
        if (!ville.isEmpty()) line2 += " | " + ville;
        h.tvLine2.setText(line2.trim());

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(i);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View vPriority;
        TextView tvLine1, tvLine2;

        VH(@NonNull View itemView) {
            super(itemView);
            vPriority = itemView.findViewById(R.id.vPriority);
            tvLine1 = itemView.findViewById(R.id.tvLine1);
            tvLine2 = itemView.findViewById(R.id.tvLine2);
        }
    }
}
