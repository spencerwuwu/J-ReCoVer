// https://searchcode.com/api/result/12331907/

package edu.macalester.acs.server;

import net.freeutils.httpserver.HTTPServer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import org.json.simple.parser.JSONParser;
import org.json.simple.JSONValue;
import edu.macalester.acs.AutocompleteTree;
import edu.macalester.acs.AutocompleteEntry;

/**
 * A simple, extensible http autocomplete server.<p>
 *
 * This file's main method takes two arguments: a port to listen on,
 * and a file that contains a transaction log of autocomplete entities.
 *
 * An autocomplete entity is an extensible (key, value) hashtables
 * that supports any types supported by json.  Three keys are required:
 * <ul>
 * <li>id (string) - the primary key in the autocomplete tre.
 * <li>name (string) - the string autocomplete queries are matched against.
 * <li>score (int or double) - the score associated with the entity in
 * the autocomplete tree; used when too many entities match an autocomplete
 * search.
 * </ul>
 * The server supports any other key, value fields specified by clients.<p>
 *
 * This server supports three actions:<p>
 *
 * <ul>
 * <li><i>update</i> updates an entry in the tree.  The HTTP method must be
 * POST, and the body of the post must be a serialized JSON hashtable with
 * (at a minimum) the three required fields (id, name, score).  The tree
 * stores the entry, and replaces any existing internal entries.  The server
 * writes the updates to the transaction log. <p>
 *
 * <li><i>autocomplete</i> returns entities that match the required "query"
 * parameter.  The optional "max" parameter specifies the maximum number of
 * results returned.  The result format is a JSON-serialized list of entries
 * in the same format that they are specified to the "update" action. <p>
 *
 * <li><i>dump</i>Clears the transaction log, and then writes the current
 * state of entities out to the log.  Though the update() command updates
 * the transaction log, if you have many updates to the same object (say
 * frequency updates), then dump() will reduce the size of the log and
 * therefore reduce your startup time.                                    <p>
 * </ul>
 *
 * The server reads the transaction log every time it starts up.<p>
 *
 * Python example of reading from, and writing to the server:
 *
 * <pre><blockquote>
        import httplib

        conn = httplib.HTTPConnection("localhost", 10101)
        conn.request("POST", "/update", '{ "id" : "34a", "name" : "Bob", "score" : 300.2, "foo" : "bar"}')
        print conn.getresponse().read()

        >>> okay

        # execute an ajax query
        conn.request("GET", "/autocomplete?query=b&max=2")
        print conn.getresponse().read()

        >>> [{"id":"34a","name":"Bob","score":300.2,"foo":"bar"},
        {"id":"20395","name":"Myrtle Beach","state":"South Carolina","score":99.9812292931998}]
  </blockquote></pre>
 *
 * TODO:
 * <ul>
 *  <li>Add option controlling use of flushing.
 *  <li>Add option for in-memory use without transaction file.
 *  <li>Figure out security scheme, especially for write actions.
 *  <li>Switch to Jetty.
 *  <li>Understand where the .5 milli overhead comes from and optimize accordingly.
 * </ul>
 * 
 * @author Shilad Sen
 */
public class AutocompleteServer {
    private Logger log = Logger.getLogger(getClass().getName());
    
    private static final String PATH_UPDATE = "/update";
    private static final String PATH_DUMP = "/dump";
    private static final String PATH_AUTOCOMPLETE = "/autocomplete";
    private static final String PATH_RELOAD = "/reload";

    BufferedWriter writer;
    final File txPath;
    JSONParser parser;

    HTTPServer server;
    AutocompleteTree<String, AbstractEntity> tree;

