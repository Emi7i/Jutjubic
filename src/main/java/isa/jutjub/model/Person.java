package isa.jutjub.model;

import isa.jutjub.validation.CustomConstraint;

import java.util.Date;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

public class Person {
    private int id;

    @NotNull
    @Setter
    @Getter
    private String name;

    @NotEmpty
    @Setter
    @Getter
    private String surname;

    @Email(regexp = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")
    @Setter
    @Getter
    private String email;

    @CustomConstraint(message = "JMBG mora da sadrzi tacno 13 cifara")
    @Setter
    @Getter
    private String jmbg;

    @Min(value = 18)
    @Setter
    @Getter
    private int age;

    @Past     // proverava da li je datum u proslosti
    private Date dateOfBirth;

    public Person(int id, String name, String surname, String email, String jmbg, int age, Date dateOfBirth) {
        this.id = id;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.jmbg = jmbg;
        this.age = age;
        this.dateOfBirth = dateOfBirth;
    }
}
