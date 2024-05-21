package com.example.bach_poc.fragments

import android.graphics.PointF
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bach_poc.classes.FaceLandmarkerHelper
import com.example.bach_poc.databinding.FaceBlendshapesResultBinding
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.util.Dictionary
import java.util.IllegalFormatWidthException
import java.util.LinkedList
import java.util.Locale
import java.util.Queue
import kotlin.math.pow
import kotlin.math.sqrt

class FaceBlendshapesResultAdapter : RecyclerView.Adapter<FaceBlendshapesResultAdapter.ViewHolder>() {

    private var perclos = 0.0

    private val frameRate = 30
    private val duration = 30
    private val durationInFrames = duration * frameRate
    private var closedFrames = 0
    private var totalFrames = 0
    private val closedFramesQueue: Queue<Boolean> = LinkedList()
    private val EAR_THRESHOLD = 0.2F
    fun updateResults(faceLandmarkerResult: FaceLandmarkerResult? = null, inputWidth : Int, inputHeight : Int) {

        var leftEyeClosed = false
        var rightEyeClosed = false
        if (faceLandmarkerResult != null && faceLandmarkerResult.faceLandmarks().isNotEmpty()) {
            val faceLandmarks = faceLandmarkerResult.faceLandmarks()[0]
            val leftEyeLandmarks = listOf(
                faceLandmarks[33], faceLandmarks[160], faceLandmarks[158],
                faceLandmarks[133], faceLandmarks[153], faceLandmarks[144])
            val rightEyeLandmarks = listOf(
                faceLandmarks[263], faceLandmarks[387], faceLandmarks[385],
                faceLandmarks[362], faceLandmarks[380], faceLandmarks[373]
            )
            val pointFListLeft: MutableList<PointF> = mutableListOf()
            val pointFListRight: MutableList<PointF> = mutableListOf()

            for (landmark in leftEyeLandmarks) {
                pointFListLeft.add(landmarkToPointF(landmark,inputWidth,inputHeight))
            }

            for (landmark in rightEyeLandmarks) {
                pointFListRight.add(landmarkToPointF(landmark,inputWidth,inputHeight))
            }

            val leftEAR = calculateEAR(pointFListLeft)
            val rightEAR = calculateEAR(pointFListRight)

            leftEyeClosed = leftEAR < EAR_THRESHOLD
            rightEyeClosed = rightEAR < EAR_THRESHOLD
        }
        val closedEyes = leftEyeClosed && rightEyeClosed

        if (closedFramesQueue.size >= durationInFrames) {
            val removed = closedFramesQueue.poll()
            if (removed == true) {
                closedFrames--
            }
        }
        closedFramesQueue.add(closedEyes)
        if (closedEyes) {
            closedFrames++
        }
        val totalFrames = closedFramesQueue.size
        perclos = (closedFrames.toDouble() / totalFrames) * 100


    }

    fun landmarkToPointF(landmark: NormalizedLandmark, frameWidth: Int, frameHeight: Int): PointF {
        return PointF(landmark.x() * frameWidth, landmark.y() * frameHeight)
    }

    fun calculateDistance(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    fun calculateEAR(eyeLandmarks: List<PointF>): Float {
        val verticalOne = calculateDistance(eyeLandmarks[1], eyeLandmarks[5])
        val verticalTwo = calculateDistance(eyeLandmarks[2], eyeLandmarks[4])
        val horizontal = calculateDistance(eyeLandmarks[0], eyeLandmarks[3])

        return (verticalOne + verticalTwo) / (2.0F * horizontal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FaceBlendshapesResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return 1;
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(perclos)
    }

    inner class ViewHolder(private val binding: FaceBlendshapesResultBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(score: Double){
            with(binding) {
                tvLabel.text = "PERCLOS"
                tvScore.text = String.format(Locale.ENGLISH, "%.2f%%", score)
                if (score < 20) tvState.text = "Alert"
                else if (score > 20 && score < 30)  tvState.text = "Drowsy"
                else if (score > 30) tvState.text = "Fatigue"
            }
        }
    }
}