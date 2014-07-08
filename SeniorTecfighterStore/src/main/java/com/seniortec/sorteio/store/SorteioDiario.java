package com.seniortec.sorteio.store;

public class SorteioDiario {

	private Participante[] participantes;
	private String[] brindes;

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
}
