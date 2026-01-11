package isa.jutjub.controller;

import isa.jutjub.model.Person;
import isa.jutjub.service.PersonService;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/person")
public class PersonController {

    private PersonService personService;

    @Autowired
    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @RequestMapping(method= RequestMethod.POST)
    public ResponseEntity<String> createPerson(@Valid @RequestBody Person person) throws ConstraintViolationException {
        this.personService.createPerson(person);
        ResponseEntity<String> stringResponseEntity = new ResponseEntity<String>(HttpStatus.OK);
        return stringResponseEntity;
    }
}
