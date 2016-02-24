package org.vivoweb.webapp.createandlink;

import com.google.gson.annotations.SerializedName;

public class CiteprocJSONModel {
    public String type;
    public String id; // Number?
    public String[] categories;
    public String language;
    public String journalAbbreviation;
    public String shortTitle;
    public Name[] author;
    @SerializedName("collection-editor")
    public Name[] collectionEditor;
    public Name[] composer;
    @SerializedName("container-author")
    public Name[] containerAuthor;
    public Name[] director;
    public Name[] editor;
    @SerializedName("editorial-director")
    public Name[] editorialDirector;
    public Name[] interviewer;
    public Name[] illustrator;
    @SerializedName("original-author")
    public Name[] originalAuthor;
    public Name[] recipient;
    @SerializedName("reviewed-author")
    public Name[] reviewedAuthor;
    public Name[] translator;
    public DateField accessed;
    public DateField container;
    @SerializedName("event-date")
    public DateField eventDate;
    public DateField issued;
    @SerializedName("original-date")
    public DateField originalDate;
    public DateField submitted;
    @SerializedName("abstract")
    public String abstractText;
    public String annote;
    public String archive;
    public String archive_location;
    public String authority;
    @SerializedName("call-number")
    public String callNumber;
    @SerializedName("chapter-number")
    public String chapterNumber;
    @SerializedName("citation-number")
    public String citationNumber;
    @SerializedName("citation-label")
    public String citationLabel;
    @SerializedName("collection-number")
    public String collectionNumber;
    @SerializedName("container-title")
    public String containerTitle;
    @SerializedName("container-title-short")
    public String containerTitleShort;
    public String dimensions;
    public String DOI;
    public String edition; // Integer?
    public String event;
    @SerializedName("event-place")
    public String eventPlace;
    @SerializedName("first-reference-note-number")
    public String firstReferenceNoteNumber;
    public String genre;
    public String ISBN;
    public String ISSN;
    public String issue; // Integer?
    public String jurisdiction;
    public String keyword;
    public String locator;
    public String medium;
    public String note;
    public String number; // Integer?
    @SerializedName("number-of-pages")
    public String numberOfPages;
    @SerializedName("number-of-volumes")
    public String numberOfVolumes; // Integer?
    @SerializedName("original-publisher")
    public String originalPublisher;
    @SerializedName("original-publisher-place")
    public String originalPublisherPlace;
    @SerializedName("original-title")
    public String originalTitle;
    public String page;
    @SerializedName("page-first")
    public String pageFirst;
    public String PMCID;
    public String PMID;
    public String publisher;
    @SerializedName("publisher-place")
    public String publisherPlace;
    public String references;
    @SerializedName("reviewed-title")
    public String reviewedTitle;
    public String scale;
    public String section;
    public String source;
    public String status;
    public String title;
    @SerializedName("title-short")
    public String titleShort;
    public String URL;
    public String version;
    public String volume; // Integer?
    @SerializedName("year-suffix")
    public String yearSuffix;

    public static class Name {
        public String family;
        public String given;
        @SerializedName("dropping-particle")
        public String droppingParticle;
        @SerializedName("non-dropping-particle")
        public String nonDroppingParticle;
        public String suffix;
        @SerializedName("comma-suffix")
        public String commaSuffix; // Number? Boolean?
        @SerializedName("staticOrdering")
        public String staticOrdering; // Number? Boolean?
        public String literal;
        @SerializedName("parse-names")
        public String parseNames; // Number? Boolean?
    }

    public static class DateField {
        @SerializedName("date-parts")
        public String[][] dateParts; // Number?
        public String season; // Number?
        public String circa; // Number? Boolean?
        public String literal;
        public String raw;
    }
}
