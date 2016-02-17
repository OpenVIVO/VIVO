/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package org.vivoweb.webapp.controller.freemarker;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import edu.cornell.mannlib.vedit.beans.LoginStatusBean;
import edu.cornell.mannlib.vitro.webapp.auth.permissions.SimplePermission;
import edu.cornell.mannlib.vitro.webapp.auth.requestedAction.AuthorizationRequest;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.SelfEditingConfiguration;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.FreemarkerHttpServlet;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.InsertException;
import edu.cornell.mannlib.vitro.webapp.dao.NewURIMakerVitro;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ChangeSet;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFServiceException;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ResultSetConsumer;
import edu.cornell.mannlib.vitro.webapp.utils.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.vivoweb.webapp.crossref.JSONModel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAndLinkResourceByDOIController extends FreemarkerHttpServlet {
    public static final AuthorizationRequest REQUIRED_ACTIONS = SimplePermission.EDIT_OWN_ACCOUNT.ACTION;

    private static final String CROSSREF_API = "http://api.crossref.org/works/";

    private static final Map<String, String> typeToClassMapp = new HashMap<String, String>();

    public static final String BIBO_ARTICLE = "http://purl.org/ontology/bibo/Article";
    public static final String BIBO_DOI = "http://purl.org/ontology/bibo/doi";
    public static final String BIBO_ISSN = "http://purl.org/ontology/bibo/issn";
    public static final String BIBO_ISSUE = "http://purl.org/ontology/bibo/issue";
    public static final String BIBO_JOURNAL = "http://purl.org/ontology/bibo/Journal";
    public static final String BIBO_PAGE_START = "http://purl.org/ontology/bibo/pageStart";
    public static final String BIBO_PAGE_END = "http://purl.org/ontology/bibo/pageEnd";
    public static final String BIBO_VOLUME = "http://purl.org/ontology/bibo/volume";

    public static final String FOAF_FIRSTNAME = "http://xmlns.com/foaf/0.1/firstName";
    public static final String FOAF_LASTNAME = "http://xmlns.com/foaf/0.1/lastName";

    public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    public static final String VIVO_AUTHORSHIP = "http://vivoweb.org/ontology/core#Authorship";
    public static final String VIVO_DATETIME = "http://vivoweb.org/ontology/core#dateTime";
    public static final String VIVO_DATETIMEPRECISION = "http://vivoweb.org/ontology/core#dateTimePrecision";
    public static final String VIVO_DATETIMEVALUE = "http://vivoweb.org/ontology/core#dateTimeValue";
    public static final String VIVO_EDITORSHIP = "http://vivoweb.org/ontology/core#Editorship";
    public static final String VIVO_HASPUBLICATIONVENUE = "http://vivoweb.org/ontology/core#hasPublicationVenue";
    public static final String VIVO_PUBLICATIONVENUEFOR = "http://vivoweb.org/ontology/core#publicationVenueFor";
    public static final String VIVO_RANK = "http://vivoweb.org/ontology/core#rank";
    public static final String VIVO_RELATEDBY = "http://vivoweb.org/ontology/core#relatedBy";
    public static final String VIVO_RELATES = "http://vivoweb.org/ontology/core#relates";

    public static final String VCARD_FAMILYNAME = "http://www.w3.org/2006/vcard/ns#familyName";
    public static final String VCARD_GIVENNAME = "http://www.w3.org/2006/vcard/ns#givenName";
    public static final String VCARD_HASNAME = "http://www.w3.org/2006/vcard/ns#hasName";
    public static final String VCARD_INDIVIDUAL = "http://www.w3.org/2006/vcard/ns#Individual";
    public static final String VCARD_NAME = "http://www.w3.org/2006/vcard/ns#Name";

    static {
        typeToClassMapp.put("journal-article", "http://purl.org/ontology/bibo/AcademicArticle");
        typeToClassMapp.put("book", "http://purl.org/ontology/bibo/Book");
        typeToClassMapp.put("book-chapter", "http://purl.org/ontology/bibo/BookSection");
        typeToClassMapp.put("proceedings-article", "http://vivoweb.org/ontology/core#ConferencePaper");
        typeToClassMapp.put("dataset", "http://vivoweb.org/ontology/core#Dataset");
    }

    @Override
    protected AuthorizationRequest requiredActions(VitroRequest vreq) {
        return REQUIRED_ACTIONS;
    }

    @Override
    protected ResponseValues processRequest(VitroRequest vreq) {
        String action = vreq.getParameter("action");
        if (action == null) {
            action = "";
        }

        UserAccount loggedInAccount = LoginStatusBean.getCurrentUser(vreq);
        String profileUri = null;

        SelfEditingConfiguration sec = SelfEditingConfiguration.getBean(vreq);
        List<Individual> assocInds = sec.getAssociatedIndividuals(vreq.getWebappDaoFactory().getIndividualDao(), loggedInAccount.getExternalAuthId());
        if (!assocInds.isEmpty()) {
            profileUri = assocInds.get(0).getURI();
        } else {
            return null;
        }

        switch (action) {
            case "confirmDOI": {
                String doi = normalizeDOI(vreq.getParameter("doi"));

                Model existingModel = getExistingResource(vreq, vreq.getParameter("vivoUri" + doi));

                Model updatedModel = ModelFactory.createDefaultModel();
                updatedModel.add(existingModel);

                // link person to vivoUri
                processConfirmationforDOI(vreq, updatedModel, profileUri, doi);

                Model removeModel = existingModel.difference(updatedModel);
                Model addModel = updatedModel.difference(existingModel);

                if (!addModel.isEmpty() || !removeModel.isEmpty()) {
                    InputStream addStream = null;
                    InputStream removeStream = null;

                    InputStream is = makeN3InputStream(updatedModel);
                    ChangeSet changeSet = vreq.getRDFService().manufactureChangeSet();

                    if (!addModel.isEmpty()) {
                        addStream = makeN3InputStream(addModel);
                        changeSet.addAddition(addStream, RDFService.ModelSerializationFormat.N3, ModelNames.ABOX_ASSERTIONS);
                    }

                    if (!removeModel.isEmpty()) {
                        removeStream = makeN3InputStream(removeModel);
                        changeSet.addRemoval(removeStream, RDFService.ModelSerializationFormat.N3, ModelNames.ABOX_ASSERTIONS);
                    }

                    try {
                        vreq.getRDFService().changeSetUpdate(changeSet);
                    } catch (RDFServiceException e) {
                    } finally {
                        if (addStream != null) {
                            try { addStream.close(); } catch (IOException e) { }
                        }

                        if (removeStream != null) {
                            try { removeStream.close(); } catch (IOException e) { }
                        }
                    }
                }

                return new TemplateResponseValues("createAndLinkResourceEnterDOI.ftl");
            }

            case "findDOI": {
                String doi = normalizeDOI(vreq.getParameter("doi"));

                Citation citation = new Citation();

                String vivoUri = null;
                String json = null;

                vivoUri = findInVIVO(vreq, doi, profileUri, citation);
                if (StringUtils.isEmpty(vivoUri)) {
                    json = findInCrossref(doi, citation);
                }

                proposeAuthorToLink(vreq, citation, profileUri);

                Map<String, Object> templateValues = new HashMap<>();

                templateValues.put("citation", citation);
                if (vivoUri != null) {
                    templateValues.put("vivoUri", vivoUri);
                    return new TemplateResponseValues("createAndLinkResourceConfirmDOI.ftl", templateValues);
                } else if (json != null) {
                    templateValues.put("json", json);
                    return new TemplateResponseValues("createAndLinkResourceConfirmDOI.ftl", templateValues);
                }

                return new TemplateResponseValues("createAndLinkResourceEnterDOI.ftl");
            }

            default:
                break;
        }

        return new TemplateResponseValues("createAndLinkResourceEnterDOI.ftl");
    }

    protected void proposeAuthorToLink(VitroRequest vreq, final Citation citation, String profileUri) {
        String query = "SELECT ?givenName ?familyName ?label\n" +
                "WHERE\n" +
                "{\n" +
                "  {\n" +
                "  \t<" + profileUri + "> <" + RDFS_LABEL +"> ?label .\n" +
                "  \t<" + profileUri + "> <" + FOAF_FIRSTNAME +"> ?givenName .\n" +
                "  \t<" + profileUri + "> <" + FOAF_LASTNAME +"> ?familyName .\n" +
                "  }\n" +
                "}\n";


        try {
            vreq.getRDFService().sparqlSelectQuery(query, new ResultSetConsumer() {
                @Override
                protected void processQuerySolution(QuerySolution qs) {
                    Literal familyName = qs.getLiteral("familyName");
                    Literal givenName  = qs.getLiteral("givenName");
                    Literal label      = qs.getLiteral("label");

                    String authorStr = null;
                    if (familyName != null) {
                        if (givenName != null) {
                            authorStr = formatAuthorString(familyName.getString(), givenName.getString());
                        } else {
                            authorStr = formatAuthorString(familyName.getString(), null);
                        }
                    } else if (label != null) {
                        authorStr = label.getString();
                        if (authorStr.indexOf(',') > -1) {
                            int endIdx = authorStr.indexOf(',');
                            while (endIdx < authorStr.length()) {
                                if (Character.isAlphabetic(authorStr.charAt(endIdx))) {
                                    break;
                                }
                            }

                            if (endIdx < authorStr.length()) {
                                authorStr = authorStr.substring(0, endIdx + 1);
                            } else {
                                authorStr = authorStr.substring(0, authorStr.indexOf(','));
                            }
                        }
                    }

                    if (authorStr != null) {
                        String authorStrLwr = authorStr.toLowerCase();
                        String authorStrUpr = authorStr.toUpperCase();
                        for (Citation.Author author : citation.authors) {
                            if (author.name != null) {
                                if (author.name.startsWith(authorStr) || authorStr.startsWith(author.name)) {
                                    author.proposed = true;
                                    break;
                                }

                                if (author.name.startsWith(authorStrUpr) || authorStrUpr.startsWith(author.name)) {
                                    author.proposed = true;
                                    break;
                                }

                                if (author.name.startsWith(authorStrLwr) || authorStrLwr.startsWith(author.name)) {
                                    author.proposed = true;
                                    break;
                                }
                            }
                        }
                    }

                }
            });
        } catch (RDFServiceException e) {
            e.printStackTrace();
        }
    }

    protected Model getExistingResource(VitroRequest vreq, String uri) {
        Model model = ModelFactory.createDefaultModel();

        try {
            String query =
                    "PREFIX vcard:    <http://www.w3.org/2006/vcard/ns#>\n" +
                    "PREFIX vivo:     <http://vivoweb.org/ontology/core#>\n" +
                    "\n" +
                    "CONSTRUCT\n" +
                    "{\n" +
                    "  <" + uri + "> ?pWork ?oWork .\n" +
                    "  ?sJournal ?pJournal ?oJournal .\n" +
                    "  ?sDateTime ?pDateTime ?oDateTime .\n" +
                    "  ?sRel ?pRel ?oRel .\n" +
                    "  ?sVCard a vcard:Individual .\n" +
                    "  ?sVCard ?pVCard ?oVCard .\n" +
                    "  ?sVCardName ?pVCardName ?oVCardName .\n" +
                    "  ?sPerson ?pPerson ?oPerson .\n" +
                    "}\n" +
                    "WHERE\n" +
                    "{\n" +
                    "  {\n" +
                    "    <" + uri + "> ?pWork ?oWork .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> vivo:hasPublicationVenue ?sJournal .\n" +
                    "    ?sJournal ?pJournal ?oJournal .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> vivo:dateTimeValue ?sDateTime .\n" +
                    "    ?sDateTime ?pDateTime ?oDateTime .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> vivo:relatedBy ?sRel .\n" +
                    "    ?sRel ?pRel ?oRel .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> vivo:relatedBy ?relationship .\n" +
                    "    ?relationship ?pRel ?sPerson .\n" +
                    "    ?sPerson ?pPerson ?oPerson .\n" +
                    "    FILTER (?sPerson != <" + uri + ">)\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> vivo:relatedBy ?relationship .\n" +
                    "    ?relationship ?pRel ?sVCard .\n" +
                    "    ?sVCard vcard:hasName ?sVCardName .\n" +
                    "    ?sVCardName ?pVCardName ?oVCardName .\n" +
                    "  }\n" +
                    "}\n";

            vreq.getRDFService().sparqlConstructQuery(query, model);
        } catch (RDFServiceException e) {
        }

        return model;
    }

    protected void processConfirmationforDOI(VitroRequest vreq, Model model, String userUri, String doi) {
        String vivoUri = vreq.getParameter("vivoUri" + doi);
        String json = vreq.getParameter("json" + doi);

        String contributor = vreq.getParameter("contributor" + doi);

        if (contributor.equalsIgnoreCase("notmine")) {
            return;
        }

        if (StringUtils.isEmpty(vivoUri)) {
            // create object from json and set vivoUri
            Gson gson = new Gson();
            CrossrefResponse response = gson.fromJson(json, CrossrefResponse.class);
            if (response == null || response.message == null) {
                return;
            }

            vivoUri = createVIVOObject(vreq, model, response.message);
        }

        if (contributor.startsWith("author")) {
            Resource authorship = model.createResource(getUnusedUri(vreq));
            authorship.addProperty(RDF.type, model.getResource(VIVO_AUTHORSHIP));

            authorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(vivoUri));
            authorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(userUri));

            model.getResource(vivoUri).addProperty(model.createProperty(VIVO_RELATEDBY), authorship);
            model.getResource(userUri).addProperty(model.createProperty(VIVO_RELATEDBY), authorship);

            if (contributor.length() > 6) {
                String posStr = contributor.substring(6);
                int rank = Integer.parseInt(posStr, 10);
                removeAuthorship(model, rank);
                try {
                    authorship.addLiteral(model.createProperty(VIVO_RANK), rank);
                } catch (NumberFormatException nfe) {
                }
            }
        } else if (contributor.startsWith("editor")) {
            Resource editorship = model.createResource(getUnusedUri(vreq));
            editorship.addProperty(RDF.type, model.getResource(VIVO_EDITORSHIP));

            editorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(vivoUri));
            editorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(userUri));

            model.getResource(vivoUri).addProperty(model.createProperty(VIVO_RELATEDBY), editorship);
            model.getResource(userUri).addProperty(model.createProperty(VIVO_RELATEDBY), editorship);
        }
    }

    protected void removeAuthorship(Model model, int rank) {
        ResIterator iter = model.listSubjects();
        while (iter.hasNext()) {
            Resource subject = iter.next();
            if (subject.hasProperty(RDF.type, model.getResource(VIVO_AUTHORSHIP))) {
                if (subject.hasLiteral(model.createProperty(VIVO_RANK), rank)) {
                    model.removeAll(null, null, subject);

                    StmtIterator stmtIterator = subject.listProperties(model.createProperty(VIVO_RELATES));
                    while (stmtIterator.hasNext()) {
                        Statement stmt = stmtIterator.next();
                        RDFNode rdfNode = stmt.getObject();
                        if (rdfNode.isResource()) {
                            Resource relatedResource = rdfNode.asResource();
                            if (relatedResource.hasProperty(RDF.type, model.getResource(VCARD_INDIVIDUAL))) {
                                removeVCard(model, relatedResource.getURI());
                            }
                        }
                    }

                    subject.removeProperties();
                }
            }
        }

    }

    protected void removeVCard(Model model, String vcardUri) {
        Resource vcard = model.getResource(vcardUri);
        StmtIterator stmtIterator = vcard.listProperties(model.createProperty(VIVO_RELATEDBY));
        if (!stmtIterator.hasNext()) {
            vcard.removeProperties();
        }
    }

    protected String createVIVOObject(VitroRequest vreq, Model model, JSONModel message) {
        String defaultNamespace = vreq.getUnfilteredWebappDaoFactory().getDefaultNamespace();
        if (!defaultNamespace.endsWith("/")) {
            defaultNamespace += "/";
        }
        String vivoUri = defaultNamespace + "doi/" + message.DOI.toLowerCase();

        Resource work = model.createResource(vivoUri);

        if (typeToClassMapp.containsKey(message.type)) {
            work.addProperty(RDF.type, model.getResource(typeToClassMapp.get(message.type)));
        } else {
            work.addProperty(RDF.type, model.getResource(BIBO_ARTICLE));
        }

        work.addProperty(RDFS.label, message.title[0]);
        work.addProperty(model.createProperty(BIBO_DOI), message.DOI.toLowerCase());

        if (message.ISSN != null && message.ISSN.length > 0) {
            String journalName = null;
            for (String container : message.containerTitle) {
                if (journalName == null || container.length() > journalName.length()) {
                    journalName = container;
                }
            }
            Resource journal = model.createResource(defaultNamespace + "issn/" + message.ISSN[0]);
            journal.addProperty(RDFS.label, journalName);
            journal.addProperty(RDF.type, model.getResource(BIBO_JOURNAL));
            for (String issn : message.ISSN) {
                journal.addProperty(model.getProperty(BIBO_ISSN), issn);
            }
            journal.addProperty(model.getProperty(VIVO_PUBLICATIONVENUEFOR), work);
            work.addProperty(model.getProperty(VIVO_HASPUBLICATIONVENUE), journal);

        }

        if (!StringUtils.isEmpty(message.volume)) {
            work.addProperty(model.createProperty(BIBO_VOLUME), message.volume);
        }

        if (!StringUtils.isEmpty(message.issue)) {
            work.addProperty(model.createProperty(BIBO_ISSUE), message.issue);
        }

        if (!StringUtils.isEmpty(message.page)) {
            if (message.page.contains("-")) {
                int hyphen = message.page.indexOf('-');
                work.addProperty(model.createProperty(BIBO_PAGE_START), message.page.substring(0, hyphen - 1));
                work.addProperty(model.createProperty(BIBO_PAGE_END), message.page.substring(hyphen + 1));
            } else {
                work.addProperty(model.createProperty(BIBO_PAGE_START), message.page);
            }
        } else if (!StringUtils.isEmpty(message.articleNumber)) {
            work.addProperty(model.createProperty(BIBO_PAGE_START), message.articleNumber);
        }

        if (!addDateToResource(vreq, work, message.publishedPrint)) {
            addDateToResource(vreq, work, message.publishedOnline);
        }

        int rank = 1;
        for (JSONModel.Author author : message.author) {
            Resource vcard = model.createResource(getVCardURI(vreq, author.family, author.given));
            vcard.addProperty(RDF.type, model.getResource(VCARD_INDIVIDUAL));

            Resource name = model.createResource(getUnusedUri(vreq));
            vcard.addProperty(model.createProperty(VCARD_HASNAME), name);
            name.addProperty(RDF.type, model.getResource(VCARD_NAME));
            if (!StringUtils.isEmpty(author.given)) {
                name.addProperty(model.createProperty(VCARD_GIVENNAME), author.given);
            }
            if (!StringUtils.isEmpty(author.family)) {
                name.addProperty(model.createProperty(VCARD_FAMILYNAME), author.family);
            }

            Resource authorship = model.createResource(getUnusedUri(vreq));
            authorship.addProperty(RDF.type, model.getResource(VIVO_AUTHORSHIP));

            authorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(vivoUri));
            authorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(vcard.getURI()));

            model.getResource(vivoUri).addProperty(model.createProperty(VIVO_RELATEDBY), authorship);
            vcard.addProperty(model.createProperty(VIVO_RELATEDBY), authorship);
            authorship.addLiteral(model.createProperty(VIVO_RANK), rank);
            rank++;
        }

        return vivoUri;
