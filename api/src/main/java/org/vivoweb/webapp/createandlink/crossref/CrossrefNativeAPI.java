package org.vivoweb.webapp.createandlink.crossref;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import edu.cornell.mannlib.vitro.webapp.utils.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.vivoweb.webapp.createandlink.Citation;
import org.vivoweb.webapp.createandlink.CreateAndLinkUtils;
import org.vivoweb.webapp.createandlink.ResourceModel;
import org.vivoweb.webapp.createandlink.utils.HttpReader;
import org.vivoweb.webapp.createandlink.utils.StringArrayAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Interface to CrossRef's native API
 */
public class CrossrefNativeAPI {
    // API endpoint address
    private static final String CROSSREF_API      = "http://api.crossref.org/works/";

    /**
     * Find the DOI in CrossRef, filling the citation object
     *
     * @param id
     * @param citation
     * @return
     */
    public String findInExternal(String id, Citation citation) {
        // Get JSON from the CrossRef API
        String json = readUrl(CROSSREF_API + id);

        if (StringUtils.isEmpty(json)) {
            return null;
        }

        // Use GSON to parse the JSON into a Java object
        Gson gson = new Gson();
        CrossrefResponse response = gson.fromJson(json, CrossrefResponse.class);
        if (response == null || response.message == null) {
            return null;
        }

        // The CrossRef API sometimes gives a false record when the DOI deosn't exist
        // So ensure that the response we got contains the DOI we asked for
        if (!id.equalsIgnoreCase(response.message.DOI)) {
            return null;
        }

        // Map the fields from the CrossRef response to the Citation object

        citation.DOI = id;
        citation.type = normalizeType(response.message.type);

        if (!ArrayUtils.isEmpty(response.message.title)) {
            citation.title = response.message.title[0];
        }

        if (!ArrayUtils.isEmpty(response.message.containerTitle)) {
            for (String journal : response.message.containerTitle) {
                if (citation.journal == null || citation.journal.length() < journal.length()) {
                    citation.journal = journal;
                }
            }
        }

        if (response.message.author != null) {
            List<Citation.Name> authors = new ArrayList<>();
            for (CrossrefResponse.ResponseModel.Author author : response.message.author) {
                Citation.Name citationAuthor = new Citation.Name();
                citationAuthor.name = CreateAndLinkUtils.formatAuthorString(author.family, author.given);
                authors.add(citationAuthor);
            }
            citation.authors = authors.toArray(new Citation.Name[authors.size()]);
        }

        citation.volume = response.message.volume;
        citation.issue = response.message.issue;
        citation.pagination = response.message.page;
        if (citation.pagination == null) {
            citation.pagination = response.message.articleNumber;
        }

        citation.publicationYear = extractYearFromDateField(response.message.publishedPrint);
        if (citation.publicationYear == null) {
            citation.publicationYear = extractYearFromDateField(response.message.publishedOnline);
        }

        return json;
    }

    /**
     * Retrieve the year from a compound date field
     *
     * @param date
     * @return
     */
    private Integer extractYearFromDateField(CrossrefResponse.ResponseModel.DateField date) {
        if (date == null) {
            return null;
        }

        if (ArrayUtils.isEmpty(date.dateParts)) {
            return null;
        }

        return date.dateParts[0][0];
    }

    /**
     * Create a full resource model from the external resource (JSON)
     * @param externalResource
     * @return
     */
    public ResourceModel makeResourceModel(String externalResource) {
        if (StringUtils.isEmpty(externalResource)) {
            return null;
        }

        // Use GSON to parse the JSON into a Java object
        Gson gson = new Gson();
        CrossrefResponse response = gson.fromJson(externalResource, CrossrefResponse.class);
        if (response == null || response.message == null) {
            return null;
        }

        if (StringUtils.isEmpty(response.message.DOI)) {
            return null;
        }

        // Map the fields from the CrossRef response to the resource model

        ResourceModel model = new ResourceModel();

        model.DOI = response.message.DOI;
        model.ISSN = response.message.ISSN;
        model.URL = response.message.URL;

        if (response.message.author != null && response.message.author.length > 0) {
            model.author = new ResourceModel.NameField[response.message.author.length];
            for (int authIdx = 0; authIdx < response.message.author.length; authIdx++) {
                if (response.message.author[authIdx] != null) {
                    model.author[authIdx] = new ResourceModel.NameField();
                    model.author[authIdx].family = response.message.author[authIdx].family;
                    model.author[authIdx].given = response.message.author[authIdx].given;
                }
            }
        }


        if (response.message.containerTitle != null && response.message.containerTitle.length > 0) {
            String journalName = null;
            for (String container : response.message.containerTitle) {
                if (journalName == null || container.length() > journalName.length()) {
                    journalName = container;
                }
            }
            model.containerTitle = journalName;
        }

        model.issue = response.message.issue;

        if (!StringUtils.isEmpty(response.message.page)) {
            if (response.message.page.contains("-")) {
                int hyphen = response.message.page.indexOf('-');
                model.pageStart = response.message.page.substring(0, hyphen);
                model.pageEnd = response.message.page.substring(hyphen + 1);
            } else {
                model.pageStart = response.message.page;
            }
        } else if (!StringUtils.isEmpty(response.message.articleNumber)) {
            model.pageStart = response.message.articleNumber;
        }

        model.publicationDate = convertDateField(response.message.publishedPrint);
        if (model.publicationDate == null) {
            model.publicationDate = convertDateField(response.message.publishedOnline);
        }

        model.publisher = response.message.publisher;
        model.subject = response.message.subject;
        if (response.message.title != null && response.message.title.length > 0) {
            model.title = response.message.title[0];
        }

        model.type = normalizeType(response.message.type);
        model.volume = response.message.volume;

        return model;
    }

