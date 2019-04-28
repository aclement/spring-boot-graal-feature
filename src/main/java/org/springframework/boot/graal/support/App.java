package org.springframework.boot.graal.support;

public class App {
	public static void main(String[] args) throws Exception {
		System.out.println("Hello World!");
		new Reflection().load();
		new DynamicProxies().compute();
		new Resources().load();
		new DelayInitialization().load();
	}
}
