package com.innovax.barberia.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;

/** Guarda DayOfWeek como entero: 1 = lunes ... 7 = domingo. */
@Converter
public class DiaSemanaConverter implements AttributeConverter<DayOfWeek, Integer> {

    @Override
    public Integer convertToDatabaseColumn(DayOfWeek dia) {
        return dia == null ? null : dia.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Integer valor) {
        return valor == null ? null : DayOfWeek.of(valor);
    }
}
