/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.graal.type;

import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author Andy Clement
 */
public class Type {
	
	public final static String AtBean = "org/springframework/context/annotation/Bean";

	public final static String AtEnableConfigurationProperties = "Lorg/springframework/boot/context/properties/EnableConfigurationProperties;";
	
	public final static String AtConditionalOnClass = "Lorg/springframework/boot/autoconfigure/condition/ConditionalOnClass;";

	public final static Type MISSING = new Type(null, null);

	public final static Type[] NO_INTERFACES = new Type[0];

	private TypeSystem typeSystem;

	private ClassNode node;
	
	private Type[] interfaces;

	public Type(TypeSystem typeSystem, ClassNode node) {
		this.typeSystem = typeSystem;
		this.node = node;
	}

	public static Type forClassNode(TypeSystem typeSystem, ClassNode node) {
		return new Type(typeSystem, node);
	}

	/**
	 * @return typename, eg. aaa/bbb/ccc/Ddd$Eee
	 */
	public String getName() {
		return node.name;
	}

	public Type getSuperclass() {
		if (node.superName == null) {
			return null;
		}
		return typeSystem.resolveSlashed(node.superName);
	}
	
	@Override
	public String toString() {
		return "Type:"+getName();
	}

	public Type[] getInterfaces() {
		if (interfaces == null) {
			List<String> itfs = node.interfaces;
			if (itfs.size() == 0) {
				interfaces = NO_INTERFACES;
			} else {
				interfaces = new Type[itfs.size()];
				for (int i = 0; i < itfs.size(); i++) {
					interfaces[i] = typeSystem.resolveSlashed(itfs.get(i));
				}
			}
		}
		return interfaces;
	}

	public boolean implementsInterface(String interfaceName) {
		Type[] interfacesToCheck = getInterfaces();
		for (Type interfaceToCheck : interfacesToCheck) {
			if (interfaceToCheck.getName().equals(interfaceName)) {
				return true;
			}
			if (interfaceToCheck.implementsInterface(interfaceName)) {
				return true;
			}
		}
		Type superclass = getSuperclass();
		while (superclass != null) {
			if (superclass.implementsInterface(interfaceName)) {
				return true;
			}
			superclass = superclass.getSuperclass();
		}
		return false;
	}

	public List<Method> getMethodsWithAnnotation(String string) {
		return node.methods.stream().filter(m -> hasAnnotation(m, string)).map(m -> wrap(m))
				.collect(Collectors.toList());
	}
	
	public List<Method> getMethodsWithAtBean() {
		return null;
//		return getMethodsWithAnnotation("")
	}

	public Method wrap(MethodNode mn) {
		return new Method(mn);
	}

	private boolean hasAnnotation(MethodNode m, String string) {
		List<AnnotationNode> visibleAnnotations = m.visibleAnnotations;
		Optional<AnnotationNode> findAny = visibleAnnotations == null ? Optional.empty()
				: visibleAnnotations.stream().filter(a -> a.desc.equals(string)).findFirst();
		return findAny.isPresent();
	}

	protected static Set<String> validBoxing = new HashSet<String>();

	static {
		validBoxing.add("Ljava/lang/Byte;B");
		validBoxing.add("Ljava/lang/Character;C");
		validBoxing.add("Ljava/lang/Double;D");
		validBoxing.add("Ljava/lang/Float;F");
		validBoxing.add("Ljava/lang/Integer;I");
		validBoxing.add("Ljava/lang/Long;J");
		validBoxing.add("Ljava/lang/Short;S");
		validBoxing.add("Ljava/lang/Boolean;Z");
		validBoxing.add("BLjava/lang/Byte;");
		validBoxing.add("CLjava/lang/Character;");
		validBoxing.add("DLjava/lang/Double;");
		validBoxing.add("FLjava/lang/Float;");
		validBoxing.add("ILjava/lang/Integer;");
		validBoxing.add("JLjava/lang/Long;");
		validBoxing.add("SLjava/lang/Short;");
		validBoxing.add("ZLjava/lang/Boolean;");
	}

