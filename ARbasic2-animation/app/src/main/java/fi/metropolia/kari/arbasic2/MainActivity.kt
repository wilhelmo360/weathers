package fi.metropolia.kari.arbasic2

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.animation.ModelAnimator
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.net.URL
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException


class MainActivity : AppCompatActivity() {

    private lateinit var fragment: ArFragment
    private lateinit var modelUri: Uri
    private lateinit var danceAnimator: ModelAnimator
    private var testRenderable: ModelRenderable? = null
    //var empDataHashMap = HashMap<String, String>()
    //var empList: ArrayList<HashMap<String, String>> = ArrayList()
    //val url = URL("view-source:https://www.ilmatieteenlaitos.fi/saa/Espoo")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



      /*val lv = findViewById<ListView>(R.id.listView)

      // Using a background thread to do network code
      val thread = object : Thread() {
        override fun run() {
          try {
            // Comment-out this line of code
            // val istream = assets.open("empdetail.xml")
            val builderFactory = DocumentBuilderFactory.newInstance()
            val docBuilder = builderFactory.newDocumentBuilder()
            val doc = docBuilder.parse(InputSource(url.openStream()))
            // reading player tags
            val nList = doc.getElementsByTagName("meteogram-temperatures")
            for (i in 0 until nList.length) {
              if (nList.item(0).nodeType.equals(Node.ELEMENT_NODE)) {
                empDataHashMap = HashMap()
                val element = nList.item(i) as Element
                //empDataHashMap.put("name", getNodeValue("name", element))
                //empDataHashMap.put("id", getNodeValue("id", element))
                empDataHashMap.put("title", getNodeValue("title", element))

                empList.add(empDataHashMap)
              }
            }

            val adapter = SimpleAdapter(
              this@MainActivity,
              empList,
              R.layout.custom_list,
              arrayOf("title"),
              intArrayOf(R.id.title)
            )

            runOnUiThread {
              lv.setAdapter(adapter)
            }
          } catch (e: IOException) {
            e.printStackTrace()
          } catch (e: ParserConfigurationException) {
            e.printStackTrace()
          } catch (e: SAXException) {
            e.printStackTrace()
          }
        }
      }

      thread.start()*/





        btn.setOnClickListener{ view -> addObject() }
        btn.text = "Check weather"
        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment;
        modelUri = Uri.parse("raincloud.sfb")

      //var txt = xmlDoc.getElementsByTagName("title")[0].childNodes[0].nodeValue;

        val renderableFuture = ModelRenderable.builder()
            .setSource(fragment.context, modelUri)
            .build()
        renderableFuture.thenAccept {
            it -> testRenderable = it
            val danceData = testRenderable!!.getAnimationData(0)
            danceAnimator = ModelAnimator(danceData, testRenderable)
        }
    }

    private fun addObject() {
        val frame = fragment.arSceneView.arFrame
        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null && testRenderable != null) {


            hits = frame.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane) {
                    val anchor = hit!!.createAnchor()
                    val anchorNode = AnchorNode(anchor)
                    anchorNode.setParent(fragment.arSceneView.scene)
                    val mNode = TransformableNode(fragment.transformationSystem)
                    mNode.setParent(anchorNode)
                    mNode.renderable = testRenderable
                    mNode.select()
                    mNode.setOnTapListener { hitTestRes: HitTestResult?, motionEv: MotionEvent? ->
                        if (!danceAnimator.isRunning) {
                            danceAnimator.repeatCount=2
                            danceAnimator.start()
                        }
                    }
                    break
                }
            }
        }
    }

 /* protected fun getNodeValue(tag: String, element: Element): String{
    val nodeList = element.getElementsByTagName(tag)
    val node = nodeList.item(0)
    if(node != null){
      if(node.hasChildNodes()){
        val child = node.firstChild
        while(child!=null){
          if(child.nodeType === org.w3c.dom.Node.TEXT_NODE)
          {
            return child.nodeValue
          }
        }
      }
    }
    return ""
  }*/

  private fun getScreenCenter(): android.graphics.Point {
    val vw = findViewById<View>(android.R.id.content) as View
    return android.graphics.Point(vw.width / 2, vw.height / 2)
  }
}



