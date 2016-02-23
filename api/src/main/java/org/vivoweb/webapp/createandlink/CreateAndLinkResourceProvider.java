package org.vivoweb.webapp.createandlink;

public interface CreateAndLinkResourceProvider {
    public String normalize(String id);

    public String getLabel();

    public ExternalIdentifiers allExternalIDsForFind(String externalId);

    public String findInExternal(String id, Citation citation);

    public ResourceModel makeResourceModel(String externalId, String externalResource);
}
