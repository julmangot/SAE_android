package com.example.sae_302_jl_nr;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class DataRepository {

    // Liste statique accessible partout
    private static List<Intervention> allInterventions = new ArrayList<>();

    // Initialisation des données (une seule fois)
    static {
        // Semaine du 19 au 25 Janvier
        allInterventions.add(new Intervention("15480", "19 Janvier",
                LocalDate.of(2026, 1, 19),
                "SAV Fibre", "Critique", "Medhi Ralouf",
                "10 rue Paris", "Rennes", "Soudure", "1h", "Soudeuse", "Planifiée"));

        allInterventions.add(new Intervention("15481", "20 Janvier",
                LocalDate.of(2026, 1, 20),
                "Installation", "Basse", "Julie Bois",
                "2 av. Briand", "Rennes", "Pose Box", "45min", "Modem", "Terminée"));

        // JOUR TEST : 21 Janvier
        allInterventions.add(new Intervention("15485", "21 Janvier",
                LocalDate.of(2026, 1, 21),
                "SAV Problème fibre", "Critique Haute", "Medhi Ralouf",
                "10 rue de Paris", "Rennes", "Test de continuité", "1h30", "Soudeuse optique", "Planifiée"));

        allInterventions.add(new Intervention("15486", "21 Janvier",
                LocalDate.of(2026, 1, 21),
                "Raccordement", "Moyenne", "Martin Delavega",
                "Zone Sud", "Rennes", "Tirage", "2h", "Echelle", "En cours"));

        allInterventions.add(new Intervention("15487", "21 Janvier",
                LocalDate.of(2026, 1, 21),
                "SAV Fibre", "Haute", "Julie Bois",
                "Rue de Fougères", "Rennes", "Soudure", "1h", "Soudeuse", "Planifiée"));

        // Autres jours
        allInterventions.add(new Intervention("15490", "22 Janvier",
                LocalDate.of(2026, 1, 22),
                "Audit", "Basse", "Thomas Le Gall",
                "Mairie", "Dinan", "Mesures", "3h", "Tablette", "Planifiée"));
    }

    public static List<Intervention> getAllInterventions() {
        return allInterventions;
    }

    // Permet de trouver la vraie intervention grâce à son ID
    public static Intervention getInterventionById(String id) {
        for (Intervention i : allInterventions) {
            if (i.idMission.equals(id)) {
                return i;
            }
        }
        return null;
    }
}