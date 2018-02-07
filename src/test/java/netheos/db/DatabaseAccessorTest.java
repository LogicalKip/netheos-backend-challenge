package com.netheos.db;

import com.mongodb.util.JSON;
import com.mongodb.*;
import java.util.*;
import com.mongodb.client.*;

import org.bson.Document;

import com.netheos.db.DatabaseAccessor;
import com.netheos.servlets.FAQServlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
 
public class DatabaseAccessorTest {
 
	private MongoDatabase database;

    @Before
    public void setUp() {
    	System.out.println("\n===================Setting up test environnement.===================");
		MongoClient mongoClient = new MongoClient(new MongoClientURI(DatabaseAccessor.MONGO_URL + ":" + DatabaseAccessor.MONGO_PORT));
		this.database = mongoClient.getDatabase(DatabaseAccessor.DATABASE_NAME);
		this.database.getCollection(DatabaseAccessor.FAQ_COLLECTION_NAME).drop();

		MongoCollection faq = this.database.getCollection(DatabaseAccessor.FAQ_COLLECTION_NAME);
		faq.insertOne(Document.parse("{question:\"I have ideas, how can I send them to you ?\", answer:\"E-mails and phone numbers can be found on our Contact page\", tags:[\"contact\"]}"));
		faq.insertOne(Document.parse("{question:\"Is it true the developper behind this is very competent and handsome ?\", answer:\"Yes, it is.\", tags:[\"meta\", \"important\"]}"));
		faq.insertOne(Document.parse("{question:\"How do I join the team ?\", answer:\"Send your CV and cover letter to the e-mail provided on our recruitement page. Also check https://github.com/Netheos/Challenge-developpement-backend\", tags:[\"contact\"]}"));
    }
 
    @After
    public void tearDown() {
    	System.out.println("\n===================Tearing down test environnement.===================");
    	this.database.getCollection(DatabaseAccessor.FAQ_COLLECTION_NAME).drop();
    }
 
    @Test
    public void getInstanceTest() throws Exception {
        assertTrue(DatabaseAccessor.getInstance() == DatabaseAccessor.getInstance()); // The same singleton should be called every time and therefore have the same adress
    }

    @Test
    public void insertNewFAQTest() throws Exception {
    	String question = "Les tests Junit fonctionneront-ils ?";
    	String answer = "On verra";
    	String tag = "meta";

    	List<String> tags = new LinkedList<String>();
    	tags.add(tag);

    	/* Insert in DB with 3 elements already */
    	String message = DatabaseAccessor.getInstance().insertNewFAQ(question, answer, tags);
    	checkLastInsertedFAQ(question, answer, tag, message, 3);

        /* Insert in empty DB */
		this.database.getCollection(DatabaseAccessor.FAQ_COLLECTION_NAME).drop();

    	message = DatabaseAccessor.getInstance().insertNewFAQ(question, answer, tags);
    	checkLastInsertedFAQ(question, answer, tag, message, 0);
    }

    @Test
    public void getWholeCollectionTest() throws Exception {
    	List<String> faqCollection = DatabaseAccessor.getInstance().getWholeCollection(FAQServlet.JSON_FORMAT, DatabaseAccessor.FAQ_COLLECTION_NAME);
        assertEquals(faqCollection.size(), 3);
        assertTrue(faqCollection.get(0).matches(".*I have ideas.*"));

        /* Wrong format parameter */
        final String EXPECTED_ERROR_MESSAGE = "Only json is accepted as a format for now";
        faqCollection = DatabaseAccessor.getInstance().getWholeCollection("SOME WRONG STRING", DatabaseAccessor.FAQ_COLLECTION_NAME);
        assertEquals(faqCollection.size(), 1);
        assertTrue(faqCollection.get(0).matches(EXPECTED_ERROR_MESSAGE));

        faqCollection = DatabaseAccessor.getInstance().getWholeCollection(null, DatabaseAccessor.FAQ_COLLECTION_NAME);
        assertEquals(faqCollection.size(), 1);
        assertTrue(faqCollection.get(0).matches(EXPECTED_ERROR_MESSAGE));
    }

    @Test
    public void getFAQMatchesTest() throws Exception {
    	List<String> matches = DatabaseAccessor.getInstance().getFAQMatches("it", FAQServlet.JSON_FORMAT);
    	assertEquals(matches.size(), 2);
        assertTrue(matches.get(0).matches(".*Is it true.*"));
        assertTrue(matches.get(1).matches(".*github.*"));

        /* Empty string */
        matches = DatabaseAccessor.getInstance().getFAQMatches("", FAQServlet.JSON_FORMAT);
    	assertEquals(matches.size(), 1);
        assertTrue(matches.get(0).matches("[^{}]*" + FAQServlet.PARAMETER_STRING_MATCH + "[^{}]*")); // Error message mentions the missing parameter and isn't json

        /* null string */
        matches = DatabaseAccessor.getInstance().getFAQMatches(null, FAQServlet.JSON_FORMAT);
    	assertEquals(matches.size(), 1);
        assertTrue(matches.get(0).matches("[^{}]*" + FAQServlet.PARAMETER_STRING_MATCH + "[^{}]*")); // Error message mentions the missing parameter and isn't json
    }

    /**
     * Performs various assert on the last element of the FAQ collection, according to given parameters, to check if the last one-of insertion went as planned.
     */
    private void checkLastInsertedFAQ(String question, String answer, String tag, String insertionMessage, int previousCollectionSize) {
    	assertTrue(insertionMessage.matches(".*succesfully.*"));
    	List<String> faqCollection = DatabaseAccessor.getInstance().getWholeCollection(FAQServlet.JSON_FORMAT, DatabaseAccessor.FAQ_COLLECTION_NAME);
    	int size = faqCollection.size();

        assertEquals(size, previousCollectionSize + 1);

        Document resultInserted = Document.parse(faqCollection.get(size-1));

        assertEquals(resultInserted.get(DatabaseAccessor.FAQ_FIELD_QUESTION), question);
        assertEquals(resultInserted.get(DatabaseAccessor.FAQ_FIELD_ANSWER), answer);

        List<String> tagsInserted = (List<String>) resultInserted.get(DatabaseAccessor.FAQ_FIELD_TAGS);
        assertEquals(tagsInserted.size(), 1);
        assertEquals(tagsInserted.get(0), tag);
    }
}