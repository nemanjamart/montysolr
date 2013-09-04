package org.apache.lucene.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


import monty.solr.util.MontySolrAbstractLuceneTestCase;
import monty.solr.util.MontySolrSetup;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MockIndexWriter;
import org.apache.lucene.index.RandomIndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.join.JoinUtil;
import org.apache.lucene.search.join.ScoreMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.junit.BeforeClass;

public class TestCitationQuery extends MontySolrAbstractLuceneTestCase {

	protected String idField;
	private Directory directory;
	private IndexReader reader;
	private IndexSearcher searcher;
	private MockIndexWriter writer;

	@BeforeClass
	public static void beforeTestCitationQuery() throws Exception {
		MontySolrSetup.addBuildProperties("contrib/invenio");
		MontySolrSetup.addToSysPath(MontySolrSetup.getMontySolrHome()
				+ "/contrib/invenio/src/python");
		//MontySolrSetup.addTargetsToHandler("monty_invenio.targets");
		//MontySolrSetup
		//		.addTargetsToHandler("monty_invenio.tests.fake_citation_query");

	}

	@Override
	public void setUp() throws Exception {
		super.setUp();
		directory = newDirectory();
		reOpenWriter(OpenMode.CREATE);

		adoc("id", "1", "references", "2", "references", "3", "references", "4", "x", "test");
		
		writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
		adoc("id", "2", "x", "test");
		adoc("id", "3", "references", "5", "references", "6", "references", "99", "x", "test");
		
		writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
		adoc("id", "4", "references", "2", "references", "1");
		adoc("id", "5");
		
		writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		adoc("id", "6");
		adoc("id", "7", "references", "5");
		
		
		
		// the same thing, but using bibcode-like data
		
		adoc("id", "11", "bibcode", "b1", "breferences", "b2", "breferences", "b3", "breferences", "b4", "b", "test");
		adoc("id", "12", "bibcode", "b2", "b", "test");
		
		writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
		adoc("id", "13", "bibcode", "b3", "breferences", "b5", "breferences", "b6", "breferences", "b99", "b", "test");
		adoc("id", "14", "bibcode", "b4", "breferences", "b2", "breferences", "b1");
		
		writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
		adoc("id", "15", "bibcode", "b5");
		adoc("id", "16", "bibcode", "b6");
		adoc("id", "17", "bibcode", "b7", "breferences", "b5");
		
		// for testing the alternate identifiers
		
		adoc("id", "25", "bibcode", "b25", "alternate_bibcode", "x25", "alternate_bibcode", "x26", "breferences", "b27");
		
		writer.commit();
		reOpenWriter(OpenMode.APPEND); // close the writer, create a new segment
		
		adoc("id", "27", "bibcode", "b27", "alternate_bibcode", "x20", "alternate_bibcode", "x21", "breferences", "x26", "breferences", "b28");
		adoc("id", "28", "bibcode", "b28", "breferences", "b25", "breferences", "x26");
		

		reader = writer.getReader();
		searcher = newSearcher(reader);
		writer.close();
	}
	
	private void reOpenWriter(OpenMode mode) throws CorruptIndexException, LockObtainFailedException, IOException {
		if (writer != null) writer.close();
		writer = new MockIndexWriter(directory, newIndexWriterConfig(TEST_VERSION_CURRENT, 
				new WhitespaceAnalyzer(TEST_VERSION_CURRENT)).setOpenMode(mode)
				//.setRAMBufferSizeMB(0.1f)
				//.setMaxBufferedDocs(500)
				);
	}
	
	@Override
	public void tearDown() throws Exception {
		reader.close();
		directory.close();
		super.tearDown();
	}

