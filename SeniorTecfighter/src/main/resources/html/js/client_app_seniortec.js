/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

	$(document).ready(function(){
		var eb = new vertx.EventBus('http://'
				+ window.location.hostname + ':8081' + '/eventbus');
		$('#coluna-sorteio').click(function() {
			// Aqui faz a firula de ficar mostrando várias fotos até chegar a hora de mostrar o sortudo, digo, sorteado.
			//setInterval(function() { console.log("setInterval: Ja passou 1 segundo!"); }, 1000);
			
			eb.send('get-participantes-palestra', {time: 'now'}, function(reply) {
				if(reply.status === 'ok') {
					var obj = JSON.parse(reply.participantes);
					var atual = 0;
					setInterval(function() {mudaFotos(atual, obj.length, "images/" + obj[atual].src + ".png")}, 1500);
			
			}});
			
			function mudaFotos(atualP, totalP, imagePath) {
				if(atualP < totalP) {
					$('#img-sorteio').attr("src", imagePath);
					atualP = atualP + 1;
				}
			}
			
			eb.send('sorteio', {time: 'now'}, function(reply) {
				if (reply.status === 'ok') {
					// Aqui busca a imagem, usando reply.username
					// E depois deve setar na célula
					$('#sorteado-div').text(reply.realname);
				} else {
					console.error('Failed to retrieve participantes: ' + reply.error);
				}
			});
			
		});
		});