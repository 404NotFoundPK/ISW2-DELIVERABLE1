import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class GetJiraIssues {
    public static void main(String[] args) throws Exception {
		Logger logger = Logger.getLogger(GetJiraIssues.class.getName());

		Map<String, String> releaseID;
		List<String> releases = new ArrayList<String>();
        String projName ="KAFKA";
		
		//Fills the arraylist with issues dates
		//Ignores issues with missing dates
		Integer i;
		String url = buildURL(projName);
		
		JSONObject json = readJsonFromUrl(url);
		var issues = json.getJSONArray("issues");
		releaseID = new HashMap<String, String>();
		for (i = 0; i < issues.length(); i++ ) {
			var key = "";
			if(issues.getJSONObject(i).has("key")) {
				key = issues.getJSONObject(i).get("key").toString();
				JSONObject fields = (JSONObject) issues.getJSONObject(i).get("fields");
				var releaseDate = fields.get("resolutiondate").toString();
				addRelease(releases, releaseID, releaseDate, key);
			}
		}

		// order releases by date
		Collections.sort(releases);  

		if (releases.size() < 6)
			return;
		String outname = projName + "-issues.csv";
		try(FileWriter fileWriter = new FileWriter(outname)) {
			//Name of CSV for output
			fileWriter.append("Issue;Date");
			fileWriter.append("\n");

			for ( i = 0; i < releases.size(); i++) {
				fileWriter.append(releases.get(i));
				fileWriter.append(";");
				fileWriter.append(releaseID.get(releases.get(i)));
				fileWriter.append("\n");
			}

			fileWriter.flush();

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in csv writer");
			e.printStackTrace();
		}
	}

	public static String buildURL(String projName) {
		var base = "https://issues.apache.org/jira/rest/api/2/search?jql=";
		var issueType = "New%20Feature";
		var status1 = "resolved";
		var status2 = "closed";
		var resolution = "fixed";
		var maxResults = "150";

		return base + "project=%22" + projName + "%22AND%22issueType%22=%22"
		+ issueType + "%22AND(%22status%22=%22"
		+ status1 + "%22OR%22status%22=%22"
		+ status2 + "%22)AND%22resolution%22=%22"
		+ resolution + "%22&fields=key,resolutiondate&maxResults=" + maxResults + "&startAt=0";
	}
			
	public static void addRelease(List<String> releases, Map<String, String> releaseID, String strDate, String id) throws ParseException {
		// parse date from json format
		strDate = strDate.replace("T", " ");
		strDate = strDate.replace(".000+0000", "");
		DateFormat fmtForParser = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		var date = fmtForParser.parse(strDate);

		// format date to excel format
		DateFormat fmtForFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
		String dateTime = fmtForFormat.format(date);
		if (!releases.contains(id)) {
			releases.add(id);
			releaseID.put(id, dateTime);
		}
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try(var rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
			String jsonText = readAll(rd);
			return new JSONObject(jsonText);
		} finally {
			is.close();
		}
	}
	
	private static String readAll(BufferedReader rd) throws IOException {
		var sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
