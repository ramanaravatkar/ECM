package com.validation.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.filenet.api.core.Factory;
import com.ibm.ecm.extension.PluginService;
import com.ibm.ecm.extension.PluginServiceCallbacks;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;

public class GetAllowedOperationsService extends PluginService {

	//private static final String PROPERTIES_FILE = "E:/ESUN_PLUGIN/conf/application.properties";
	private static final String PROPERTIES_FILE = "C:/ICN_Config/application.properties";

	private String jsonFilePath;

	@Override
	public String getId() {
		return "GetAllowedOperationsService";
	}

	private void loadConfiguration() throws Exception {

		if (jsonFilePath != null) {
			return;
		}

		Properties props = new Properties();
		System.out.println("Properties file path = " + PROPERTIES_FILE);

		File file = new File(PROPERTIES_FILE);

		try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE)) {

			props.load(fis);
		}

		jsonFilePath = props.getProperty("group.json.path");

		if (jsonFilePath == null || jsonFilePath.trim().isEmpty()) {
			throw new IllegalStateException("group.json.path not found in application.properties");
		}

	}

	@Override
	public void execute(PluginServiceCallbacks callbacks, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		loadConfiguration();

		String repositoryId = request.getParameter("repositoryId");

		System.out.println("repositoryId: " + repositoryId);

		JSONObject config = loadConfig();
		JSONArray managed = asArray(config.get("managedOperations"));
		JSONObject groupsMap = (JSONObject) config.get("groups");

		JSONObject defaultConfig = (JSONObject) config.get("default");

		Set<String> userGroups = resolveUserGroups(callbacks, repositoryId);

		for (String g : userGroups) {
			System.out.println("  -> " + g);
		}

		Set<String> allowed = new LinkedHashSet<String>();
		Set<String> allowedFeatures = new LinkedHashSet<String>();
		boolean matchedAnyGroup = false;

		if (groupsMap != null) {

			for (Object keyObj : groupsMap.keySet()) {

				String groupName = String.valueOf(keyObj);

				System.out.println("Checking JSON Group : " + groupName);

				boolean exists = userGroups.contains(groupName.toLowerCase(Locale.ROOT));

				System.out.println("Match Found  : " + exists);

				if (exists) {

					matchedAnyGroup = true;

					JSONObject groupConfig = (JSONObject) groupsMap.get(keyObj);

					JSONArray ops = asArray(groupConfig.get("operations"));

					if (ops != null) {
						for (Object op : ops) {
							allowed.add(String.valueOf(op));
						}
					}

					JSONArray features = asArray(groupConfig.get("features"));

					if (features != null) {
						for (Object feature : features) {
							allowedFeatures.add(String.valueOf(feature));
						}
					}
				}
			}
		}

		if (!matchedAnyGroup && defaultConfig != null) {
			JSONArray defaultOps = asArray(defaultConfig.get("operations"));
			System.out.println("No group matched. Using DEFAULT operations.");
			if (defaultOps != null) {
				for (Object op : defaultOps) {
					allowed.add(String.valueOf(op));
				}
			}
			JSONArray defaultFeatures = asArray(defaultConfig.get("features"));
			if (defaultFeatures != null) {
				for (Object feature : defaultFeatures) {
					allowedFeatures.add(String.valueOf(feature));
				}
			}

		}

		System.out.println("Final Allowed Operations = " + allowed);

		JSONObject out = new JSONObject();
		out.put("managedOperations", managed);

		JSONArray featureArr = new JSONArray();
		featureArr.addAll(allowedFeatures);
		out.put("allowedFeatures", featureArr);

		JSONArray allowedArr = new JSONArray();
		allowedArr.addAll(allowed);
		out.put("allowedOperations", allowedArr);

		JSONArray groupsArr = new JSONArray();
		groupsArr.addAll(userGroups);
		out.put("userGroups", groupsArr);

		response.setContentType("application/json; charset=UTF-8");
		PrintWriter w = response.getWriter();
		w.write(out.serialize());
		w.flush();
		w.close();
	}

	private JSONObject loadConfig() throws Exception {

		InputStream is = new FileInputStream(jsonFilePath);

		try {
			return JSONObject.parse(is);
		} finally {
			is.close();
		}
	}

	private Set<String> resolveUserGroups(PluginServiceCallbacks callbacks, String repositoryId) {

		Set<String> groups = new LinkedHashSet<String>();

		try {

			System.out.println("Getting P8 Connection...");

			com.filenet.api.core.Connection conn = callbacks.getP8Connection(repositoryId);

			System.out.println("Connection = " + conn);

			System.out.println("Fetching Current User...");

			com.filenet.api.security.User user = Factory.User.fetchCurrent(conn, null);

			System.out.println("Current User Object = " + user);

			System.out.println("Current User Name = " + user.get_DisplayName());

			com.filenet.api.collection.GroupSet memberOf = user.get_MemberOfGroups();

			Iterator<?> it = memberOf.iterator();

			while (it.hasNext()) {

				com.filenet.api.security.Group g = (com.filenet.api.security.Group) it.next();

				System.out.println("Group Name = " + g.get_Name());

				groups.add(g.get_Name().toLowerCase(Locale.ROOT));

				if (g.get_DistinguishedName() != null) {
					groups.add(g.get_DistinguishedName().toLowerCase(Locale.ROOT));
				}
			}

		} catch (Throwable t) {

			t.printStackTrace();

		}

		return groups;
	}

	private static JSONArray asArray(Object o) {
		return (o instanceof JSONArray) ? (JSONArray) o : new JSONArray();
	}
}
