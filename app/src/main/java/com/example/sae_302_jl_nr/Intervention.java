package com.example.sae_302_jl_nr;

import java.io.Serializable;
import java.time.LocalDate;

public class Intervention implements Serializable {
    public String idMission, dateTexte, type, priority, technician, address, city, actions, time, material, status;
    public LocalDate dateDoc; // Utilisé pour le filtrage par calendrier

    public Intervention(String idMission, String dateTexte, LocalDate dateDoc, String type, String priority,
                        String technician, String address, String city, String actions, String time,
                        String material, String status) {
        this.idMission = idMission;
        this.dateTexte = dateTexte;
        this.dateDoc = dateDoc;
        this.type = type;
        this.priority = priority;
        this.technician = technician;
        this.address = address;
        this.city = city;
        this.actions = actions;
        this.time = time;
        this.material = material;
        this.status = status;
    }
}