<%@ include file="/init.jsp" %>

<%
JSONArray hitsJSONArray = (JSONArray)request.getAttribute("hits");
%>

<h1>My Documents</h1>

<ul class="sample-list">

	<%
	for (int i = 0; i < hitsJSONArray.length(); i++) {
		JSONObject hitJsonObject = hitsJSONArray.getJSONObject(i);
	%>

		<li><img src="<%= hitJsonObject.getString("imageSrc") %>" />
			<a href="<%= hitJsonObject.getString("viewURL") %>"><%= hitJsonObject.getString("title") %></a>
		</li>

	<%
	}
	%>

</ul>