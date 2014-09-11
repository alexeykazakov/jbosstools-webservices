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
package org.jboss.tools.ws.jaxrs.core.internal.metamodel.builder;

import static org.eclipse.jdt.core.IJavaElementDelta.ADDED;
import static org.eclipse.jdt.core.IJavaElementDelta.CHANGED;
import static org.eclipse.jdt.core.IJavaElementDelta.REMOVED;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_ELEMENT_KIND;
import static org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta.F_METHOD_RETURN_TYPE;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsEndpointFactory;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsHttpMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsMetamodel;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregator;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorField;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsParameterAggregatorProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResource;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceElement;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceMethod;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.domain.JaxrsResourceProperty;
import org.jboss.tools.ws.jaxrs.core.internal.metamodel.search.JavaElementsSearcher;
import org.jboss.tools.ws.jaxrs.core.internal.utils.Logger;
import org.jboss.tools.ws.jaxrs.core.jdt.Annotation;
import org.jboss.tools.ws.jaxrs.core.jdt.Flags;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementCategory;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.EnumElementKind;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IAnnotatedSourceType;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsApplication;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsElement;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.IJaxrsEndpoint;
import org.jboss.tools.ws.jaxrs.core.metamodel.domain.JaxrsElementDelta;
import org.jboss.tools.ws.jaxrs.core.utils.JaxrsClassnames;

public class JaxrsElementChangedProcessorDelegate {

	public static void processEvent(final JaxrsElementDelta event) throws CoreException {
		final IJaxrsElement element = event.getElement();
		final EnumElementCategory elementKind = element.getElementKind().getCategory();
		final Flags flags = event.getFlags();
		switch (event.getDeltaKind()) {
		case ADDED:
			switch (elementKind) {
			case APPLICATION:
				processAddition((IJaxrsApplication) element);
				break;
			case HTTP_METHOD:
				processAddition((JaxrsHttpMethod) element);
				break;
			case RESOURCE_METHOD:
				processAddition((JaxrsResourceMethod) element);
				break;
			case RESOURCE_FIELD:
				processAddition((JaxrsResourceElement<?>) element);
				break;
			case RESOURCE_PROPERTY:
				processAddition((JaxrsResourceProperty) element);
				break;
			case PARAMETER_AGGREGATOR_FIELD:
				processAddition((JaxrsParameterAggregatorField) element, flags);
				break;
			case PARAMETER_AGGREGATOR_PROPERTY:
				processAddition((JaxrsParameterAggregatorProperty) element, flags);
				break;
			default:
				Logger.trace("No direct impact on JAX-RS Endpoints after change on element:" + elementKind);
				break;
			}
			break;
		case CHANGED:
			switch (elementKind) {
			case APPLICATION:
				processChange((IJaxrsApplication) element, flags);
				break;
			case HTTP_METHOD:
				processChange((JaxrsHttpMethod) element, flags);
				break;
			case RESOURCE:
				processChange((JaxrsResource) element, flags);
				break;
			case RESOURCE_METHOD:
				processChange((JaxrsResourceMethod) element, flags);
				break;
			case RESOURCE_FIELD:
				processChange((JaxrsResourceElement<?>) element, flags);
				break;
			case RESOURCE_PROPERTY:
				processChange((JaxrsResourceProperty) element, flags);
				break;
			case PARAMETER_AGGREGATOR_FIELD:
				processChange((JaxrsParameterAggregatorField) element, flags);
				break;
			case PARAMETER_AGGREGATOR_PROPERTY:
				processChange((JaxrsParameterAggregatorProperty) element, flags);
				break;
			default:
				Logger.trace("No direct impact on JAX-RS Endpoints after change on element:" + elementKind);
				break;
			}
			break;
		case REMOVED:
			switch (elementKind) {
			case APPLICATION:
				processRemoval((IJaxrsApplication) element);
				break;
			case HTTP_METHOD:
				processRemoval((JaxrsHttpMethod) element);
				break;
			case RESOURCE_METHOD:
				processRemoval((JaxrsResourceMethod) element);
				break;
			case RESOURCE_FIELD:
				processRemoval((JaxrsResourceElement<?>) element, flags);
				break;
			case RESOURCE_PROPERTY:
				processRemoval((JaxrsResourceProperty) element, flags);
				break;
			case PARAMETER_AGGREGATOR_FIELD:
				processRemoval((JaxrsParameterAggregatorField) element, flags);
				break;
			case PARAMETER_AGGREGATOR_PROPERTY:
				processRemoval((JaxrsParameterAggregatorProperty) element, flags);
				break;
			default:
				Logger.trace("No direct impact on JAX-RS Endpoints after change on element:" + elementKind);
				break;
			}
		}
	}

