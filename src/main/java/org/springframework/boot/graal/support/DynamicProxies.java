package org.springframework.boot.graal.support;

import java.io.InputStream;

import org.springframework.boot.graal.domain.proxies.ProxiesDescriptor;
import org.springframework.boot.graal.domain.proxies.ProxiesDescriptorJsonMarshaller;

public class DynamicProxies {

	public ProxiesDescriptor compute() {
		try {
			InputStream s = this.getClass().getResourceAsStream("/proxies.json");
			ProxiesDescriptor pd = ProxiesDescriptorJsonMarshaller.read(s);
			return pd;
		} catch (Exception e) {
			return null;
		}
	}
}
