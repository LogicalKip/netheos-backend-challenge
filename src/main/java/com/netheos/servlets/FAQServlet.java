package com.netheos.servlets;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

import com.netheos.db.DatabaseAccessor;
import com.netheos.SecurityManager;


/**
* Servlet processing all REST calls concerning the Frequently Asked Questions.
* It may be split into different classes/servlets if it becomes big enough, or on the contrary, merged with other features if they are small enough and/or close enough in behavior. <BR>
*
* There are 3 possible uses now : <BR>
* - Use case 1 (send a new couple question/answer (+tags) to add to the database) : Use POST with the question, answer and tags as parameters. Set also {@link #PARAMETER_STRING_REQUEST_TYPE the request type} to {@link #REQUEST_TYPE_ADD_NEW_FAQ}. Tags are meant to be sent as letters-only Strings, separated by {@link #TAG_SEPARATOR the tag separator (ex tag1;tag2)}. Admin use only, you must also send admin username/password <BR>
* - Use case 2 (retrieve all data from the FAQ) : Use GET. Set {@link #PARAMETER_STRING_REQUEST_TYPE the request type} to {@link #REQUEST_TYPE_GET_ALL_FAQ} and {@link #PARAMETER_STRING_FORMAT the format} to {@link #JSON_FORMAT}. Admin use only, you must also send admin username/password <BR>
* - Use case 3 (get all FAQ whose question or answer contains a given pattern) : Use GET with the {@link #PARAMETER_STRING_MATCH pattern to match} as parameter. Set also {@link #PARAMETER_STRING_REQUEST_TYPE the request type} to {@link #REQUEST_TYPE_GET_CORRESPONDING_FAQ} and {@link #PARAMETER_STRING_FORMAT the format} to {@link #JSON_FORMAT}. <BR>
*/
public class FAQServlet extends HttpServlet {
	/** Name of the main table of this project. Although the implementation is technically a collection and not a table, DB matters (here, the fact that it's mongoDB) do not belong in the controller : it could be a table, if DatabaseAccessor used SQL for example. Therefore "table" is used as a general case. */
	public static final String FAQ_TABLE_NAME = DatabaseAccessor.FAQ_COLLECTION_NAME;


	/* Here, the parameter names (in the URL after the "?") are the same as the fields in the collections for simplicity, but it could very well not be the case */
	public static final String PARAMETER_STRING_QUESTION 	= DatabaseAccessor.FAQ_FIELD_QUESTION;
	public static final String PARAMETER_STRING_ANSWER 	= DatabaseAccessor.FAQ_FIELD_ANSWER;
	public static final String PARAMETER_STRING_TAGS 	= DatabaseAccessor.FAQ_FIELD_TAGS;

	/** Name of the parameter specifying what part of the question/answer should be present to be returned to the user */
	public static final String PARAMETER_STRING_MATCH 	= "match";
	/** Name of the parameter specifying what format should be used to return the data (ex : json) */
	public static final String PARAMETER_STRING_FORMAT 	= "format";
	/** Name of the parameter specifying username */
	public static final String PARAMETER_STRING_USERNAME 	= "username";
	/** Name of the parameter specifying password (not safe, @see com.netheos.SecurityManager) */
	public static final String PARAMETER_STRING_PASSWORD 	= "password";
	/** Name of the parameter specifying the use case */
	public static final String PARAMETER_STRING_REQUEST_TYPE = "request_type";


	/** Constant indicating user story 1 */
	public static final String REQUEST_TYPE_ADD_NEW_FAQ = "add_new";
	/** Constant indicating user story 2 */
	public static final String REQUEST_TYPE_GET_ALL_FAQ = "get_all";
	/** Constant indicating user story 3 */
	public static final String REQUEST_TYPE_GET_CORRESPONDING_FAQ = "get_match";

	/** One of the accepted formats for the server response (actually the only one as of now) */
	public static final String JSON_FORMAT = "json";

	/** Used to send a list of letters-only tags as a simple string. Tags will be split according to this pattern */
	public static final String TAG_SEPARATOR = ";";

	/** Name of the attribute send to JSP through request dispatching, it contains the server response and will be displayed in the final HTML */
	public static final String ATTRIBUTE_MESSAGE = "message";

	/** URL to the view */
	public static final String JSP_URL = "/WEB-INF/pages/index.jsp";

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> parameters = sanitizeParameters(request);

		String requestType = parameters.get(PARAMETER_STRING_REQUEST_TYPE).trim().toLowerCase();
		String messageReturned; // Not initialized, so that IDE will show a warning if it's being used without being set

		boolean adminAccess = SecurityManager.hasAdminAccess(
			parameters.get("username"), 
			parameters.get("password"));

		if (requestType == null) {
			messageReturned = getIncorrectRequestTypeMessage();
		} else if (requestType.equals(REQUEST_TYPE_ADD_NEW_FAQ)) {
			messageReturned = "A POST request, as well as admin access, is expected to add a new FAQ. Use POST instead of GET.";
		} else if (requestType.equals(REQUEST_TYPE_GET_ALL_FAQ)) {
			if (adminAccess) {
				messageReturned = processGetAllFAQRequest(parameters.get(PARAMETER_STRING_FORMAT));
			} else {
				messageReturned = getAccessDeniedMessage();
			}
		} else if (requestType.equals(REQUEST_TYPE_GET_CORRESPONDING_FAQ)) {
			messageReturned = processMatchingFAQRequest(parameters.get(PARAMETER_STRING_MATCH), parameters.get(PARAMETER_STRING_FORMAT));
		} else {
			messageReturned = getIncorrectRequestTypeMessage();
		}

		request.setAttribute(ATTRIBUTE_MESSAGE, messageReturned);

