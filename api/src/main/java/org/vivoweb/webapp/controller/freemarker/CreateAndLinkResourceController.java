/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package org.vivoweb.webapp.controller.freemarker;

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
import org.apache.commons.lang.StringUtils;
import org.vivoweb.webapp.createandlink.Citation;
import org.vivoweb.webapp.createandlink.CreateAndLinkResourceProvider;
import org.vivoweb.webapp.createandlink.CreateAndLinkUtils;
import org.vivoweb.webapp.createandlink.ExternalIdentifiers;
import org.vivoweb.webapp.createandlink.ResourceModel;
import org.vivoweb.webapp.createandlink.crossref.CrossrefCreateAndLinkResourceProvider;
import org.vivoweb.webapp.createandlink.pubmed.PubMedCreateAndLinkResourceProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateAndLinkResourceController extends FreemarkerHttpServlet {
    public static final AuthorizationRequest REQUIRED_ACTIONS = SimplePermission.EDIT_OWN_ACCOUNT.ACTION;

    private static final Map<String, String> typeToClassMap = new HashMap<String, String>();

    private static final Map<String, CreateAndLinkResourceProvider> providers = new HashMap<String, CreateAndLinkResourceProvider>();

    public static final String BIBO_ARTICLE = "http://purl.org/ontology/bibo/Article";
    public static final String BIBO_DOI = "http://purl.org/ontology/bibo/doi";
    public static final String BIBO_ISSN = "http://purl.org/ontology/bibo/issn";
    public static final String BIBO_ISSUE = "http://purl.org/ontology/bibo/issue";
    public static final String BIBO_JOURNAL = "http://purl.org/ontology/bibo/Journal";
    public static final String BIBO_PAGE_START = "http://purl.org/ontology/bibo/pageStart";
    public static final String BIBO_PAGE_END = "http://purl.org/ontology/bibo/pageEnd";
    public static final String BIBO_PMID = "http://purl.org/ontology/bibo/pmid";
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
    public static final String VIVO_PMCID = "http://vivoweb.org/ontology/core#pmcid";
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
        typeToClassMap.put("journal-article", "http://purl.org/ontology/bibo/AcademicArticle");
        typeToClassMap.put("book", "http://purl.org/ontology/bibo/Book");
        typeToClassMap.put("book-chapter", "http://purl.org/ontology/bibo/BookSection");
        typeToClassMap.put("proceedings-article", "http://vivoweb.org/ontology/core#ConferencePaper");
        typeToClassMap.put("dataset", "http://vivoweb.org/ontology/core#Dataset");

        providers.put("doi",  new CrossrefCreateAndLinkResourceProvider());
        providers.put("pmid", new PubMedCreateAndLinkResourceProvider());
    }

    @Override
    protected AuthorizationRequest requiredActions(VitroRequest vreq) {
        return REQUIRED_ACTIONS;
    }

    @Override
    protected ResponseValues processRequest(VitroRequest vreq) {
        String requestURI = vreq.getRequestURI();

        CreateAndLinkResourceProvider provider = null;

        String externalProvider = null;
        int typePos = requestURI.indexOf("/createAndLink") + 15;
        if (typePos < requestURI.length()) {
            if (requestURI.lastIndexOf('/') > typePos) {
                externalProvider = requestURI.substring(typePos, requestURI.lastIndexOf('/') - 1);
            } else {
                externalProvider = requestURI.substring(typePos);
            }

            externalProvider = externalProvider.trim().toLowerCase();
            if (providers.containsKey(externalProvider)) {
                provider = providers.get(externalProvider);
            }
        }

        if (provider == null) {
            return new TemplateResponseValues("unknownResourceType.ftl");
        }

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
            case "confirmID": {
                String externalId = provider.normalize(vreq.getParameter("externalId"));

                Model existingModel = getExistingResource(vreq, vreq.getParameter("vivoUri" + externalId));

                Model updatedModel = ModelFactory.createDefaultModel();
                updatedModel.add(existingModel);

                String vivoUri = vreq.getParameter("vivoUri" + externalId);
                if (!"notmine".equalsIgnoreCase(vreq.getParameter("contributor" + externalId))) {
                    if (StringUtils.isEmpty(vivoUri)) {
                        ResourceModel resourceModel = null;
                        String resourceProvider = vreq.getParameter("externalProvider" + externalId);
                        if (providers.containsKey(resourceProvider)) {
                            resourceModel = providers.get(resourceProvider).makeResourceModel(externalId, vreq.getParameter("externalResource" + externalId));
                        } else {
                            resourceModel = provider.makeResourceModel(externalId, vreq.getParameter("externalResource" + externalId));
                        }
                        if (resourceModel != null) {
                            vivoUri = createVIVOObject(vreq, updatedModel, resourceModel);
                        }
                    }

                    // link person to vivoUri
                    processRelationships(vreq, updatedModel, vivoUri, profileUri, vreq.getParameter("contributor" + externalId));
                }

                writeChanges(vreq.getRDFService(), existingModel, updatedModel);

                Map<String, Object> templateValues = new HashMap<>();
                templateValues.put("link", profileUri);
                templateValues.put("label", provider.getLabel());
                return new TemplateResponseValues("createAndLinkResourceEnterID.ftl", templateValues);
            }

            case "findID": {
                String externalId = provider.normalize(vreq.getParameter("externalId"));

                Citation citation = new Citation();
                citation.externalId = externalId;

                String vivoUri = null;
                String externalResource = null;

                ExternalIdentifiers allExternalIds = provider.allExternalIDsForFind(externalId);
                vivoUri = findInVIVO(vreq, allExternalIds, profileUri, citation);
                if (StringUtils.isEmpty(vivoUri)) {
                    if (!StringUtils.isEmpty(allExternalIds.DOI)) {
                        CreateAndLinkResourceProvider doiProvider = providers.get("doi");
                        if (doiProvider != null) {
                            externalResource = doiProvider.findInExternal(allExternalIds.DOI, citation);
                            if (!StringUtils.isEmpty(externalResource)) {
                                externalProvider = "doi";
                            }
                        }
                    }

                    if (StringUtils.isEmpty(externalResource)) {
                        externalResource = provider.findInExternal(externalId, citation);
                    }
                }

                proposeAuthorToLink(vreq, citation, profileUri);

                Map<String, Object> templateValues = new HashMap<>();

                if (vivoUri != null) {
                    templateValues.put("citation", citation);
                    templateValues.put("vivoUri", vivoUri);
                    return new TemplateResponseValues("createAndLinkResourceConfirm.ftl", templateValues);
                } else if (externalResource != null) {
                    templateValues.put("citation", citation);
                    templateValues.put("externalResource", externalResource);
                    templateValues.put("externalProvider", externalProvider);
                    return new TemplateResponseValues("createAndLinkResourceConfirm.ftl", templateValues);
                }

                templateValues.put("notfound", true);
                templateValues.put("label", provider.getLabel());
                return new TemplateResponseValues("createAndLinkResourceEnterID.ftl", templateValues);
            }

            default:
                break;
        }

        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put("label", provider.getLabel());
        return new TemplateResponseValues("createAndLinkResourceEnterID.ftl", templateValues);
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
                    Literal familyName = qs.contains("famileName") ? qs.getLiteral("familyName") : null;
                    Literal givenName  = qs.contains("givenName") ? qs.getLiteral("givenName") : null;
                    Literal label      = qs.contains("label") ? qs.getLiteral("label") : null;

                    String authorStr = null;
                    if (familyName != null) {
                        if (givenName != null) {
                            authorStr = CreateAndLinkUtils.formatAuthorString(familyName.getString(), givenName.getString());
                        } else {
                            authorStr = CreateAndLinkUtils.formatAuthorString(familyName.getString(), null);
                        }
                    } else if (label != null) {
                        authorStr = label.getString();
                        if (authorStr.indexOf(',') > -1) {
                            int endIdx = authorStr.indexOf(',');
                            while (endIdx < authorStr.length()) {
                                if (Character.isAlphabetic(authorStr.charAt(endIdx))) {
                                    break;
                                }
                                endIdx++;
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

    protected void processRelationships(VitroRequest vreq, Model model, String vivoUri, String userUri, String relationship) {
        if (relationship.startsWith("author")) {
            Resource authorship = model.createResource(getUnusedUri(vreq));
            authorship.addProperty(RDF.type, model.getResource(VIVO_AUTHORSHIP));

            authorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(vivoUri));
            authorship.addProperty(model.createProperty(VIVO_RELATES), model.getResource(userUri));

            model.getResource(vivoUri).addProperty(model.createProperty(VIVO_RELATEDBY), authorship);
            model.getResource(userUri).addProperty(model.createProperty(VIVO_RELATEDBY), authorship);

            if (relationship.length() > 6) {
                String posStr = relationship.substring(6);
                int rank = Integer.parseInt(posStr, 10);
                removeAuthorship(model, rank);
                try {
                    authorship.addLiteral(model.createProperty(VIVO_RANK), rank);
                } catch (NumberFormatException nfe) {
                }
            }
        } else if (relationship.startsWith("editor")) {
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

    protected String createVIVOObject(VitroRequest vreq, Model model, ResourceModel resourceModel) {
        String vivoUri = null;
        String defaultNamespace = vreq.getUnfilteredWebappDaoFactory().getDefaultNamespace();
        if (!defaultNamespace.endsWith("/")) {
            defaultNamespace += "/";
        }

        if (!StringUtils.isEmpty(resourceModel.DOI)) {
            vivoUri = defaultNamespace + "doi/" + resourceModel.DOI.toLowerCase();
        } else {
            vivoUri = getUnusedUri(vreq);
        }

        Resource work = model.createResource(vivoUri);

        if (typeToClassMap.containsKey(resourceModel.type)) {
            work.addProperty(RDF.type, model.getResource(typeToClassMap.get(resourceModel.type)));
        } else {
            work.addProperty(RDF.type, model.getResource(BIBO_ARTICLE));
        }

        if (!StringUtils.isEmpty(resourceModel.title)) {
            work.addProperty(RDFS.label, resourceModel.title);
        }

        if (!StringUtils.isEmpty(resourceModel.DOI)) {
            work.addProperty(model.createProperty(BIBO_DOI), resourceModel.DOI.toLowerCase());
        }

        if (!StringUtils.isEmpty(resourceModel.PubMedID)) {
            work.addProperty(model.createProperty(BIBO_PMID), resourceModel.PubMedID.toLowerCase());
        }

        if (!StringUtils.isEmpty(resourceModel.PubMedID)) {
            work.addProperty(model.createProperty(VIVO_PMCID), resourceModel.PubMedID.toLowerCase());
        }

        if (resourceModel.ISSN != null && resourceModel.ISSN.length > 0) {
            Resource journal = model.createResource(defaultNamespace + "issn/" + resourceModel.ISSN[0]);
            journal.addProperty(RDFS.label, resourceModel.containerTitle);
            journal.addProperty(RDF.type, model.getResource(BIBO_JOURNAL));
            for (String issn : resourceModel.ISSN) {
                journal.addProperty(model.getProperty(BIBO_ISSN), issn);
            }
            journal.addProperty(model.getProperty(VIVO_PUBLICATIONVENUEFOR), work);
            work.addProperty(model.getProperty(VIVO_HASPUBLICATIONVENUE), journal);

        }

        if (!StringUtils.isEmpty(resourceModel.volume)) {
            work.addProperty(model.createProperty(BIBO_VOLUME), resourceModel.volume);
        }

        if (!StringUtils.isEmpty(resourceModel.issue)) {
            work.addProperty(model.createProperty(BIBO_ISSUE), resourceModel.issue);
        }

        if (!StringUtils.isEmpty(resourceModel.pageStart)) {
            work.addProperty(model.createProperty(BIBO_PAGE_START), resourceModel.pageStart);
        }
        if (!StringUtils.isEmpty(resourceModel.pageEnd)) {
            work.addProperty(model.createProperty(BIBO_PAGE_END), resourceModel.pageEnd);
        }

        if (!addDateToResource(vreq, work, resourceModel.publishedPrint)) {
            addDateToResource(vreq, work, resourceModel.publishedOnline);
        }

        int rank = 1;
        for (ResourceModel.Author author : resourceModel.author) {
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

    protected boolean addDateToResource(VitroRequest vreq, Resource work, ResourceModel.DateField date) {
        Model model = work.getModel();

        if (date == null || date.year == null) {
            return false;
        }

        String formattedDate = null;
        String precision = null;

        if (date.month != null) {
            if (date.day != null) {
                formattedDate = String.format("%04d-%02d-%02dT00:00:00", date.year, date.month, date.day);
                precision = "http://vivoweb.org/ontology/core#dayPrecision";
            } else {
                formattedDate = String.format("%04d-%02d-01T00:00:00", date.year, date.month);
                precision = "http://vivoweb.org/ontology/core#monthPrecision";
            }
        } else {
            formattedDate = String.format("%04d-01-01T00:00:00", date.year);
            precision = "http://vivoweb.org/ontology/core#yearPrecision";
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

    private String getVIVOUriForDOI(RDFService rdfService, String doi) {
        if (!StringUtils.isEmpty(doi)) {
            final List<String> works = new ArrayList<String>();
            String query = "SELECT ?work\n" +
                    "WHERE\n" +
                    "{\n" +
                    "  {\n" +
                    "  \t?work <http://purl.org/ontology/bibo/doi> \"" + doi + "\" .\n" +
                    "  }\n" +
                    "}\n";

            try {
                rdfService.sparqlSelectQuery(query, new ResultSetConsumer() {
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
                return works.get(0);
            }
        }

        return null;
    }

    private String getVIVOUriForPubMedID(RDFService rdfService, String pmid) {
        if (!StringUtils.isEmpty(pmid)) {
            final List<String> works = new ArrayList<String>();
            String query = "SELECT ?work\n" +
                    "WHERE\n" +
                    "{\n" +
                    "  {\n" +
                    "  \t?work <http://purl.org/ontology/bibo/pmid> \"" + pmid + "\" .\n" +
                    "  }\n" +
                    "}\n";

            try {
                rdfService.sparqlSelectQuery(query, new ResultSetConsumer() {
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
                return works.get(0);
            }
        }

        return null;
    }

    protected String findInVIVO(VitroRequest vreq, ExternalIdentifiers ids, String profileUri, Citation citation) {
        String vivoUri = null;

        vivoUri = getVIVOUriForDOI(vreq.getRDFService(), ids.DOI);
        if (StringUtils.isEmpty(vivoUri)) {
            vivoUri = getVIVOUriForPubMedID(vreq.getRDFService(), ids.PubMedID);
        }

        if (!StringUtils.isEmpty(vivoUri)) {
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
                                        newAuthor.name = CreateAndLinkUtils.formatAuthorString(familyName.getString(), givenName.getString());
                                    } else {
                                        newAuthor.name = CreateAndLinkUtils.formatAuthorString(familyName.getString(), null);
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
                while (i < newAuthors.length && unrankedAuthors.size() > 0) {
                    newAuthors[i] = unrankedAuthors.remove(0);
                    i++;
                }
                citation.authors = newAuthors;
            } else {
                citation.authors = rankedAuthors;
            }

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

    protected void writeChanges(RDFService rdfService, Model existingModel, Model updatedModel) {
        Model removeModel = existingModel.difference(updatedModel);
        Model addModel = updatedModel.difference(existingModel);

        if (!addModel.isEmpty() || !removeModel.isEmpty()) {
            InputStream addStream = null;
            InputStream removeStream = null;

            InputStream is = makeN3InputStream(updatedModel);
            ChangeSet changeSet = rdfService.manufactureChangeSet();

            if (!addModel.isEmpty()) {
                addStream = makeN3InputStream(addModel);
                changeSet.addAddition(addStream, RDFService.ModelSerializationFormat.N3, ModelNames.ABOX_ASSERTIONS);
            }

            if (!removeModel.isEmpty()) {
                removeStream = makeN3InputStream(removeModel);
                changeSet.addRemoval(removeStream, RDFService.ModelSerializationFormat.N3, ModelNames.ABOX_ASSERTIONS);
            }

            try {
                rdfService.changeSetUpdate(changeSet);
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
    }
}
