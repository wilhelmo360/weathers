package fi.metropolia.kari.arbasic2

import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.webkit.WebView
import fi.metropolia.kari.arbasic2.network.Entry
import fi.metropolia.kari.arbasic2.network.WeatherParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class NetworkActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_network)
  }


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

    val entries: List<Entry> = downloadUrl(urlString)?.use { stream ->
      // Instantiate the parser
      WeatherParser().parse(stream)
    } as List<Entry>? ?: emptyList()

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
