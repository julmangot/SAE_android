package com.example.sae_302_jl_nr;

import java.time.LocalDate;

public class Intervention {

    public String titre;
    public String sousTitre;
    public LocalDate date;

    public Intervention(String titre, String sousTitre, LocalDate date) {
        this.titre = titre;
        this.sousTitre = sousTitre;
        this.date = date;
    }
}
