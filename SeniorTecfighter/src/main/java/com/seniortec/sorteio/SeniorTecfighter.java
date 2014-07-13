package com.seniortec.sorteio;

import org.vertx.java.core.Handler;
import org.vertx.java.core.file.FileSystem;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.platform.Verticle;

public class SeniorTecfighter extends Verticle {

	@Override
	public void start() {
		this.vertx.eventBus().publish("load-participantes-palestra", "now");
		HttpServer server = this.vertx.createHttpServer();
		server.requestHandler(new Handler<HttpServerRequest>() {

			@Override
			public void handle(HttpServerRequest event) {
				final StringBuilder response = new StringBuilder();
				switch (event.path()) {
				default:
					if (event.path().equals("/")) {
						event.response().sendFile("html/Seniortec.html");
						return;
					} else if (event.path().equals("/teste")) {
						event.response().sendFile("html/index.html");
						return;
					} else {
						FileSystem fs = vertx.fileSystem();
						if (fs.existsSync("html" + event.path())) {
							event.response().sendFile("html" + event.path());
							return;
						} else {
							response.append("Arquivo não encontrado");
						}
					}
					break;
				}
				String responseString = response.toString();
				HttpServerResponse httpResponse = event.response();
				httpResponse.putHeader("content-type", "text/plain");
				httpResponse.end(responseString);
			}
		});
		server.listen(8080);
	}

	@Override
	public void stop() {
		super.stop();
	}
}
