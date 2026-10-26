package it.maicol07.mmail.ui.components

import org.jsoup.Jsoup
import org.jsoup.safety.Cleaner
import org.jsoup.safety.Safelist
import it.maicol07.mmail.data.RenderingOptions

internal fun emailHtmlDocument(html: String, options: RenderingOptions = RenderingOptions()): String {
    val source = Jsoup.parse(html)
    source.body().prependChildren(source.head().select("style,link[rel=stylesheet],script"))
    val safelist = Safelist.relaxed()
        .addTags("style", "font", "center")
        .addAttributes(":all", "style", "class", "id", "dir", "lang", "align", "width", "height", "bgcolor")
        .addAttributes("font", "color", "face", "size")
        .addAttributes("table", "cellpadding", "cellspacing", "border")
        .addAttributes("td", "valign")
    if (options.images) safelist.addProtocols("img", "src", "data")
    if (options.externalStyles) {
        safelist.addTags("link").addAttributes("link", "rel", "href", "type", "media")
            .addProtocols("link", "href", "https")
    }
    if (options.javaScript) {
        safelist.addTags("script").addAttributes("script", "src", "type", "defer", "async")
            .addProtocols("script", "src", "https")
        val events = source.allElements.flatMap { it.attributes().asList() }.map { it.key }
            .filter { it.startsWith("on", ignoreCase = true) }.distinct()
        if (events.isNotEmpty()) safelist.addAttributes(":all", *events.toTypedArray())
    }
    val document = Cleaner(safelist).clean(source)
    // Cleaner drops root attributes, which email templates can need for contrast and layout.
    for (tag in listOf("html", "body")) {
        for (attribute in source.selectFirst(tag)!!.attributes()) {
            if (attribute.key in listOf("style", "class", "id", "dir", "lang", "bgcolor") ||
                (options.javaScript && attribute.key.startsWith("on", ignoreCase = true))) {
                document.selectFirst(tag)!!.attr(attribute.key, attribute.value)
            }
        }
    }
    val styles = if (options.externalStyles) "'unsafe-inline' https:" else "'unsafe-inline'"
    val scripts = if (options.javaScript) "'unsafe-inline' https:" else "'none'"
    val images = if (options.images) "https: data:" else "'none'"
    val fonts = if (options.externalStyles) "https: data:" else "'none'"
    val connections = if (options.javaScript) "https:" else "'none'"
    document.head().appendElement("meta").attr("http-equiv", "Content-Security-Policy")
        .attr("content", "default-src 'none'; style-src $styles; script-src $scripts; img-src $images; font-src $fonts; connect-src $connections; form-action 'none'; base-uri 'none'")
    document.head().appendElement("meta").attr("name", "viewport").attr("content", "width=device-width, initial-scale=1")
    document.head().appendElement("style").text(
        "html{color-scheme:light;background:#fff;color:#202124}body{margin:0;padding:8px;font:16px sans-serif;overflow-wrap:anywhere}img{max-width:100%;height:auto}",
    )
    return document.outerHtml()
}
