/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package org.vivoweb.webapp.controller.freemarker;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.hp.hpl.jena.vocabulary.RDFS;
import edu.cornell.mannlib.orcidclient.context.OrcidClientContext;
import edu.cornell.mannlib.orcidclient.orcidmessage.Keyword;
import edu.cornell.mannlib.vedit.beans.LoginStatusBean;
import edu.cornell.mannlib.vitro.webapp.auth.permissions.PermissionSets;
import edu.cornell.mannlib.vitro.webapp.beans.Individual;
import edu.cornell.mannlib.vitro.webapp.beans.SelfEditingConfiguration;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.config.ConfigurationProperties;
import edu.cornell.mannlib.vitro.webapp.controller.VitroRequest;
import edu.cornell.mannlib.vitro.webapp.controller.authenticate.Authenticator;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.FreemarkerHttpServlet;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.UrlBuilder;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.DirectRedirectResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.RedirectResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.ResponseValues;
import edu.cornell.mannlib.vitro.webapp.controller.freemarker.responsevalues.TemplateResponseValues;
import edu.cornell.mannlib.vitro.webapp.dao.IndividualDao;
import edu.cornell.mannlib.vitro.webapp.dao.InsertException;
import edu.cornell.mannlib.vitro.webapp.dao.NewURIMakerVitro;
import edu.cornell.mannlib.vitro.webapp.dao.UserAccountsDao;
import edu.cornell.mannlib.vitro.webapp.modelaccess.ModelNames;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ChangeSet;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFService;
import edu.cornell.mannlib.vitro.webapp.rdfservice.RDFServiceException;
import edu.cornell.mannlib.vitro.webapp.rdfservice.ResultSetConsumer;
import edu.cornell.mannlib.vitro.webapp.utils.http.HttpClientFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class OrcidAuthController extends FreemarkerHttpServlet {
    private String clientId;
    private String clientSecret;
    private String callbackUrl;


    @Override
    public void init() throws ServletException {
        super.init();

        ConfigurationProperties configProperties = ConfigurationProperties.getBean(getServletContext());
        clientId     = configProperties.getProperty("auth.orcid.clientId");
        clientSecret = configProperties.getProperty("auth.orcid.clientPassword");
        callbackUrl  = configProperties.getProperty("auth.orcid.callbackUrl");
    }

    /**
     * Main method for the resource claiming (create and link) workflow
     *
     * @param vreq
     * @return
     */
    @Override
    protected ResponseValues processRequest(VitroRequest vreq) {
        // Get the current URL for parsing
        String requestURI = vreq.getRequestURI();

        OrcidClientContext orcidClientContext = OrcidClientContext.getInstance();
        if (orcidClientContext == null) {
            return new TemplateResponseValues("orcidNotConfigured.ftl");
        }

        // Get the currently logged in user
        UserAccount loggedInAccount = LoginStatusBean.getCurrentUser(vreq);
        if (loggedInAccount != null) {
            String profileUri = getProfileUri(vreq, loggedInAccount);

            // We have a user with a profile, so redirect
            if (profileUri != null) {
                return new DirectRedirectResponseValues(UrlBuilder.getIndividualProfileUrl(profileUri, vreq));
            }
/*
            if (StringUtils.isEmpty(loggedInAccount.getEmailAddress())) {
                loggedInAccount.setEmailAddress("temp@openvivo.org");
                UserAccountsDao userAccountsDao = vreq.getWebappDaoFactory().getUserAccountsDao();
                userAccountsDao.updateUserAccount(loggedInAccount);
            }
*/
            return new TemplateResponseValues("unknownProfile.ftl");
        }

        if (requestURI.contains("callback")) {
            boolean newAccount = false;
            OrcidTokenResponse orcidToken = null;
            String code = vreq.getParameter("code");
            if (code != null) {
                List<NameValuePair> nvps = new ArrayList<NameValuePair>();

                nvps.add(new BasicNameValuePair("client_id", clientId));
                nvps.add(new BasicNameValuePair("client_secret", clientSecret));
                nvps.add(new BasicNameValuePair("grant_type", "authorization_code"));
                nvps.add(new BasicNameValuePair("redirect_uri", callbackUrl));
                nvps.add(new BasicNameValuePair("code", code));

                String json = readJSON("https://pub.orcid.org/oauth/token", nvps);
                if (!StringUtils.isEmpty("json")) {
                    Gson gson = new Gson();
                    orcidToken = gson.fromJson(json, OrcidTokenResponse.class);
                }
            }

            if (orcidToken == null || StringUtils.isEmpty(orcidToken.orcid)) {
                return new TemplateResponseValues("notAuthenticated.ftl");
            }

            String profileUri = null;
            UserAccount userAccount = getAuthenticator(vreq).getAccountForExternalAuth(orcidToken.orcid);
            if (userAccount != null) {
                profileUri = getProfileUri(vreq, userAccount);
            }

            OrcidResponse orcidBio = getOrcidBio(orcidToken);

            String familyName = null;
            String givenName = null;

            if (orcidBio.orcidProfile != null && orcidBio.orcidProfile.orcidBio != null && orcidBio.orcidProfile.orcidBio.personalDetails != null) {
                if (orcidBio.orcidProfile.orcidBio.personalDetails.familyName != null) {
                    familyName = orcidBio.orcidProfile.orcidBio.personalDetails.familyName.value;
                }
                if (orcidBio.orcidProfile.orcidBio.personalDetails.givenNames != null) {
                    givenName = orcidBio.orcidProfile.orcidBio.personalDetails.givenNames.value;
                }
            }

            if (profileUri == null) {
                if (orcidBio == null) {
                    return new TemplateResponseValues("noProfile.ftl");
                }

                // Get the default namespace to create a URI
                String defaultNamespace = vreq.getUnfilteredWebappDaoFactory().getDefaultNamespace();
                if (!defaultNamespace.endsWith("/")) {
                    defaultNamespace += "/";
                }

                profileUri = defaultNamespace + "orcid" + orcidToken.orcid;
                if (!profileExists(vreq, profileUri)) {
                    Model model = ModelFactory.createDefaultModel();

                    Resource profile = model.createResource(profileUri);
                    profile.addProperty(RDF.type, model.getResource("http://xmlns.com/foaf/0.1/Person"));
                    if (!StringUtils.isEmpty(familyName)) {
                        profile.addLiteral(model.getProperty("http://xmlns.com/foaf/0.1/lastName"), familyName);
                        if (!StringUtils.isEmpty(givenName)) {
                            profile.addLiteral(model.getProperty("http://xmlns.com/foaf/0.1/firstName"), givenName);
                            profile.addProperty(RDFS.label, familyName + ", " + givenName);
                        } else {
                            profile.addProperty(RDFS.label, familyName);
                        }
                    }

                    if (orcidBio.orcidProfile != null) {
                        if (orcidBio.orcidProfile.orcidBio != null) {
                            if (orcidBio.orcidProfile.orcidBio.biography != null && !StringUtils.isEmpty(orcidBio.orcidProfile.orcidBio.biography.value)) {
                                profile.addLiteral(model.getProperty("http://vivoweb.org/ontology/core#overview"), orcidBio.orcidProfile.orcidBio.biography.value);
                            }

                            if (orcidBio.orcidProfile.orcidBio.keywords != null) {
                                if (orcidBio.orcidProfile.orcidBio.keywords.keyword != null) {
                                    for (VisibilityString keyword : orcidBio.orcidProfile.orcidBio.keywords.keyword) {
                                        if (!StringUtils.isEmpty(keyword.value)) {
                                            String[] splitKeywords = keyword.value.split("\\s*,\\s*");
                                            for (String splitKeyword : splitKeywords) {
                                                profile.addLiteral(model.getProperty("http://vivoweb.org/ontology/core#freetextKeyword"), splitKeyword);
                                            }
                                        }
                                    }
                                }
                            }

                            if (orcidBio.orcidProfile.orcidBio.contactDetails != null) {
                                if (orcidBio.orcidProfile.orcidBio.contactDetails.address != null && orcidBio.orcidProfile.orcidBio.contactDetails.address.country != null) {
                                    if (!StringUtils.isEmpty(orcidBio.orcidProfile.orcidBio.contactDetails.address.country.value)) {
                                        String countryUri = findCountryFor(vreq.getRDFService(), orcidBio.orcidProfile.orcidBio.contactDetails.address.country.value);
                                        if (!StringUtils.isEmpty(countryUri)) {
                                            profile.addProperty(model.getProperty("http://purl.obolibrary.org/obo/RO_0001025"), model.getResource(countryUri));
                                        }
                                    }
                                }

                                if (orcidBio.orcidProfile.orcidBio.contactDetails.email != null || orcidBio.orcidProfile.orcidBio.researcherUrls != null) {
                                    Resource contactDetails = model.createResource(getUnusedUri(vreq));
                                    contactDetails.addProperty(RDF.type, model.getResource("http://www.w3.org/2006/vcard/ns#Individual"));
                                    contactDetails.addProperty(model.getProperty("http://purl.obolibrary.org/obo/ARG_2000029"), profile);
                                    profile.addProperty(model.getProperty("http://purl.obolibrary.org/obo/ARG_2000028"), contactDetails);

                                    if (orcidBio.orcidProfile.orcidBio.contactDetails.email != null) {
                                        for (OrcidResponse.OrcidProfile.OrcidBio.ContactDetails.Email email : orcidBio.orcidProfile.orcidBio.contactDetails.email) {
                                            if (!StringUtils.isEmpty(email.value)) {
                                                Resource emailResource = model.createResource(getUnusedUri(vreq));
                                                emailResource.addProperty(RDF.type, model.getResource("http://www.w3.org/2006/vcard/ns#Email"));
                                                emailResource.addLiteral(model.getProperty("http://www.w3.org/2006/vcard/ns#email"), email.value);
                                                if (email.primary) {
                                                    emailResource.addProperty(RDF.type, model.getResource("http://www.w3.org/2006/vcard/ns#Work"));
                                                }

                                                contactDetails.addProperty(model.getProperty("http://www.w3.org/2006/vcard/ns#hasEmail"), emailResource);
                                            }
                                        }
                                    }

                                    if (orcidBio.orcidProfile.orcidBio.researcherUrls != null && orcidBio.orcidProfile.orcidBio.researcherUrls.researcherUrl != null) {
                                        for (OrcidResponse.OrcidProfile.OrcidBio.ResearcherUrls.ResearcherUrl researcherUrl : orcidBio.orcidProfile.orcidBio.researcherUrls.researcherUrl) {
                                            if (researcherUrl.url != null && !StringUtils.isEmpty(researcherUrl.url.value)) {
                                                Resource url = model.createResource(getUnusedUri(vreq));

                                                url.addProperty(RDF.type, model.getResource("http://www.w3.org/2006/vcard/ns#URL"));
                                                url.addProperty(model.getProperty("http://www.w3.org/2006/vcard/ns#url"), researcherUrl.url.value);

                                                if (researcherUrl.urlName != null && !StringUtils.isEmpty(researcherUrl.urlName.value)) {
                                                    url.addProperty(RDFS.label, researcherUrl.urlName.value);
                                                }

                                                contactDetails.addProperty(model.getProperty("http://www.w3.org/2006/vcard/ns#hasURL"), url);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (orcidBio.orcidProfile.orcidIdentifier != null) {
                            if (!StringUtils.isEmpty(orcidBio.orcidProfile.orcidIdentifier.uri)) {
                                Resource orcid = model.createResource(orcidBio.orcidProfile.orcidIdentifier.uri);
                                orcid.addProperty(RDF.type, model.getProperty("http://www.w3.org/2002/07/owl#Thing"));

                                profile.addProperty(model.getProperty("http://vivoweb.org/ontology/core#orcidId"), orcid);
                            }
                        }

                    }

                    writeChanges(vreq.getRDFService(), null, model);
                }

                if (userAccount == null) {
                    userAccount = new UserAccount();
                    userAccount.setEmailAddress(orcidToken.orcid + "@openvivo.org");
                    userAccount.setFirstName(givenName);
                    userAccount.setLastName(familyName);
                    userAccount.setExternalAuthId(orcidToken.orcid);
                    userAccount.setPasswordChangeRequired(false);
                    userAccount.setPasswordLinkExpires(0);
                    userAccount.setExternalAuthOnly(true);
                    userAccount.setLoginCount(0);
                    userAccount.setStatus(UserAccount.Status.ACTIVE);
                    userAccount.setPermissionSetUris(Collections.singleton(PermissionSets.URI_SELF_EDITOR));

                    UserAccountsDao userAccountsDao = vreq.getWebappDaoFactory().getUserAccountsDao();
                    userAccountsDao.insertUserAccount(userAccount);

                    newAccount = true;
                }

                SelfEditingConfiguration.getBean(vreq).associateIndividualWithUserAccount(
                        vreq.getWebappDaoFactory().getIndividualDao(),
                        vreq.getWebappDaoFactory().getDataPropertyStatementDao(),
                        userAccount,
                        profileUri
                );
            }

            if (userAccount == null) {
                return new TemplateResponseValues("profileError.ftl");
            }

            try {
                getAuthenticator(vreq).recordLoginAgainstUserAccount(userAccount, LoginStatusBean.AuthenticationSource.EXTERNAL);
            } catch (Authenticator.LoginNotPermitted loginNotPermitted) {
                return new TemplateResponseValues("notAuthenticated.ftl");
            }

            String[] dois = getOrcidDOIs(vreq.getRDFService(), profileUri, orcidToken.orcid);

            if (newAccount || dois != null && dois.length > 0) {
                Map<String, Object> templateValues = new HashMap<>();
                templateValues.put("newAccount", newAccount);
                if (!StringUtils.isEmpty(familyName)) {
                    if (!StringUtils.isEmpty(givenName)) {
                        templateValues.put("profileName", givenName + " " + familyName);
                    } else {
                        templateValues.put("profileName", familyName);
                    }
                } else if (!StringUtils.isEmpty(givenName)) {
                    templateValues.put("profileName", givenName);
                }
                templateValues.put("profileUri", profileUri);

                if (dois != null && dois.length > 0) {
                    templateValues.put("externalIds", StringUtils.join(dois, "\n"));
                    templateValues.put("externalCount", dois.length);
                    templateValues.put("profileUri", profileUri);
                }

                return new TemplateResponseValues("claimDOIs.ftl", templateValues);
            }

            return new DirectRedirectResponseValues(UrlBuilder.getIndividualProfileUrl(profileUri, vreq));
        } else {
            String location = "https://orcid.org/oauth/authorize"
                    + "?client_id=" + clientId
                    + "&response_type=code"
                    + "&scope=/authenticate"
//                        + "&scope=/read-public"
                    + "&show_login=true"
                    + "&redirect_uri=" + callbackUrl;

            return new RedirectResponseValues(location);
        }
    }

    private String[] getOrcidDOIs(RDFService rdfService, String profileUri, String orcid) {
        OrcidResponse orcidResponse = null;
        try {
            String url = "https://pub.orcid.org/v1.2/" + orcid + "/orcid-works/";

            HttpClient client = HttpClientFactory.getHttpClient();
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");

            HttpResponse response = client.execute(request);
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    try (InputStream in = response.getEntity().getContent()) {
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(in, writer, "UTF-8");

                        String json = writer.toString();
                        Gson gson = new Gson();
                        orcidResponse = gson.fromJson(json, OrcidResponse.class);
                    }
            }
        } catch (IOException e) {
        }

        if (orcidResponse == null || orcidResponse.orcidProfile == null || orcidResponse.orcidProfile.orcidActivities == null || orcidResponse.orcidProfile.orcidActivities.orcidWorks == null) {
            return null;
        }

        if (orcidResponse.orcidProfile.orcidActivities.orcidWorks.orcidWork == null) {
            return null;
        }

        Model relationships = getExistingRelationships(rdfService, profileUri);

        List<String> dois = new ArrayList<String>();

        for (OrcidResponse.OrcidProfile.OrcidActivities.OrcidWorks.OrcidWork work : orcidResponse.orcidProfile.orcidActivities.orcidWorks.orcidWork) {
            if (work.workExternalIdentifiers != null && work.workExternalIdentifiers.workExternalIdentifier != null) {
                for (OrcidResponse.OrcidProfile.OrcidActivities.OrcidWorks.OrcidWork.WorkExternalIdentifiers.WorkExternalIdentifier externalIdentifier : work.workExternalIdentifiers.workExternalIdentifier) {
                    if ("DOI".equalsIgnoreCase(externalIdentifier.workExternalIdentifierType)) {
                        if (externalIdentifier.workExternalIdentifierId != null && !StringUtils.isEmpty(externalIdentifier.workExternalIdentifierId.value)) {
                            if (!relationships.contains(null, relationships.getProperty("http://purl.org/ontology/bibo/doi"), externalIdentifier.workExternalIdentifierId.value.toLowerCase())) {
                                dois.add(externalIdentifier.workExternalIdentifierId.value.toLowerCase());
                            }
                        }
                    }
                }
            }
        }

        return dois.toArray(new String[dois.size()]);
    }

    private Authenticator getAuthenticator(HttpServletRequest req) {
        return Authenticator.getInstance(req);
    }

    private OrcidResponse getOrcidBio(OrcidTokenResponse token) {
        try {
            String url = "https://pub.orcid.org/v1.2/" + token.orcid + "/orcid-bio/";

            HttpClient client = HttpClientFactory.getHttpClient();
            HttpGet request = new HttpGet(url);
            request.setHeader("Accept", "application/json");
//            request.setHeader("Authorization", "Bearer " + token.accessToken);

            HttpResponse response = client.execute(request);
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    try (InputStream in = response.getEntity().getContent()) {
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(in, writer, "UTF-8");

                        String json = writer.toString();
                        Gson gson = new Gson();
                        return gson.fromJson(json, OrcidResponse.class);
                    }
            }
        } catch (IOException e) {
        }

        return null;
    }


    /**
     * Read JSON from the URL
     * @param url
     * @return
     */
    private String readJSON(String url, List<NameValuePair> nvps) {
        try {
            HttpClient client = HttpClientFactory.getHttpClient();
            HttpPost request = new HttpPost(url);
            request.setEntity(new UrlEncodedFormEntity(nvps));

            // Content negotiate for csl / citeproc JSON
            request.setHeader("Accept", "application/json");

            HttpResponse response = client.execute(request);
            switch (response.getStatusLine().getStatusCode()) {
                case 200:
                    try (InputStream in = response.getEntity().getContent()) {
                        StringWriter writer = new StringWriter();
                        IOUtils.copy(in, writer, "UTF-8");
                        return writer.toString();
                    }
            }
        } catch (IOException e) {
        }

        return null;
    }

    private String getProfileUri(VitroRequest vreq, UserAccount userAccount) {
        SelfEditingConfiguration sec = SelfEditingConfiguration.getBean(vreq);

        // Find the profile(s) associated with this user
        List<Individual> assocInds = sec.getAssociatedIndividuals(vreq.getWebappDaoFactory().getIndividualDao(), userAccount.getExternalAuthId());
        if (!assocInds.isEmpty()) {
            // If we have associated profiles, ensure that a valid person profile really does exist
            return assocInds.get(0).getURI();
        }

        return null;
    }

    private boolean profileExists(VitroRequest vreq, String uri) {
        IndividualDao dao = vreq.getWebappDaoFactory().getIndividualDao();
        return dao.getIndividualByURI(uri) != null;
    }

    /**
     * @param vreq
     * @param uri
     * @return
     */
    protected Model getExistingProfile(VitroRequest vreq, String uri) {
        Model model = ModelFactory.createDefaultModel();

        try {
            String query =
                    "PREFIX vcard:    <http://www.w3.org/2006/vcard/ns#>\n" +
                    "PREFIX vivo:     <http://vivoweb.org/ontology/core#>\n" +
                    "\n" +
                    "CONSTRUCT\n" +
                    "{\n" +
                    "  <" + uri + "> ?pPerson ?oPerson .\n" +
                    "  ?sContactInfo ?pContactInfo ?oContactInfo .\n" +
                    "  ?sEmail ?pEmail ?oEmail .\n" +
                    "  ?sUrl ?pUrl ?oUrl .\n" +
                    "  ?sOrcid ?pOrcid ?oOrcid .\n" +
                    "  ?sResearchArea ?pResearchArea ?oResearchArea .\n" +
                    "}\n" +
                    "WHERE\n" +
                    "{\n" +
                    "  {\n" +
                    "    <" + uri + "> ?pPerson ?oPerson .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> <http://purl.obolibrary.org/obo/ARG_2000028> ?sContactInfo .\n" +
                    "    ?sContactInfo ?pContactInfo ?oContactInfo .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> <http://purl.obolibrary.org/obo/ARG_2000028> ?sContactInfo .\n" +
                    "    ?sContactInfo <http://www.w3.org/2006/vcard/ns#hasEmail> ?sEmail .\n" +
                    "    ?sEmail ?pEmail ?oEmail .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> <http://purl.obolibrary.org/obo/ARG_2000028> ?sContactInfo .\n" +
                    "    ?sContactInfo <http://www.w3.org/2006/vcard/ns#hasURL> ?sUrl .\n" +
                    "    ?sUrl ?pUrl ?oUrl .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> <http://vivoweb.org/ontology/core#orcidId> ?sOrcid .\n" +
                    "    ?sOrcid ?pOrcid ?oOrcid .\n" +
                    "  }\n" +
                    "  UNION\n" +
                    "  {\n" +
                    "    <" + uri + "> <http://vivoweb.org/ontology/core#hasResearchArea> ?sResearchArea .\n" +
                    "    ?sResearchArea ?pResearchArea ?oResearchArea .\n" +
                    "  }\n" +
                    "}\n";

            vreq.getRDFService().sparqlConstructQuery(query, model);
        } catch (RDFServiceException e) {
        }

        return model;
    }

    private String getUnusedUri(VitroRequest vreq) {
        NewURIMakerVitro uriMaker = new NewURIMakerVitro(vreq.getWebappDaoFactory());
        try {
            return uriMaker.getUnusedNewURI(null);
        } catch (InsertException e) {
        }

        return null;
    }

    protected Model getExistingRelationships(RDFService rdfService, String uri) {
        Model model = ModelFactory.createDefaultModel();

        try {
            String query =
                    "PREFIX vcard:    <http://www.w3.org/2006/vcard/ns#>\n" +
                    "PREFIX vivo:     <http://vivoweb.org/ontology/core#>\n" +
                    "\n" +
                    "CONSTRUCT\n" +
                    "{\n" +
                    "  ?sRelationship ?pRelationship ?oRelationship .\n" +
                    "  ?oRelationship <http://purl.org/ontology/bibo/doi> ?oDoi .\n" +
                    "}\n" +
                    "WHERE\n" +
                    "{\n" +
                    "  {\n" +
                    "    ?sRelationship vivo:relates <" + uri + "> .\n" +
                    "    ?sRelationship ?pRelationship ?oRelationship .\n" +
                    "    ?oRelationship <http://purl.org/ontology/bibo/doi> ?oDoi .\n" +
                    "  }\n" +
                    "}\n";

            rdfService.sparqlConstructQuery(query, model);
        } catch (RDFServiceException e) {
        }

        return model;
    }

    private String findCountryFor(RDFService rdfService, String code) {
        final List<String> countries = new ArrayList<String>();
        String query = "SELECT ?country\n" +
                "WHERE\n" +
                "{\n" +
                "  {\n" +
                "  \t?country <http://aims.fao.org/aos/geopolitical.owl#codeISO2> \"" + code + "\"^^<http://www.w3.org/2001/XMLSchema#string> .\n" +
                "  }\n" +
                "}\n";

        try {
            rdfService.sparqlSelectQuery(query, new ResultSetConsumer() {
                @Override
                protected void processQuerySolution(QuerySolution qs) {
                    Resource country = qs.getResource("country");
                    if (country != null) {
                        countries.add(country.getURI());
                    }
                }
            });
        } catch (RDFServiceException e) {
        }

        if (countries.size() > 0) {
            return countries.get(0);
        }

        return null;
    }

    /**
     * Determine the difference between the "existing" and "updated" models, and write the changes to VIVO
     *
     * @param rdfService
     * @param existingModel
     * @param updatedModel
     */
    protected void writeChanges(RDFService rdfService, Model existingModel, Model updatedModel) {
        Model removeModel = existingModel == null ? ModelFactory.createDefaultModel() : existingModel.difference(updatedModel);
        Model addModel = existingModel == null ? updatedModel : updatedModel.difference(existingModel);

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

    /**
     * Convert the model into an N3 stream
     *
     * @param m
     * @return
     */
    private InputStream makeN3InputStream(Model m) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        m.write(out, "N3");
        return new ByteArrayInputStream(out.toByteArray());
    }

    private class OrcidTokenResponse {
        @SerializedName("access_token")
        String accessToken;

        String name;
        String orcid;

        @SerializedName("refresh_token")
        String refreshToken;
    }

    private class OrcidResponse {
        @SerializedName("error-desc")
        String errorDesc;

        @SerializedName("message-version")
        String messageVersion;

        @SerializedName("orcid-profile")
        OrcidProfile orcidProfile;

        private class OrcidProfile {
            @SerializedName("orcid-activities")
            OrcidActivities orcidActivities;

            @SerializedName("orcid-bio")
            OrcidBio orcidBio;

            @SerializedName("orcid-history")
            OrcidHistory orcidHistory;

            @SerializedName("orcid-identifier")
            OrcidIdentifier orcidIdentifier;

            private class OrcidActivities {
                @SerializedName("orcid-works")
                OrcidWorks orcidWorks;

                private class OrcidWorks {
                    @SerializedName("orcid-work")
                    OrcidWork[] orcidWork;

                    private class OrcidWork {
                        @SerializedName("work-external-identifiers")
                        WorkExternalIdentifiers workExternalIdentifiers;

                        private class WorkExternalIdentifiers {
                            @SerializedName("work-external-identifier")
                            WorkExternalIdentifier[] workExternalIdentifier;

                            private class WorkExternalIdentifier {
                                @SerializedName("work-external-identifier-id")
                                VisibilityString workExternalIdentifierId;

                                @SerializedName("work-external-identifier-type")
                                String workExternalIdentifierType;
                            }
                        }
                    }
                }
            }

            private class OrcidBio {
                VisibilityString biography;

                @SerializedName("contact-details")
                ContactDetails contactDetails;

                Keywords keywords;

                @SerializedName("personal-details")
                PersonalDetails personalDetails;

                @SerializedName("researcher-urls")
                ResearcherUrls researcherUrls;

                private class ContactDetails {
                    Address address;
                    Email[] email;

                    private class Address {
                        VisibilityString country;
                    }

                    private class Email {
                        boolean primary;
                        String value;
                        String visibility;
                    }
                }

                private class Keywords {
                    VisibilityString[] keyword;
                    String visibility;
                }

                private class PersonalDetails {
                    @SerializedName("family-name")
                    VisibilityString familyName;

                    @SerializedName("given-names")
                    VisibilityString givenNames;
                }

                private class ResearcherUrls {
                    @SerializedName("researcher-url")
                    ResearcherUrl[] researcherUrl;
                    String visibility;

                    private class ResearcherUrl {
                        VisibilityString url;

                        @SerializedName("url-name")
                        VisibilityString urlName;
                    }
                }
            }

            private class OrcidHistory {
                @SerializedName("last-modified-date")
                DateValue lastModifiedDate;

                private class DateValue {
                    long value;
                }
            }

            private class OrcidIdentifier {
                String host;
                String path;
                String uri;
                String value;
            }
        }
    }

    private class VisibilityString {
        String value;
        String visibility;
    }
}
