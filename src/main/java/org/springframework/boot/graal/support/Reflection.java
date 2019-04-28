package org.springframework.boot.graal.support;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.boot.graal.domain.reflect.JsonMarshaller;
import org.springframework.boot.graal.domain.reflect.ReflectionDescriptor;

public class Reflection {

	public void load() throws Exception {
		InputStream s = this.getClass().getResourceAsStream("/reflect.json");
		System.out.println(s);
		ReflectionDescriptor read = JsonMarshaller.read(s);
		System.out.println(read);
	}
}