	public boolean isAssignableFrom(Type other) {
		if (other.isPrimitiveType()) {
			if (validBoxing.contains(this.getName() + other.getName())) {
				return true;
			}
		}
		if (this == other) {
			return true;
		}

		if (this.getName().equals("Ljava/lang/Object;")) {
			return true;
		}

//		if (!isTypeVariableReference()
//				&& other.getSignature().equals("Ljava/lang/Object;")) {
//			return false;
//		}

//		boolean thisRaw = this.isRawType();
//		if (thisRaw && other.isParameterizedOrGenericType()) {
//			return isAssignableFrom(other.getRawType());
//		}
//
//		boolean thisGeneric = this.isGenericType();
//		if (thisGeneric && other.isParameterizedOrRawType()) {
//			return isAssignableFrom(other.getGenericType());
//		}
//
//		if (this.isParameterizedType()) {
//			// look at wildcards...
//			if (((ReferenceType) this.getRawType()).isAssignableFrom(other)) {
//				boolean wildcardsAllTheWay = true;
//				ResolvedType[] myParameters = this.getResolvedTypeParameters();
//				for (int i = 0; i < myParameters.length; i++) {
//					if (!myParameters[i].isGenericWildcard()) {
//						wildcardsAllTheWay = false;
//					} else {
//						BoundedReferenceType boundedRT = (BoundedReferenceType) myParameters[i];
//						if (boundedRT.isExtends() || boundedRT.isSuper()) {
//							wildcardsAllTheWay = false;
//						}
//					}
//				}
//				if (wildcardsAllTheWay && !other.isParameterizedType()) {
//					return true;
//				}
//				// we have to match by parameters one at a time
//				ResolvedType[] theirParameters = other
//						.getResolvedTypeParameters();
//				boolean parametersAssignable = true;
//				if (myParameters.length == theirParameters.length) {
//					for (int i = 0; i < myParameters.length
//							&& parametersAssignable; i++) {
//						if (myParameters[i] == theirParameters[i]) {
//							continue;
//						}
//						// dont do this: pr253109
//						// if
//						// (myParameters[i].isAssignableFrom(theirParameters[i],
//						// allowMissing)) {
//						// continue;
//						// }
//						ResolvedType mp = myParameters[i];
//						ResolvedType tp = theirParameters[i];
//						if (mp.isParameterizedType()
//								&& tp.isParameterizedType()) {
//							if (mp.getGenericType().equals(tp.getGenericType())) {
//								UnresolvedType[] mtps = mp.getTypeParameters();
//								UnresolvedType[] ttps = tp.getTypeParameters();
//								for (int ii = 0; ii < mtps.length; ii++) {
//									if (mtps[ii].isTypeVariableReference()
//											&& ttps[ii]
//													.isTypeVariableReference()) {
//										TypeVariable mtv = ((TypeVariableReferenceType) mtps[ii])
//												.getTypeVariable();
//										boolean b = mtv
//												.canBeBoundTo((ResolvedType) ttps[ii]);
//										if (!b) {// TODO incomplete testing here
//													// I think
//											parametersAssignable = false;
//											break;
//										}
//									} else {
//										parametersAssignable = false;
//										break;
//									}
//								}
//								continue;
//							} else {
//								parametersAssignable = false;
//								break;
//							}
//						}
//						if (myParameters[i].isTypeVariableReference()
//								&& theirParameters[i].isTypeVariableReference()) {
//							TypeVariable myTV = ((TypeVariableReferenceType) myParameters[i])
//									.getTypeVariable();
//							// TypeVariable theirTV =
//							// ((TypeVariableReferenceType)
//							// theirParameters[i]).getTypeVariable();
//							boolean b = myTV.canBeBoundTo(theirParameters[i]);
//							if (!b) {// TODO incomplete testing here I think
//								parametersAssignable = false;
//								break;
//							} else {
//								continue;
//							}
//						}
//						if (!myParameters[i].isGenericWildcard()) {
//							parametersAssignable = false;
//							break;
//						} else {
//							BoundedReferenceType wildcardType = (BoundedReferenceType) myParameters[i];
//							if (!wildcardType.alwaysMatches(theirParameters[i])) {
//								parametersAssignable = false;
//								break;
//							}
//						}
//					}
//				} else {
//					parametersAssignable = false;
//				}
//				if (parametersAssignable) {
//					return true;
//				}
//			}
//		}
//
//		// eg this=T other=Ljava/lang/Object;
//		if (isTypeVariableReference() && !other.isTypeVariableReference()) {
//			TypeVariable aVar = ((TypeVariableReference) this)
//					.getTypeVariable();
//			return aVar.resolve(world).canBeBoundTo(other);
//		}
//
//		if (other.isTypeVariableReference()) {
//			TypeVariableReferenceType otherType = (TypeVariableReferenceType) other;
//			if (this instanceof TypeVariableReference) {
//				return ((TypeVariableReference) this)
//						.getTypeVariable()
//						.resolve(world)
//						.canBeBoundTo(
//								otherType.getTypeVariable().getFirstBound()
//										.resolve(world));// pr171952
//				// return
//				// ((TypeVariableReference)this).getTypeVariable()==otherType
//				// .getTypeVariable();
//			} else {
//				// FIXME asc should this say canBeBoundTo??
//				return this.isAssignableFrom(otherType.getTypeVariable()
//						.getFirstBound().resolve(world));
//			}
//		}

		Type[] interfaces = other.getInterfaces();
		for (Type intface : interfaces) {
			boolean b;
//			if (thisRaw && intface.isParameterizedOrGenericType()) {
//				b = this.isAssignableFrom(intface.getRawType(), allowMissing);
//			} else {
			b = this.isAssignableFrom(intface);
//			}
			if (b) {
				return true;
			}
		}
		Type superclass = other.getSuperclass();
		if (superclass != null) {
			boolean b;
//			if (thisRaw && superclass.isParameterizedOrGenericType()) {
//				b = this.isAssignableFrom(superclass.getRawType(), allowMissing);
//			} else {
			b = this.isAssignableFrom(superclass);
//			}
			if (b) {
				return true;
			}
		}
		return false;
	}

