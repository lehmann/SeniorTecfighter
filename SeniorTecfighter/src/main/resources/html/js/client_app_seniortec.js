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
		var sound = new Audio("audio/videoplayback.wav");
		sound.preload = 'auto';
		sound.load();
		
		$('#coluna-sorteio').click(function() {
			$('#sorteado_div').text("");
			$('#sorteado_div').css("visibility", "hidden");
			$('#img-sorteio').attr("src", "images/_boneco-seniortec.png");
			$('#youwin_div').css("visibility", "hidden");
			
			// Aqui faz a firula de ficar mostrando várias fotos até chegar a hora de mostrar o sortudo, digo, sorteado.
			//setInterval(function() { console.log("setInterval: Ja passou 1 segundo!"); }, 1000);
			
			var contador = 0;
			var tempo = 100;
			var total = 0;
			var totalParcial = 0;
			var totalg1 = 0;
			var totalg2 = 0;
			var totalg3 = 0;
			var interval = 0;
			
			var feliz; // (:
			
			sorteio();
			
			eb.send('get-participantes-palestra', {time: 'now'}, function(reply) {
				if(reply.status === 'ok') {
					var obj = reply.participantes;
					var atual = 0;
					var username = obj[atual].Username;
					var nome = obj[atual].Nome;
					
					total = obj.length;
					
				    // primeiro grupo= total / 3
					var terco = Math.round(total / 3);
					totalg1 = Math.round(terco * 0.7);
					totalg2 = Math.round(terco * 0.2);
					totalg3 = Math.round(terco * 0.1);
					contador = 0;
					
					// Exemplo do incremento circular: atual = (atual + 1) % total;
					totalParcial = totalg1;
					interval = setInterval(function() {atual = mudaFotos(atual, totalParcial, "images/" + obj[atual].Username + ".png")}, tempo);
			}});
			
			function mudaFotos(atualP, totalP, imagePath) {
				if(contador < totalP) {
					$('#img-sorteio').attr("src", imagePath);
					if ((contador+1) === totalg1 ) {
						tempo = 200;
						totalParcial = totalg1 + totalg2; 
					} else if ((contador+1) === (totalg1 + totalg2)){
						tempo = 400;
						totalParcial = totalg1 + totalg2 + totalg3; 
					}
					contador++;
					return ((atualP + 1) % total);
				} else {
					clearInterval(interval);
					botaFelizardoNaTela();
				}
			}

			function botaFelizardoNaTela() {
				if(feliz.status === 'ok') {
					$('#sorteado_div').text(feliz.sortudo.Nome);
					$('#sorteado_div').css("visibility", "visible");
					$('#img-sorteio').attr("src", "images/" + feliz.sortudo.Username + ".png");
					$('#youwin_div').css("visibility", "visible");				
					var click=sound.cloneNode();
					click.play();
				} else {
					$('#sorteado_div').text(feliz.content);
					$('#sorteado_div').css("visibility", "visible");
					$('#img-sorteio').attr("src", "images/_boneco-seniortec.png");
				}
			}

			function sorteio() {
				eb.send('sorteio', {time: 'now'}, function(reply) {
					feliz = reply;
				});
			}
			
		});
		
		$(document).keypress(function(e) {
		    if(e.which === 13) { // colocar o ENTER do teclado número também.
		    	if(!$('#personagens').is('visible')) {
		    		$('#start').css("opacity", "0");
		    		$('#personagens').css("visibility", "visible");		    		
		    	}
		    }
		});
		
		});
