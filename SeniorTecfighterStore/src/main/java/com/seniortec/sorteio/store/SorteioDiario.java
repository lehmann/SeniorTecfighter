package com.seniortec.sorteio.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SorteioDiario {

	private Participante[] participantes = new Participante[0];
	private String[] brindes = new String[0];

	public String[] getBrindes() {
		return brindes;
	}

	public void setBrindes(String[] brindes) {
		this.brindes = brindes;
	}

	public Participante[] getParticipantes() {
		return participantes;
	}

	public void setParticipantes(Participante[] participantes) {
		this.participantes = participantes;
	}

	public boolean contains(Participante value) {
		for (int i = 0; i < participantes.length; i++) {
			if(participantes[i].getUsername().equals(value.getUsername())) {
				return true;
			}
		}
		return false;
	}

	public void addParticipantes(Participante value) {
		List<Participante> parts = new ArrayList<>(Arrays.asList(participantes));
		parts.add(value);
		participantes = parts.toArray(new Participante[participantes.length]);
	}
}
