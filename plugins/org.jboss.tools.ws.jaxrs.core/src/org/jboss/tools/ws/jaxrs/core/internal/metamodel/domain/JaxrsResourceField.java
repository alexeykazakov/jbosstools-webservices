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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain;

import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.BEAN_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.DEFAULT_VALUE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.MATRIX_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.PATH_PARAM;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.QUERY_PARAM;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.AnnotationUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.jdt.FlagsUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.JdtUtils;
import org.jboss.tools.ws.jaxrs.core.jdt.SourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResource;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsResourceField;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;

/**
 * JAX-RS Resource Field.
 * 
 * @author xcoulon
 */
public class JaxrsResourceField extends JaxrsResourceElement<IField> implements IJaxrsResourceField {

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @param ast
	 *            the associated AST
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IField field, final CompilationUnit ast) {
		return new Builder(field, ast);
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IField javaField;
		private final CompilationUnit ast;
		private Map<String, Annotation> annotations;
		private JaxrsResource parentResource;
		private JaxrsMetamodel metamodel;
		private SourceType javaFieldType;

		private Builder(final IField javaField, final CompilationUnit ast) {
			this.javaField = javaField;
			this.ast = ast;
		}

		public Builder withParentResource(final JaxrsResource parentResource) {
			this.parentResource = parentResource;
			return this;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}
		
		public Builder withAnnotations(final Map<String, Annotation> annotations) {
			this.annotations = annotations;
			return this;
		}

		public JaxrsResourceField build() throws CoreException {
			return build(true);
		}
		
		JaxrsResourceField build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				// skip if element does not exist or if it has compilation errors
				if (javaField == null || !javaField.exists() || !javaField.isStructureKnown()) {
					return null;
				}
				JdtUtils.makeConsistentIfNecessary(javaField);
				javaFieldType = JdtUtils.resolveFieldType(javaField, ast);
				final IType parentType = (IType) javaField.getParent();
				// lookup parent resource in metamodel
				if (parentResource == null && metamodel != null) {
					final JaxrsJavaElement<?> parentElement = (JaxrsJavaElement<?>) metamodel.findElement(parentType);
					if (parentElement != null
							&& parentElement.getElementKind().getCategory() == EnumElementCategory.RESOURCE) {
						parentResource = (JaxrsResource) parentElement;
					} else {
						Logger.trace("Skipping {}.{} because parent Resource does not exist", parentType.getFullyQualifiedName(), javaField.getElementName());
						return null;
					}
				}
				if(this.annotations == null) {
					this.annotations = JdtUtils.resolveAllAnnotations(javaField, ast);
				}
				if (JaxrsParamAnnotations.matchesAtLeastOne(annotations.keySet())) {
					final JaxrsResourceField field = new JaxrsResourceField(this);
					// this operation is only performed after creation
					if(joinMetamodel) {
						field.joinMetamodel();
					}
					return field;
				}
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS Resource Method in {}ms", (end - start));
			}
		}

	}
	
	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder.
	 */
	private JaxrsResourceField(final Builder builder) {
		this(builder.javaField, builder.annotations, builder.metamodel, builder.javaFieldType, builder.parentResource,
				null);
	}

	/**
	 * Full constructor.
	 * 
	 * @param javaField
	 *            the java field
	 * @param annotations
	 *            the java element annotations (or null)
	 * @param metamodel
	 *            the metamodel in which this element exist, or null if this
	 *            element is transient.
	 * @param javaFieldType the {@link SourceType} associated with the given javaField
	 * @param parentResource
	 * 			the parent element
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	private JaxrsResourceField(final IField javaField, final Map<String, Annotation> annotations, final JaxrsMetamodel metamodel,
			final SourceType javaFieldType, final JaxrsResource parentResource, final JaxrsResourceField primaryCopy) {
		super(javaField, annotations, metamodel, javaFieldType, parentResource, primaryCopy);
		if(getParentResource() != null) {
			getParentResource().addField(this);
		}
	}

	/**
	 * Creates a working copy of this {@link JaxrsResourceField}, but detached
	 * from the primary copy parent {@link JaxrsResource} element.
	 */
	@Override
	public JaxrsResourceField createWorkingCopy() {
		synchronized (this) {
			final JaxrsResource parentWorkingCopy = getParentResource().getWorkingCopy();
			return parentWorkingCopy.getFields().get(this.javaElement.getHandleIdentifier());
		}
	}
	
	protected JaxrsResourceField createWorkingCopy(final JaxrsResource parentWorkingCopy) {
		return new JaxrsResourceField(getJavaElement(), AnnotationUtils.createWorkingCopies(getAnnotations()),
				getMetamodel(), getType(), parentWorkingCopy, this);
	}
	
	@Override
	public JaxrsResourceField getWorkingCopy() {
		return (JaxrsResourceField) super.getWorkingCopy();
	}

	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		if (javaElement == null) {
			remove(FlagsUtils.computeElementFlags(this));
		} else {
			// NOTE: the given javaElement may be an ICompilationUnit (after
			// resource change) !!
			switch (javaElement.getElementType()) {
			case IJavaElement.COMPILATION_UNIT:
				final IType primaryType = ((ICompilationUnit) javaElement).findPrimaryType();
				if (primaryType != null) {
					final IField field = primaryType.getField(getJavaElement().getElementName());
					update(field, ast);
				}
				break;
			case IJavaElement.FIELD:
				update(from((IField) javaElement, ast).build(false));
			}
		} 
	}

	/**
	 * Updates this {@link JaxrsResourceField} from the given {@code transientField}
	 * @param transientField
	 * @throws CoreException
	 */
	void update(final JaxrsResourceField transientField) throws CoreException {
		synchronized (this) {
			final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
			if (transientField == null) {
				remove(annotationsFlags);
			} else {
				final Flags updateAnnotationsFlags = updateAnnotations(transientField.getAnnotations());
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
				if (isMarkedForRemoval()) {
					remove(annotationsFlags);
				} else if(hasMetamodel()){
					getMetamodel().update(delta);
				}
			}
		}
	}

	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsResourceField} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		final boolean hasPathParamAnnotation = hasAnnotation(PATH_PARAM);
		final boolean hasQueryParamAnnotation = hasAnnotation(QUERY_PARAM);
		final boolean hasMatrixParamAnnotation = hasAnnotation(MATRIX_PARAM);
		// element should be removed if it has neither @PathParam, @QueryParam
		// nor @MatrixParam annotation
		return !(hasPathParamAnnotation || hasQueryParamAnnotation || hasMatrixParamAnnotation);
	}
	
	/**
	 * Remove {@code this} from the parent {@link IJaxrsResource} before calling {@code super.remove()} which deals with removal from the {@link JaxrsMetamodel}. 
	 */
	@Override
	public void remove(final Flags flags) throws CoreException {
		getParentResource().removeField(this);
		super.remove(flags);
	}


	public Annotation getPathParamAnnotation() {
		return getAnnotation(PATH_PARAM);
	}

	public Annotation getQueryParamAnnotation() {
		return getAnnotation(QUERY_PARAM);
	}

	public Annotation getMatrixParamAnnotation() {
		return getAnnotation(MATRIX_PARAM);
	}

	public Annotation getDefaultValueAnnotation() {
		return getAnnotation(DEFAULT_VALUE);
	}
	
	public Annotation getBeanParamAnnotation() {
		return getAnnotation(BEAN_PARAM);
	}
	
	@Override
	public EnumElementKind getElementKind() {
		if (getPathParamAnnotation() != null) {
			return EnumElementKind.PATH_PARAM_FIELD;
		} 
		if (getQueryParamAnnotation() != null) {
			return EnumElementKind.QUERY_PARAM_FIELD;
		}
		if (getMatrixParamAnnotation() != null) {
			return EnumElementKind.MATRIX_PARAM_FIELD;
		}
		if (getBeanParamAnnotation() != null) {
			return EnumElementKind.BEAN_PARAM_FIELD;
		}
		return EnumElementKind.UNDEFINED_RESOURCE_FIELD;
	}

}
