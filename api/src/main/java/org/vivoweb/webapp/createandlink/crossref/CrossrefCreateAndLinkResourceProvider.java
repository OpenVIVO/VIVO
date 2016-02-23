package org.vivoweb.webapp.createandlink.crossref;

import org.vivoweb.webapp.createandlink.Citation;
import org.vivoweb.webapp.createandlink.CreateAndLinkResourceProvider;
import org.vivoweb.webapp.createandlink.ExternalIdentifiers;
import org.vivoweb.webapp.createandlink.ResourceModel;

public class CrossrefCreateAndLinkResourceProvider implements CreateAndLinkResourceProvider {
    @Override
    public String normalize(String id) {
        if (id != null) {
            String doiTrimmed = id.trim().toLowerCase();

            if (doiTrimmed.startsWith("https://dx.doi.org/")) {
                return doiTrimmed.substring(19);
            } else if (doiTrimmed.startsWith("http://dx.doi.org/")) {
                return doiTrimmed.substring(18);
            }

            return doiTrimmed;
        }

        return null;
    }

    @Override
    public String getLabel() {
        return "DOI";
    }

    @Override
    public ExternalIdentifiers allExternalIDsForFind(String externalId) {
        ExternalIdentifiers ids = new ExternalIdentifiers();
        ids.DOI = externalId;
        return ids;
    }

    @Override
    public String findInExternal(String id, Citation citation) {
        String json = null;

        CrossrefResolverAPI resolverAPI = new CrossrefResolverAPI();
        json = resolverAPI.findInExternal(id, citation);

        if (json == null) {
            CrossrefNativeAPI nativeAPI = new CrossrefNativeAPI();
            json = nativeAPI.findInExternal(id, citation);
        }

        return json;
    }

    @Override
    public ResourceModel makeResourceModel(String externalId, String externalResource) {
        ResourceModel resourceModel = null;

        CrossrefResolverAPI resolverAPI = new CrossrefResolverAPI();
        resourceModel = resolverAPI.makeResourceModel(externalResource);

        if (resourceModel == null) {
            CrossrefNativeAPI nativeAPI = new CrossrefNativeAPI();
            resourceModel =  nativeAPI.makeResourceModel(externalResource);
        }

        return resourceModel;
    }
}
