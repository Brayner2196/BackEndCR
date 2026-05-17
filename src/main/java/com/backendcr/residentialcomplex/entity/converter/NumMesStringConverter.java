package com.backendcr.residentialcomplex.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class NumMesStringConverter implements AttributeConverter<Integer, String> {

	private static final String[] MESES = {
			"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
			"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
	};

	@Override
	public String convertToDatabaseColumn(Integer numMes) {
		if (numMes == null || numMes < 1 || numMes > 12) {
			throw new IllegalArgumentException("Número de mes inválido: " + numMes);
		}
		return MESES[numMes - 1];
	}

	@Override
	public Integer convertToEntityAttribute(String mes) {
		for (int i = 0; i < MESES.length; i++) {
			if (MESES[i].equalsIgnoreCase(mes)) {
				return i + 1;
			}
		}
		throw new IllegalArgumentException("Nombre de mes inválido: " + mes);
	}

}
