/******************************************************************************* 
 * Copyright (c) 2008 - 2014 Red Hat, Inc. and others. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Xavier Coulon - Initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.ws.jaxrs.core.jdt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.LocalVariable;
import org.jboss.tools.ws.jaxrs.core.internal.utils.CollectionUtils;

/**
 * Annotation wrapper for IAnnotation on types, fields, methods and method
 * parameters as well. Annotation wrappers should follow the same lifecycle as
 * their underlying java elements, which means that in the particular case of
 * the ILocalVariable wrapper (java method parameter), the Annotation maybe
 * destroy/re-created as the ILocalVariable is re-created, too.
 * 
 * @author Xavier Coulon
 * 
 */
@SuppressWarnings("restriction")
public class Annotation {
	
	/** Default name of the attribute when it is alone in the annotation. */
	public static final String VALUE = "value";

	/**
	 * Underlying java annotation, which may change in the case of method
	 * parameter annotation, which are managed by {@link LocalVariable} in JDT.
	 * In this particular case, a new LocalVariable instance is created after
	 * content changes, and this new instance should be kept for source range
	 * resolution.
	 */
	private final IAnnotation javaAnnotation;

	/** The Java annotation fully qualified name. */
	private final String javaAnnotationName;

	/** The java annotation member value pairs. */
	private final Map<String, List<String>> javaAnnotationElements;
	
	/** the primary copy of this annotation, or {@code this}. */
	private final Annotation primaryCopy;

	private final boolean isWorkingCopy;
	
	/** the working copy of this annotation, or {@code null} if none exists (yet). */
	private Annotation workingCopy = null;
	
	/**
	 * Full constructor
	 * 
	 * @param annotation the underlying {@link IAnnotation}
	 * @param annotationName the fully qualified name of the underlying {@link IAnnotation} 
	 * @param annotationElements the members of the annotation, indexed by their key.
	 * @param sourceRange
	 * @throws JavaModelException
	 */
	public Annotation(final IAnnotation javaAnnotation, final String javaAnnotationName,
			final Map<String, List<String>> javaAnnotationElements) {
		this.javaAnnotation = javaAnnotation;
		this.javaAnnotationName = javaAnnotationName;
		this.javaAnnotationElements = new HashMap<String, List<String>>(javaAnnotationElements);
		this.primaryCopy = null;
		this.isWorkingCopy = false;
	}

	/**
	 * Full constructor for the working copy
	 * 
	 * @param annotation the underlying {@link IAnnotation}
	 * @param annotationName the fully qualified name of the underlying {@link IAnnotation} 
	 * @param annotationElements the members of the annotation, indexed by their key.
	 * @param sourceRange
	 * @throws JavaModelException
	 */
	private Annotation(final IAnnotation javaAnnotation, final String javaAnnotationName,
			final Map<String, List<String>> javaAnnotationElements, final Annotation primaryCopy) {
		this.javaAnnotation = javaAnnotation;
		this.javaAnnotationName = javaAnnotationName;
		this.javaAnnotationElements = new HashMap<String, List<String>>(javaAnnotationElements);
		this.primaryCopy = primaryCopy;
		this.isWorkingCopy = true;
		this.workingCopy = this;
		primaryCopy.workingCopy = this;
	}

	/**
	 * Full constructor with a single unnamed 'value'
	 * 
	 * @param annotation
	 * @param annotationName
	 * @param annotationValue
	 * @param sourceRange
	 * @throws JavaModelException
	 */
	public Annotation(final IAnnotation annotation, final String annotationName, final String annotationValue) {
		this(annotation, annotationName, CollectionUtils.toMap(VALUE, Arrays.asList(annotationValue)));
	}

