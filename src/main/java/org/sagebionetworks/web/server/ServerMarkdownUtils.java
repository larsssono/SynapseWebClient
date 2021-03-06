package org.sagebionetworks.web.server;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.pegdown.PegDownProcessor;
import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.widget.entity.registration.WidgetConstants;

public class ServerMarkdownUtils {
	
	/**
	 * This converts the given markdown to html using the given markdown processor.
	 * It also post processes the output html, including:
	 * *sending all links to a new window.
	 * *applying the markdown css classname to entities supported by the markdown.
	 * *auto detects Synapse IDs (and creates links out of them)
	 * *auto detects generic urls (and creates links out of them)
	 * *resolve Widgets!
	 * @param panel
	 */
	public static String markdown2Html(String markdown, String attachmentUrl, Boolean isPreview, PegDownProcessor markdownProcessor) {
		if (markdown == null) return "";
		//before processing, replace all '\n' with '  \n' so that all newlines are correctly interpreted as manual breaks!
		if (markdown != null) {
			markdown = markdown.replace("\n", "  \n");
		}
		String html = markdownProcessor.markdownToHtml(markdown);
		if (html == null) {
			//if the markdown processor fails to convert the md to html (will return null in this case), return the raw markdown instead. (as ugly as it might be, it's better than no information).
			return markdown; 
		}
		//using jsoup, since it's already in this project!
		Document doc = Jsoup.parse(html);
		ServerMarkdownUtils.sendAllLinksToNewWindow(doc);
		Elements anchors = doc.getElementsByTag("a");
		anchors.addClass("link");
		//TODO: remove old attachment image syntax support (now that we have widgets)?
		ServerMarkdownUtils.resolveAttachmentImages(doc, attachmentUrl);
		ServerMarkdownUtils.addWidgets(doc, isPreview);
		ServerMarkdownUtils.addSynapseLinks(doc);
		//URLs are automatically resolved from the markdown processor
		String returnHtml = "<div class=\"markdown\">" + doc.html() + "</div>";
		return returnHtml;
	}

	public static void sendAllLinksToNewWindow(Document doc) {
		Elements elements = doc.getElementsByTag("a");
		elements.attr("target", "_blank");
	}

	public static void resolveAttachmentImages(Document doc, String attachmentUrl) {
		Elements images = doc.select("img");
		for (Iterator iterator = images.iterator(); iterator.hasNext();) {
			Element img = (Element) iterator.next();
			String src = img.attr("src");
			if (src.startsWith(DisplayConstants.ENTITY_DESCRIPTION_ATTACHMENT_PREFIX)){
		    	String[] tokens = src.split("/");
		    	if (tokens.length > 5) {
			        String entityId = tokens[2];
				    String tokenId = tokens[4] +"/"+ tokens[5];
				    img.attr("src", createAttachmentUrl(attachmentUrl, entityId, tokenId, tokenId,DisplayUtils.ENTITY_PARAM_KEY));
		    	}
			}
		}
	}


	/**
	 * Create the url to an attachment image.
	 * @param baseURl
	 * @param id
	 * @param tokenId
	 * @param fileName
	 * @return
	 */
	public static String createAttachmentUrl(String baseURl, String id, String tokenId, String fileName, String paramKey){
	        StringBuilder builder = new StringBuilder();
	        builder.append(baseURl);
	        builder.append("?"+paramKey+"=");
	        builder.append(id);
	        builder.append("&"+DisplayUtils.TOKEN_ID_PARAM_KEY+"=");
	        builder.append(tokenId);
	        builder.append("&"+DisplayUtils.WAIT_FOR_URL+"=true");
	        return builder.toString();
	}