    /**
     * Creates a new autocomplete server
     * @param tree The autocomplete tree
     * @param txPath The transaction path (populates and persists the tree)
     * @param port The port to listen on
     * @throws IOException
     */
    public AutocompleteServer(AutocompleteTree<String, AbstractEntity> tree, File txPath, int port) throws IOException {
        this.txPath = txPath;
        this.tree = tree;
        server = new HTTPServer(port);
        parser = new JSONParser();
        writer = new BufferedWriter(new FileWriter(txPath, true));
        HTTPServer.VirtualHost host = server.getVirtualHost(null);
        
        host.addContext("/", new HTTPServer.ContextHandler() {
            public int serve(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
                return handle(request, response);
            }
        });

        reloadData();

    }

    /**
     * Clears the tree and reloads it with entries from the transaction file.
     * @throws IOException
     */
    public void reloadData() throws IOException {
        log.info("loading entries from " + txPath);
        int i = 0;
        synchronized (txPath) {
            tree.clear();
            BufferedReader reader = new BufferedReader(new FileReader(txPath));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                try {
                    AbstractEntity entity = AbstractEntity.deserialize(line.trim());
                    AutocompleteEntry<String, AbstractEntity> entry =
                            new AutocompleteEntry<String, AbstractEntity>(entity.getId(), entity, entity.getScore());
                    tree.add(entry);
                    i++;
                } catch (IllegalArgumentException e) {
                    System.err.println("invalid line: '" + line.trim() + "': " + e.getMessage());
                }
            }
        }
        log.info("loaded " + i + " entries from " + txPath);
    }


    /**
     * Replaces the transaction file with the contents of the current tree.
     * This can significantly reduce the size of the transaction file if there
     * are many updates to entities.
     * 
     * @throws IOException
     */
    public void dump() throws IOException {
        synchronized (txPath) {
            File newFile = new File(txPath.toString() + ".new");
            BufferedWriter newWriter = new BufferedWriter(new FileWriter(newFile));
            for (AutocompleteEntry<String, AbstractEntity> entry : tree.getEntries()) {
                newWriter.write(entry.getValue().serialize() + "\n");
            }
            newWriter.flush();
            newWriter.close();
            writer.close();
            txPath.delete();
            newFile.renameTo(txPath);
            writer = new BufferedWriter(new FileWriter(txPath, true));
        }
    }

    /**
     * Starts the server.
     * @throws IOException
     */
    public void start() throws IOException {
        server.start();
    }

    /**
     * Handles requests to the server.
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    public int handle(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        log.info("Received request " + request.getPath());

        if (request.getPath().startsWith(PATH_DUMP)) {
            dump();
            sendOkay(request, response, "okay");
        } else if (request.getPath().startsWith(PATH_UPDATE)) {
            handleUpdate(request, response);
        } else if (request.getPath().startsWith(PATH_AUTOCOMPLETE)) {
            handleAutocomplete(request, response);
        } else if (request.getPath().startsWith(PATH_RELOAD)) {
            reloadData();
            sendOkay(request, response, "okay");
        } else {
            sendError(400, request, response, "unknown path: " + request.getPath());
        }
        
        return 0;
    }

    /**
     * Completes an autocomplete query request.
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    public int handleAutocomplete(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        Map<String, String> params = request.getParams();
        if (!params.containsKey("query")) {
            return sendError(request, response, "query parameter not specified");
        }
        String query = params.get("query");
        int maxResults = 10;
        if (params.containsKey("max")) {
            maxResults = Integer.parseInt(params.get("max"));
        }
        SortedSet<AutocompleteEntry<String, AbstractEntity>> results = tree.autocomplete(query, maxResults);
        String body = "";
        for (AutocompleteEntry<String, AbstractEntity> entry : results) {
            if (body.length() > 0) {
                body += ",";
            }
            body += entry.getValue().serialize();
        }
        body = "[" + body + "]";
        response.getHeaders().add("Content-type", "application/json");
        return sendOkay(request, response, body);
    }

    /**
     * Completes a request to update an entry in the tree.
     * @param request
     * @param response
     * @return
     * @throws IOException
     */
    public int handleUpdate(HTTPServer.Request request, HTTPServer.Response response) throws IOException {
        if (!request.getMethod().equals("POST")) {
            return sendError(request, response, "only POST method allowed for " + PATH_UPDATE);
        }
        String body = readBody(request);
        Object object = JSONValue.parse(body);
        if (object == null || !(object instanceof Map)) {
            return sendError(request, response, "body must contain a JSON map");
        }
        Map map = (Map)object;
        AbstractEntity entity = null;
        try {
            entity = new AbstractEntity(map);
        } catch (IllegalArgumentException e) {
            return sendError(request, response, e.getMessage());
        }

        if (tree.contains(entity.getId())) {
            tree.remove(entity.getId());
        }
        AutocompleteEntry<String, AbstractEntity> entry =
                new AutocompleteEntry<String, AbstractEntity>(entity.getId(), entity, entity.getScore());
        tree.add(entry);

        String json = entry.getValue().serialize();
        synchronized (txPath) {
            // Should we flush, or not?
            writer.write(json + "\n");
            writer.flush();
        }
        
        return sendOkay(request, response, "okay");
    }

    /**
     * Returns an error message to the client.
     * @param code
     * @param request
     * @param response
     * @param message
     * @return
     * @throws IOException
     */
    private int sendError(int code, HTTPServer.Request request, HTTPServer.Response response, String message) throws IOException {
        log.warning("error occured processing " + request.getPath() + ": " + message);
        if (!message.endsWith("\n")) {
            message += "\n";
        }
        response.sendError(code, message);
        return code;
    }

    /**
     * Returns an error message to the client with status code 500.
     * @param request
     * @param response
     * @param message
     * @return
     * @throws IOException
     */
    private int sendError(HTTPServer.Request request, HTTPServer.Response response, String message) throws IOException {
        return sendError(500, request, response, message);
    }

    /**
     * Returns a message to the client with status 200 (OKAY)
     * @param request
     * @param response
     * @param message
     * @return
     * @throws IOException
     */
    private int sendOkay(HTTPServer.Request request, HTTPServer.Response response, String message) throws IOException {
        if (!message.endsWith("\n")) {
            message += "\n";
        }
        response.send(200, message);
        return 200;
    }

    /**
     * Returns the contents of an HTTP post request as a string.
     * @param request
     * @return
     * @throws IOException
     */
    private String readBody(HTTPServer.Request request) throws IOException {
        String body = "";
        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getBody()));
        while (true) {
            String line = reader.readLine();
            if (line == null)
                break;
            body += line;
        }
        return body;
    }

    private void loadCities() throws IOException {
        File path = new File("data/cities.txt");
        BufferedReader reader = new BufferedReader(new FileReader(path));
        Random random = new Random();
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            String [] tokens = line.trim().split(",", 3);
            if (tokens.length != 3 || tokens[0].length() == 0 || tokens[2].length() == 0) {
                System.err.println("bad line in '" + path + "': " + line.trim());
                continue;
            }
            String id = tokens[0];
            String state = tokens[1];
            String name = tokens[2];
            double score = random.nextDouble() * 100;
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("id", id);
            map.put("name", name);
            map.put("state", state);
            map.put("score", new Double(score));
            AbstractEntity entity = new AbstractEntity(map);
            AutocompleteEntry<String, AbstractEntity> entry =
                    new AutocompleteEntry<String, AbstractEntity>(entity.getId(), entity, entity.getScore());
            tree.add(entry);
        }
        dump();
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        if (args.length == 0) {
            System.err.printf("Usage: %s txLog port%n",
                AutocompleteServer.class.getName());
            return;
        }
        File txPath = new File(args[0]);
        int port = Integer.parseInt(args[1]);
        AutocompleteTree<String, AbstractEntity> tree
                = new AutocompleteTree<String, AbstractEntity>();
        AutocompleteServer server = new AutocompleteServer(tree, txPath, port);
        server.start();
//        server.loadCities();
        System.out.println("server listening on port " + port);

        // Hack: what is correct?
        while (true) {
            Thread.sleep(100000);
        }
    }

}

