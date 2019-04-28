package org.springframework.boot.graal.support;

import java.io.InputStream;

import org.springframework.boot.graal.domain.resources.ResourcesDescriptor;
import org.springframework.boot.graal.domain.resources.ResourcesJsonMarshaller;

public class Resources {

	public ResourcesDescriptor load() throws Exception {
		try {
			InputStream s = this.getClass().getResourceAsStream("/resources.json");
			ResourcesDescriptor read = ResourcesJsonMarshaller.read(s);
			return read;
		} catch (Exception e) {
			return null;
		}
	}
}
