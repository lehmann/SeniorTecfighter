package com.seniortec.sorteio.store;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.platform.Verticle;

public class SorteioStore extends Verticle {

	private final class ParticipantePalestraLoader implements
			Handler<Message<JsonObject>> {
		@Override
		public void handle(Message<JsonObject> arg0) {
			try {
				loadGraphDb();
				JsonObject ret = new JsonObject();
				try {

					if (arg0.body().getString("time").equals("now")) {
						carregaParticipantes();
						arg0.reply(ret.putString("status", "ok").putString(
								"content", "Conteúdo carregado com sucesso"));
					}
				} catch (Exception e) {
					arg0.reply(ret.putString("status", "not ok")
							.putString("content", "Erro ao carregar arquivo")
							.putString("error", e.getMessage()));
				}
			} finally {
				graphDb.shutdown();
			}
		}
	}

	private final class ParticipantePalestraGetter implements
			Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> arg0) {
			try {
				loadGraphDb();
				JsonObject ret = new JsonObject();
				if (arg0.body().getString("time").equals("now")) {
					Transaction ignored = graphDb.beginTx();
					try {
						String[] readDirSync = new File("./data/").list();
						String dia = readDirSync[0].substring(
								"participantes-".length(),
								readDirSync[0].indexOf(".txt"));
						ResourceIterator<Node> iterator = graphDb
								.findNodesByLabelAndProperty(Papeis.PALESTRA,
										"Dia", dia).iterator();
						try {
							if (!iterator.hasNext()) {
								SorteioStore.this.carregaParticipantes();
							}
						} finally {
							iterator.close();
						}
						ExecutionResult result = engine
								.execute("MATCH (n:PARTICIPANTE:NAO_SORTEADO) return n.Username AS Username, n.Nome AS Nome");
						ResourceIterator<Map<String, Object>> iteratorParticipantes = result
								.iterator();
						try {
							if (!iteratorParticipantes.hasNext()) {
								arg0.reply(ret
										.putString("status", "not ok")
										.putString("error",
												"Muita gente sortuda por aqui. TODO mundo já ganhou!"));
								return;
							}
							ret = ret.putString("status", "ok");
							List<Map<String, Object>> participantes = new ArrayList<>();
							while (iteratorParticipantes.hasNext()) {
								Map<String, Object> map = (Map<String, Object>) iteratorParticipantes
										.next();
								participantes.add(map);
							}
							arg0.reply(ret.putArray(
									"participantes",
									new JsonArray(participantes
											.toArray(new Map[participantes
													.size()]))));
							return;
						} finally {
							iteratorParticipantes.close();
						}
					} finally {
						ignored.success();
					}
				}
				arg0.reply(ret.putString("status", "ok").putString("content",
						"Resposta"));

			} finally {
				graphDb.shutdown();
			}
		}
	}

	private void carregaParticipantes() {
		// se não houve a carga dos participantes do dia de hoje
		Transaction tx = graphDb.beginTx();
		String[] readDirSync = new File("./data/").list();
		String fileName = readDirSync[0];
		String dia = fileName.substring(fileName.indexOf("participantes-")
				+ "participantes-".length(), fileName.indexOf(".txt"));
		ResourceIterator<Node> iterator = graphDb.findNodesByLabelAndProperty(
				Papeis.PALESTRA, "Dia", dia).iterator();
		if (!iterator.hasNext()) {
			try {
				System.out.println("Carregando participantes do dia: " + dia);
				// lê o arquivo e carrega
				String content;
				try {
					content = new String(Files.readAllBytes(Paths.get("./data/"
							+ fileName)));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				SorteioDiario sorteio = Json.decodeValue(content.toString(),
						SorteioDiario.class);

				Node diaNode = graphDb.createNode(Papeis.PALESTRA);
				diaNode.setProperty("Dia", dia);
				Participante[] participantes = sorteio.getParticipantes();
				for (int i = 0; i < participantes.length; i++) {
					ResourceIterator<Node> partIterator = graphDb
							.findNodesByLabelAndProperty(Papeis.PARTICIPANTE,
									"Username", participantes[i].getUsername())
							.iterator();
					try {
						Node participanteNode;
						if (!partIterator.hasNext()) {
							participanteNode = graphDb.createNode(
									Papeis.PARTICIPANTE, Papeis.NAO_SORTEADO);
							participanteNode.setProperty("Nome",
									participantes[i].getNome());
							participanteNode.setProperty("Username",
									participantes[i].getUsername());
						} else {
							participanteNode = partIterator.next();
						}
						diaNode.createRelationshipTo(participanteNode,
								Relacoes.PARTICIPANTE_DO_DIA);
						participanteNode.createRelationshipTo(diaNode,
								Relacoes.PARTICIPOU_EM);
					} finally {
						partIterator.close();
					}
				}
				tx.success();
			} catch (Exception e) {
				tx.failure();
			}
		} else {
			tx.failure();
		}
	}

	private final class SorteioHandler implements Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> arg0) {
			try {
				loadGraphDb();
				JsonObject ret = new JsonObject();
				if (arg0.body().getString("time").equals("now")) {
					try (Transaction tx = graphDb.beginTx()) {
						String[] readDirSync = new File("./data/").list();
						String dia = readDirSync[0].substring(
								"participantes-".length(),
								readDirSync[0].indexOf(".txt"));
						System.out.println("Dia: " + dia);
						ResourceIterator<Node> iteratorSorteioDoDia = graphDb
								.findNodesByLabelAndProperty(Papeis.PALESTRA,
										"Dia", dia).iterator();
						try {
							if (!iteratorSorteioDoDia.hasNext()) {
								System.out
										.println("Carregar participantes do dia de hoje");
								SorteioStore.this.carregaParticipantes();
							}
						} finally {
							iteratorSorteioDoDia.close();
						}
						tx.success();
					}
					try (Transaction ignored = graphDb.beginTx()) {
						ExecutionResult result = engine
								.execute("MATCH (n:PARTICIPANTE:NAO_SORTEADO) return n.Username AS Username, n.Nome AS Nome");
						ResourceIterator<Map<String, Object>> iterator = result
								.iterator();
						if (!iterator.hasNext()) {
							arg0.reply(ret
									.putString("status", "not ok")
									.putString("error",
											"Muita gente sortuda por aqui. TODO mundo já ganhou!"));
							return;
						}
						ret = ret.putString("status", "ok");
						List<JsonObject> sortudos = new ArrayList<>();
						while (iterator.hasNext()) {
							Map<java.lang.String, java.lang.Object> map = (Map<java.lang.String, java.lang.Object>) iterator
									.next();
							sortudos.add(new JsonObject(map));
						}
						int sortudosSize = sortudos.size();
						int theBiggestSortudo = new Random(System.nanoTime())
								.nextInt(sortudosSize);
						JsonObject value = sortudos.get(theBiggestSortudo);
						HashMap<String, Object> params = new HashMap<>();
						params.put("user", value.getString("Username"));
						result = engine
								.execute(
										"MATCH (n:PARTICIPANTE { Username: {user} }) remove n :NAO_SORTEADO return n",
										params);
						arg0.reply(ret.putObject("sortudo", value));
						ignored.success();
						return;
					}
				}
				arg0.reply(ret.putString("status", "ok").putString("content",
						"Resposta"));
			} finally {
				graphDb.shutdown();
			}
		}

	}

	private GraphDatabaseService graphDb;
	private ExecutionEngine engine;
	private static final String DB_PATH = "./SeniorTec.db";

	@Override
	public void start() {
		EventBus bus = vertx.eventBus();
		bus.registerHandler("load-participantes-palestra",
				new ParticipantePalestraLoader());
		bus.registerHandler("get-participantes-palestra",
				new ParticipantePalestraGetter());
		bus.registerHandler("sorteio", new SorteioHandler());
		HttpServer server = vertx.createHttpServer();
		SockJSServer sockJSServer = vertx.createSockJSServer(server);
		JsonObject config = new JsonObject();
		config.putString("prefix", "/eventbus");
		JsonArray permitted = new JsonArray();
		permitted.add(new JsonObject());

		sockJSServer.bridge(config, permitted, permitted);

		server.listen(8081);
	}

	private void loadGraphDb() {
		graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(DB_PATH)
				.loadPropertiesFromURL(
						this.getClass().getClassLoader()
								.getResource("./config/configuration.db"))
				.newGraphDatabase();
		engine = new ExecutionEngine(graphDb);
	}

	@Override
	public void stop() {
		super.stop();
	}

	private static void registerShutdownHook(final GraphDatabaseService graphDb) {
		// Registers a shutdown hook for the Neo4j instance so that it
		// shuts down nicely when the VM exits (even if you "Ctrl-C" the
		// running application).
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				graphDb.shutdown();
			}
		});
	}
}
