package com.innovax.barberia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class BarberiaApplication {

    public static void main(String[] args) {
        // Toda la app trabaja en UTC; la zona de cada barbería se aplica al mostrar
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(BarberiaApplication.class, args);
    }
}