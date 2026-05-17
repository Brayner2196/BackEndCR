package com.backendcr.residentialcomplex.entity.converter;


public class NumMesStringConverter {

	private static final String[] MESES = {
			"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
			"Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
	};

	public String convertToDatabaseColumn(Integer numMes) {
		if (numMes == null || numMes < 1 || numMes > 12) {
			throw new IllegalArgumentException("Número de mes inválido: " + numMes);
		}
		return MESES[numMes - 1];
	}

	public Integer convertToEntityAttribute(String mes) {
		for (int i = 0; i < MESES.length; i++) {
			if (MESES[i].equalsIgnoreCase(mes)) {
				return i + 1;
			}
		}
		throw new IllegalArgumentException("Nombre de mes inválido: " + mes);
	}

}
