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
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_HIERARCHY;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_APPLICATION_PATH_VALUE_OVERRIDE;
import static org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames.APPLICATION_PATH;

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
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
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsJavaApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

/**
 * This domain element describes a subtype of {@link jvax.ws.rs.Application}
 * annotated with {@link jvax.ws.rs.ApplicationPath}.
 * 
 * @author xcoulon
 */
public class JaxrsJavaApplication extends JaxrsJavaElement<IType> implements IJaxrsJavaApplication {

	/**
	 * Builder initializer
	 * 
	 * @param javaElement
	 *            the underlying {@link IJavaElement} that on which this JAX-RS
	 *            Element will be built.
	 * @return the Builder
	 * @throws JavaModelException
	 */
	public static Builder from(final IJavaElement javaElement) throws JavaModelException {
		final CompilationUnit ast = JdtUtils.parse(javaElement, new NullProgressMonitor());
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

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
	public static Builder from(final IJavaElement javaElement, final CompilationUnit ast) {
		switch (javaElement.getElementType()) {
		case IJavaElement.COMPILATION_UNIT:
			return new Builder(((ICompilationUnit) javaElement).findPrimaryType(), ast);
		case IJavaElement.TYPE:
			return new Builder((IType) javaElement, ast);
		}
		return null;
	}

	/**
	 * Internal Builder
	 * 
	 * @author xcoulon
	 * 
	 */
	public static class Builder {

		private final IType javaType;
		private final CompilationUnit ast;
		private Map<String, Annotation> annotations;
		private JaxrsMetamodel metamodel = null;
		private boolean isApplicationSubclass = false;

		private Builder(final IType javaType, final CompilationUnit ast) {
			this.javaType = javaType;
			this.ast = ast;
		}

		public Builder withMetamodel(final JaxrsMetamodel metamodel) {
			this.metamodel = metamodel;
			return this;
		}

		public JaxrsJavaApplication build() throws CoreException {
			return build(true);
		}
		
		JaxrsJavaApplication build(final boolean joinMetamodel) throws CoreException {
			final long start = System.currentTimeMillis();
			try {
				if (javaType == null || !javaType.exists() || !javaType.isStructureKnown()) {
					return null;
				}
				JdtUtils.makeConsistentIfNecessary(javaType);
				final IType applicationSupertype = JdtUtils.resolveType(JaxrsClassnames.APPLICATION,
						javaType.getJavaProject(), new NullProgressMonitor());
				this.isApplicationSubclass = JdtUtils.isTypeOrSuperType(applicationSupertype, javaType);
				this.annotations = JdtUtils.resolveAllAnnotations(javaType, ast);
				final Annotation applicationPathAnnotation = annotations.get(APPLICATION_PATH);
				if (isApplicationSubclass || applicationPathAnnotation != null) {
					final JaxrsJavaApplication application = new JaxrsJavaApplication(this);
					// this operation is only performed after creation
					if(joinMetamodel) {
						application.joinMetamodel();
					}
					return application;
				}
				return null;
			} finally {
				final long end = System.currentTimeMillis();
				Logger.tracePerf("Built JAX-RS JavaApplication in {}ms", (end - start));
			}
		}

	}

	/**
	 * Indicates whether the underlying Java type is a subclass of
	 * <code>javax.ws.rs.core.Application</code>.
	 */
	private boolean isApplicationSubclass = false;

	/**
	 * The ApplicationPath overriden value that can be configured in the
	 * web.xml.
	 */
	private String applicationPathOverride = null;

	/**
	 * Full constructor.
	 * 
	 * @param builder
	 *            the fluent builder
	 */
	private JaxrsJavaApplication(final Builder builder) {
		this(builder.javaType, builder.annotations, builder.metamodel, builder.isApplicationSubclass, null);
	}

	/**
	 * 
	 * @param javaType
	 * @param annotations
	 * @param metamodel
	 * @param isApplicationSubclass
	 * @param primaryCopy
	 *            the associated primary copy element, or {@code null} if this
	 *            instance is already the primary element
	 */
	private JaxrsJavaApplication(final IType javaType, final Map<String, Annotation> annotations, final JaxrsMetamodel metamodel,
			final boolean isApplicationSubclass, final JaxrsJavaApplication primaryCopy) {
		super(javaType, annotations, metamodel, primaryCopy);
		this.isApplicationSubclass = isApplicationSubclass;
		if (hasMetamodel()) {
			final JaxrsWebxmlApplication webxmlApplication = getMetamodel().findWebxmlApplicationByClassName(
					getJavaClassName());
			if (webxmlApplication != null) {
				this.applicationPathOverride = webxmlApplication.getApplicationPath();
			}
		}
	}

	@Override
	public JaxrsBaseElement createWorkingCopy() {
		synchronized (this) {
			return new JaxrsJavaApplication(getJavaElement(), AnnotationUtils.createWorkingCopies(getAnnotations()),
					getMetamodel(), this.isApplicationSubclass, this);
		}
	}

	@Override
	public JaxrsJavaApplication getWorkingCopy() {
		return (JaxrsJavaApplication) super.getWorkingCopy();
	}
	
	/**
	 * @return {@code true} if this element should be removed (ie, it does not meet the requirements to be a {@link JaxrsJavaApplication} anymore) 
	 */
	@Override
	boolean isMarkedForRemoval() {
		// element should be removed if it's not an application subclass and it
		// has no ApplicationPath annotation
		return !(isApplicationSubclass || getApplicationPathAnnotation() != null);
	}

	@Override
	public EnumElementKind getElementKind() {
		return EnumElementKind.APPLICATION_JAVA;
	}

	@Override
	public boolean isWebXmlApplication() {
		return false;
	}

	@Override
	public boolean isJavaApplication() {
		return true;
	}

	public boolean isJaxrsCoreApplicationSubclass() {
		return isApplicationSubclass;
	}

	public void setJaxrsCoreApplicationSubclass(final boolean isApplicationSubclass) {
		this.isApplicationSubclass = isApplicationSubclass;
	}

	@Override
	public String getJavaClassName() {
		return getJavaElement().getFullyQualifiedName();
	}

	/**
	 * Sets the ApplicationPath override that can be configured from web.xml
	 * 
	 * @param applicationPathOverride
	 *            the override value
	 * @throws CoreException
	 */
	public void setApplicationPathOverride(final String applicationPathOverride) throws CoreException {
		Logger.debug("Override @ApplicationPath value with '{}'", applicationPathOverride);
		this.applicationPathOverride = applicationPathOverride;
		if (hasMetamodel()) {
			getMetamodel().update(new JaxrsElementDelta(this, CHANGED, F_APPLICATION_PATH_VALUE_OVERRIDE));
		}
	}

	/**
	 * Unsets the ApplicationPath override that can be configured from web.xml
	 * 
	 * @throws CoreException
	 */
	public void unsetApplicationPathOverride() throws CoreException {
		Logger.debug("Unoverriding @ApplicationPath value");
		this.applicationPathOverride = null;
		if (hasMetamodel()) {
			getMetamodel().update(new JaxrsElementDelta(this, CHANGED, F_APPLICATION_PATH_VALUE_OVERRIDE));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getApplicationPath() {
		if (applicationPathOverride != null) {
			return applicationPathOverride;
		}
		final Annotation applicationPathAnnotation = getApplicationPathAnnotation();
		if (applicationPathAnnotation != null) {
			return applicationPathAnnotation.getValue();
		}
		return null;
	}

	/**
	 * @return the
	 *         <code>javax.ws.rs.ApplicationPath<code> annotation set on the underlying javatype, or null if none exists.
	 */
	public Annotation getApplicationPathAnnotation() {
		return getAnnotation(APPLICATION_PATH);
	}

	public boolean isOverriden() {
		if (getMetamodel() != null) {
			return (getMetamodel().findWebxmlApplicationByClassName(this.getJavaClassName()) != null);
		}
		return false;
	}

	/**
	 * Updates the current {@link JaxrsJavaApplication} from the given
	 * {@link IJavaElement} If the given transientApplication is null, this
	 * element will be removed.
	 * 
	 * @param element
	 * @param ast
	 * @return
	 * @throws CoreException
	 */
	@Override
	public void update(final IJavaElement javaElement, final CompilationUnit ast) throws CoreException {
		synchronized (this) {
			final JaxrsJavaApplication transientApplication = from(javaElement, ast).build(false);
			final Flags annotationsFlags = FlagsUtils.computeElementFlags(this);
			if (transientApplication == null) {
				remove(annotationsFlags);
			} else {
				final Flags updateAnnotationsFlags = updateAnnotations(transientApplication.getAnnotations());
				final JaxrsElementDelta delta = new JaxrsElementDelta(this, CHANGED, updateAnnotationsFlags);
				if (this.isJaxrsCoreApplicationSubclass() != transientApplication.isJaxrsCoreApplicationSubclass()) {
					this.isApplicationSubclass = transientApplication.isJaxrsCoreApplicationSubclass();
					delta.addFlag(F_APPLICATION_HIERARCHY);
				}
				if (isMarkedForRemoval()) {
					remove(annotationsFlags);
				}
				// update indexes for this element.
				else if(hasMetamodel()){
					getMetamodel().update(delta);
				}
			}
		}
	}

}