	/**
	 * @return a working copy of this Annotation.
	 */
	public Annotation createWorkingCopy() {
		synchronized (this) {
			final Map<String, List<String>> duplicateJavaAnnotationElements = new HashMap<String, List<String>>();
			for(Entry<String, List<String>> entry : javaAnnotationElements.entrySet()) {
				duplicateJavaAnnotationElements.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
			}
			return new Annotation(javaAnnotation, javaAnnotationName, duplicateJavaAnnotationElements, this);
		}
	}

	public Annotation getWorkingCopy() {
		return workingCopy;
	}
	
	public Annotation getPrimaryCopy() {
		return primaryCopy;
	}
	
	public boolean isWorkingCopy() {
		return isWorkingCopy;
	}


	/**
	 * Update this Annotation from the given other annotation.
	 * 
	 * @param otherAnnotation
	 * @return true if some updates in the annotation elements (member pair
	 *         values) were performed, false otherwise.
	 */
	public boolean update(final Annotation otherAnnotation) {
		synchronized (this) {
			if (otherAnnotation == null || !hasChanges(otherAnnotation)) {
				return false;
			}
			this.javaAnnotationElements.clear();
			this.javaAnnotationElements.putAll(otherAnnotation.getJavaAnnotationElements());
			return true;
		}
	}

	/**
	 * Returns true if the given 'otherAnnotation' is different from this
	 * annotation, false otherwise.
	 * 
	 * @param otherAnnotation
	 * @return
	 */
	public boolean hasChanges(final Annotation otherAnnotation) {
		if (this.javaAnnotationElements.equals(otherAnnotation.getJavaAnnotationElements())) {
			return false;
		}
		return true;
	}

	public IAnnotation getJavaAnnotation() {
		return javaAnnotation;
	}

	public IJavaElement getJavaParent() {
		if (javaAnnotation == null) {
			return null;
		}
		return javaAnnotation.getParent();
	}

	public String getFullyQualifiedName() {
		return javaAnnotationName;
	}

	public Map<String, List<String>> getJavaAnnotationElements() {
		return javaAnnotationElements;
	}

	/** @return the value */
	public List<String> getValues(final String elementName) {
		return javaAnnotationElements.get(elementName);
	}

	/** @return the default value when it is a single element*/
	public String getValue() {
		return getValue(VALUE);
	}

	/** @return the default value when it is a list of elements */
	public List<String> getValues() {
		return getValues(VALUE);
	}

	/** @return the value */
	public String getValue(final String elementName) {
		final List<String> values = javaAnnotationElements.get(elementName);
		if (values != null) {
			assert !(values.size() > 1);
			if (values.size() == 1) {
				return values.get(0);
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append('@').append(getFullyQualifiedName());
		if(!javaAnnotationElements.isEmpty()) {
			builder.append('(');
			for(Iterator<Entry<String, List<String>>> iterator = javaAnnotationElements.entrySet().iterator(); iterator.hasNext();) {
				final Entry<String, List<String>> entry = iterator.next();
				builder.append(entry.getKey()).append('=');
				if(entry.getValue().size() == 1) {
					builder.append('\"').append(entry.getValue().get(0)).append('\"');
				} else {
					builder.append(entry.getValue());
				}
				if(iterator.hasNext()) {
					builder.append(", ");
				}
			}
			builder.append(')');
		}
		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((javaAnnotation == null) ? 0 : javaAnnotation.getHandleIdentifier().hashCode());
		result = prime * result + ((javaAnnotationElements == null) ? 0 : javaAnnotationElements.hashCode());
		result = prime * result + ((javaAnnotationName == null) ? 0 : javaAnnotationName.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Annotation other = (Annotation) obj;
		if (javaAnnotationElements == null) {
			if (other.javaAnnotationElements != null) {
				return false;
			}
		} else if (!javaAnnotationElements.equals(other.javaAnnotationElements)) {
			return false;
		}
		if (javaAnnotationName == null) {
			if (other.javaAnnotationName != null) {
				return false;
			}
		} else if (!javaAnnotationName.equals(other.javaAnnotationName)) {
			return false;
		}
		return true;
	}

}
