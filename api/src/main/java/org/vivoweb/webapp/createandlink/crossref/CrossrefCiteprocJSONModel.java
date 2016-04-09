package org.vivoweb.webapp.createandlink.crossref;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

/**
 * Note that ISSN and ISBN are arrays in Crossref, whereas Citeproc defines them to be a single value.
 *
 */
public class CrossrefCiteprocJSONModel {
    // Crossref Specific Fields

    public String[] ISSN;
    public String[] ISBN;

    public DateField created;
//    public DateField deposited;
//    public DateField indexed;

//    public String member;
    public String prefix;

    @SerializedName("article-number")
    public String articleNumber;

    @SerializedName("published-online")
    public DateField publishedOnline;

    @SerializedName("published-print")
    public DateField publishedPrint;

//    @SerializedName("reference-count")
//    public Integer referenceCount;
    public Double score;
    public String[] subject;
    public String[] subtitle;

    // Standard Citeproc fields

    public String type;
    public String id; // Number?
//    public String[] categories;
    public String language;
//    public String journalAbbreviation;
//    public String shortTitle;
    public NameField[] author;
//    @SerializedName("collection-editor")
//    public NameField[] collectionEditor;
//    public NameField[] composer;
//    @SerializedName("container-author")
//    public NameField[] containerAuthor;
//    public NameField[] director;
    public NameField[] editor;
    @SerializedName("editorial-director")
//    public NameField[] editorialDirector;
//    public NameField[] interviewer;
//    public NameField[] illustrator;
//    @SerializedName("original-author")
//    public NameField[] originalAuthor;
//    public NameField[] recipient;
//    @SerializedName("reviewed-author")
//    public NameField[] reviewedAuthor;
    public NameField[] translator;
//    public DateField accessed;
    public DateField container;
//    @SerializedName("event-date")
//    public DateField eventDate;
    public DateField issued;
//    @SerializedName("original-date")
//    public DateField originalDate;
    public DateField submitted;
    @SerializedName("abstract")
    public String abstractText;
//    public String annote;
//    public String archive;
//    public String archive_location;
//    public String authority;
//    @SerializedName("call-number")
//    public String callNumber;
//    @SerializedName("chapter-number")
//    public String chapterNumber;
//    @SerializedName("citation-number")
//    public String citationNumber;
//    @SerializedName("citation-label")
//    public String citationLabel;
//    @SerializedName("collection-number")
//    public String collectionNumber;
    @SerializedName("container-title")
    public String containerTitle;
//    @SerializedName("container-title-short")
//    public String containerTitleShort;
//    public String dimensions;
    public String DOI;
//    public String edition; // Integer?
    public String event;
//    @SerializedName("event-place")
//    public String eventPlace;
//    @SerializedName("first-reference-note-number")
//    public String firstReferenceNoteNumber;
//    public String genre;
    public String issue; // Integer?
//    public String jurisdiction;
//    public String keyword;
//    public String locator;
//    public String medium;
    public String note;
    public String number; // Integer?
//    @SerializedName("number-of-pages")
//    public String numberOfPages;
//    @SerializedName("number-of-volumes")
//    public String numberOfVolumes; // Integer?
//    @SerializedName("original-publisher")
//    public String originalPublisher;
//    @SerializedName("original-publisher-place")
//    public String originalPublisherPlace;
//    @SerializedName("original-title")
//    public String originalTitle;
    public String page;
//    @SerializedName("page-first")
//    public String pageFirst;
    public String PMCID;
    public String PMID;
    public String publisher;
//    @SerializedName("publisher-place")
//    public String publisherPlace;
//    public String references;
//    @SerializedName("reviewed-title")
//    public String reviewedTitle;
    public String scale;
    public String section;
    public String source;
    public String status;
    public String title;
//    @SerializedName("title-short")
//    public String titleShort;
    public String URL;
    public String version;
    public String volume; // Integer?
//    @SerializedName("year-suffix")
//    public String yearSuffix;

    public static class NameField {
        // Crossref specific fields

//        public String[] affiliation;

        // Standard Citeproc fields

        public String family;
        public String given;
//        @SerializedName("dropping-particle")
//        public String droppingParticle;
//        @SerializedName("non-dropping-particle")
//        public String nonDroppingParticle;
        public String suffix;
//        @SerializedName("comma-suffix")
//        public String commaSuffix; // Number? Boolean?
//        @SerializedName("staticOrdering")
//        public String staticOrdering; // Number? Boolean?
        public String literal;
//        @SerializedName("parse-names")
//        public String parseNames; // Number? Boolean?
    }

    public static class DateField {
        // Crossref specific fields

        @SerializedName("date-time")
        public Date dateTime;
//        public Long timestamp;

        // Standard Citeproc fields

        @SerializedName("date-parts")
        public String[][] dateParts; // Number?
//        public String season; // Number?
//        public String circa; // Number? Boolean?
        public String literal;
//        public String raw;
    }
}