/*
    -- Unmapped
    public String URL;
    @SerializedName("reference-count")
    public Integer referenceCount;
    public String[] subject;
 */
    }

    protected String getVCardURI(VitroRequest vreq, String familyName, String givenName) {
        if (!StringUtils.isEmpty(familyName)) {
            String vcardUri = vreq.getUnfilteredWebappDaoFactory().getDefaultNamespace();
            if (!vcardUri.endsWith("/")) {
                vcardUri += "/";
            }

            vcardUri += "vcard/" + familyName.replaceAll("[^a-zA-Z0-9/]" , "-");

            if (!StringUtils.isEmpty(givenName)) {
                vcardUri += "/" + givenName.replaceAll("[^a-zA-Z0-9/]" , "-");
            }

            return vcardUri;
        }

        return getUnusedUri(vreq);
    }

    protected boolean addDateToResource(VitroRequest vreq, Resource work, JSONModel.DateField date) {
        Model model = work.getModel();

        if (date == null || date.dateParts == null || date.dateParts.length == 0) {
            return false;
        }

        String formattedDate = null;
        String precision = null;

        switch (date.dateParts.length) {
            case 1:
                formattedDate = String.format("%04d-01-01T00:00:00", date.dateParts[0]);
                precision = "http://vivoweb.org/ontology/core#yearPrecision";
                break;

            case 2:
                formattedDate = String.format("%04d-%02d-01T00:00:00", date.dateParts[0], date.dateParts[1]);
                precision = "http://vivoweb.org/ontology/core#monthPrecision";
                break;

            default:
                formattedDate = String.format("%04d-%02d-%02dT00:00:00", date.dateParts[0], date.dateParts[1], date.dateParts[2]);
                precision = "http://vivoweb.org/ontology/core#dayPrecision";
                break;
        }

        String dateUri = vreq.getUnfilteredWebappDaoFactory().getDefaultNamespace();
        if (dateUri.endsWith("/")) {
            dateUri += "date/" + formattedDate;
        } else {
            dateUri += "/date/" + formattedDate;
        }

        Resource dateResource = model.createResource(dateUri).addProperty(RDF.type, model.getResource("http://vivoweb.org/ontology/core#DateTimeValue"));
        dateResource.addProperty(model.createProperty(VIVO_DATETIME), formattedDate);
        dateResource.addProperty(model.createProperty(VIVO_DATETIMEPRECISION), precision);

        work.addProperty(model.createProperty(VIVO_DATETIMEVALUE), dateResource);
        return true;
    }

    private InputStream makeN3InputStream(Model m) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out, "N3");
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String getUnusedUri(VitroRequest vreq) {
        NewURIMakerVitro uriMaker = new NewURIMakerVitro(vreq.getWebappDaoFactory());
        try {
            return uriMaker.getUnusedNewURI(null);
        } catch (InsertException e) {
        }

        return null;
    }

    protected String findInVIVO(VitroRequest vreq, String doi, String profileUri, Citation citation) {
        String query = "SELECT ?work\n" +
                "WHERE\n" +
                "{\n" +
                "  {\n" +
                "  \t?work <http://purl.org/ontology/bibo/doi> \"" + doi + "\" .\n" +
                "  }\n" +
                "}\n";

        final List<String> works = new ArrayList<String>();
        try {
            vreq.getRDFService().sparqlSelectQuery(query, new ResultSetConsumer() {
                @Override
                protected void processQuerySolution(QuerySolution qs) {
                    Resource work = qs.getResource("work");
                    if (work != null) {
                        works.add(work.getURI());
                    }
                }
            });
        } catch (RDFServiceException e) {
        }

        if (works.size() == 1) {
            String vivoUri = works.get(0);

            Model model = getExistingResource(vreq, vivoUri);

            Resource work = model.getResource(vivoUri);
            StmtIterator stmtIterator = work.listProperties();
            String pageStart = null;
            String pageEnd = null;
            Citation.Author[] rankedAuthors = null;
            ArrayList<Citation.Author> unrankedAuthors = new ArrayList<Citation.Author>();

            while (stmtIterator.hasNext()) {
                Statement stmt = stmtIterator.next();

                switch (stmt.getPredicate().getURI()) {
                    case RDFS_LABEL:
                        citation.title = stmt.getString();
                        break;

                    case BIBO_VOLUME:
                        citation.volume = stmt.getString();
                        break;

                    case BIBO_ISSUE:
                        citation.issue = stmt.getString();
                        break;

                    case BIBO_PAGE_START:
                        pageStart = stmt.getString();
                        break;

                    case BIBO_PAGE_END:
                        pageEnd = stmt.getString();
                        break;

                    case VIVO_DATETIMEVALUE:
                        Resource dateTime = stmt.getResource();
                        if (dateTime != null) {
                            Statement stmtDate = dateTime.getProperty(model.getProperty(VIVO_DATETIME));
                            if (stmtDate != null) {
                                String dateTimeValue = stmtDate.getString();
                                if (dateTimeValue != null && dateTimeValue.length() > 3) {
                                    citation.publicationYear = Integer.parseInt(dateTimeValue.substring(0, 4), 10);
                                }
                            }
                        }
                        break;

                    case VIVO_HASPUBLICATIONVENUE:
                        Resource journal = stmt.getResource();
                        if (journal != null) {
                            Statement stmtJournalName = journal.getProperty(RDFS.label);
                            if (stmtJournalName != null) {
                                citation.journal = stmtJournalName.getString();
                            }
                        }
                        break;

                    case VIVO_RELATEDBY:
                        Resource relationship = stmt.getResource();
                        if (relationship != null) {
                            Integer rank = null;
                            if (!isResourceOfType(relationship, VIVO_AUTHORSHIP)) {
                                break;
                            }

                            Citation.Author newAuthor = null;
                            Resource authorResource = null;
                            StmtIterator relationshipIter = relationship.listProperties();
                            while (relationshipIter.hasNext()) {
                                Statement relationshipStmt = relationshipIter.next();
                                switch (relationshipStmt.getPredicate().getURI()) {
                                    case VIVO_RELATES:
                                        if (!vivoUri.equals(relationshipStmt.getResource().getURI())) {
                                            authorResource = relationshipStmt.getResource();
                                        }
                                        break;

                                    case VIVO_RANK:
                                        rank = relationshipStmt.getInt();
                                        break;
                                }
                            }

                            if (authorResource != null) {
                                if (profileUri.equals(authorResource.getURI())) {
                                    citation.alreadyClaimed = true;
                                }

                                boolean linked = false;
                                Statement familyName = null;
                                Statement givenName = null;
                                if (isResourceOfType(authorResource, VCARD_INDIVIDUAL)) {
                                    Statement vcardName = authorResource.getProperty(model.getProperty(VCARD_HASNAME));
                                    if (vcardName != null) {
                                        givenName = vcardName.getProperty(model.getProperty(VCARD_GIVENNAME));
                                        familyName = vcardName.getProperty(model.getProperty(VCARD_FAMILYNAME));
                                    }
                                } else {
                                    givenName = authorResource.getProperty(model.getProperty(FOAF_FIRSTNAME));
                                    familyName = authorResource.getProperty(model.getProperty(FOAF_LASTNAME));
                                    linked = true;
                                }

                                if (familyName != null) {
                                    newAuthor = new Citation.Author();
                                    if (givenName != null) {
                                        newAuthor.name = formatAuthorString(familyName.getString(), givenName.getString());
                                    } else {
                                        newAuthor.name = formatAuthorString(familyName.getString(), null);
                                    }
                                    newAuthor.linked = linked;
                                }
                            }

                            if (newAuthor != null) {
                                if (rank != null) {
                                    if (rankedAuthors == null) {
                                        rankedAuthors = new Citation.Author[rank];
                                    } else if (rankedAuthors.length < rank) {
                                        Citation.Author[] newAuthors = new Citation.Author[rank];
                                        for (int i = 0; i < rankedAuthors.length; i++) {
                                            newAuthors[i] = rankedAuthors[i];
                                        }
                                        rankedAuthors = newAuthors;
                                    }
                                    rankedAuthors[rank - 1] = newAuthor;
                                } else {
                                    unrankedAuthors.add(newAuthor);
                                }
                            }
                        }
                        break;
                }
            }

//            citation.journal;
//
            if (!StringUtils.isEmpty(pageStart)) {
                if (!StringUtils.isEmpty(pageEnd)) {
                    citation.pagination = pageStart + "-" + pageEnd;
                } else {
                    citation.pagination = pageStart;
                }
            }

            if (unrankedAuthors.size() > 0) {
                Citation.Author[] newAuthors = new Citation.Author[rankedAuthors.length + unrankedAuthors.size()];
                int i = 0;
                while (i < rankedAuthors.length) {
                    newAuthors[i] = rankedAuthors[i];
                    i++;
                }
                while (i < unrankedAuthors.size()) {
                    newAuthors[i] = unrankedAuthors.remove(0);
                    i++;
                }
                citation.authors = newAuthors;
            } else {
                citation.authors = rankedAuthors;
            }

            citation.DOI = doi;

            return vivoUri;
        }

        return null;
    }

    protected boolean isResourceOfType(Resource resource, String typeUri) {
        if (resource == null) {
            return false;
        }

        StmtIterator iter = resource.listProperties(RDF.type);
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            if (typeUri.equals(stmt.getResource().getURI())) {
                return true;
            }
        }

        return false;
    }

    protected String findInCrossref(String doi, Citation citation) {
        String json = readUrl(CROSSREF_API + doi);

        Gson gson = new Gson();
        CrossrefResponse response = gson.fromJson(json, CrossrefResponse.class);
        if (response == null || response.message == null) {
            return null;
        }

        if (!doi.equalsIgnoreCase(response.message.DOI)) {
            return null;
        }

        citation.DOI = doi;

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

        List<Citation.Author> authors = new ArrayList<>();
        for (JSONModel.Author author : response.message.author ) {
            Citation.Author citationAuthor = new Citation.Author();
            citationAuthor.name = formatAuthorString(author.family, author.given);
            authors.add(citationAuthor);
        }
        citation.authors = authors.toArray(new Citation.Author[authors.size()]);

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


//        messageMap.get("ISSN"); // List -> String
//        messageMap.get("type"); // String

        return json;
    }

    private String formatAuthorString(String familyName, String givenName) {
        StringBuilder authorBuilder = new StringBuilder(familyName);

        if (!StringUtils.isEmpty(givenName)) {
            authorBuilder.append(", ");
            boolean addToAuthor = true;
            for (char ch : givenName.toCharArray()) {
                if (addToAuthor) {
                    if (Character.isAlphabetic(ch)) {
                        authorBuilder.append(Character.toUpperCase(ch));
                        addToAuthor = false;
                    }
                } else {
                    if (!Character.isAlphabetic(ch)) {
                        addToAuthor = true;
                    }
                }
            }
        }

        return authorBuilder.toString();
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

    protected String readUrl(String url) {
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


    private String extractAuthorFromJSON(Map author) {
        StringBuilder authorBuilder = new StringBuilder((String)author.get("family"));

        String given  = (String)author.get("given");

        if (!StringUtils.isEmpty(given)) {
            authorBuilder.append(", ");
            boolean addToAuthor = true;
            for (char ch : given.toCharArray()) {
                if (addToAuthor) {
                    if (Character.isAlphabetic(ch)) {
                        authorBuilder.append(Character.toUpperCase(ch));
                        addToAuthor = false;
                    }
                } else {
                    if (!Character.isAlphabetic(ch)) {
                        addToAuthor = true;
                    }
                }
            }
        }

        return authorBuilder.toString();
    }

    private String normalizeDOI(String doi) {
        if (doi != null) {
            String doiTrimmed = doi.trim().toLowerCase();

            if (doiTrimmed.startsWith("https://dx.doi.org/")) {
                return doiTrimmed.substring(19);
            } else if (doiTrimmed.startsWith("http://dx.doi.org/")) {
                return doiTrimmed.substring(18);
            }

            return doiTrimmed;
        }

        return null;
    }

    public static class Citation {
        public String title;
        public Author[] authors;
        public String journal;
        public String volume;
        public String issue;
        public String pagination;
        public Integer publicationYear;
        public String DOI;

        public boolean alreadyClaimed = false;

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

    private static class CrossrefResponse {
        public JSONModel message;

        @SerializedName("message-type")
        public String messageType;

        @SerializedName("message-version")
        public String messageVersion;

        public String status;
    }
}
