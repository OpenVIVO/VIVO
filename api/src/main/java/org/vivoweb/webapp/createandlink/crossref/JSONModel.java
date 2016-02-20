package org.vivoweb.webapp.createandlink.crossref;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class JSONModel {
    public String DOI;
    public String[] ISSN;
    public String URL;

    public Author[] author;

//    @SerializedName("alternative-id")
//    public String[] alternativeId;

    @SerializedName("container-title")
    public String containerTitle;
    public DateField created;
    public DateField deposited;
    public DateField indexed;
    public String issue;
    public DateField issued;

    // TODO
    // license
    // link

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
    public String source;
    public String[] subject;
    public String[] subtitle;
    public String title;
    public String type;
    public String volume;

    public static class Author {
        public String[] affiliation;
        public String family;
        public String given;
        public String literal;
    }

    public static class DateField {
        @SerializedName("date-parts")
        public Integer[][] dateParts;

        @SerializedName("date-time")
        public Date dateTime;

        public Long timestamp;
    }
}
