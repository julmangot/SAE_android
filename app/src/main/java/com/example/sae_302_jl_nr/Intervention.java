package com.example.sae_302_jl_nr;

import java.time.LocalDate;

public class Intervention {

    // Identifiant DB
    public String reference;   // ex: "REF123"
    // Alias (si dans ton projet tu utilisais déjà idMission)
    public String idMission;

    // Affichage
    public String libelleCourt;        // ex: "22 janvier"
    public LocalDate dateIntervention; // vraie date

    public String type;
    public String prioriteStr; // "Basse" / "Moyenne" / "Haute"
    public int priorite;       // 1/2/3 pour couleur rapide

    public String technicien;
    public String adresse;
    public String ville;
    public String action;
    public String duree;
    public String materiel;

    public String statut;      // "Planifié" / "En cours" / "Terminé"

    public Intervention(
            String reference,
            String libelleCourt,
            LocalDate dateIntervention,
            String type,
            String prioriteStr,
            String technicien,
            String adresse,
            String ville,
            String action,
            String duree,
            String materiel,
            String statut
    ) {
        this.reference = reference;
        this.idMission = reference; // alias pour compat
        this.libelleCourt = libelleCourt;
        this.dateIntervention = dateIntervention;

        this.type = type;
        this.prioriteStr = prioriteStr;
        this.technicien = technicien;
        this.adresse = adresse;
        this.ville = ville;
        this.action = action;
        this.duree = duree;
        this.materiel = materiel;
        this.statut = statut;
    }
}