	/**
	 * Process changes in the JAX-RS Metamodel when a new Application element is
	 * added. There should be only one, though...
	 * 
	 * @param application
	 * @return
	 */
	private static void processAddition(final IJaxrsApplication application) {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		// if the given application becomes the used application in the
		// metamodel
		if (application.equals(metamodel.findApplication())) {
			for (Iterator<IJaxrsEndpoint> iterator = metamodel.getAllEndpoints().iterator(); iterator.hasNext();) {
				JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
				endpoint.update(application);
			}
		}
	}

	private static void processAddition(final JaxrsHttpMethod httpMethod) {
		return;
	}

	private static void processAddition(final JaxrsResourceMethod resourceMethod) throws CoreException {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) resourceMethod.getMetamodel();
		final JaxrsResource resource = resourceMethod.getParentResource();
		if (resource == null) {
			Logger.warn("Found an orphan resource method: " + resourceMethod);
		} else {
			switch(resourceMethod.getElementKind()) {
			case RESOURCE_METHOD:
			case SUBRESOURCE_METHOD:
				if(resource.isRootResource()) {
					processRootResourceMethodAddition(resourceMethod);
				} else {
					processSubresourceMethodAddition(resourceMethod, metamodel);
				}
				break;
			case SUBRESOURCE_LOCATOR:
				// FIXME : support multiple levels of subresource locators
				if(resource.isRootResource()) {
					processSubresourceLocatorAddition(resourceMethod, metamodel);
				}
				break;
			default:
				break;
			}
		}
	}

	private static void processAddition(final JaxrsResourceElement<?> resourceElement) throws CoreException {
		final JaxrsResource resource = resourceElement.getParentResource();
		if (resource == null) {
			Logger.warn("Found an orphan resource element: " + resourceElement);
		} else {
			final Set<JaxrsEndpoint> resourceEndpoints = resource.getMetamodel().findEndpoints(resource);
			final Flags flags = computeAnnotationChangeFlags(resourceElement);
			if(flags.hasValue()) {
				for(JaxrsEndpoint endpoint : resourceEndpoints) {
					endpoint.update(flags);
				}
			}
		}
	}

	private static void processAddition(final JaxrsParameterAggregatorElement<?> aggregatorElement, final Flags flags) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = aggregatorElement.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan aggregator element: " + aggregatorElement);
		} else {
			processCascadeChange(aggregatorElement, parentAggregator, flags);
		}
	}
	
	private static void processChange(final JaxrsParameterAggregatorElement<?> aggregatorElement, final Flags flags) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = aggregatorElement.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan aggregator element: " + aggregatorElement);
		} else {
			processCascadeChange(aggregatorElement, parentAggregator, flags);
		}
	}
	
	private static void processRemoval(final JaxrsParameterAggregatorElement<?> aggregatorElement, final Flags flags) throws CoreException {
		final JaxrsParameterAggregator parentAggregator = aggregatorElement.getParentParameterAggregator();
		if (parentAggregator == null) {
			Logger.warn("Found an orphan aggregator element: " + aggregatorElement);
		} else {
			processCascadeChange(aggregatorElement, parentAggregator, flags);
		}
	}
	
	/**
	 * @param changedElement the element that changed
	 * @return the {@link Flags} that describe the kind of annotation change that occurred
	 */
	public static Flags computeAnnotationChangeFlags(final IAnnotatedSourceType changedElement) {
		final Flags flags = new Flags();
		for(Entry<String, Annotation> entry : changedElement.getAnnotations().entrySet()) {
			if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.QUERY_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_QUERY_PARAM_ANNOTATION);
			} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.MATRIX_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_MATRIX_PARAM_ANNOTATION);
			} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.PATH_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_PATH_PARAM_ANNOTATION);
			} else if(entry.getValue().getFullyQualifiedName().equals(JaxrsClassnames.BEAN_PARAM)) {
				flags.addFlags(JaxrsElementDelta.F_BEAN_PARAM_ANNOTATION);
			}
		}
		return flags;
	}
	
	private static void processCascadeChange(final IAnnotatedSourceType parameterAggregatorElement, final JaxrsParameterAggregator parentAggregator, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = parentAggregator.getMetamodel();
		final List<IType> knownTypes = metamodel.getAllJavaElements(IJavaElement.TYPE);
		final Set<IType> relatedTypes = JavaElementsSearcher.findRelatedTypes(parentAggregator.getJavaElement(), knownTypes, null);
		final Set<JaxrsEndpoint> resourceEndpoints = new HashSet<JaxrsEndpoint>();
		for(IType relatedType : relatedTypes) {
			resourceEndpoints.addAll(parentAggregator.getMetamodel().findEndpoints(relatedType));
		}
		if(flags.hasValue()) {
			for(JaxrsEndpoint endpoint : resourceEndpoints) {
				endpoint.update(flags);
			}
		}
	}

	private static void processRootResourceMethodAddition(final JaxrsResourceMethod resourceMethod)
			throws CoreException {
		JaxrsEndpointFactory.createEndpoints(resourceMethod);
	}

	// FIXME: support chain of subresource locators
	private static void processSubresourceLocatorAddition(final JaxrsResourceMethod subresourceLocator,
			final JaxrsMetamodel metamodel) throws CoreException {
		JaxrsEndpointFactory.createEndpointsFromSubresourceLocator(subresourceLocator);
	}
	
	private static void processSubresourceMethodAddition(final JaxrsResourceMethod resourceMethod,
			final JaxrsMetamodel metamodel) throws CoreException {
		JaxrsEndpointFactory.createEndpointsFromSubresourceMethod(resourceMethod);
	}

	private static void processChange(final IJaxrsApplication application, final Flags flags) {
		final JaxrsMetamodel metamodel = (JaxrsMetamodel) application.getMetamodel();
		if (application.equals(metamodel.findApplication())) {
			for (Iterator<IJaxrsEndpoint> iterator = metamodel.getAllEndpoints().iterator(); iterator.hasNext();) {
				final JaxrsEndpoint endpoint = (JaxrsEndpoint) iterator.next();
				endpoint.update(application);
			}
		}
	}

	private static void processChange(final JaxrsHttpMethod httpMethod, final Flags flags) {
		final Set<JaxrsEndpoint> endpoints = ((JaxrsMetamodel) httpMethod.getMetamodel()).findEndpoints(httpMethod);
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.update(httpMethod);
		}
	}

	private static void processChange(final JaxrsResource resource, final Flags flags) throws CoreException {
		// no structural change in the resource: refresh its methods
		if (!flags.hasValue(F_ELEMENT_KIND)) {
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				processChange(resourceMethod, flags);
			}
		}
		// structural change : remove all endpoints associated with its methods
		// and create new ones
		else {
			for (JaxrsResourceMethod resourceMethod : resource.getMethods().values()) {
				processRemoval(resourceMethod);
				processAddition(resourceMethod);
			}
		}
	}

	private static void processChange(final JaxrsResourceMethod changedResourceMethod, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = changedResourceMethod.getMetamodel();
		if (flags.hasValue(F_ELEMENT_KIND)
				|| (changedResourceMethod.getElementKind() == EnumElementKind.SUBRESOURCE_LOCATOR && flags
						.hasValue(F_METHOD_RETURN_TYPE))) {
			// remove endpoints using this resoureMethod:
			metamodel.removeEndpoints(changedResourceMethod);
			// create endpoints using this resourceMethod:
			processAddition(changedResourceMethod);
		}
		// simply refresh all endpoints using this resourceMethod
		else {
			final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(changedResourceMethod);
			for (JaxrsEndpoint endpoint : endpoints) {
				endpoint.update(changedResourceMethod, flags);
			}
		}
	}

	private static void processChange(final JaxrsResourceElement<?> changedResourceElement, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = changedResourceElement.getMetamodel();
		final Collection<JaxrsEndpoint> endpoints = metamodel.findEndpoints(changedResourceElement.getParentResource());
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.update(flags);
		}
	}
	
	private static void processRemoval(final JaxrsHttpMethod httpMethod) {
		httpMethod.getMetamodel().removeEndpoints(httpMethod);
	}

	private static void processRemoval(final IJaxrsApplication application) {
		final Collection<JaxrsEndpoint> endpoints = ((JaxrsMetamodel) application.getMetamodel()).findEndpoints(application);
		for (JaxrsEndpoint endpoint : endpoints) {
			endpoint.remove(application);
		}
	}

	private static void processRemoval(final JaxrsResourceMethod resourceMethod) {
		final JaxrsMetamodel metamodel = resourceMethod.getMetamodel();
		if(metamodel != null) {
			metamodel.removeEndpoints(resourceMethod);
		}
	}

	private static void processRemoval(final JaxrsResourceElement<?> resourceElement, final Flags flags) throws CoreException {
		final JaxrsMetamodel metamodel = resourceElement.getMetamodel();
		if(metamodel != null) {
			final Collection<JaxrsEndpoint> affectedEndpoints = metamodel.findEndpoints(resourceElement.getParentResource());
			if(flags.hasValue()) {
				for(JaxrsEndpoint endpoint : affectedEndpoints) {
					endpoint.update(flags);
				}
			}
		}
	}

}
