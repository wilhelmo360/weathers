/*package fi.metropolia.kari.arbasic2

import android.app.Activity
import android.os.AsyncTask
import android.os.DropBoxManager
import android.preference.PreferenceManager
import android.util.Xml
import android.webkit.WebView
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL




private val ns: String? = null

class WeatherParser {

  @Throws(XmlPullParserException::class, IOException::class)
  fun parse(inputStream: InputStream): List<*> {
    inputStream.use { inputStream ->
      val parser: XmlPullParser = Xml.newPullParser()
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
      parser.setInput(inputStream, null)
      parser.nextTag()
      return readFeed(parser)
    }
  }


  //...
  @Throws(XmlPullParserException::class, IOException::class)
  private fun readFeed(parser: XmlPullParser): List<DropBoxManager.Entry> {
    val entries = mutableListOf<DropBoxManager.Entry>()

    parser.require(XmlPullParser.START_TAG, ns, "feed")
    while (parser.next() != XmlPullParser.END_TAG) {
      if (parser.eventType != XmlPullParser.START_TAG) {
        continue
      }
      // Starts by looking for the entry tag
      if (parser.name == "entry") {
        entries.add(readEntry(parser))
      } else {
        skip(parser)
      }
    }
    return entries
  }

}

data class Entry(val title: String?, val summary: String?, val link: String?)

// Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them off
// to their respective "read" methods for processing. Otherwise, skips the tag.
@Throws(XmlPullParserException::class, IOException::class)
private fun readEntry(parser: XmlPullParser): DropBoxManager.Entry {
  parser.require(XmlPullParser.START_TAG, ns, "entry")
  var title: String? = null
  var summary: String? = null
  var link: String? = null
  while (parser.next() != XmlPullParser.END_TAG) {
    if (parser.eventType != XmlPullParser.START_TAG) {
      continue
    }
    when (parser.name) {
      "title" -> title = readTitle(parser)
      "summary" -> summary = readSummary(parser)
      "link" -> link = readLink(parser)
      else -> skip(parser)
    }
  }
  return Entry(title, summary, link)
}

// Processes title tags in the feed.
@Throws(IOException::class, XmlPullParserException::class)
private fun readTitle(parser: XmlPullParser): String {
  parser.require(XmlPullParser.START_TAG, ns, "title")
  val title = readText(parser)
  parser.require(XmlPullParser.END_TAG, ns, "title")
  return title
}

// Processes link tags in the feed.
@Throws(IOException::class, XmlPullParserException::class)
private fun readLink(parser: XmlPullParser): String {
  var link = ""
  parser.require(XmlPullParser.START_TAG, ns, "link")
  val tag = parser.name
  val relType = parser.getAttributeValue(null, "rel")
  if (tag == "link") {
    if (relType == "alternate") {
      link = parser.getAttributeValue(null, "href")
      parser.nextTag()
    }
  }
  parser.require(XmlPullParser.END_TAG, ns, "link")
  return link
}

// Processes summary tags in the feed.
@Throws(IOException::class, XmlPullParserException::class)
private fun readSummary(parser: XmlPullParser): String {
  parser.require(XmlPullParser.START_TAG, ns, "summary")
  val summary = readText(parser)
  parser.require(XmlPullParser.END_TAG, ns, "summary")
  return summary
}



// For the tags title and summary, extracts their text values.
@Throws(IOException::class, XmlPullParserException::class)
private fun readText(parser: XmlPullParser): String {
  var result = ""
  if (parser.next() == XmlPullParser.TEXT) {
    result = parser.text
    parser.nextTag()
  }
  return result
}

@Throws(XmlPullParserException::class, IOException::class)
private fun skip(parser: XmlPullParser) {
  if (parser.eventType != XmlPullParser.START_TAG) {
    throw IllegalStateException()
  }
  var depth = 1
  while (depth != 0) {
    when (parser.next()) {
      XmlPullParser.END_TAG -> depth--
      XmlPullParser.START_TAG -> depth++
    }
  }
}

class NetworkActivity : Activity() {

  companion object {

    const val WIFI = "Wi-Fi"
    const val ANY = "Any"
    const val SO_URL = "view-source:https://www.ilmatieteenlaitos.fi/saa/Espoo"
    // Whether there is a Wi-Fi connection.
    private var wifiConnected = false
    // Whether there is a mobile connection.
    private var mobileConnected = false

    // Whether the display should be refreshed.
    var refreshDisplay = true
    // The user's current network preference setting.
    var sPref: String? = null
  }

  //...

  // Uses AsyncTask subclass to download the XML feed from stackoverflow.com.
  // Uses AsyncTask to download the XML feed from stackoverflow.com.
  fun loadPage() {

    if (sPref.equals(ANY) && (wifiConnected || mobileConnected)) {
      DownloadXmlTask().execute(SO_URL)
    } else if (sPref.equals(WIFI) && wifiConnected) {
      DownloadXmlTask().execute(SO_URL)
    } else {
      // show error
    }
  }

  private inner class DownloadXmlTask : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg urls: String): String {
      return try {
        loadXmlFromNetwork(urls[0])
      } catch (e: IOException) {
        resources.getString(R.string.connection_error)
      } catch (e: XmlPullParserException) {
        resources.getString(R.string.xml_error)
      }
    }

    override fun onPostExecute(result: String) {
      setContentView(R.layout.activity_main)
      // Displays the HTML string in the UI via a WebView
      findViewById<WebView>(R.id.webview)?.apply {
        loadData(result, "text/html", null)
      }
    }
  }

  // Uploads XML from stackoverflow.com, parses it, and combines it with
// HTML markup. Returns HTML string.
  @Throws(XmlPullParserException::class, IOException::class)
  private fun loadXmlFromNetwork(urlString: String): String {
    // Checks whether the user set the preference to include summary text
    val pref: Boolean = PreferenceManager.getDefaultSharedPreferences(this)?.run {
      getBoolean("summaryPref", false)
    } ?: false

    val entries: List<DropBoxManager.Entry> = downloadUrl(urlString)?.use { stream ->
      // Instantiate the parser
      WeatherParser().parse(stream)
    } as List<DropBoxManager.Entry>? ?: emptyList()

    return StringBuilder().apply {
      append("<h3>${resources.getString(R.string.page_title)}</h3>")
      append("<em>${resources.getString(R.string.updated)} ")
      //append("${formatter.format(rightNow.time)}</em>")
      // StackOverflowXmlParser returns a List (called "entries") of Entry objects.
      // Each Entry object represents a single post in the XML feed.
      // This section processes the entries list to combine each entry with HTML markup.
      // Each entry is displayed in the UI as a link that optionally includes
      // a text summary.
      entries.forEach { entry ->
        append("<p><a href='")
        append(entry.link)
        append("'>" + entry.title + "</a></p>")
        // If the user set the preference to include summary text,
        // adds it to the display.
        if (pref) {
          append(entry.summary)
        }
      }
    }.toString()
  }

  // Given a string representation of a URL, sets up a connection and gets
// an input stream.
  @Throws(IOException::class)
  private fun downloadUrl(urlString: String): InputStream? {
    val url = URL(urlString)
    return (url.openConnection() as? HttpURLConnection)?.run {
      readTimeout = 10000
      connectTimeout = 15000
      requestMethod = "GET"
      doInput = true
      // Starts the query
      connect()
      inputStream
    }
  }
}

*/
