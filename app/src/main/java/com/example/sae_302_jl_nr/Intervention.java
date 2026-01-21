package com.example.sae_302_jl_nr;

import java.time.LocalDate;

public class Intervention {

    public String idMission;
    public String libelleCourt;     // ex: "19 Janvier" (si tu veux un libellé date court)
    public LocalDate date;

    public String typeMission;      // ex: "SAV Fibre"
    public String prioriteTexte;    // ex: "Critique", "Moyenne", "Basse"
    public int priorite;            // 1=vert, 2=orange, 3=rouge

    public String technicien;
    public String adresse;
    public String ville;

    public String action;
    public String tempsEstime;
    public String materiel;
    public String statut;           // ex: "Planifiée"

    public Intervention(
            String idMission,
            String libelleCourt,
            LocalDate date,
            String typeMission,
            String prioriteTexte,
            String technicien,
            String adresse,
            String ville,
            String action,
            String tempsEstime,
            String materiel,
            String statut
    ) {
        this.idMission = idMission;
        this.libelleCourt = libelleCourt;
        this.date = date;

        this.typeMission = typeMission;
        this.prioriteTexte = prioriteTexte;
        this.priorite = mapPriorite(prioriteTexte);

        this.technicien = technicien;
        this.adresse = adresse;
        this.ville = ville;

        this.action = action;
        this.tempsEstime = tempsEstime;
        this.materiel = materiel;
        this.statut = statut;
    }

    // Convertit "Critique" -> 3 (rouge), "Moyenne" -> 2 (orange), sinon 1 (vert)
    private int mapPriorite(String txt) {
        if (txt == null) return 1;
        String p = txt.toLowerCase();
        if (p.contains("crit")) return 3;
        if (p.contains("haut")) return 3;
        if (p.contains("moy")) return 2;
        return 1;
    }

    // Texte à afficher dans la carte (ligne 1)
    public String getTitreCarte() {
        return "SAV | " + (typeMission != null ? typeMission : "") + " | " + (technicien != null ? technicien : "");
    }

    // Texte à afficher dans la carte (ligne 2)
    public String getSousTitreCarte() {
        return (statut != null ? statut : "") + " | " + (ville != null ? ville : "");
    }
}
