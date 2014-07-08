package com.seniortec.sorteio.store;

import org.neo4j.graphdb.RelationshipType;

public enum Relacoes implements RelationshipType {

	POSSUI_FOTO, SORTEADO_EM, BRINDE_DO_DIA, PARTICIPANTE_DO_DIA, PARTICIPOU_EM, FOTO, FOTO_DE;
}
