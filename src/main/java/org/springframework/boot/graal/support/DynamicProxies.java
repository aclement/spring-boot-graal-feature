package org.springframework.boot.graal.support;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature.DuringSetupAccess;
import org.springframework.boot.graal.domain.proxies.ProxiesDescriptor;
import org.springframework.boot.graal.domain.proxies.ProxiesDescriptorJsonMarshaller;

import com.oracle.svm.core.jdk.proxy.DynamicProxyRegistry;
import com.oracle.svm.hosted.ImageClassLoader;
import com.oracle.svm.hosted.FeatureImpl.DuringSetupAccessImpl;

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

	public void register(DuringSetupAccess a) {
    	ProxiesDescriptor pd = compute();
    	System.out.println("SBG: Proxy registration: #"+pd.getProxyDescriptors().size()+" proxies");
    	DuringSetupAccessImpl access = (DuringSetupAccessImpl) a;
    	ImageClassLoader imageClassLoader = access.getImageClassLoader();
    	// Should have been registered by DynamicProxyFeature already
    	DynamicProxyRegistry dynamicProxySupport = ImageSingletons.lookup(DynamicProxyRegistry.class);
//      DynamicProxySupport dynamicProxySupport = new DynamicProxySupport(imageClassLoader.getClassLoader());
//      ImageSingletons.add(DynamicProxyRegistry.class, dynamicProxySupport);
    	Consumer<List<String>> proxyRegisteringConsumer = interfaceNames -> {
    		System.out.println(interfaceNames);
            Class<?>[] interfaces = new Class<?>[interfaceNames.size()];
            for (int i = 0; i < interfaceNames.size(); i++) {
                String className = interfaceNames.get(i);
                Class<?> clazz = imageClassLoader.findClassByName(className, false);
                if (clazz == null) {
                    throw new RuntimeException("Class " + className + " not found");
                }
                if (!clazz.isInterface()) {
                    throw new RuntimeException("The class \"" + className + "\" is not an interface.");
                }
                interfaces[i] = clazz;
            }
            /* The interfaces array can be empty. The java.lang.reflect.Proxy API allows it. */
            dynamicProxySupport.addProxyClass(interfaces);
    	};
    	pd.consume(proxyRegisteringConsumer);
	}
}
