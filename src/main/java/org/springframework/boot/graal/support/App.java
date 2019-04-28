package org.springframework.boot.graal.support;

public class App {
	public static void main(String[] args) throws Exception {
		System.out.println("Exercising...");
		new Reflection().compute();
		new DynamicProxies().compute();
		new Resources().compute();
		new DelayInitialization().compute();
	}
}
