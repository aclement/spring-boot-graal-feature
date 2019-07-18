package app.main;

import app.main.model.Foo;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {

	/**
	 * Send a message with JSON content and <code>content_type=application/json</code> in
	 * the properties.
	 */
	@RabbitListener(queues = "#{queue.name}")
	public void receive(Foo message) {
		System.out.println("Received <" + message + ">");
	}

}