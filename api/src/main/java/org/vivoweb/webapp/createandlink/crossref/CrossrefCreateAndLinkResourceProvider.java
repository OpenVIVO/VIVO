package org.vivoweb.webapp.createandlink.crossref;

import org.vivoweb.webapp.createandlink.Citation;
import org.vivoweb.webapp.createandlink.CreateAndLinkResourceProvider;
import org.vivoweb.webapp.createandlink.ExternalIdentifiers;
import org.vivoweb.webapp.createandlink.ResourceModel;

/**
 * Provider for looking up DOIs in CrossRef
 */
public class CrossrefCreateAndLinkResourceProvider implements CreateAndLinkResourceProvider {
    /**
     * Make a normalized version of the ID
     *
     * @param id
     * @return
     */
    @Override
    public String normalize(String id) {
        if (id != null) {
            // Trim and lower case
            String doiTrimmed = id.trim().toLowerCase();

            // If we have been passed the resolver URI, strip it down to the bare DOI
            if (doiTrimmed.startsWith("https://dx.doi.org/")) {
                return doiTrimmed.substring(19);
            } else if (doiTrimmed.startsWith("http://dx.doi.org/")) {
                return doiTrimmed.substring(18);
            }

            return doiTrimmed;
        }

        return null;
    }

    /**
     * Label for the UI
     *
     * @return
     */
    @Override
    public String getLabel() {
        return "DOI";
    }

    /**
     * Resolve the DOI into other external identifiers
     *
     * @param externalId
     * @return
     */
    @Override
    public ExternalIdentifiers allExternalIDsForFind(String externalId) {
        // For now, just return the DOI
        ExternalIdentifiers ids = new ExternalIdentifiers();
        ids.DOI = externalId;
        return ids;
    }

    /**
     * Look up the DOI in CrossRef, and populate a citation object
     *
     * @param id
     * @param citation
     * @return
     */
    @Override
    public String findInExternal(String id, Citation citation) {
        String json = null;

        // Use content negotiation on the resolver API (wider variety of sources)
        CrossrefResolverAPI resolverAPI = new CrossrefResolverAPI();
        json = resolverAPI.findInExternal(id, citation);

        // If the content negotiation failed, use the CrossRef Native API
        if (json == null) {
            CrossrefNativeAPI nativeAPI = new CrossrefNativeAPI();
            json = nativeAPI.findInExternal(id, citation);
        }

        // Return the JSON fragment
        return json;
    }

    /**
     * Create an internmediate model of the external resource (JSON string)
     *
     * @param externalId
     * @param externalResource
     * @return
     */
    @Override
    public ResourceModel makeResourceModel(String externalId, String externalResource) {
        ResourceModel resourceModel = null;

        // Note that the external resource may be slightly different, depending on whether it came from
        // the resolver or native api

        // First, try the resolver API format to create the model
        CrossrefResolverAPI resolverAPI = new CrossrefResolverAPI();
        resourceModel = resolverAPI.makeResourceModel(externalResource);

        // Otherwise, try the native API format to create the model
        if (resourceModel == null) {
            CrossrefNativeAPI nativeAPI = new CrossrefNativeAPI();
            resourceModel =  nativeAPI.makeResourceModel(externalResource);
        }

        // Return the created resource model
        return resourceModel;
    }
}