	public static void addSynapseLinks(Document doc) {
		// in this case, I still need a regular expression to find the synapse ids.
		// find all elements whose text contains a synapse id pattern (but not anchors)
		// replace the TextNode element children with Elements, whose html contain a link to relevant synapse entity.
		// regular expression: look for non-word characters (0 or more), followed by "syn" and a number, followed by more non-word characters (0 or more).
		// capture the synapse id in a group (the paranthesis).
		String regEx = "\\W*(syn\\d+)\\W*";
		Elements elements = doc.select("*:matchesOwn(" + regEx + "):not(a,code)");  	// selector is case insensitive
		Pattern pattern = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
		for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			//only process the TextNode children (ignore others)
			for (Iterator iterator2 = element.childNodes().iterator(); iterator2.hasNext();) {
				Node childNode = (Node) iterator2.next();
				if (childNode instanceof TextNode) {
					String oldText = ((TextNode) childNode).text();
					// find it in the text
					Matcher matcher = pattern.matcher(oldText);
					StringBuilder sb = new StringBuilder();
					int previousFoundIndex = 0;
					while (matcher.find() && matcher.groupCount() == 1) {
						sb.append(oldText.substring(previousFoundIndex, matcher.start(1)));
						sb.append(ServerMarkdownUtils.getSynAnchorHtml(matcher.group(1))); //the actual synapse Id group (not the non-word characters that might surround it)
						previousFoundIndex = matcher.end(1);
					}
					if (previousFoundIndex < oldText.length() - 1)
						// substring, go from the previously found index to the end
						sb.append(oldText.substring(previousFoundIndex));
					Element newElement = doc.createElement("span"); //wrap new html in a span, since it needs a container!
					newElement.html(sb.toString());
					childNode.replaceWith(newElement);		
				}
			}
		}
	}
	
	public static void addWidgets(Document doc, Boolean isPreview) {
		String suffix = isPreview ? DisplayConstants.DIV_ID_PREVIEW_SUFFIX : "";
		// using a regular expression to find our special widget notation, replace with a div with the widget name
		String regEx = "\\W*?("+WidgetConstants.WIDGET_START_MARKDOWN_ESCAPED+"([^\\}]*)\\})\\W*?"; //reluctant qualification so that it finds multiple per line
		Elements elements = doc.select("*:matchesOwn(" + regEx + ")");  	// selector is case insensitive
		Pattern pattern = Pattern.compile(regEx, Pattern.CASE_INSENSITIVE);
		int widgetsFound = 0;
		for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
			Element element = (Element) iterator.next();
			//only process the TextNode children (ignore others)
			for (Iterator iterator2 = element.childNodes().iterator(); iterator2.hasNext();) {
				Node childNode = (Node) iterator2.next();
				if (childNode instanceof TextNode) {
					String oldText = ((TextNode) childNode).text();
					// find it in the text
					Matcher matcher = pattern.matcher(oldText);
					StringBuilder sb = new StringBuilder();
					int previousFoundIndex = 0;
					while (matcher.find()) {
						if (matcher.groupCount() == 2) {
							sb.append(oldText.substring(previousFoundIndex, matcher.start()));
							sb.append(ServerMarkdownUtils.getWidgetHTML(widgetsFound, suffix, matcher.group(2)));
							widgetsFound++;
							previousFoundIndex = matcher.end(1);
						}
					}
					if (previousFoundIndex < oldText.length() - 1)
						// substring, go from the previously found index to the end
						sb.append(oldText.substring(previousFoundIndex));
					Element newElement = doc.createElement("div"); //wrap new html in a span, since it needs a container!
					newElement.html(sb.toString());
					childNode.replaceWith(newElement);
				}
			}
		}
	}

	public static String getUrlHtml(String url){
		StringBuilder sb = new StringBuilder();
		sb.append("<a target=\"_blank\" class=\"link auto-detected-url\" href=\"");
	    sb.append(url.trim());
	    sb.append("\">");
	    sb.append(url);
	    sb.append("</a>");
	    return sb.toString();
	}

	public static String getSynAnchorHtml(String synId){
		StringBuilder sb = new StringBuilder();
		sb.append("<a target=\"_blank\" class=\"link auto-detected-synapse-link\" href=\"#Synapse:");
	    sb.append(synId.toLowerCase().trim());
	    sb.append("\">");
	    sb.append(synId);
	    sb.append("</a>");
	    return sb.toString();
	}

	public static String getYouTubeHTML(String videoId){
		
		StringBuilder sb = new StringBuilder();
		sb.append("<iframe width=\"560\" height=\"315\" src=\"http://www.youtube.com/embed/");
		sb.append(videoId);
		sb.append("\" frameborder=\"0\" allowfullscreen></iframe>");
	    return sb.toString();
	}
	
	public static String getWidgetHTML(int widgetIndex, String suffix, String widgetProperties){
		StringBuilder sb = new StringBuilder();
		sb.append("<div id=\"");
		sb.append(DisplayConstants.DIV_ID_WIDGET_PREFIX);
		sb.append(widgetIndex);
		sb.append(suffix);
		sb.append("\" widgetParams=\"");
		sb.append(widgetProperties);
		sb.append("\">");
		sb.append("</div>");
	    return sb.toString();
	}

}
