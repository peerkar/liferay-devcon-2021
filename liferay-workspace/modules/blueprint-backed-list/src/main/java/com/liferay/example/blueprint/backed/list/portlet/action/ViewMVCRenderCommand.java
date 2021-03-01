package com.liferay.example.blueprint.backed.list.portlet.action;

import com.liferay.example.blueprint.backed.list.constants.BlueprintBackedListPortletKeys;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.json.JSONFactory;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCRenderCommand;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.ResourceBundleUtil;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.search.searcher.SearchResponse;
import com.liferay.portal.search.tuning.blueprints.attributes.BlueprintsAttributes;
import com.liferay.portal.search.tuning.blueprints.attributes.BlueprintsAttributesBuilder;
import com.liferay.portal.search.tuning.blueprints.engine.util.BlueprintsEngineHelper;
import com.liferay.portal.search.tuning.blueprints.message.Messages;
import com.liferay.portal.search.tuning.blueprints.model.Blueprint;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.SearchResponseJSONTranslator;
import com.liferay.portal.search.tuning.blueprints.searchresponse.json.translator.constants.ResponseAttributeKeys;
import com.liferay.portal.search.tuning.blueprints.service.BlueprintService;
import com.liferay.portal.search.tuning.blueprints.util.attributes.BlueprintsAttributesHelper;

import java.util.ResourceBundle;

import javax.portlet.PortletPreferences;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + BlueprintBackedListPortletKeys.BLUEPRINTBACKEDLIST,
		"mvc.command.name=/"
	},
	service = MVCRenderCommand.class
)
public class ViewMVCRenderCommand implements MVCRenderCommand {

	@Override
	public String render(
		RenderRequest renderRequest, RenderResponse renderResponse) {

		Blueprint blueprint = _getBlueprint(renderRequest);

		if (blueprint == null) {
			return "/error.jsp";
		}

		JSONObject hitsJSONObject = _getResults(
			renderRequest, renderResponse, blueprint);

		if (hitsJSONObject != null) {
			renderRequest.setAttribute("hits", hitsJSONObject.get("hits"));
		}

		return "/view.jsp";
	}

	private Blueprint _getBlueprint(RenderRequest renderRequest) {
		PortletPreferences preferences = renderRequest.getPreferences();

		long blueprintId = GetterUtil.getLong(
			preferences.getValue("blueprintId", "0"));

		if (blueprintId == 0) {
			return null;
		}

		try {
			return _blueprintService.getBlueprint(blueprintId);
		}
		catch (PortalException portalException) {
			_log.error(portalException.getMessage(), portalException);
		}

		return null;
	}

	private BlueprintsAttributes _getBlueprintsRequestAttributes(
		RenderRequest renderRequest, Blueprint blueprint) {

		BlueprintsAttributesBuilder blueprintsAttributesBuilder =
			_blueprintsAttributesHelper.getBlueprintsRequestAttributesBuilder(
				renderRequest, blueprint);

		return blueprintsAttributesBuilder.build();
	}

	private BlueprintsAttributes _getBlueprintsResponseAttributes(
		RenderRequest renderRequest, RenderResponse renderResponse,
		BlueprintsAttributes blueprintsRequestAttributes, Blueprint blueprint) {

		BlueprintsAttributesBuilder blueprintsAttributesBuilder =
			_blueprintsAttributesHelper.getBlueprintsResponseAttributesBuilder(
				renderRequest, renderResponse, blueprint,
				blueprintsRequestAttributes);

		blueprintsAttributesBuilder.addAttribute(
			ResponseAttributeKeys.INCLUDE_RESULT, true);

		blueprintsAttributesBuilder.addAttribute(
			ResponseAttributeKeys.INCLUDE_RESULT_THUMBNAIL, true);

		return blueprintsAttributesBuilder.build();
	}

	private ResourceBundle _getResourceBundle(RenderRequest renderRequest) {
		ThemeDisplay themeDisplay = (ThemeDisplay)renderRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		return ResourceBundleUtil.getBundle(
			"content.Language", themeDisplay.getLocale(), getClass());
	}

	private JSONObject _getResults(
		RenderRequest renderRequest, RenderResponse renderResponse,
		Blueprint blueprint) {

		try {
			Messages messages = new Messages();

			BlueprintsAttributes blueprintsRequestAttributes =
				_getBlueprintsRequestAttributes(renderRequest, blueprint);

			SearchResponse searchResponse = _blueprintsEngineHelper.search(
				blueprint, blueprintsRequestAttributes, messages);

			return _translateResponse(
				renderRequest, renderResponse, blueprint,
				blueprintsRequestAttributes, searchResponse, messages);
		}
		catch (Exception exception) {
			_log.error(exception.getMessage(), exception);
		}

		return null;
	}

	private JSONObject _translateResponse(
		RenderRequest renderRequest, RenderResponse renderResponse,
		Blueprint blueprint, BlueprintsAttributes blueprintsRequestAttributes,
		SearchResponse searchResponse, Messages messages) {

		BlueprintsAttributes blueprintsResponseAttributes =
			_getBlueprintsResponseAttributes(
				renderRequest, renderResponse, blueprintsRequestAttributes,
				blueprint);

		try {
			return _searchResponseJSONTranslator.translate(
				searchResponse, blueprint, blueprintsResponseAttributes,
				_getResourceBundle(renderRequest), messages);
		}
		catch (PortalException portalException) {
			_log.error(portalException.getMessage(), portalException);
		}

		return null;
	}

	private static final Log _log = LogFactoryUtil.getLog(
		ViewMVCRenderCommand.class);

	@Reference
	private BlueprintsAttributesHelper _blueprintsAttributesHelper;

	@Reference
	private BlueprintsEngineHelper _blueprintsEngineHelper;

	@Reference
	private BlueprintService _blueprintService;

	@Reference
	private JSONFactory _jsonFactory;

	@Reference
	private SearchResponseJSONTranslator _searchResponseJSONTranslator;

}