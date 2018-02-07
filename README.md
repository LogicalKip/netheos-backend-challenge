**Présentation :**
Ce projet est un challenge de l'entreprise Netheos, proposé aux potentiels futurs employés, afin d'avoir un exemple concret de développement. Le lien original est https://github.com/Netheos/Challenge-developpement-backend

**Utilisation :**
Les technologies nécessaires sont MongoDB, Java, curl, Maven, et un conteneur de servlet tel que Tomcat.

Lancez mongod.exe.  
Lancez apache-tomcat-X.Y.Z/bin/startup.bat (ou équivalent).  

Compilez l'application vous-même avec `mvn clean package` ou utilisez le .war fourni dans target et installez sur Tomcat (ou autre) 

Pour remplir la base de données (attention, les tests Junit de `mvn package` auront supprimé la base), lancez mongo.exe puis tapez les commandes suivantes :  
`use netheos`  
`db.faq.insertMany([{question:"I have ideas, how can I send them to you ?", answer:"E-mails and phone numbers can be found on our Contact page", tags:["contact"]}, {question:"Is it true the developper behind this is very competent and handsome ?", answer:"Yes, it is.", tags:["meta", "important"]}, {question:"How do I join the team ?", answer:"Send your CV and cover letter to the e-mail provided on our recruitement page. Also check https://github.com/Netheos/Challenge-developpement-backend", tags:["contact"]}])`  
`exit`



*Pour tester (les commandes correspondent respectivement aux user stories 1, 2 et 3. La première commande peut prendre plus de temps, dû à la création de la servlet) :*  
`curl --data "request_type=add_new&username=admin&password=jGrC4Kp3Nr30&question=Une%20question?&answer=Une%20réponse&tags=test;othertag" http://localhost:8080/netheos-challenge/faq`  
`curl --data "format=json&request_type=get_all&username=admin&password=jGrC4Kp3Nr30" http://localhost:8080/netheos-challenge/faq --get`  
`curl --data "format=json&request_type=get_match&match=it" http://localhost:8080/netheos-challenge/faq --get`  

Ces données permettent de tester certains cas particuliers comme les caractères spéciaux de https://github.com ou le 'é' de 'Une réponse', pour pouvoir vérifier sur le navigateur que l'encodage est correct.


**Choix techniques :**

*Base de données :*
J'ai choisi MongoDB, un moteur de base de données NoSQL open-source. Je l'avais peu utilisé auparavant, mais il me semblait plus adapté que d'autres choix plus classiques tels que MySQL. L'aspect NoSQL n'impose pas de créer des tables avec un schéma précis et était donc plus simple à mettre en place, notamment pour un projet de ce genre. De plus, MongoDB permettrait facilement de chercher les FAQ possédant un ou des tags particuliers, une feature qui serait probablement demandée rapidement.  
Pour l'instant, les collections sont très simples, mais on pourrait avoir besoin plus tard de documents dans une même collection avec des attributs différents, ce qui est impossible en SQL. Ensuite, même s'il est difficile d'estimer les besoins futurs pour un projet de ce genre, MongoDB est connu pour traiter efficacement les données, même en très grande quantité. Enfin, MongoDB est moins sensible aux attaques de type "injection SQL", car la requête n'est pas parsée comme en SQL. Il y a toujours le risque de code Javascript dans le JSON, j'ai donc assaini les entrées utilisateurs en enlevant les caractères spéciaux. La conséquence est que les expressions régulières sont beaucoup moins puissantes lors des recherches de questions (.* est utilisable, mais pas {} par exemple). Bien sûr, veut-on seulement que les utilisateurs puissent utiliser des expressions régulières ? De la réponse à cette question pourrait dépendre l'implémentation des futures protections anti-injections.

*Vue :*
Ce projet étant côté serveur, la vue a été réduite au strict minimum. Dans une optique MVC, elle n'est pas présente dans la servlet Java, mais dans un fichier JSP, plus adapté à l'écriture de HTML qu'un fichier Java classique.  
Le résultat peut être visible via un navigateur, ou plus simplement grâce aux commandes curl fournies plus haut.  
Les données renvoyées au client sont donc au final du HTML pour s'assurer du fonctionnement, mais selon les besoins du [front-end](https://github.com/Netheos/Challenge-developpement-frontend), toutes sortes de format et/ou de données seraient envisageables (XML, de simples chaînes en langage naturel, JSON, des objets Java...)

*Sécurité :*
La gestion du statut d'administrateur est ici basique et peu fiable, car elle n'est protégée que par un mot de passe, qui se trouve en clair dans le code.  
Les choix concernant la sécurité et leur implémentation sont un sujet complexe et important dépassant le cadre de ce projet de présentation, et c'est pourquoi cette protection sommaire a été implémentée (voir la Javadoc de src/main/java/com/netheos/SecurityManager pour plus d'informations).  
Certaines sécurités additionnelles ont tout de même été prises, telle que la protection contre les injections "SQL", ou le placement du fichier JSP sous WEB-INF afin qu'il ne soit pas accessible directement. Les données de l'utilisateurs sont purifiées et ne posent pas de problèmes même en cas de chaîne vide (par exemple les tags vides ne sont tout simplement pas ajoutés à la base). Enfin, divers tests Junit testent les fonctions basiques (échec comme réussite), de nombreux autres pourraient et devraient être ajoutés. Dû à son statut particulier, la servlet elle-même ne peut pas être testée de façon classique, mais à long terme, il serait envisageable d'utiliser des frameworks comme Mockito afin de simuler les appels HTTP à la servlet.

*Divers :*
Les questions/réponses sont tirées de la base de données, rassemblées en listes pour être au final compilées en une seule String. C'est bien sûr redondant pour l'instant, mais sera plus pratique et plus facile à gérer lorsque les interactions seront plus complexes (si par exemple on devait les envoyer sous forme de tableau au front-end).  
Le style de formattage du code (camelCase, accolades sur la même ligne, etc) est celui que j'utilise habituellement mais pourrait bien entendu être différent si l'équipe possède un guide de style à suivre.  
La javadoc et le site du projet ont été générés et sont disponibles dans target/site/apidocs.  
Je suis conscient que le JSON tiré de la collection et envoyé à l'utilisateur contient les id utilisés par MongoDB. Je ne suis pas complètement sûr si cela pourrait poser un problème de sécurité (est-ce l'utilisateur qui les reçoit ou les développeurs front-end ?). Il serait en tout cas facile de les en enlever.  
Mongo 2 est parfois encore utilisé, peut-être même dans certains anciens projets avec lesquels on pourrait vouloir communiquer, mais comme ce projet a été écrit à partir de rien, j'ai préféré utiliser uniquement la version à jour et recommandée de MongoDB, Mongo 3.  
De nombreux autres choix techniques mineurs sont mentionnés dans les commentaires et la Javadoc, la plupart dus au statut "artificiel" de ce projet, n'hésitez-pas à les lire.
