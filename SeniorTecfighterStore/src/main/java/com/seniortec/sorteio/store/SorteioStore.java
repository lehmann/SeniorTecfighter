package com.seniortec.sorteio.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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

	private final class ParticipantePalestraGetter implements
			Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> arg0) {
			loadData();
			JsonObject ret = new JsonObject();
			if (arg0.body().getString("time").equals("now")) {
				String[] readDirSync = new File("./data/").list();
				String dia = readDirSync[0].substring(
						"participantes-".length(),
						readDirSync[0].indexOf(".txt"));
				SorteioDiario diario = getDataFile(dia);
				Participante[] participantesArray = diario.getParticipantes();
				List<Map<String, Object>> participantes = new ArrayList<>();
				for (int i = 0; i < participantesArray.length; i++) {
					Map<String, Object> map = new HashMap<>();
					map.put("Username", participantesArray[i].getUsername());
					map.put("Nome", participantesArray[i].getNome());
					participantes.add(map);
				}
				ret = ret.putString("status", "ok");
				arg0.reply(ret.putArray("participantes", new JsonArray(
						participantes.toArray(new Map[participantes.size()]))));
				return;
			}
			arg0.reply(ret.putString("status", "ok").putString("content",
					"Resposta"));

		}
	}

	private final class SorteioHandler implements Handler<Message<JsonObject>> {

		@Override
		public void handle(Message<JsonObject> arg0) {
			loadData();
			JsonObject ret = new JsonObject();
			if (arg0.body().getString("time").equals("now")) {
				String[] readDirSync = new File("./data/").list();
				String dia = readDirSync[0].substring(
						"participantes-".length(),
						readDirSync[0].indexOf(".txt"));
				System.out.println("Dia: " + dia);
				SorteioDiario diario = getDataFile(dia);
				SorteioDiario sortudos = getSortudosFile();
				List<String> brindes = new ArrayList<String>(Arrays.asList(diario
						.getBrindes()));
				if(brindes.isEmpty()) {
					ret = ret.putString("status", "not ok").putString("content", "Acabaram os brindes por hoje :(");
					arg0.reply(ret);
					return;
				}

				ret = ret.putString("status", "ok");
				List<Participante> participantes = Arrays.asList(diario
						.getParticipantes());
				int sortudosSize = participantes.size();
				Random random = new Random(System.nanoTime());
				int theBiggestSortudo = random.nextInt(sortudosSize);
				Participante value = participantes.get(theBiggestSortudo);
				Sorteado choosen = new Sorteado();
				choosen.setUsername(value.getUsername());
				choosen.setNome(value.getNome());
				for (int i = 0; sortudos.contains(choosen)
						&& i < diario.getParticipantes().length; i++) {
					theBiggestSortudo = random.nextInt(sortudosSize);
					value = participantes.get(theBiggestSortudo);
					choosen.setUsername(value.getUsername());
					choosen.setNome(value.getNome());
				}
				if (!sortudos.contains(choosen)) {
					choosen.setPalestra(dia);
					choosen.setBrinde(brindes.remove(0));
					diario.setBrindes(brindes.toArray(new String[brindes.size()]));
					sortudos.addSorteado(choosen);
					storeDataFile(dia, diario);
					storeSortudosFile(sortudos);
					arg0.reply(ret.putObject("sortudo", new JsonObject()
							.putString("Username", value.getUsername())
							.putString("Nome", value.getNome())));
				} else {
					arg0.reply(new JsonObject().putString("status", "not ok").putString("content", "Muito pé de coelho concentrado. TODO mundo já ganhou!"));
				}
				return;
			}
			arg0.reply(ret.putString("content", "Resposta"));
		}

	}

	private static final String DB_PATH = "./SeniorTec.db";

	@Override
	public void start() {
		EventBus bus = vertx.eventBus();
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

	public void storeDataFile(String dia, SorteioDiario diario) {
		String fileName = "participantes-" + dia + ".txt";

		File dataFile = new File(DB_PATH, fileName);
		String content = Json.encode(diario);
		try (FileOutputStream fos = new FileOutputStream(dataFile)) {
			fos.write(content.getBytes());
			fos.flush();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void storeSortudosFile(SorteioDiario sortudos) {
		String fileName = "Sortudos.db";

		File dataFile = new File(DB_PATH, fileName);
		String content = Json.encode(sortudos);
		try (FileOutputStream fos = new FileOutputStream(dataFile)) {
			fos.write(content.getBytes());
			fos.flush();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public SorteioDiario getDataFile(String dia) {
		String fileName = "participantes-" + dia + ".txt";
		try {
			byte[] readAllBytes = Files.readAllBytes(Paths.get(DB_PATH,
					fileName));
			return Json.decodeValue(new String(readAllBytes),
					SorteioDiario.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public SorteioDiario getSortudosFile() {
		String fileName = "Sortudos.db";
		try {
			byte[] readAllBytes = Files.readAllBytes(Paths.get(DB_PATH,
					fileName));
			return Json.decodeValue(new String(readAllBytes),
					SorteioDiario.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void loadData() {
		loadDatabase();

		String[] readDirSync = new File("./data/").list();
		String fileName = readDirSync[0];
		File dbFolder = new File(DB_PATH, fileName.substring(fileName
				.indexOf("participantes-")));
		if (!dbFolder.exists()) {
			String content;
			try {
				content = new String(Files.readAllBytes(Paths.get("./data/"
						+ fileName)));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			try (FileOutputStream fos = new FileOutputStream(dbFolder)) {
				fos.write(content.getBytes());
				fos.flush();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void loadDatabase() {
		File dbFolder = new File(DB_PATH);
		if (!dbFolder.exists()) {
			dbFolder.mkdir();
		}
		File sortudosFile = new File(DB_PATH, "Sortudos.db");
		if (!sortudosFile.exists()) {
			try {
				sortudosFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			SorteioDiario sd = new SorteioDiario();
			String content = Json.encode(sd);
			try (FileOutputStream fos = new FileOutputStream(sortudosFile)) {
				fos.write(content.getBytes());
				fos.flush();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
