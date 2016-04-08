package edu.cornell.mannlib.semservices.service.impl;

import com.hp.hpl.jena.graph.Triple;
import org.apache.jena.riot.RiotReader;
import org.apache.jena.riot.lang.LangRIOT;
import org.apache.jena.riot.system.StreamRDFBase;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.io.InputStream;

public class FASTSearch {
    private final static RAMDirectory idx = new RAMDirectory();
    public static IndexSearcher searcher;

    static {
        try {
            final IndexWriter writer = new IndexWriter(idx, new IndexWriterConfig(new StandardAnalyzer()));

            InputStream in = FASTSearch.class.getResourceAsStream("FAST-labels.nt");
            LangRIOT parser = RiotReader.createParserNTriples(in, new StreamRDFBase() {
                @Override
                public void triple(Triple triple) {
                    try {
                        Document doc = new Document();
                        doc.add(new Field("uri", triple.getSubject().getURI(), StoredField.TYPE));
                        doc.add(new Field("label", (String)triple.getObject().getLiteralValue(), TextField.TYPE_STORED));
                        writer.addDocument(doc);
                    } catch (IOException ioe) {
                        searcher = null;
                    }
                }
            });

            parser.parse();

            writer.close();

            searcher = new IndexSearcher(DirectoryReader.open(idx));
        } catch (IOException ioe) {
            searcher = null;
        }
    }
}
