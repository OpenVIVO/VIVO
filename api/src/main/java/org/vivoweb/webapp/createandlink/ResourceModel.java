package org.vivoweb.webapp.createandlink;

import java.util.Date;

public class ResourceModel {
    public String DOI;
    public String PubMedID;
    public String PubMedCentralID;
    public String[] ISSN;
    public String URL;

    public Author[] author;

    public String containerTitle;
    public DateField created;
    public String issue;
    public String pageStart;
    public String pageEnd;

    public DateField publishedOnline;

    public DateField publishedPrint;

    public String publisher;

    public String[] subject;
    public String title;
    public String type;
    public String volume;


    public static class Author {
        public String family;
        public String given;
    }

    public static class DateField {
        public Integer year;
        public Integer month;
        public Integer day;
    }
}
