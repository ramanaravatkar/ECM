package com.validation.requestFilter;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import com.ibm.ecm.extension.PluginRequestFilter;
import com.ibm.ecm.extension.PluginServiceCallbacks;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONArtifact;
import com.ibm.json.java.JSONObject;

public class BlankSearchRequestFilter extends PluginRequestFilter {

	private static final boolean LOG_ONLY = false;

	@Override

	public String[] getFilteredServices() {

		return new String[] { "/p8/search" };

	}

	@Override

	public JSONObject filter(PluginServiceCallbacks callbacks,

			HttpServletRequest request,

			JSONArtifact jsonRequest) throws Exception {

		try {

			System.out.println(" path: " + request.getServletPath() + " " + String.valueOf(request.getPathInfo()));

			Enumeration<?> names = request.getParameterNames();

			while (names.hasMoreElements()) {

				String name = String.valueOf(names.nextElement());

				System.out.println(" param "

						+ name + " = " + request.getParameter(name));

			}

			if (jsonRequest != null) {

				System.out.println(" jsonRequest = "

						+ jsonRequest.serialize());

			} else {

				System.out.println("[jsonRequest = null");

			}

		} catch (Throwable t) {

			System.out.println("could not dump request : " + t);

		}

		if (LOG_ONLY) {

			return null;

		}

		try {

			if (isBlankSearch(request, jsonRequest)) {

				System.out.println("Blocking blank search.");

				return buildErrorResponse();

			}

		} catch (Throwable t) {

			System.out.println(" Validation failed : " + t);

		}

		return null;

	}

	private boolean isBlankSearch(HttpServletRequest request,

			JSONArtifact jsonRequest) throws Exception {

		String criteriasParam = request.getParameter("criterias");

		if (criteriasParam != null && criteriasParam.trim().length() > 0) {

			JSONArtifact parsed = JSONArray.parse(criteriasParam);

			if (parsed instanceof JSONArray) {

				JSONArray arr = (JSONArray) parsed;

				for (int i = 0; i < arr.size(); i++) {

					if (criterionHasInput(arr.get(i))) {

						return false;

					}

				}

			}

		}

		if (hasTextSearch(request.getParameter("json_post"))) {

			return false;

		}

		return true;

	}

	private boolean criterionHasInput(Object criterion) {

		if (!(criterion instanceof JSONObject)) {

			return false;

		}

		JSONObject c = (JSONObject) criterion;

		Object op = c.get("operator");

		if (op != null) {

			String operator = String.valueOf(op).trim().toUpperCase();

			if ("NULL".equals(operator)

					|| "NOTNULL".equals(operator)) {

				return true;

			}

		}

		return nonEmpty(c.get("values"));

	}

	private boolean hasTextSearch(String jsonPost) {

		if (jsonPost == null || jsonPost.trim().isEmpty()) {

			return false;

		}

		try {

			JSONObject post = JSONObject.parse(jsonPost);

			Object list = post.get("search_text_criteria");

			if (list instanceof JSONArray) {

				JSONArray arr = (JSONArray) list;

				for (int i = 0; i < arr.size(); i++) {

					if (arr.get(i) instanceof JSONObject && nonEmpty(((JSONObject) arr.get(i)).get("text"))) {

						return true;

					}

				}

			}

			Object single = post.get("textSearchCriteria");

			if (single instanceof JSONObject && nonEmpty(((JSONObject) single).get("text"))) {

				return true;

			}

		} catch (Exception e) {

			System.out.println("json_post parse failed : " + e);

			// fail open

			return true;

		}

		return false;

	}

	private boolean nonEmpty(Object value) {

		if (value == null) {

			return false;

		}

		if (value instanceof JSONArray) {

			JSONArray arr = (JSONArray) value;

			for (int i = 0; i < arr.size(); i++) {

				if (nonEmpty(arr.get(i))) {

					return true;

				}

			}

			return false;

		}

		return String.valueOf(value).trim().length() > 0;

	}

	private JSONObject buildErrorResponse() {

		JSONObject error = new JSONObject();

		error.put("number", "0");

		error.put("text", "");

		error.put("explanation", "");

		error.put("userResponse", "");

		error.put("moreInformation", "");

		JSONArray errors = new JSONArray();

		errors.add(error);

		JSONObject response = new JSONObject();

		response.put("errors", errors);

		return response;

	}

}