		// Send to view
		this.getServletContext().getRequestDispatcher(JSP_URL).forward(request, response);
	}

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map<String, String> parameters = sanitizeParameters(request);

		String requestType = parameters.get(PARAMETER_STRING_REQUEST_TYPE); // Redundant right now, as there is only one reason to send a POST request (the first user story), but it will be useful later when POST can mean several things
		String messageReturned; // Not initialized, so that IDE will show a warning if it's being used without being set

		if (requestType == null) {
			messageReturned = getIncorrectRequestTypeMessage();
		} else if (requestType.equals(REQUEST_TYPE_ADD_NEW_FAQ)) {
			messageReturned = addNewFaq(parameters);
		}  else {
			messageReturned = getIncorrectRequestTypeMessage();
		}

		request.setAttribute(ATTRIBUTE_MESSAGE, messageReturned);

		// Send to view
		this.getServletContext().getRequestDispatcher(JSP_URL).forward(request, response);
	}

	/**
	 * Uses the given request parameters to play out use case n°1 : an admin adding a new question/answer + associated tags to the existing FAQ in the database. 
	 * @return A message indicating how the procedure went, possibly with some advice.
	 */
	private String addNewFaq(Map<String, String> parameters) {
		String messageReturned;

		boolean adminAccess = SecurityManager.hasAdminAccess(
			parameters.get(PARAMETER_STRING_USERNAME), 
			parameters.get(PARAMETER_STRING_PASSWORD));
		if (adminAccess) {	
			String questionToInsert = parameters.get(PARAMETER_STRING_QUESTION);
			String answerToInsert = parameters.get(PARAMETER_STRING_ANSWER);
			String tagString = parameters.get(PARAMETER_STRING_TAGS);

			if (questionToInsert == null || answerToInsert == null || tagString == null) {
				messageReturned = "Please set all of " + PARAMETER_STRING_QUESTION + ", " + PARAMETER_STRING_ANSWER + " and " + PARAMETER_STRING_TAGS + ". " + PARAMETER_STRING_TAGS + " is a string of concatened tags, letters only, separated by \"" + TAG_SEPARATOR + "\".";
			}

			String tagsToInsert[] = tagString.trim().toLowerCase().split(TAG_SEPARATOR);			

			messageReturned = DatabaseAccessor.getInstance().insertNewFAQ(questionToInsert, answerToInsert, filterBadTags(tagsToInsert));
		} else {
			messageReturned = getAccessDeniedMessage();
		}
		return messageReturned;
	}

	/**
	 * Calls the database to find all FAQ whose question OR answer contains the given pattern. Compacted in a single String.
	 */
	private String processMatchingFAQRequest(String patternToMatch, String format) {
		List<String> matches = DatabaseAccessor.getInstance().getFAQMatches(patternToMatch, format);
		return transformListInString(matches, "Matching documents");
	}

	/**
	 * Calls the database and retrieves all FAQ. Compacted in a single String.
	 */
	private String processGetAllFAQRequest(String format) {
		List<String> allFAQ = DatabaseAccessor.getInstance().getWholeCollection(format, FAQ_TABLE_NAME);
		return transformListInString(allFAQ, "All documents");
	}

	/**
	 * Returns a map corresponding to the request parameters (parameter name->parameter value), but every parameter was cleaned to avoid injections.
	 */
	private Map<String, String> sanitizeParameters(HttpServletRequest request) {
		Map<String, String> res = new HashMap<>();

		for (Enumeration<String> cursor = request.getParameterNames() ; cursor.hasMoreElements() ; ) {
			String name = cursor.nextElement();
			res.put(name, sanitizeInput(request.getParameter(name)));
		}

		return res;
	}

	/**
	 * Removes special characters to help avoid injection attacks. Should be made on front-end as well, just in case.
	 */
	private String sanitizeInput(String input) {
		return input.
		    replaceAll("\"", "").
		    replaceAll("\\\\", "").
		    replaceAll("\\{", "").
		    replaceAll("\\}", "").
		    replaceAll("\\$", "");
	}

	/** 
	 * Removes tags not conforming, such as empty strings or those containing non-letter characters.
	 * This avoids both user errors and malicious intent.
	 */
	private List<String> filterBadTags(String tagsToInsert[]) {
		List<String> tagsToInsertCleaned = new LinkedList<>();
		for (int i = 0 ; i < tagsToInsert.length ; i++) {
			if (tagsToInsert[i].matches("[a-zA-Z]{1,100}")) { // Set a limit on max string size to avoid buffer overflow shenanigans
				tagsToInsertCleaned.add(tagsToInsert[i]);
			}
		}
		return tagsToInsertCleaned;
	}

	/**
	 * Returns an error message informing the user they don't have admin access.
	 */
	private String getAccessDeniedMessage() {
		return "This feature is only available to admin users. Check your username and password.";
	}

	/**
	 * Generate an error message for when the request type was not as expected.
	 */
	private String getIncorrectRequestTypeMessage() {
		return "Please provide a correct request type. Expected values are " + REQUEST_TYPE_ADD_NEW_FAQ + ", " + REQUEST_TYPE_GET_ALL_FAQ + " and " + REQUEST_TYPE_GET_CORRESPONDING_FAQ + ".";
	}


	/**
	 * @return the given String list as a single String, in a format expected by front-end. Here, with html line breaks (BR) between each element, with a given String at the top to explain what is displayed.
	 */
	private String transformListInString(List<String> list, String header) {
		String res = header + " : <BR/>\n";
		if (list.isEmpty()) {
			res += "[none]";
		} else {
			for (String s : list) {
				res += s + "<BR/>\n";
			}
		}
		return res;
	}
}
