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
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;
import edu.cornell.mannlib.vitro.webapp.dao.InsertException;
import edu.cornell.mannlib.vitro.webapp.dao.NewURIMakerVitro;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ChangeSet;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFServiceException;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ResultSetConsumer;
import org.apache.commons.lang.ArrayUtils;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CreateAndLinkResourceController extends FreemarkerHttpServlet {
    public static final AuthorizationRequest REQUIRED_ACTIONS = SimplePermission.EDIT_OWN_ACCOUNT.ACTION;

    private static final Map<String, String> typeToClassMap = new HashMap<String, String>();

    private static final Map<String, CreateAndLinkResourceProvider> providers = new HashMap<String, CreateAndLinkResourceProvider>();

    public static final String BIBO_ABSTRACT = "http://purl.org/ontology/bibo/abstract";
    public static final String BIBO_ARTICLE = "http://purl.org/ontology/bibo/Article";
    public static final String BIBO_DOI = "http://purl.org/ontology/bibo/doi";
    public static final String BIBO_ISBN10 = "http://purl.org/ontology/bibo/isbn10";
    public static final String BIBO_ISBN13 = "http://purl.org/ontology/bibo/isbn13";
    public static final String BIBO_ISSN = "http://purl.org/ontology/bibo/issn";
    public static final String BIBO_ISSUE = "http://purl.org/ontology/bibo/issue";
    public static final String BIBO_JOURNAL = "http://purl.org/ontology/bibo/Journal";
    public static final String BIBO_PAGE_COUNT = "http://purl.org/ontology/bibo/numPages";
    public static final String BIBO_PAGE_END = "http://purl.org/ontology/bibo/pageEnd";
    public static final String BIBO_PAGE_START = "http://purl.org/ontology/bibo/pageStart";
    public static final String BIBO_PMID = "http://purl.org/ontology/bibo/pmid";
    public static final String BIBO_VOLUME = "http://purl.org/ontology/bibo/volume";

    public static final String FOAF_FIRSTNAME = "http://xmlns.com/foaf/0.1/firstName";
    public static final String FOAF_LASTNAME = "http://xmlns.com/foaf/0.1/lastName";

    public static final String OBO_HAS_CONTACT_INFO = "http://purl.obolibrary.org/obo/ARG_2000028";
    public static final String OBO_CONTACT_INFO_FOR = "http://purl.obolibrary.org/obo/ARG_2000029";

    public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    public static final String VIVO_AUTHORSHIP = "http://vivoweb.org/ontology/core#Authorship";
    public static final String VIVO_DATETIME = "http://vivoweb.org/ontology/core#dateTime";
    public static final String VIVO_DATETIMEPRECISION = "http://vivoweb.org/ontology/core#dateTimePrecision";
    public static final String VIVO_DATETIMEVALUE = "http://vivoweb.org/ontology/core#dateTimeValue";
    public static final String VIVO_EDITORSHIP = "http://vivoweb.org/ontology/core#Editorship";
    public static final String VIVO_HASPUBLICATIONVENUE = "http://vivoweb.org/ontology/core#hasPublicationVenue";
    public static final String VIVO_PMCID = "http://vivoweb.org/ontology/core#pmcid";
    public static final String VIVO_PUBLICATIONVENUEFOR = "http://vivoweb.org/ontology/core#publicationVenueFor";
    public static final String VIVO_PUBLISHER = "http://vivoweb.org/ontology/core#publisher";
    public static final String VIVO_PUBLISHER_CLASS = "http://vivoweb.org/ontology/core#Publisher";
    public static final String VIVO_PUBLISHER_OF = "http://vivoweb.org/ontology/core#publisherOf";
    public static final String VIVO_RANK = "http://vivoweb.org/ontology/core#rank";
    public static final String VIVO_RELATEDBY = "http://vivoweb.org/ontology/core#relatedBy";
    public static final String VIVO_RELATES = "http://vivoweb.org/ontology/core#relates";

    public static final String VCARD_FAMILYNAME = "http://www.w3.org/2006/vcard/ns#familyName";
    public static final String VCARD_GIVENNAME = "http://www.w3.org/2006/vcard/ns#givenName";
    public static final String VCARD_HAS_NAME = "http://www.w3.org/2006/vcard/ns#hasName";
    public static final String VCARD_HAS_URL = "http://www.w3.org/2006/vcard/ns#hasURL";
    public static final String VCARD_INDIVIDUAL = "http://www.w3.org/2006/vcard/ns#Individual";
    public static final String VCARD_KIND = "http://www.w3.org/2006/vcard/ns#Kind";
    public static final String VCARD_NAME = "http://www.w3.org/2006/vcard/ns#Name";
    public static final String VCARD_URL_CLASS = "http://www.w3.org/2006/vcard/ns#URL";
    public static final String VCARD_URL_PROPERTY = "http://www.w3.org/2006/vcard/ns#url";

    static {
        typeToClassMap.put("article", "http://purl.org/ontology/bibo/Article");
        typeToClassMap.put("article-journal", "http://purl.org/ontology/bibo/AcademicArticle");
        typeToClassMap.put("book", "http://purl.org/ontology/bibo/Book");
        typeToClassMap.put("chapter", "http://purl.org/ontology/bibo/Chapter");
        typeToClassMap.put("dataset", "http://vivoweb.org/ontology/core#Dataset");
        typeToClassMap.put("figure", "http://purl.org/ontology/bibo/Image");
        typeToClassMap.put("graphic", "http://purl.org/ontology/bibo/Image");
        typeToClassMap.put("legal_case", "http://purl.org/ontology/bibo/LegalCaseDocument");
        typeToClassMap.put("legislation", "http://purl.org/ontology/bibo/Legislation");
        typeToClassMap.put("manuscript", "http://purl.org/ontology/bibo/Manuscript");
        typeToClassMap.put("map", "http://purl.org/ontology/bibo/Map");
        typeToClassMap.put("musical_score", "http://vivoweb.org/ontology/core#Score");
        typeToClassMap.put("paper-conference", "http://vivoweb.org/ontology/core#ConferencePaper");
        typeToClassMap.put("patent", "http://purl.org/ontology/bibo/Patent");
        typeToClassMap.put("personal_communication", "http://purl.org/ontology/bibo/PersonalCommunicationDocument");
        typeToClassMap.put("post-weblog", "http://vivoweb.org/ontology/core#BlogPosting");
        typeToClassMap.put("report", "http://purl.org/ontology/bibo/Report");
        typeToClassMap.put("review", "http://vivoweb.org/ontology/core#Review");
        typeToClassMap.put("speech", "http://vivoweb.org/ontology/core#Speech");
        typeToClassMap.put("thesis", "http://purl.org/ontology/bibo/Thesis");
        typeToClassMap.put("webpage", "http://purl.org/ontology/bibo/Webpage");

        /*
                    "article-magazine",
                    "article-newspaper",
                    "bill",
                    "broadcast",
                    "entry",
                    "entry-dictionary",
                    "entry-encyclopedia",
                    "interview",
                    "motion_picture",
                    "pamphlet",
                    "post",
                    "review-book",
                    "song",
                    "treaty",
         */

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

        IndividualDao individualDao = vreq.getWebappDaoFactory().getIndividualDao();
        Individual person = null;

        UserAccount loggedInAccount = LoginStatusBean.getCurrentUser(vreq);
        String profileUri = vreq.getParameter("profileUri");

        if (!StringUtils.isEmpty(profileUri)) {
            person = individualDao.getIndividualByURI(profileUri);
        }

        if (person == null) {
            SelfEditingConfiguration sec = SelfEditingConfiguration.getBean(vreq);
            List<Individual> assocInds = sec.getAssociatedIndividuals(vreq.getWebappDaoFactory().getIndividualDao(), loggedInAccount.getExternalAuthId());
            if (!assocInds.isEmpty()) {
                profileUri = assocInds.get(0).getURI();
                if (!StringUtils.isEmpty(profileUri)) {
                    person = individualDao.getIndividualByURI(profileUri);
                }
            }
        }

        if (person == null) {
            return new TemplateResponseValues("unknownProfile.ftl");
        }

        Map<String, Object> templateValues = new HashMap<>();
        templateValues.put("link", profileUri);
        templateValues.put("label", provider.getLabel());
        templateValues.put("provider", externalProvider);
        templateValues.put("profileUri", profileUri);
        if (person != null) {
            templateValues.put("personLabel",    person.getRdfsLabel());
            templateValues.put("personThumbUrl", person.getThumbUrl());
        }

        String externalIdsToFind = null;

        if ("confirmID".equals(action)) {
            String[] externalIds = vreq.getParameterValues("externalId");
            if (!ArrayUtils.isEmpty(externalIds)) {
                Model existingModel = ModelFactory.createDefaultModel();
                Model updatedModel = ModelFactory.createDefaultModel();
                for (String externalId : externalIds) {
                    externalId = provider.normalize(externalId);
                    if (!StringUtils.isEmpty(externalId)) {
                        Model existingResourceModel = getExistingResource(vreq, vreq.getParameter("vivoUri" + externalId));
                        existingModel.add(existingModel);
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
                    }
                }
                writeChanges(vreq.getRDFService(), existingModel, updatedModel);
            }

            externalIdsToFind = vreq.getParameter("remainderIds");

            if (StringUtils.isEmpty(externalIdsToFind)) {
                return new TemplateResponseValues("createAndLinkResourceEnterID.ftl", templateValues);
            }
        } else if ("findID".equals(action)) {
            externalIdsToFind = vreq.getParameter("externalIds");
        }

        if (!StringUtils.isEmpty(externalIdsToFind)) {
            List<String> uniqueIds = new ArrayList<>();
            Set<String> remainderIds = new HashSet<>();

            List<Citation> citations = new ArrayList<Citation>();
            String[] externalIdArr = externalIdsToFind.split("[\\s,;]+");

            for (String externalId : externalIdArr) {
                externalId = provider.normalize(externalId);
                if (!uniqueIds.contains(externalId)) {
                    uniqueIds.add(externalId);
                }
            }

            int idCount = 0;
            for (String externalId : uniqueIds) {
                if (!StringUtils.isEmpty(externalId)) {
                    if (idCount > 4) {
                        remainderIds.add(externalId);
                    } else {
                        Citation citation = new Citation();
                        citation.externalId = externalId;

                        ExternalIdentifiers allExternalIds = provider.allExternalIDsForFind(externalId);
                        citation.vivoUri = findInVIVO(vreq, allExternalIds, profileUri, citation);
                        if (StringUtils.isEmpty(citation.vivoUri)) {
                            if (!StringUtils.isEmpty(allExternalIds.DOI)) {
                                CreateAndLinkResourceProvider doiProvider = providers.get("doi");
                                if (doiProvider != null) {
                                    citation.externalResource = doiProvider.findInExternal(allExternalIds.DOI, citation);
                                    if (!StringUtils.isEmpty(citation.externalResource)) {
                                        citation.externalProvider = "doi";
                                    }
                                }
                            }

                            if (StringUtils.isEmpty(citation.externalResource)) {
                                citation.externalResource = provider.findInExternal(externalId, citation);
                                citation.externalProvider = externalProvider;
                            }
                        }

                        proposeAuthorToLink(vreq, citation, profileUri);

                        if (citation.vivoUri != null || citation.externalResource != null) {
                            citations.add(citation);
                        }

                        idCount++;
                    }
                }
            }

            if (citations.size() > 0) {
                templateValues.put("citations", citations);
                if (remainderIds.size() > 0) {
                    templateValues.put("remainderIds", String.join("\n", remainderIds));
                    templateValues.put("remainderCount", remainderIds.size());
                }
                return new TemplateResponseValues("createAndLinkResourceConfirm.ftl", templateValues);
            } else {
                templateValues.put("notfound", true);
                return new TemplateResponseValues("createAndLinkResourceEnterID.ftl", templateValues);
            }
        }

        return new TemplateResponseValues("createAndLinkResourceEnterID.ftl", templateValues);
    }

    protected void proposeAuthorToLink(VitroRequest vreq, final Citation citation, String profileUri) {
        if (citation.authors == null) {
            return;
        }

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
                        for (Citation.Name author : citation.authors) {
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

        if (!StringUtils.isEmpty(resourceModel.PubMedCentralID)) {
            work.addProperty(model.createProperty(VIVO_PMCID), resourceModel.PubMedCentralID.toLowerCase());
        }

        if (resourceModel.ISSN != null && resourceModel.ISSN.length > 0) {
            Resource journal = model.createResource(defaultNamespace + "issn/" + resourceModel.ISSN[0]);
            journal.addProperty(RDFS.label, resourceModel.containerTitle);
            journal.addProperty(RDF.type, model.getResource(BIBO_JOURNAL));
            for (String issn : resourceModel.ISSN) {
                journal.addProperty(model.getProperty(BIBO_ISSN), issn);
            }

            if (!StringUtils.isEmpty(resourceModel.publisher)) {
                Resource publisher = model.createResource(getPublisherURI(vreq, resourceModel.publisher));
                publisher.addProperty(RDFS.label, resourceModel.publisher);
                publisher.addProperty(RDF.type, model.getResource(VIVO_PUBLISHER_CLASS));
                publisher.addProperty(model.createProperty(VIVO_PUBLISHER_OF), journal);
                journal.addProperty(model.createProperty(VIVO_PUBLISHER), publisher);
            }

            journal.addProperty(model.getProperty(VIVO_PUBLICATIONVENUEFOR), work);
            work.addProperty(model.getProperty(VIVO_HASPUBLICATIONVENUE), journal);
        }

        if (resourceModel.ISBN != null && resourceModel.ISBN.length > 0) {
            for (String isbn : resourceModel.ISBN) {
                int length = getDigitCount(isbn);
                if (length == 10) {
                    work.addProperty(model.getProperty(BIBO_ISBN10), isbn);
                } else {
                    work.addProperty(model.getProperty(BIBO_ISBN13), isbn);
                }
            }
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

        if (!StringUtils.isEmpty(resourceModel.pageStart) && !StringUtils.isEmpty(resourceModel.pageEnd)) {
            try {
                int pageStart = Integer.parseInt(resourceModel.pageStart, 10);
                int pageEnd   = Integer.parseInt(resourceModel.pageEnd, 10);

                if (pageStart > 0) {
                    if (pageEnd > pageStart) {
                        work.addLiteral(model.createProperty(BIBO_PAGE_COUNT), pageEnd - pageStart);
                    } else if (pageEnd == pageStart) {
                        work.addLiteral(model.createProperty(BIBO_PAGE_COUNT), 1);
                    }
                }
            } catch (NumberFormatException nfe) {
            }
        }

        addDateToResource(vreq, work, resourceModel.publicationDate);

        if (!StringUtils.isEmpty(resourceModel.abstractText)) {
            work.addProperty(model.createProperty(BIBO_ABSTRACT), resourceModel.abstractText);
        }

        if (resourceModel.author != null) {
            int rank = 1;
            for (ResourceModel.NameField author : resourceModel.author) {
                if (author != null) {
                    Resource vcard = model.createResource(getVCardURI(vreq, author.family, author.given));
                    vcard.addProperty(RDF.type, model.getResource(VCARD_INDIVIDUAL));

                    Resource name = model.createResource(getUnusedUri(vreq));
                    vcard.addProperty(model.createProperty(VCARD_HAS_NAME), name);
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
                }
                rank++;
            }
        }

        if (!StringUtils.isEmpty(resourceModel.URL)) {
            try {
                URL url = new URL(resourceModel.URL);
                Resource urlModel = model.createResource(getUnusedUri(vreq));
                urlModel.addProperty(RDF.type, model.getResource(VCARD_URL_CLASS));
                urlModel.addLiteral(model.createProperty(VCARD_URL_PROPERTY), url);

                Resource kindModel = model.createResource(getUnusedUri(vreq));
                kindModel.addProperty(RDF.type, model.getResource(VCARD_KIND));
                kindModel.addProperty(model.createProperty(VCARD_HAS_URL), urlModel);
                kindModel.addProperty(model.createProperty(OBO_CONTACT_INFO_FOR), work);

                work.addProperty(model.createProperty(OBO_HAS_CONTACT_INFO), kindModel);
            } catch (MalformedURLException e) {
            }
        }

        // editor
        // translator
        // subject
        // status
        // presented at
        // keyword

        // http://purl.org/ontology/bibo/status
        // http://vivoweb.org/ontology/core#hasSubjectArea
        // http://purl.org/ontology/bibo/presentedAt
        // http://vivoweb.org/ontology/core#freetextKeyword
        // http://purl.org/ontology/bibo/translator (Direct to user)

        // http://vivoweb.org/ontology/core#Editorship

        return vivoUri;
    }

    protected String getPublisherURI(VitroRequest vreq, String publisher) {
        if (!StringUtils.isEmpty(publisher)) {
            String publisherUri = vreq.getUnfilteredWebappDaoFactory().getDefaultNamespace();
            if (!publisherUri.endsWith("/")) {
                publisherUri += "/";
            }

            publisherUri += "publisher/" + publisher.trim().replaceAll("[^a-zA-Z0-9/]" , "-");

            return publisherUri;
        }

        return getUnusedUri(vreq);
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

    private int getDigitCount(String id) {
        int digits = 0;

        if (id != null) {
            for (char ch : id.toCharArray()) {
                if (Character.isDigit(ch)) {
                    digits++;
                }
            }
        }

        return digits;
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
            Citation.Name[] rankedAuthors = null;
            ArrayList<Citation.Name> unrankedAuthors = new ArrayList<Citation.Name>();

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

                            Citation.Name newAuthor = null;
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
                                    Statement vcardName = authorResource.getProperty(model.getProperty(VCARD_HAS_NAME));
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
                                    newAuthor = new Citation.Name();
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
                                        rankedAuthors = new Citation.Name[rank];
                                    } else if (rankedAuthors.length < rank) {
                                        Citation.Name[] newAuthors = new Citation.Name[rank];
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
                Citation.Name[] newAuthors = new Citation.Name[rankedAuthors.length + unrankedAuthors.size()];
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