	private void adoc(String... fields) throws IOException {
		Document doc = new Document();
		for (int i = 0; i < fields.length; i = i + 2) {
			doc.add(newField(fields[i], fields[i + 1], StringField.TYPE_STORED));
		}
		writer.addDocument(doc);
	}
	
	
	public void testCitationQueries() throws Exception {
		
		
		// for the queries that use the Integer values
		// -------------------------------------------
		
		String refField = "references";
		String[] idField = new String[] {"id"};
		int idPrefix = 0;
		
		TermQuery q1 = new TermQuery(new Term("id", String.valueOf(idPrefix + 1)));
		TermQuery q2 = new TermQuery(new Term("id", String.valueOf(idPrefix + 2)));
		TermQuery q3 = new TermQuery(new Term("id", String.valueOf(idPrefix + 3)));
		TermQuery q4 = new TermQuery(new Term("id", String.valueOf(idPrefix + 4)));
		TermQuery q5 = new TermQuery(new Term("id", String.valueOf(idPrefix + 5)));
		TermQuery q6 = new TermQuery(new Term("id", String.valueOf(idPrefix + 6)));
		TermQuery q7 = new TermQuery(new Term("id", String.valueOf(idPrefix + 7)));
		TermQuery q99 = new TermQuery(new Term("id", String.valueOf(idPrefix + 99)));
		TermQuery xTest = new TermQuery(new Term("x", "test"));
		TermQuery bTest = new TermQuery(new Term("b", "test"));
		
		BooleanQuery bq13 = new BooleanQuery();
		bq13.add(q1, Occur.SHOULD);
		bq13.add(q3, Occur.SHOULD);
		
		BooleanQuery bq123 = new BooleanQuery();
		bq123.add(q1, Occur.SHOULD);
		bq123.add(q2, Occur.SHOULD);
		bq123.add(q3, Occur.SHOULD);
		
		BooleanQuery bq1234 = new BooleanQuery();
		bq1234.add(q1, Occur.SHOULD);
		bq1234.add(q2, Occur.SHOULD);
		bq1234.add(q3, Occur.SHOULD);
		bq1234.add(q4, Occur.SHOULD);
		
		BooleanQuery bq15 = new BooleanQuery();
		bq15.add(q1, Occur.SHOULD);
		bq15.add(q5, Occur.SHOULD);
		
		// just a test that index is OK
		assertEquals(1, searcher.search(q1, 10).totalHits);
		assertEquals(1, searcher.search(q2, 10).totalHits);
		assertEquals(1, searcher.search(q3, 10).totalHits);
		assertEquals(0, searcher.search(q99, 10).totalHits);
		assertEquals(2, searcher.search(bq13, 10).totalHits);
		assertEquals(3, searcher.search(xTest, 10).totalHits);
		assertEquals(3, searcher.search(bTest, 10).totalHits);
		
		DictionaryRecIdCache.INSTANCE.clear();
		
		// now test of references ( X --> (x))
		Map<Integer, Integer> cache = DictionaryRecIdCache.INSTANCE.getTranslationCache(searcher.getIndexReader(), idField);
		Map<Integer, Integer> cache2 = DictionaryRecIdCache.INSTANCE.getTranslationCache(searcher.getIndexReader(), idField);
		assertTrue(cache.hashCode() == cache2.hashCode());
		assertTrue(cache == cache2);
		
		
		assertEquals(3, searcher.search(new SecondOrderQuery(q1, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q2, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q3, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q4, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q5, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q6, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q99, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(bq123, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(xTest, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		
		// the same thing using lucene joins
		assertEquals(3, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q1, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q2, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q3, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q4, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q5, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q6, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q99, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(5, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], bq13, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(5, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], bq123, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(5, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], xTest, searcher, ScoreMode.Max), 10).totalHits);

		compareCitations(idField, refField, q1);
		compareCitations(idField, refField, q2);
		compareCitations(idField, refField, q3);
		compareCitations(idField, refField, q4);
		compareCitations(idField, refField, q5);
		compareCitations(idField, refField, q6);
		compareCitations(idField, refField, q99);
		compareCitations(idField, refField, bq13);
		compareCitations(idField, refField, bq123);
		
			
		ScoreDoc[] docs = searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField)), 10).scoreDocs;
		
		ArrayList<Integer> ar = new ArrayList<Integer>();
		for (ScoreDoc d: docs) {
		  Document doc = reader.document(d.doc);
			ar.add(Integer.valueOf(doc.get("id")));
		}
		List<Integer> er = Arrays.asList(idPrefix + 2, idPrefix + 3, idPrefix + 4, idPrefix + 5, idPrefix + 6);
		assertTrue(ar.containsAll(er));
		
		
		int[][] invCache = DictionaryRecIdCache.INSTANCE.getUnInvertedDocids(reader, idField, refField);
		int[][] invCache2 = DictionaryRecIdCache.INSTANCE.getUnInvertedDocids(reader, idField, refField);
		
		assertTrue(invCache == invCache2);
		assertTrue(invCache.hashCode() == invCache.hashCode());
		
		assertEquals(1, searcher.search(new SecondOrderQuery(q1, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q2, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(1, searcher.search(new SecondOrderQuery(q3, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(1, searcher.search(new SecondOrderQuery(q4, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q5, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(1, searcher.search(new SecondOrderQuery(q6, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q99, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(bq123, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(bq1234, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(3, searcher.search(new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(xTest, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		
		// the same thing, but using lucene join query
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q1, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q2, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q3, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q4, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q5, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q6, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q99, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq13, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq123, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq1234, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(3, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq15, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, xTest, searcher, ScoreMode.Avg), 10).totalHits);
		
		compareCitedBy(idField, refField, q1);
		compareCitedBy(idField, refField, q2);
		compareCitedBy(idField, refField, q3);
		compareCitedBy(idField, refField, q4);
		compareCitedBy(idField, refField, q5);
		compareCitedBy(idField, refField, q6);
		compareCitedBy(idField, refField, q99);
		compareCitedBy(idField, refField, bq13);
		compareCitedBy(idField, refField, bq123);
		
				
		ar = new ArrayList<Integer>();
		for (ScoreDoc d: searcher.search(new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).scoreDocs) {
		  Document doc = reader.document(d.doc);
			ar.add(Integer.valueOf(doc.get("id")));
		}
		er = Arrays.asList(idPrefix + 3, idPrefix + 4, idPrefix + 7);
		assertTrue(ar.containsAll(er));
		
		
		
		SecondOrderQuery c1 = new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField));
		SecondOrderQuery c2 = new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField));
		assertTrue(c1.equals(c2));
		
		SecondOrderQuery c3 = new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField));
		SecondOrderQuery c4 = new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField));
		assertTrue(c3.equals(c4));
		
		
		// for the queries that use the String values
		// ------------------------------------------
		
		refField = "breferences";
		idField = new String[] {"bibcode"};
		idPrefix = 10;
		
		q1 = new TermQuery(new Term("id", String.valueOf(idPrefix + 1)));
		q2 = new TermQuery(new Term("id", String.valueOf(idPrefix + 2)));
		q3 = new TermQuery(new Term("id", String.valueOf(idPrefix + 3)));
		q4 = new TermQuery(new Term("id", String.valueOf(idPrefix + 4)));
		q5 = new TermQuery(new Term("id", String.valueOf(idPrefix + 5)));
		q6 = new TermQuery(new Term("id", String.valueOf(idPrefix + 6)));
		q7 = new TermQuery(new Term("id", String.valueOf(idPrefix + 7)));
		q99 = new TermQuery(new Term("id", String.valueOf(idPrefix + 99)));
		
		bq13 = new BooleanQuery();
		bq13.add(q1, Occur.SHOULD);
		bq13.add(q3, Occur.SHOULD);
		
		bq123 = new BooleanQuery();
		bq123.add(q1, Occur.SHOULD);
		bq123.add(q2, Occur.SHOULD);
		bq123.add(q3, Occur.SHOULD);
		
		bq1234 = new BooleanQuery();
		bq1234.add(q1, Occur.SHOULD);
		bq1234.add(q2, Occur.SHOULD);
		bq1234.add(q3, Occur.SHOULD);
		bq1234.add(q4, Occur.SHOULD);
		
		bq15 = new BooleanQuery();
		bq15.add(q1, Occur.SHOULD);
		bq15.add(q5, Occur.SHOULD);
		
		// just a test that index is OK
		assertEquals(1, searcher.search(q1, 10).totalHits);
		assertEquals(1, searcher.search(q2, 10).totalHits);
		assertEquals(1, searcher.search(q3, 10).totalHits);
		assertEquals(0, searcher.search(q99, 10).totalHits);
		assertEquals(2, searcher.search(bq13, 10).totalHits);
		
		
		// now test of references ( X --> (x))
		Map<String, Integer> scache = DictionaryRecIdCache.INSTANCE.getTranslationCacheString(searcher, idField);
		Map<String, Integer> scache2 = DictionaryRecIdCache.INSTANCE.getTranslationCacheString(searcher, idField);
		assertTrue(scache.hashCode() == scache2.hashCode());
		assertTrue(scache == scache2);
		
		
		assertEquals(3, searcher.search(new SecondOrderQuery(q1, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q2, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q3, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q4, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q5, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q6, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q99, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(bq123, null, new SecondOrderCollectorCites(idField, refField)), 10).totalHits);
		
		assertEquals(3, searcher.search(new SecondOrderQuery(q1, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q2, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q3, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q4, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q5, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q6, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q99, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		assertEquals(5, searcher.search(new SecondOrderQuery(bq123, null, new SecondOrderCollectorCitesRAM(idField, refField)), 10).totalHits);
		
		
		// the same thing using lucene joins
		assertEquals(3, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q1, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q2, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q3, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q4, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q5, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q6, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], q99, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(5, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], bq13, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(5, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], bq123, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(5, searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], bTest, searcher, ScoreMode.Max), 10).totalHits);
		
		docs = searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField)), 10).scoreDocs;
		
		ar = new ArrayList<Integer>();
		for (ScoreDoc d: docs) {
			Document doc = reader.document(d.doc);
			ar.add(Integer.valueOf(doc.get("id")));
		}
		er = Arrays.asList(idPrefix + 2, idPrefix + 3, idPrefix + 4, idPrefix + 5, idPrefix + 6);
		assertTrue(ar.containsAll(er));
		
		
		invCache = DictionaryRecIdCache.INSTANCE.getUnInvertedDocidsStrField(searcher, idField, refField);
		invCache2 = DictionaryRecIdCache.INSTANCE.getUnInvertedDocidsStrField(searcher, idField, refField);
		
		assertTrue(invCache.equals(invCache2));
		assertTrue(invCache == invCache2);
		
		
		assertEquals(1, searcher.search(new SecondOrderQuery(q1, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q2, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(1, searcher.search(new SecondOrderQuery(q3, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(1, searcher.search(new SecondOrderQuery(q4, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(q5, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(1, searcher.search(new SecondOrderQuery(q6, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(0, searcher.search(new SecondOrderQuery(q99, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		
		assertEquals(2, searcher.search(new SecondOrderQuery(bq13, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(bq123, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(2, searcher.search(new SecondOrderQuery(bq1234, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		assertEquals(3, searcher.search(new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).totalHits);
		
		// the same thing, but using lucene join query
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q1, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q2, searcher, ScoreMode.Max), 10).totalHits);
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q3, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q4, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q5, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(1, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q6, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(0, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, q99, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq13, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq123, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq1234, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(3, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bq15, searcher, ScoreMode.Avg), 10).totalHits);
		assertEquals(2, searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, bTest, searcher, ScoreMode.Avg), 10).totalHits);
		
		ar = new ArrayList<Integer>();
		for (ScoreDoc d: searcher.search(new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField)), 10).scoreDocs) {
			Document doc = reader.document(d.doc);
			ar.add(Integer.valueOf(doc.get("id")));
		}
		er = Arrays.asList(idPrefix + 3, idPrefix + 4, idPrefix + 7);
		assertTrue(ar.containsAll(er));
		
		
		
		c1 = new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField));
		c2 = new SecondOrderQuery(bq15, null, new SecondOrderCollectorCitedBy(idField, refField));
		assertTrue(c1.equals(c2));
		
		c3 = new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField));
		c4 = new SecondOrderQuery(bq13, null, new SecondOrderCollectorCites(idField, refField));
		assertTrue(c3.equals(c4));
		
		FieldCache.DEFAULT.purgeAllCaches();
		
		
		// now test alternate-bibcodes 
		
		TermQuery q25 = new TermQuery(new Term("id", "25"));
		TermQuery q27 = new TermQuery(new Term("id", "27"));
		TermQuery q28 = new TermQuery(new Term("id", "28"));
		
		idField = new String[] {"bibcode", "alternate_bibcode"};
		hasResults(new SecondOrderQuery(q25, null, new SecondOrderCollectorCitedBy(idField, refField)),
				Arrays.asList("27", "28"));
		hasResults(new SecondOrderQuery(q27, null, new SecondOrderCollectorCitedBy(idField, refField)),
				Arrays.asList("25"));
		hasResults(new SecondOrderQuery(q28, null, new SecondOrderCollectorCitedBy(idField, refField)),
				Arrays.asList("27"));
		
		// check the cache was not rebuilt
		int[][] yc = DictionaryRecIdCache.INSTANCE.getUnInvertedDocidsStrField(searcher, idField, refField);
		int[][] yc2 = DictionaryRecIdCache.INSTANCE.getUnInvertedDocidsStrField(searcher, idField, refField);
		assertTrue(yc.hashCode() == yc2.hashCode());
		assertTrue(yc == yc2);
		
		
		hasResults(new SecondOrderQuery(q25, null, new SecondOrderCollectorCites(idField, refField)),
				Arrays.asList("27"));
		hasResults(new SecondOrderQuery(q27, null, new SecondOrderCollectorCites(idField, refField)),
				Arrays.asList("25", "28"));
		hasResults(new SecondOrderQuery(q28, null, new SecondOrderCollectorCites(idField, refField)),
				Arrays.asList("25"));
		
		Map<String, Integer> xcache = DictionaryRecIdCache.INSTANCE.getTranslationCacheString(searcher, idField);
		Map<String, Integer> xcache2 = DictionaryRecIdCache.INSTANCE.getTranslationCacheString(searcher, idField);
		
		assertTrue(xcache == xcache2);
		assertTrue(xcache.hashCode() == xcache2.hashCode());
		
		hasResults(new SecondOrderQuery(q25, null, new SecondOrderCollectorCitesRAM(idField, refField)),
				Arrays.asList("27"));
		hasResults(new SecondOrderQuery(q27, null, new SecondOrderCollectorCitesRAM(idField, refField)),
				Arrays.asList("25", "28"));
		hasResults(new SecondOrderQuery(q28, null, new SecondOrderCollectorCitesRAM(idField, refField)),
				Arrays.asList("25"));
		
		Map<Integer, List<Integer>> acache = DictionaryRecIdCache.INSTANCE.getCacheTranslatedMultiValuesString(searcher,	idField, refField);
		Map<Integer, List<Integer>> acache2 = DictionaryRecIdCache.INSTANCE.getCacheTranslatedMultiValuesString(searcher,	idField, refField);
		
		assertTrue(acache == acache2);
		assertTrue(acache.hashCode() == acache2.hashCode());
		
	}
	
	
	private void hasResults(Query q, List<String> expected) throws IOException {
		ArrayList<String> ar = new ArrayList<String>();
		for (ScoreDoc d: searcher.search(q, 100).scoreDocs) {
			Document doc = reader.document(d.doc);
			ar.add(doc.get("id"));
		}
		if (expected == null) {
			assertTrue(ar.size() == 0);
			return;
		}
		assertTrue(ar.containsAll(expected));
		assertTrue(ar.size() == expected.size());
	}
	
	private void compareCitedBy(String[] idField,
      String refField, Query query) throws IOException {
		ScoreDoc[] r1 = searcher.search(new SecondOrderQuery(query, null, new SecondOrderCollectorCitedBy(idField, refField)), 100).scoreDocs;
		ScoreDoc[] r2 = searcher.search(JoinUtil.createJoinQuery(idField[0], false, refField, query, searcher, ScoreMode.Max), 100).scoreDocs;
		ArrayList<Integer> a1 = new ArrayList<Integer>();
		ArrayList<Integer> a2 = new ArrayList<Integer>();
		for (int i=0;i<r1.length;i++) {
			a1.add(r1[i].doc);
			a2.add(r2[i].doc);
		}
		Collections.sort(a1);
		Collections.sort(a2);
		assertEquals("The implementations return different results", a1, a2);
  }
	
	private void compareCitations(String[] idField,
      String refField, Query query) throws IOException {
		ScoreDoc[] r1 = searcher.search(new SecondOrderQuery(query, null, new SecondOrderCollectorCites(idField, refField)), 100).scoreDocs;
		ScoreDoc[] r2 = searcher.search(JoinUtil.createJoinQuery(refField, true, idField[0], query, searcher, ScoreMode.Max), 100).scoreDocs;
		ArrayList<Integer> a1 = new ArrayList<Integer>();
		ArrayList<Integer> a2 = new ArrayList<Integer>();
		for (int i=0;i<r1.length;i++) {
			a1.add(r1[i].doc);
			a2.add(r2[i].doc);
		}
		Collections.sort(a1);
		Collections.sort(a2);
		assertEquals("The implementations return different results", a1, a2);
  }

	// Uniquely for Junit 3
	public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(TestCitationQuery.class);
    }
}