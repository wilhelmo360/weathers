package com.example.weathers

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import com.google.ar.sceneform.animation.ModelAnimator

class MainActivity : AppCompatActivity() {

    private lateinit var fragment: ArFragment
    private lateinit var modelUri: Uri
    private lateinit var danceAnimator: ModelAnimator
    private var testRenderable: ModelRenderable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn.setOnClickListener{ view -> addObject() }
        btn.text = "Check weather"
        fragment = supportFragmentManager.findFragmentById(R.id.sceneform_fragment) as ArFragment;
        modelUri = Uri.parse("raincloud.sfb")

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

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.width / 2, vw.height / 2)
    }

}