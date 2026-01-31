package com.example.sae_302_jl_nr;

import java.time.LocalDate;

/**
 * Classe Intervention
 * -------------------
 * Modèle de données représentant une intervention.
 *
 * Rôle :
 * - Stocker toutes les informations d’une intervention
 * - Servir d’objet métier entre :
 *   → l’API (JSON)
 *   → l’UI (RecyclerView, DetailActivity, etc.)
 *
 * Cette classe ne contient PAS de logique :
 * elle sert uniquement à transporter des données.
 */
public class Intervention {

    // ===== Identifiants =====

    // Référence unique en base de données (clé principale côté API)
    public String reference;   // ex: "REF123"

    // Alias utilisé si une ancienne partie du code parle de "idMission"
    // (évite de casser le reste du projet)
    public String idMission;

    // ===== Données d’affichage =====

    // Texte court affiché dans la liste (ex: "22 janvier")
    public String libelleCourt;

    // Date réelle de l’intervention (utile pour tri, comparaison, formatage)
    public LocalDate dateIntervention;

    // ===== Informations métier =====

    // Type d’intervention (ex: maintenance, installation, dépannage)
    public String type;

    // Priorité sous forme texte (affichée à l’écran)
    public String prioriteStr; // "Basse", "Moyenne", "Haute", "Critique"

    // Priorité sous forme numérique (utile pour couleurs / tris rapides)
    // ex: 1 = basse, 2 = moyenne, 3 = haute, 4 = critique
    public int priorite;

    // Technicien assigné
    public String technicien;

    // Adresse (rue)
    public String adresse;

    // Ville
    public String ville;

    // Action réalisée ou prévue
    public String action;

    // Durée de l’intervention (ex: "1h30")
    public String duree;

    // Matériel utilisé
    public String materiel;

    // Statut actuel de l’intervention
    // ex: "Planifiée", "En cours", "Terminée"
    public String statut;

    /**
     * Constructeur principal
     *
     * Utilisé lors :
     * - du parsing JSON venant de l’API
     * - de la création d’objets Intervention pour l’affichage
     *
     * Tous les champs sont injectés ici pour avoir
     * un objet prêt à l’emploi.
     */
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
        // Identifiants
        this.reference = reference;
        this.idMission = reference; // même valeur, juste un autre nom

        // Affichage
        this.libelleCourt = libelleCourt;
        this.dateIntervention = dateIntervention;

        // Métier
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