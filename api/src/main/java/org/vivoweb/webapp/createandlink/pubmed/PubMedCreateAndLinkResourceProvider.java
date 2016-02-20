package org.vivoweb.webapp.createandlink.pubmed;

import com.google.gson.Gson;
import edu.cornell.mannlib.vitro.webapp.utils.http.HttpClientFactory;
import org.apache.axis.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.vivoweb.webapp.createandlink.Citation;
import org.vivoweb.webapp.createandlink.CreateAndLinkResourceProvider;
import org.vivoweb.webapp.createandlink.ExternalIdentifiers;
import org.vivoweb.webapp.createandlink.ResourceModel;
import org.vivoweb.webapp.createandlink.crossref.CrossrefCreateAndLinkResourceProvider;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class PubMedCreateAndLinkResourceProvider implements CreateAndLinkResourceProvider {
    public final static String PUBMED_ID_API = "http://www.ncbi.nlm.nih.gov/pmc/utils/idconv/v1.0/?format=json&ids=";
    @Override
    public String normalize(String id) {
        return id.trim();
    }

    @Override
    public String getLabel() {
        return "PubMed ID";
    }

    @Override
    public ExternalIdentifiers allExternalIDsForFind(String externalId) {
        ExternalIdentifiers ids = new ExternalIdentifiers();
        ids.PubMedID = externalId;

        String json = readUrl(PUBMED_ID_API + externalId);
        Gson gson = new Gson();
        PubMedIDResponse response = gson.fromJson(json, PubMedIDResponse.class);
        if (response != null && !ArrayUtils.isEmpty(response.records)) {
            ids.DOI = response.records[0].doi;
            ids.PubMedCentralID = response.records[0].pmcid;
        }

        return ids;
    }

    @Override
    public String findInExternal(String id, Citation citation) {
        ExternalIdentifiers ids = allExternalIDsForFind(id);
        if (ids != null && !StringUtils.isEmpty(ids.DOI)) {
            CreateAndLinkResourceProvider crossrefProvider = new CrossrefCreateAndLinkResourceProvider();
            String result = crossrefProvider.findInExternal(ids.DOI, citation);
            if (!StringUtils.isEmpty(result)) {
                return result;
            }
        }

        return null;
    }

    @Override
    public ResourceModel makeResourceModel(String externalResource) {
        CreateAndLinkResourceProvider crossrefProvider = new CrossrefCreateAndLinkResourceProvider();
        return crossrefProvider.makeResourceModel(externalResource);
    }

    private String readUrl(String url) {
        try {
            HttpClient client = HttpClientFactory.getHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            try (InputStream in = response.getEntity().getContent()) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(in, writer, "UTF-8");
                return writer.toString();
            }
        } catch (IOException e) {
        }

        return null;
    }

    private static class PubMedIDResponse {
        public String status;
        public String responseDate;
        public String request;
        public String warning;

        public PubMedIDRecord[] records;

        public static class PubMedIDRecord {
            String pmcid;
            String pmid;
            String doi;

            // Don't need versions
        }
    }
}
