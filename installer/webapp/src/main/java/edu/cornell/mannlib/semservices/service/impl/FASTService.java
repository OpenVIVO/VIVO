/* $This file is distributed under the terms of the license in /doc/license.txt$ */

package edu.cornell.mannlib.semservices.service.impl;

import edu.cornell.mannlib.semservices.bo.Concept;
import edu.cornell.mannlib.semservices.exceptions.ConceptsNotFoundException;
import edu.cornell.mannlib.semservices.service.ExternalConceptService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author jaf30
 *
 */
public class FASTService implements ExternalConceptService {
   protected final Log logger = LogFactory.getLog(getClass());
//   private static final String baseUri = "http://link.informatics.stonybrook.edu/umls/CUI/";
   private static final String schemeURI = "http://id.worldcat.org/fast/ontology/1.0/#fast";

	public List<Concept> getConcepts(String term) throws Exception {
		List<Concept> conceptList = new ArrayList<Concept>();

        if (FASTSearch.searcher != null) {
            QueryParser parser = new QueryParser("label", new StandardAnalyzer());
            Query query = null;
            try {
                query = parser.parse(term);
                ScoreDoc[] hits = FASTSearch.searcher.search(query, null, 1000).scoreDocs;
                if (hits.length > 0) {
                    for (ScoreDoc hit : hits) {
                        Document hitDoc = FASTSearch.searcher.doc(hit.doc);

                        Concept concept = new Concept();
                        concept.setDefinedBy(schemeURI);
                        concept.setBestMatch("true");
                        concept.setLabel(hitDoc.getField("label").stringValue());
                        concept.setUri(hitDoc.getField("uri").stringValue());
                        concept.setConceptId(stripConceptId(hitDoc.getField("uri").stringValue()));
                        concept.setType("");
                        concept.setDefinition("");
                        concept.setSchemeURI(schemeURI);
                        conceptList.add(concept);
                    }
                }
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return conceptList;
	}

   public List<Concept> processResults(String term) throws Exception {
      return getConcepts(term);
   }

   /**
    * @param uri
    * @return
    */
	public List<Concept> getConceptsByURIWithSparql(String uri)
			throws Exception {
		// deprecating this method...just return an empty list
		List<Concept> conceptList = new ArrayList<Concept>();
		return conceptList;
	}

    protected String stripConceptId(String uri) {
        int lastslash = uri.lastIndexOf('/');
        return uri.substring(lastslash + 1, uri.length());
    }
}
