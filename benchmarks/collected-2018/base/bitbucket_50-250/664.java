// https://searchcode.com/api/result/55760880/

package org.bytesparadise.tools.jaxrs.registry.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.bytesparadise.tools.common.service.IJdtUtilsService;
import org.bytesparadise.tools.jaxrs.registry.service.IJaxrsAnnotationsScanner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.BinaryType;

/**
 * Class that scan the projec's classpath to find JAX-RS Resources and Providers
 * 
 */
@SuppressWarnings("restriction")
public class JaxrsAnnotationsScannerImpl implements IJaxrsAnnotationsScanner {

	private IJdtUtilsService jdtUtilsService = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bytesparadise.tools.resteasy.registry.service.impl.
	 * IJaxrsAnnotationsScanner#getProviders(org.eclipse.jdt.core.IJavaProject)
	 */
	public List<IType> getProviders(IJavaProject javaProject, boolean includeLibraries, IProgressMonitor progressMonitor)
			throws CoreException {

		IJavaSearchScope searchScope = null;
		if (includeLibraries) {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject },
					IJavaSearchScope.SOURCES | IJavaSearchScope.APPLICATION_LIBRARIES
							| IJavaSearchScope.REFERENCED_PROJECTS);
		} else {
			searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject },
					IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS);
		}
		// IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new
		// IJavaElement[] { javaProject });

		return searchForAnnotatedTypes("javax.ws.rs.ext.Provider", searchScope, progressMonitor);
	}

	public boolean isProvider(IType type, IProgressMonitor progressMonitor) throws CoreException {
		// returns true if the annotation exists and is valid.
		return (jdtUtilsService.getAnnotation(type, "javax.ws.rs.ext.Provider") != null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bytesparadise.tools.resteasy.registry.service.impl.
	 * IJaxrsAnnotationsScanner
	 * #getRootResources(org.eclipse.jdt.core.IJavaProject)
	 */
	public List<IType> getRootResources(IJavaProject javaProject, IProgressMonitor progressMonitor)
			throws CoreException {
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { javaProject },
				IJavaSearchScope.SOURCES | IJavaSearchScope.REFERENCED_PROJECTS);
		List<IType> resources = searchForAnnotatedTypes("javax.ws.rs.Path", searchScope, progressMonitor);
		for (IType resource : resources) {
			if (resource instanceof BinaryType) {
				resources.remove(resource);
			}
		}
		return resources;
	}

	public boolean isRootResource(IType type, IProgressMonitor progressMonitor) throws CoreException {
		// returns true if the annotation exists and is valid.
		return (jdtUtilsService.getAnnotation(type, "javax.ws.rs.Path") != null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.bytesparadise.tools.resteasy.registry.service.impl.
	 * IJaxrsAnnotationsScanner
	 * #getHttpMethods(org.eclipse.jdt.core.IJavaProject)
	 */
	public List<IType> getHttpMethods(IJavaProject project, IProgressMonitor progressMonitor) throws CoreException {
		IJavaSearchScope searchScope = SearchEngine.createJavaSearchScope(new IJavaElement[] { project });
		return searchForAnnotatedTypes("javax.ws.rs.HttpMethod", searchScope, progressMonitor);
	}

	public boolean isHttpMethod(IType type, IProgressMonitor progressMonitor) throws CoreException {
		// returns true if the annotation exists and is valid.
		return (jdtUtilsService.getAnnotation(type, "javax.ws.rs.HttpMethod") != null);
	}

	/**
	 * @param searchScope
	 * @param annotationName
	 * @return
	 * @throws CoreException
	 */
	protected List<IType> searchForAnnotatedTypes(final String annotationName, IJavaSearchScope searchScope,
			IProgressMonitor progressMonitor) throws CoreException {
		final List<IType> resources = new ArrayList<IType>();
		SearchRequestor requestor = new SearchRequestor() {
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				Object element = match.getElement();
				// FIXME : search result also includes methods, but there is no
				// need for such elements in the current cases.
				if (element instanceof IType) {
					IType type = (IType) element;
					// if (type.getAnnotation(annotationName).exists()) {
					resources.add(type);
					// }
				}
			}
		};
		// FIXME : reduce the search scope to Type objects only...
		SearchPattern pattern = SearchPattern.createPattern(annotationName, IJavaSearchConstants.ANNOTATION_TYPE,
				IJavaSearchConstants.ANNOTATION_TYPE_REFERENCE | IJavaSearchConstants.TYPE, SearchPattern.R_EXACT_MATCH
						| SearchPattern.R_CASE_SENSITIVE);
		// perform search, results are added/filtered by the custom
		// searchRequestor defined above
		new SearchEngine().search(pattern, new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() },
				searchScope, requestor, progressMonitor);
		return resources;
	}

	public void unbindJdtUtilService() {
		jdtUtilsService = null;
	}

	public void bindJdtUtilService(IJdtUtilsService jdtUtilsService) {
		this.jdtUtilsService = jdtUtilsService;
	}

}

