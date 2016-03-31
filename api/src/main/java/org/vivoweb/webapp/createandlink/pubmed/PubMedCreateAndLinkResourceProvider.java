package org.vivoweb.webapp.createandlink.pubmed;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

public class PubMedCreateAndLinkResourceProvider implements CreateAndLinkResourceProvider {
    public final static String PUBMED_ID_API = "http://www.ncbi.nlm.nih.gov/pmc/utils/idconv/v1.0/?format=json&ids=";
    public final static String PUBMED_SUMMARY_API = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=json&tool=my_tool&email=my_email@example.com&id=";

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
        String json = readUrl(PUBMED_SUMMARY_API + id);

        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement tree = parser.parse(json);
        if (tree != null) {
            JsonObject object = tree.getAsJsonObject();
            JsonObject result = object.getAsJsonObject("result");
            JsonObject data = result.getAsJsonObject(id);

            PubMedSummaryResponse response = gson.fromJson(data, PubMedSummaryResponse.class);
            if (response != null) {
                citation.title = response.title;
                citation.authors = new Citation.Name[response.authors.length];
                for (int idx = 0; idx < response.authors.length; idx++) {
                    citation.authors[idx] = new Citation.Name();
                    citation.authors[idx].name = response.authors[idx].name;
                }
                citation.journal = response.fulljournalname;
                citation.volume = response.volume;
                citation.issue = response.issue;
                citation.pagination = response.pages;
                if (!StringUtils.isEmpty(response.pubdate) && response.pubdate.length() >= 4) {
                    citation.publicationYear = Integer.parseInt(response.pubdate.substring(0, 4), 10);
                }

                citation.type = getCiteprocTypeForPubType(response.pubtype);

                return json;
            }
        }

        return null;
    }

    @Override
    public ResourceModel makeResourceModel(String externalId, String externalResource) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement tree = parser.parse(externalResource);
        if (tree != null) {
            JsonObject object = tree.getAsJsonObject();
            JsonObject result = object.getAsJsonObject("result");
            JsonObject data = result.getAsJsonObject(externalId);

            PubMedSummaryResponse response = gson.fromJson(data, PubMedSummaryResponse.class);
            if (response != null) {
                ResourceModel resourceModel = new ResourceModel();
                resourceModel.PubMedID = externalId;
                resourceModel.title = response.title;
                resourceModel.author = new ResourceModel.NameField[response.authors.length];
                for (int idx = 0; idx < response.authors.length; idx++) {
                    resourceModel.author[idx] = new ResourceModel.NameField();
                    if (response.authors[idx].name.lastIndexOf(' ') > 0) {
                        resourceModel.author[idx].family = response.authors[idx].name.substring(0, response.authors[idx].name.lastIndexOf(' '));
                        resourceModel.author[idx].given = response.authors[idx].name.substring(response.authors[idx].name.lastIndexOf(' ') + 1);
                    } else {
                        resourceModel.author[idx].family = response.authors[idx].name;
                    }
                }

                resourceModel.containerTitle = response.fulljournalname;
                if (!StringUtils.isEmpty(response.issn)) {
                    resourceModel.ISSN = new String[1];
                    resourceModel.ISSN[0] = response.issn;
                } else if (!StringUtils.isEmpty(response.eissn)) {
                    resourceModel.ISSN = new String[1];
                    resourceModel.ISSN[0] = response.eissn;
                }

                resourceModel.volume = response.volume;
                resourceModel.issue = response.issue;
                if (response.pages.contains("-")) {
                    int hyphen = response.pages.indexOf('-');
                    resourceModel.pageStart = response.pages.substring(0, hyphen);
                    resourceModel.pageEnd = response.pages.substring(hyphen + 1);
                } else {
                    resourceModel.pageStart = response.pages;
                }

                if (!StringUtils.isEmpty(response.pubdate) && response.pubdate.length() >= 4) {
                    resourceModel.publicationDate = new ResourceModel.DateField();
                    resourceModel.publicationDate.year = Integer.parseInt(response.pubdate.substring(0, 4), 10);
                }

                if (response.articleids != null) {
                    for (PubMedSummaryResponse.ArticleID articleID : response.articleids) {
                        if ("doi".equalsIgnoreCase(articleID.idtype)) {
                            resourceModel.DOI = articleID.value;
                        } else if ("pmc".equalsIgnoreCase(articleID.idtype)) {
                            resourceModel.PubMedCentralID = articleID.idtype;
                        }
                    }
                }

                resourceModel.type = getCiteprocTypeForPubType(response.pubtype);
                resourceModel.publisher = response.publishername;
                resourceModel.status = response.pubstatus;

/*
    public DateField created;
    public String[] subject;
    public String presentedAt;
    public String[] keyword;
    public String abstractText;
 */
                return resourceModel;
            }
        }

        return null;
    }

    private String getCiteprocTypeForPubType(String[] pubTypes) {
        if (pubTypes != null && pubTypes.length > 0) {
            for (String pubType : pubTypes) {
                switch (pubType) {
                    case "Journal Article":
                        return "article-journal";

                    case "Incunabula":
                    case "Monograph":
                    case "Textbooks":
                        return "book";

                    case "Dataset":
                        return "dataset";

                    case "Legal Cases":
                        return "legal_case";

                    case "Legislation":
                        return "legislation";

                    case "Manuscripts":
                        return "manuscript";

                    case "Maps":
                        return "map";

                    case "Meeting Abstracts":
                        return "paper-conference";

                    case "Patents":
                        return "patent";

                    case "Letter":
                        return "personal_communication";

                    case "Blogs":
                        return "post-weblog";

                    case "Review":
                        return "review";

                    case "Academic Dissertations":
                        return "thesis";
                }
            }
        }

        return "article-journal";
    }

    private String readUrl(String url) {
        try {
            HttpClient client = HttpClientFactory.getHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    try (InputStream in = response.getEntity().getContent()) {
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(in, writer, "UTF-8");
                        return writer.toString();
                    }
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

    private static class PubMedSummaryResponse {
        public String uid;
        public String pubdate;
        public String epubdate;
        public String source;
        public NameField[] authors;
        public String lastauthor;
        public String title;
        public String sorttitle;
        public String volume;
        public String issue;
        public String pages;
        public String[] lang;
        public String nlmuniqueid;
        public String issn;
        public String eissn;
        public String[] pubtype;
        public String recordstatus;
        public String pubstatus;
        public ArticleID[] articleids;
        public History[] history;
        //public String[] references;
        public String[] attributes;
        public Integer pmcrefcount;
        public String fulljournalname;
        public String elocationid;
        public Integer viewcount;
        public String doctype;
        //public String[] srccontriblist;
        public String booktitle;
        public String medium;
        public String edition;
        public String publisherlocation;
        public String publishername;
        public String srcdate;
        public String reportnumber;
        public String availablefromurl;
        public String locationlabel;
        //public String[] doccontriblist;
        public String docdate;
        public String bookname;
        public String chapter;
        public String sortpubdate;
        public String sortfirstauthor;
        public String vernaculartitle;

        public static class NameField {
            public String name;
            public String authtype;
            public String clusterid;
        }

        public static class ArticleID {
            public String idtype;
            public Integer idtypen;
            public String value;
        }

        public static class History {
            public String pubstatus;
            public String date;
        }
    }
}
