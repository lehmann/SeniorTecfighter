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

(function DemoViewModel() {

	var that = this;
	var eb = new vertx.EventBus('http://'
			+ window.location.hostname + ':8081' + '/eventbus');

	$('#click-me').on('click', function() {
		eb.send('get-participantes-palestra', {time: 'now'}, function(reply) {
			if (reply.status === 'ok') {
				$('.container-fluid').text(reply.content);
			} else {
				console.error('Failed to get participantes: ' + reply.content);
			}
		});
	});

	$('#load-participantes').on('click', function() {
		eb.send('load-participantes-palestra', {time: 'now'}, function(reply) {
			if (reply.status === 'ok') {
				$('.container-participantes').text(reply.content);
			} else {
				console.error('Failed to retrieve participantes: ' + reply.error);
			}
		});
	});

	$('#sorteio').on('click', function() {
		eb.send('sorteio', {time: 'now'}, function(reply) {
			if (reply.status === 'ok') {
				$('.container-sorteio').text(reply.sortudo.Username);
			} else {
				console.error('Failed to retrieve participantes: ' + reply.error);
			}
		});
	});
})();