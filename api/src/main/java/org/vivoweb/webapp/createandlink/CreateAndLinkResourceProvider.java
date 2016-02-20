package org.vivoweb.webapp.createandlink;

import com.hp.hpl.jena.rdf.model.Model;
import org.vivoweb.webapp.controller.freemarker.CreateAndLinkResourceController;

public interface CreateAndLinkResourceProvider {
    public String normalize(String id);

    public String getLabel();

    public ExternalIdentifiers allExternalIDsForFind(String externalId);

    public String findInExternal(String id, Citation citation);

    public ResourceModel makeResourceModel(String externalResource);
}
