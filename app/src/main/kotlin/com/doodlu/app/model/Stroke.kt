package com.doodlu.app.model

data class Stroke(
    val points: List<Pair<Float, Float>>,
    val color: String = "#FFFFFF",
    val width: Float = 4f
)