	private boolean isPrimitiveType() {
//		System.out.println("is primitive? "+this.getName());
		return false;
	}

	public boolean isInterface() {
		return Modifier.isInterface(node.access);
	}

	public boolean hasAnnotationInHierarchy(String lookingFor) {
		return hasAnnotationInHierarchy(lookingFor, new ArrayList<String>());
	}

	public boolean hasAnnotationInHierarchy(String lookingFor, List<String> seen) {
		for (AnnotationNode anno : node.visibleAnnotations) {
			if (seen.contains(anno.desc))
				continue;
			seen.add(anno.desc);
//			System.out.println("Comparing "+anno.desc+" with "+lookingFor);
			if (anno.desc.equals(lookingFor)) {
				return true;
			}
			try {
				Type resolve = typeSystem.Lresolve(anno.desc);
				if (resolve.hasAnnotationInHierarchy(lookingFor, seen)) {
					return true;
				}
			} catch (MissingTypeException mte) {
				// not on classpath, that's ok
			}
		}
		return false;
	}

	public boolean isMetaAnnotated(String slashedTypeDescriptor) {
		return isMetaAnnotated(slashedTypeDescriptor, new HashSet<>());
	}

	public boolean isMetaAnnotated(String slashedTypeDescriptor, Set<String> seen) {
//		System.out.println("Looking at "+this.getName()+" for "+slashedTypeDescriptor);
		for (Type t : this.getAnnotations()) {
			String tname = t.getName();
			if (tname.equals(slashedTypeDescriptor)) {
				return true;
			}
			if (!seen.contains(tname)) {
				seen.add(tname);
				if (t.isMetaAnnotated(slashedTypeDescriptor, seen)) {
					return true;
				}
			}
		}
		return false;
	}

	List<Type> annotations = null;

	public static final List<Type> NO_ANNOTATIONS = Collections.emptyList();

	@SuppressWarnings("unchecked")
	public List<String> findConditionalOnClassValue() {
		return findAnnotationValue(AtConditionalOnClass);
//		if (node.visibleAnnotations != null) {
//			for (AnnotationNode an : node.visibleAnnotations) {
//				if (an.desc.equals("Lorg/springframework/boot/autoconfigure/condition/ConditionalOnClass;")) {
//					List<Object> values = an.values;
//					for (int i=0;i<values.size();i+=2) {
//						if (values.get(i).equals("value")) {
//							return ( (List<org.objectweb.asm.Type>)values.get(i+1))
//									.stream()
//									.map(t -> t.getDescriptor())
//									.collect(Collectors.toList());
//						}
//					}
////					for (Object o: values) {
////						System.out.println("<> "+o+"  "+(o==null?"":o.getClass()));
////					}
//					// value Class
//					// name String
//				}
////					annotations.add(this.typeSystem.Lresolve(an.desc));
//			}
//		}
//		return null;
	}
	
	public List<String> findEnableConfigurationPropertiesValue() {
		return findAnnotationValue(AtEnableConfigurationProperties);
	}
		
