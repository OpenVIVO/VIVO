package org.vivoweb.webapp.createandlink;

public class Citation {
    public String externalId;
    public String type;
    public String title;
    public Author[] authors;
    public String journal;
    public String volume;
    public String issue;
    public String pagination;
    public Integer publicationYear;
    public String DOI;

    public boolean alreadyClaimed = false;

    public String getExternalId() { return externalId; }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Author[] getAuthors() {
        return authors;
    }

    public String getJournal() {
        return journal;
    }

    public String getVolume() {
        return volume;
    }

    public String getIssue() {
        return issue;
    }

    public String getPagination() {
        return pagination;
    }

    public Integer getPublicationYear() {
        return publicationYear;
    }

    public String getDOI() {
        return DOI;
    }

    public boolean getAlreadyClaimed() { return alreadyClaimed; }

    public static class Author {
        public String name;
        public boolean linked = false;
        public boolean proposed = false;

        public String getName() {
            return name;
        }

        public boolean getLinked() {
            return linked;
        }

        public boolean getProposed() {
            return proposed;
        }
    }
}