    /**
     * Map non-standard publication types into the CiteProc types
     *
     * @param type
     * @return
     */
    private String normalizeType(String type) {
        if (type != null) {
            switch (type.toLowerCase()) {
                case "journal-article":
                    return "article-journal";

                case "book-chapter":
                    return "chapter";

                case "proceedings-article":
                    return "paper-conference";
            }
        }

        return type;
    }

    /**
     * Convert a date field from the CrossRef response to the internal resource model format
     *
     * @param dateField
     * @return
     */
    private ResourceModel.DateField convertDateField(CrossrefResponse.ResponseModel.DateField dateField) {
        if (dateField != null) {
            ResourceModel.DateField resourceDate = new ResourceModel.DateField();
            if (dateField.dateParts != null && dateField.dateParts.length > 0 && dateField.dateParts[0].length > 0) {
                if (dateField.dateParts.length == 1) {
                    resourceDate.year = dateField.dateParts[0][0];
                } else if (dateField.dateParts.length == 2) {
                    resourceDate.year = dateField.dateParts[0][0];
                    resourceDate.month = dateField.dateParts[0][1];
                } else {
                    resourceDate.year = dateField.dateParts[0][0];
                    resourceDate.month = dateField.dateParts[0][1];
                    resourceDate.day = dateField.dateParts[0][2];
                }
            }
            return resourceDate;
        }

        return null;
    }

    /**
     * Read JSON from the given URL
     *
     * @param url
     * @return
     */
    private String readUrl(String url) {
        try {
            HttpClient client = HttpClientFactory.getHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            return HttpReader.fromResponse(response);
        } catch (IOException e) {
        }

        return null;
    }

    /**
     * Java object representation of the JSON returned by CrossRef
     */
    private static class CrossrefResponse {
        public ResponseModel message;

        @SerializedName("message-type")
        public String messageType;

        @SerializedName("message-version")
        public String messageVersion;

        public String status;

        public static class ResponseModel {
            public String DOI;
            @JsonAdapter(StringArrayAdapter.class)
            public String[] ISSN;
            public String URL;

            @SerializedName("alternative-id")
            @JsonAdapter(StringArrayAdapter.class)
            public String[] alternativeId;

            public Author[] author;

            @SerializedName("container-title")
            @JsonAdapter(StringArrayAdapter.class)
            public String[] containerTitle;
            public DateField created;
            public DateField deposited;
            public DateField indexed;
            public String issue;
            public DateField issued;
            public String member;
            public String page;
            public String prefix;

            @SerializedName("article-number")
            public String articleNumber;

            @SerializedName("published-online")
            public DateField publishedOnline;

            @SerializedName("published-print")
            public DateField publishedPrint;

            public String publisher;

            @SerializedName("reference-count")
            public Integer referenceCount;
            public Double score;
            @JsonAdapter(StringArrayAdapter.class)
            public String[] subject;
            @JsonAdapter(StringArrayAdapter.class)
            public String[] subtitle;
            @JsonAdapter(StringArrayAdapter.class)
            public String[] title;
            public String type;
            public String volume;


            public static class Author {
                @JsonAdapter(StringArrayAdapter.class)
                public String[] affiliation;
                public String family;
                public String given;
            }

            public static class DateField {
                @SerializedName("date-parts")
                public Integer[][] dateParts;

                @SerializedName("date-time")
                public Date dateTime;

                public Long timestamp;
            }
        }
    }
}