	public List<String> findAnnotationValue(String annotationType) {
		if (node.visibleAnnotations != null) {
			for (AnnotationNode an : node.visibleAnnotations) {
				if (an.desc.equals(annotationType)) {
					List<Object> values = an.values;
					if (values != null) {
						for (int i=0;i<values.size();i+=2) {
							if (values.get(i).equals("value")) {
								return ( (List<org.objectweb.asm.Type>)values.get(i+1))
										.stream()
										.map(t -> t.getDescriptor())
										.collect(Collectors.toList());
							}
						}
					}
				}
			}
		}
		return null;
	}

	private List<Type> getAnnotations() {
		if (annotations == null) {
			annotations = new ArrayList<>();
			if (node.visibleAnnotations != null) {
				for (AnnotationNode an : node.visibleAnnotations) {
					try {
						annotations.add(this.typeSystem.Lresolve(an.desc));
					} catch (MissingTypeException mte) {
						// that's ok you weren't relying on it anyway!
					}
				}
			}
//			if (node.invisibleAnnotations != null) {
//			for (AnnotationNode an: node.invisibleAnnotations) {
//				try {
//					annotations.add(this.typeSystem.Lresolve(an.desc));
//				} catch (MissingTypeException mte) {
//					// that's ok you weren't relying on it anyway!
//				}
//			}
//			}
			if (annotations.size() == 0) {
				annotations = NO_ANNOTATIONS;
			}
		}
		return annotations;
	}

	public Type findAnnotation(Type searchType) {
		List<Type> annos = getAnnotations();
		for (Type anno : annos) {
			if (anno.equals(searchType)) {
				return anno;
			}
		}
		return null;
	}

	/**
	 * @return true if meta annotated with org.springframework.stereotype.Indexed
	 */
	public Entry<String,String> isIndexed() {
		Type indexedType = isMetaAnnotated2("Lorg/springframework/stereotype/Indexed;");
		if (indexedType != null) {
			return new AbstractMap.SimpleImmutableEntry<String,String>(this.node.name.replace("/", "."),indexedType.getName().replace("/", "."));
		} else {
			return null;
		}
//		public List<String> findConditionalOnClassValue() {
//			if (node.visibleAnnotations != null) {
//				for (AnnotationNode an : node.visibleAnnotations) {
//					if (an.desc.equals("Lorg/springframework/boot/autoconfigure/condition/ConditionalOnClass;")) {
//						List<Object> values = an.values;
//						for (int i=0;i<values.size();i+=2) {
//							if (values.get(i).equals("value")) {
//								return ( (List<org.objectweb.asm.Type>)values.get(i+1))
//										.stream()
//										.map(t -> t.getDescriptor())
//										.collect(Collectors.toList());
//							}
//						}
////						for (Object o: values) {
////							System.out.println("<> "+o+"  "+(o==null?"":o.ge
	}
	
	private Type isMetaAnnotated2(String Ldescriptor) {
		return isMetaAnnotated2(Ldescriptor, new HashSet<>());
	}
	
	private Type isMetaAnnotated2(String Ldescriptor, Set<String> seen) {
		if (node.visibleAnnotations != null) {
			for (AnnotationNode an: node.visibleAnnotations) {
				if (seen.add(an.desc)) { 
					if (an.desc.equals(Ldescriptor)) {
						return this;//typeSystem.Lresolve(an.desc);
					} else {
						Type annoType = typeSystem.Lresolve(an.desc);
						Type meta = annoType.isMetaAnnotated2(Ldescriptor, seen);
						if (meta != null) {
							return meta;
						}
					}
				}
			}
		}
		return null;
	}

	public List<Type> getNestedTypes() {
		List<Type> result = null;
		List<InnerClassNode> innerClasses = node.innerClasses;
		for (InnerClassNode inner: innerClasses) {	
			if (!inner.outerName.equals(getName())) {
//				System.out.println("SKIPPPING "+inner.name+" because outer is "+inner.outerName+" and we are looking at "+getName());
				continue;
			}
			if (inner.name.equals(getName())) {
				continue;
			}
			Type t = typeSystem.resolve(inner.name); // aaa/bbb/ccc/Ddd$Eee
			if (result == null) {
				result = new ArrayList<>();
			}
			result.add(t);
		}
		return result==null?Collections.emptyList():result;
	}

}