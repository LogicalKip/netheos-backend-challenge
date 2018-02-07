package com.netheos.db;


// I usually avoid using *, and just let the IDE import all that is needed, one by one, but I didn't use and set up a real, complete, fully functionnal IDE for such a small project 
import com.mongodb.*;
import java.util.*;
import com.mongodb.client.*;

import org.bson.Document;
import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;
import java.util.regex.Pattern;

import com.netheos.servlets.FAQServlet;

/**
 * Singleton class allowing access to the database through various methods.
 * Intended to avoid database code in the controller, therefore good only for a single kind of database (here mongoDB) ; a new class will be needed if a new database is used (ex: mysql)
 */
public class DatabaseAccessor {
	/** The only instance of the class allowed to exist, as per the singleton design pattern */
	private static DatabaseAccessor instance;

	/** The database containing the various collections. The whole class could be handled differently if there were more than one database */
	private MongoDatabase database;

	/* The following constants could be fetched from config files, depending on the project context. Ideally in a dedicated class */
	public static final String MONGO_URL = "mongodb://localhost";
	public static final String MONGO_PORT = "27017";
	public static final String DATABASE_NAME = "netheos";
	public static final String FAQ_COLLECTION_NAME = "faq";
	public static final String FAQ_FIELD_QUESTION = "question";
	public static final String FAQ_FIELD_ANSWER = "answer";
	public static final String FAQ_FIELD_TAGS = "tags";

	/** Private constructor and no public constructor, as per the singleton design pattern */
	private DatabaseAccessor() {
		MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGO_URL + ":" + MONGO_PORT));
		this.database = mongoClient.getDatabase(DATABASE_NAME);
	}

	/** Returns the singleton instance, creating it first if it doesn't exist */
	public static DatabaseAccessor getInstance() {
		if (instance == null) {
			instance = new DatabaseAccessor();
		}
		return instance;
	}

	/**
	 * Returns all documents from the collection with provided name, in provided format.
	 * Here, format is the json used by documents in mongo, but depending on what the front-end/client needs, it could be anything : XML, HTML, some Java object, simple text formatted in a user-friendly readable way...
	 * @param format the string "json". Only one accepted for now, but could accept "xml", "human_readable" or more later. How the format is specified could be different (instead of a hardcoded String) depending on what's more convenient, readable and less error-prone. Ex : Integer constants, Strings in a file (xml ? json ? cvs ?), booleans if only 2 formats, enums, different methods for each format... or something else
	 */
	public List<String> getWholeCollection(String format, String collectionName) {
		List<String> res = new LinkedList<>();
		if (format != null && format.trim().toLowerCase().equals(FAQServlet.JSON_FORMAT)) {
			MongoCursor<Document> cursor = database.getCollection(collectionName).find().iterator();
			
			while (cursor.hasNext()) {
				res.add(cursor.next().toJson());
			}
		} else {
			res.add("Only json is accepted as a format for now");
		}
		return res;
	}

	/**
	 * Returns a list of FAQ when either the question or answer contains the given String
	 * @param format the string "json". Only one accepted for now, but could accept "xml", "human_readable" or more later. How the format is specified could be different (instead of a hardcoded String) depending on what's more convenient, readable and less error-prone. Ex : Integer constants, Strings in a file (xml ? json ? cvs ?), booleans if only 2 formats, enums, different methods for each format... or something else.
	 *
	 * FIXME possible flaws found, to discuss with client :
	 * - What if patternString is 2 (or more) words ? Shouldn't we check if those 2 words appear separately, possibly in another order (word2 stuff word1) ? 
	 * - What if only one of them appears ? Does that count as a match ?
	 * - Should there be a list of stop-words like in SEO, to avoid words that are too common and meaningless to the search (in, the, at, be, it, etc) ?
	 * - What about when the pattern is inside a word ? Should that still count as a match ?
	 */
	public List<String> getFAQMatches(String patternString, String format) { 
		List<String> res = new LinkedList<>();

		if (patternString == null || patternString.equals("")) {
			res.add("\"" + FAQServlet.PARAMETER_STRING_MATCH + "\" parameter must be set.");
		} else {
			if (format != null && format.trim().toLowerCase().equals(FAQServlet.JSON_FORMAT)) {
				final String MATCH_REGEX = "^.*" + patternString + ".*$";

				Pattern pattern = Pattern.compile(MATCH_REGEX);
				Bson query = Filters.or(
					Filters.regex(FAQ_FIELD_QUESTION, pattern),
					Filters.regex(FAQ_FIELD_ANSWER, pattern));

				MongoCursor<Document> cursor = database.getCollection(FAQ_COLLECTION_NAME).find(query).iterator();

				while (cursor.hasNext()) {
					res.add(cursor.next().toJson());
				}
			} else {
				res.add("Only json is accepted as a format for now");
			}
		}
		return res;
	}

	/**
	 * Insert in the database a couple question/answer, along with given tags.
	 * @return a message describing the result of the insertion
	 */
	public String insertNewFAQ(String question, String answer, List<String> tags) {
		Document document = new Document();
		document.put(FAQ_FIELD_QUESTION, question);
		document.put(FAQ_FIELD_ANSWER, answer);
		document.put(FAQ_FIELD_TAGS, tags);

		database.getCollection(FAQ_COLLECTION_NAME).insertOne(document);

		return "Document was succesfully inserted";// Exception will have already be thrown if something fails. We might want to send a specific message if that happens.
	}
}