package com.example.sae_302_jl_nr;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class InterventionAdapter extends RecyclerView.Adapter<InterventionAdapter.ViewHolder> {

    private List<Intervention> data = new ArrayList<>();

    public void setData(List<Intervention> newData) {
        this.data = newData != null ? newData : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_intervention, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Intervention i = data.get(position);

        // Affichage dynamique
        holder.tvLine1.setText(i.getTitreCarte());
        holder.tvLine2.setText(i.getSousTitreCarte());

        // Couleur priorité
        switch (i.priorite) {
            case 3:
                holder.vPriority.setBackgroundColor(Color.parseColor("#F05A5A"));
                break;
            case 2:
                holder.vPriority.setBackgroundColor(Color.parseColor("#F5A623"));
                break;
            default:
                holder.vPriority.setBackgroundColor(Color.parseColor("#4CAF50"));
                break;
        }

        // Clic : on envoie juste l'ID pour récupérer le vrai objet plus tard
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DetailActivity.class);
            intent.putExtra(DetailActivity.EXTRA_ID_MISSION, i.idMission);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLine1, tvLine2;
        View vPriority;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLine1 = itemView.findViewById(R.id.tvLine1);
            tvLine2 = itemView.findViewById(R.id.tvLine2);
            vPriority = itemView.findViewById(R.id.vPriority);
        }
    }
}