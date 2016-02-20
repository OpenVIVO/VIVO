package org.vivoweb.webapp.createandlink.crossref;

import com.google.gson.Gson;
import edu.cornell.mannlib.vitro.webapp.utils.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.vivoweb.webapp.createandlink.Citation;
import org.vivoweb.webapp.createandlink.CreateAndLinkUtils;
import org.vivoweb.webapp.createandlink.ResourceModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class CrossrefResolverAPI {
    private static final String CROSSREF_RESOLVER = "http://dx.doi.org/";

    public String findInExternal(String id, Citation citation) {
        String json = readJSON(CROSSREF_RESOLVER + id);
        // readUrl(CROSSREF_API + id);

        Gson gson = new Gson();
        JSONModel jsonModel = gson.fromJson(json, JSONModel.class);
        if (jsonModel == null) {
            return null;
        }

        if (!id.equalsIgnoreCase(jsonModel.DOI)) {
            return null;
        }

        citation.DOI = id;
        citation.title = jsonModel.title;
        citation.journal = jsonModel.containerTitle;

        List<Citation.Author> authors = new ArrayList<>();
        for (JSONModel.Author author : jsonModel.author ) {
            splitAuthorLiteral(author);
            Citation.Author citationAuthor = new Citation.Author();
            citationAuthor.name = CreateAndLinkUtils.formatAuthorString(author.family, author.given);
            authors.add(citationAuthor);
        }
        citation.authors = authors.toArray(new Citation.Author[authors.size()]);

        citation.volume = jsonModel.volume;
        citation.issue = jsonModel.issue;
        citation.pagination = jsonModel.page;
        if (citation.pagination == null) {
            citation.pagination = jsonModel.articleNumber;
        }

        citation.publicationYear = extractYearFromDateField(jsonModel.publishedPrint);
        if (citation.publicationYear == null) {
            citation.publicationYear = extractYearFromDateField(jsonModel.publishedOnline);
        }


//        messageMap.get("type"); // String

        return json;
    }

    private Integer extractYearFromDateField(JSONModel.DateField date) {
        if (date == null) {
            return null;
        }

        if (ArrayUtils.isEmpty(date.dateParts)) {
            return null;
        }

        return date.dateParts[0][0];
    }

    public ResourceModel makeResourceModel(String externalResource) {
        Gson gson = new Gson();
        JSONModel jsonModel = gson.fromJson(externalResource, JSONModel.class);
        if (jsonModel == null) {
            return null;
        }

        ResourceModel model = new ResourceModel();

        model.DOI = jsonModel.DOI;
        model.ISSN = jsonModel.ISSN;
        model.URL = jsonModel.URL;

        if (jsonModel.author != null && jsonModel.author.length > 0) {
            model.author = new ResourceModel.Author[jsonModel.author.length];
            for (int authIdx = 0; authIdx < jsonModel.author.length; authIdx++) {
                if (jsonModel.author[authIdx] != null) {
                    splitAuthorLiteral(jsonModel.author[authIdx]);
                    model.author[authIdx] = new ResourceModel.Author();
                    model.author[authIdx].family = jsonModel.author[authIdx].family;
                    model.author[authIdx].given = jsonModel.author[authIdx].given;
                }
            }
        }

        model.containerTitle = jsonModel.containerTitle;

        model.created = convertDateField(jsonModel.created);

        model.issue = jsonModel.issue;

        if (!StringUtils.isEmpty(jsonModel.page)) {
            if (jsonModel.page.contains("-")) {
                int hyphen = jsonModel.page.indexOf('-');
                model.pageStart = jsonModel.page.substring(0, hyphen - 1);
                model.pageEnd = jsonModel.page.substring(hyphen + 1);
            } else {
                model.pageStart = jsonModel.page;
            }
        } else if (!StringUtils.isEmpty(jsonModel.articleNumber)) {
            model.pageStart = jsonModel.articleNumber;
        }

        model.publishedOnline = convertDateField(jsonModel.publishedOnline);
        model.publishedPrint = convertDateField(jsonModel.publishedPrint);

        model.publisher = jsonModel.publisher;
        model.subject = jsonModel.subject;
        model.title = jsonModel.title;
        model.type = jsonModel.type;
        model.volume = jsonModel.volume;

        return model;
    }

    private void splitAuthorLiteral(JSONModel.Author author) {
        if (StringUtils.isEmpty(author.family) && StringUtils.isEmpty(author.given)) {
            if (!StringUtils.isEmpty(author.literal)) {
                if (author.literal.contains(",")) {
                    author.family = author.literal.substring(0, author.literal.indexOf(','));
                    author.given = author.literal.substring(author.literal.indexOf(',') + 1);
                } else if (author.literal.lastIndexOf(' ') > -1) {
                    author.family = author.literal.substring(author.literal.lastIndexOf(' ') + 1);
                    author.given = author.literal.substring(0, author.literal.lastIndexOf(' '));
                }
            }
        }
    }

    private ResourceModel.DateField convertDateField(JSONModel.DateField dateField) {
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

    private String readJSON(String url) {
        try {
            HttpClient client = HttpClientFactory.getHttpClient();
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/vnd.citationstyles.csl+json;q=1.0");
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
}
