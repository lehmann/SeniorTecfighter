package com.seniortec.sorteio.store;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.eventbus.impl.JsonObjectMessage;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.json.impl.Json;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.platform.Verticle;

public class SorteioStore extends Verticle {

	private final class ParticipantePalestraLoader implements Handler<Message<JsonObject>> {
		@Override
		public void handle(Message<JsonObject> arg0) {
			try {

				if (arg0.body().getString("time").equals("now")) {
					String now = new SimpleDateFormat().format(new Date());
					// se não houve a carga dos participantes do dia de hoje
					ResourceIterator<Node> iterator = graphDb
							.findNodesByLabelAndProperty(Papeis.DIA_SORTEIO, "Dia",
									now).iterator();
					if (!iterator.hasNext()) {
						// lê o arquivo e carrega
						Buffer fileContent = vertx.fileSystem().readFileSync(
								"./data/participantes.txt");
						SorteioDiario sorteio = Json.decodeValue(
								fileContent.toString(), SorteioDiario.class);

						Transaction tx = graphDb.beginTx();
						Node diaNode = graphDb.createNode(Papeis.DIA_SORTEIO);
						diaNode.setProperty("Dia", now);
						String[] brindes = sorteio.getBrindes();
						for (int i = 0; i < brindes.length; i++) {
							Node brindeNode = graphDb.createNode(Papeis.BRINDE);
							brindeNode.setProperty("Nome", brindes[i]);
							diaNode.createRelationshipTo(brindeNode,
									Relacoes.BRINDE_DO_DIA);
							brindeNode.createRelationshipTo(diaNode,
									Relacoes.SORTEADO_EM);
						}
						Participante[] participantes = sorteio.getParticipantes();
						for (int i = 0; i < participantes.length; i++) {
							ResourceIterator<Node> partIterator = graphDb
									.findNodesByLabelAndProperty(
											Papeis.PARTICIPANTE, "Username",
											participantes[i].getUsername())
									.iterator();
							Node participanteNode;
							if (!partIterator.hasNext()) {
								participanteNode = graphDb
										.createNode(Papeis.PARTICIPANTE);
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
							ResourceIterator<Node> fotosIterator = graphDb
									.findNodesByLabelAndProperty(Papeis.FOTO,
											"Username",
											participantes[i].getUsername())
									.iterator();
							if (!fotosIterator.hasNext()) {
								vertx.eventBus().publish("load-foto",
										participantes[i].getUsername());
							}
						}
						tx.success();
					}
					arg0.reply(new JsonObject().putString("status", "ok").putString("content", "Conteúdo carregado com sucesso"));
				}
			} catch (Exception e) {
				arg0.reply(new JsonObject().putString("status", "not ok").putString("content", "Erro ao carregar arquivo").putString("error", e.getMessage()));
			}
		}
	}

	private final class ParticipantePalestraGetter implements Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> arg0) {
			if (arg0.body().getString("time").equals("now")) {
				String now = new SimpleDateFormat().format(new Date());
				Transaction tx = graphDb.beginTx();
				ResourceIterator<Node> iterator = graphDb
						.findNodesByLabelAndProperty(Papeis.DIA_SORTEIO, "Dia",
								now).iterator();
				tx.failure();
			}
			arg0.reply(new JsonObject().putString("status", "ok").putString("content", "Resposta"));
		}

	}

	private final class SorteioHandler implements Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> arg0) {
			if (arg0.body().getString("time").equals("now")) {
				String now = new SimpleDateFormat().format(new Date());
				Transaction tx = graphDb.beginTx();
				ResourceIterator<Node> iterator = graphDb
						.findNodesByLabelAndProperty(Papeis.DIA_SORTEIO, "Dia",
								now).iterator();
				tx.failure();
			}
			arg0.reply(new JsonObject().putString("status", "ok").putString("content", "Resposta"));
		}

	}

	private GraphDatabaseService graphDb;
	private static final String DB_PATH = "./SeniorTec.db";

	@Override
	public void start() {
		graphDb = new GraphDatabaseFactory()
				.newEmbeddedDatabaseBuilder(DB_PATH)
				.loadPropertiesFromURL(this.getClass().getClassLoader().getResource("./config/configuration.db"))
				.newGraphDatabase();
		registerShutdownHook(graphDb);
		EventBus bus = vertx.eventBus();
		bus.registerHandler("load-participantes-palestra", new ParticipantePalestraLoader());
		bus.registerHandler("get-participantes-palestra", new ParticipantePalestraGetter());
		bus.registerHandler("sorteio", new SorteioHandler());
		HttpServer server = vertx.createHttpServer();
		SockJSServer sockJSServer = vertx.createSockJSServer(server);
		JsonObject config = new JsonObject();
		config.putString("prefix", "/eventbus");
		JsonArray permitted = new JsonArray();
		permitted.add(new JsonObject());
		
		sockJSServer.bridge(config, permitted, permitted );
		
		server.listen(8081);
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
