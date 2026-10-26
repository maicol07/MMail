package it.maicol07.mmail.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jsoup.Jsoup
import it.maicol07.mmail.data.RenderingOptions

class EmailHtmlTest {
    @Test
    fun preservesEmailFormattingAndRemovesActiveContent() {
        val html = emailHtmlDocument("""
            <html><head><style>.price{color:red}</style>
            <base href="https://attacker.example/"><meta http-equiv="refresh" content="0;url=https://attacker.example/"></head>
            <body onload="steal()"><table><tr><td class="price" style="font-weight:bold">€ 25 &amp; tax</td></tr></table>
            <script>steal()</script><iframe src="https://attacker.example/"></iframe>
            <form action="https://attacker.example/"><input name="password"></form>
            <a href="javascript:steal()" onclick="steal()">Unsafe</a>
            <a href="https://example.com/">Safe</a><a href="mailto:test@example.com">Mail</a>
            <img src="file:///private" onerror="steal()"><img src="content://private">
            </body></html>
        """.trimIndent())
        val document = Jsoup.parse(html)
        assertTrue(document.select("script,iframe,object,embed,form,input,base,[onload],[onclick],[onerror]").isEmpty())
        assertEquals("€ 25 & tax", document.select("table td.price").text())
        assertEquals("font-weight:bold", document.select("td").attr("style"))
        assertTrue(document.select("style").html().contains(".price{color:red}"))
        assertEquals(listOf("https://example.com/", "mailto:test@example.com"), document.select("a[href]").map { it.attr("href") })
        assertTrue(document.select("img[src]").isEmpty())
        val policy = document.select("meta[http-equiv]").single()
        assertEquals("Content-Security-Policy", policy.attr("http-equiv"))
        assertTrue(policy.attr("content").contains("script-src 'none'; img-src 'none'"))
        assertTrue(policy.attr("content").contains("connect-src 'none'; form-action 'none'; base-uri 'none'"))
    }

    @Test
    fun handlesMalformedHtmlAndUnicodeWithoutDroppingVisibleText() {
        val document = Jsoup.parse(emailHtmlDocument("<p>Caffè &amp; tè<table><tr><td>こんにちは"))
        assertEquals("Caffè & tè こんにちは", document.body().text())
        assertEquals("width=device-width, initial-scale=1", document.select("meta[name=viewport]").attr("content"))
    }

    @Test
    fun preservesRootStylesAndLegacyEmailFormatting() {
        val document = Jsoup.parse(emailHtmlDocument("""
            <html class="notification"><head><style>.notification .content{color:white}</style></head>
            <body style="background:#222" bgcolor="#222222" class="content" dir="rtl">
            <center><table cellpadding="8" cellspacing="0"><tr><td valign="top"><font face="Arial" size="3">Notification</font></td></tr></table></center>
            </body></html>
        """))
        assertEquals("notification", document.selectFirst("html")!!.className())
        assertEquals("background:#222", document.body().attr("style"))
        assertEquals("#222222", document.body().attr("bgcolor"))
        assertEquals("rtl", document.body().attr("dir"))
        assertEquals("8", document.select("table").attr("cellpadding"))
        assertEquals("Notification", document.select("center font[face=Arial]").text())
    }

    @Test
    fun permissionsEnableOnlyTheirOwnResourceTypes() {
        val html = """<head><script src="https://example.com/code.js"></script><link rel="stylesheet" href="https://example.com/style.css"></head>
            <body onload="ready()"><script>document.title='enabled'</script><p onclick="run()">Text</p><iframe src="https://example.com/"></iframe>
            <img src="data:image/png;base64,AA=="><img src="file:///private"></body>"""
        val imagesOnly = Jsoup.parse(emailHtmlDocument(html, RenderingOptions(images = true)))
        assertTrue(imagesOnly.select("script,link,[onclick],iframe").isEmpty())
        assertEquals(1, imagesOnly.select("img[src]").size)
        assertTrue(imagesOnly.select("meta[http-equiv]").attr("content").contains("img-src https: data:"))

        val scriptsOnly = Jsoup.parse(emailHtmlDocument(html, RenderingOptions(javaScript = true)))
        assertEquals(2, scriptsOnly.select("script").size)
        assertEquals("ready()", scriptsOnly.body().attr("onload"))
        assertEquals("run()", scriptsOnly.select("p").attr("onclick"))
        assertTrue(scriptsOnly.select("link,iframe").isEmpty())
        assertTrue(scriptsOnly.select("meta[http-equiv]").attr("content").contains("img-src 'none'"))

        val stylesOnly = Jsoup.parse(emailHtmlDocument(html, RenderingOptions(externalStyles = true)))
        assertEquals("https://example.com/style.css", stylesOnly.select("link").attr("href"))
        assertTrue(stylesOnly.select("script,iframe,[onclick]").isEmpty())
        assertTrue(stylesOnly.select("meta[http-equiv]").attr("content").contains("font-src https: data:"))
    }
}
