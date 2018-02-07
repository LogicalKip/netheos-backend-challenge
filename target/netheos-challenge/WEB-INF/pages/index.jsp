<%@ page pageEncoding="UTF-8" %>

<!DOCTYPE html>

<html>
    <head>
        <meta charset="utf-8" />
        <title>Netheos project</title>
    </head>
    
    <body>

    	<p>
    		<% 
            String message = (String) request.getAttribute("message");
            out.println("The server responded with : " + message);
            %>
    	</p>

		<br/>
		<br/>

		<p>
			The user interface is still under development due to the lack of front-end developpers, we apologize for the rough formatting.
		</p>

    </body>
</html>