package com.backendcr.residentialcomplex.entity;

import javax.persistence.*;

@Entity
@Table(name = "common_areas")
public class CommonArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;
    private boolean isAvailable;
    // Additional fields and methods
}