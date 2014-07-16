package com.seniortec.sorteio.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SorteioDiario {

	private Participante[] participantes = new Participante[0];
	private String[] brindes = new String[0];
	private Sorteado[] sorteados = new Sorteado[0];

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

	public Sorteado[] getSorteados() {
		return sorteados;
	}

	public void setSorteados(Sorteado[] sorteados) {
		this.sorteados = sorteados;
	}

	public boolean contains(Sorteado value) {
		for (int i = 0; i < sorteados.length; i++) {
			if(sorteados[i].getUsername().equals(value.getUsername())) {
				return true;
			}
		}
		return false;
	}

	public void addSorteado(Sorteado value) {
		List<Sorteado> parts = new ArrayList<>(Arrays.asList(sorteados));
		parts.add(value);
		sorteados = parts.toArray(new Sorteado[participantes.length]);
	}
}
