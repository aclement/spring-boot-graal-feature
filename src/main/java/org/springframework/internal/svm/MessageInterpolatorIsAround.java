package org.springframework.internal.svm;

import java.util.function.BooleanSupplier;

public class MessageInterpolatorIsAround implements BooleanSupplier {

	@Override
	public boolean getAsBoolean() {
		try {
			Class.forName("javax.validation.MessageInterpolator");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

}
