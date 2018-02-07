package com.netheos;




/**
* Class for all involving security, authentication, etc. <BR>
* FIXME TODO The behavior of this class is *NOT* secure ! <BR>
* Passwords (and usernames) are now sent unencrypted as HTTP parameters. This is a problem because : <BR>
* - Attackers may listen to the connection and see the passwords <BR>
* - When typing the password as part of the curl call, it will remain in bash history. Same with the browser history when sent through "?password=XXX" <BR>
* - Password is hard-coded in an unencrypted java file, which is both dangerous if the code was discovered (and obviously unthinkable if open-source), and hard to maintain. <BR>
* - ... Probably more <BR>
*
* Other methods such as digest or better, SSL, should be used ASAP and should be chosen after extended discussion with the client, project owner, security team, front-end UI team, etc.
* 
* Meanwhile, this will be used as a slight security improvement over nothing, as one still needs to know the password to access admin features.
*/
public class SecurityManager {

	/**
	 * Depending on the solution eventually chosen, this method could check a user database for admin rights.
	 * Currently not safe. See SecurityManager javadoc
	 */
	public static boolean hasAdminAccess(String username, String password) {
		return username != null && password != null && 
		username.equals("admin") && password.equals("jGrC4Kp3Nr30"); // Password randomly generated for this project
	}
}
