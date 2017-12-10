
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HttpsURLConnection;

/**
 * This class uploads to a hastebin client.
 *
 * @author aoutler
 */
public class HastebinLogger {

    /**
     * Uploads content to a Hastebin client. This will split the content into
     * mulitple parts.
     *
     * @param content The content to be sent to the hastebin server
     * @param server The server to be uploaded to.
     * @param credentials username and password. These should be sent in
     * "username", "password" order.
     * @return link to the hastebin.
     * @throws IOException if an error occurs, the error will be returned in
     * exception format.
     */
    public static String uploadToPastebin(String content, String server, String... credentials) throws IOException {
        List<String> parts = splitIntoSizedChunks(content);
        String lastLink = null;
        for (String part : parts) {
            if (lastLink != null) {
                part += "\n" + lastLink;
            }
            lastLink = upload(part, server, credentials);
        }
        return lastLink;
    }

    private static String upload(String content, String serverName, String... credentials) throws IOException {
        URLConnection connection = null;
        try {
            connection = getConnection(serverName);
            connection.setRequestProperty("Content-length", String.valueOf(content.length()));
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "Mozilla/4.0");
            ((HttpURLConnection) connection).setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            if (credentials.length == 2) {
                String encoded = Base64.getEncoder().encodeToString((credentials[0] + ":" + credentials[1]).getBytes(StandardCharsets.UTF_8));  //Java 8
                connection.setRequestProperty("Authorization", "Basic " + encoded);
            }
            writeData(connection.getOutputStream(), content);

            return getLink(connection.getInputStream(), serverName);
        } finally {
            if (connection != null) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }

    private static URLConnection getConnection(String serverName) throws MalformedURLException, IOException {
        URL url = new URL(serverName + "/documents");
        if (serverName.startsWith("https")) {
            return ((HttpsURLConnection) url.openConnection());
        } else {
            return ((HttpURLConnection) url.openConnection());
        }
    }

    private static void writeData(OutputStream outputStream, String content) throws IOException {
        try (DataOutputStream wr = new DataOutputStream(outputStream)) {
            wr.writeBytes(content);
        }
    }

    private static String getLink(InputStream inputStream, String servername) throws IOException {
        String key;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            Gson parser = new GsonBuilder().create();
            StringBuilder sb = new StringBuilder();
            while (reader.ready()) {
                sb.append(reader.readLine());
            }

            LinkedTreeMap json = parser.fromJson(sb.toString(), LinkedTreeMap.class);

            key = (String) json.get("key");
        }

        return servername + "/" + key;
    }

    private static List splitIntoSizedChunks(String content) {
        ArrayList<String> sortedSet = new ArrayList<>();
        int index = 0;
        while (index < content.length()) {
            sortedSet.add(content.substring(index, Math.min(index + 390000, content.length())));
            index += 390000;

        }
        return sortedSet;
    }

    /**
     * example usage
     *
     * @param args "http://server.com" "data to log"
     */
    public static void main(String[] args) {
        String log;
        String server;
        if (args.length == 2) {
            server = args[0];
            log = args[2];
        } else {
            log = "Hastebin Client written by Adam Outler.\nusage: this.jar <server> <data> [username] [password]\n  eg.\n java -jar this.jar \"http(s)://server.com\" \"data I want to log\" \"[username]\" \"[password]\" ";
            server = "https://pastebin.adamoutler.com";
        }

        try {
            System.out.println(HastebinLogger.uploadToPastebin(log, server)
            );

        } catch (IOException ex) {
            Logger.getLogger(HastebinLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
