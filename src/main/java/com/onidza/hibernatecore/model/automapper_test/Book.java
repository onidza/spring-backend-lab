package com.onidza.hibernatecore.model.automapper_test;

import jakarta.persistence.*;
import lombok.*;

@Generated
@Setter
@Getter
//@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "book")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "author", nullable = false)
    private String author;

    public Book(String name, String author) {
        this.name = name;
        this.author = author;
    }
}
