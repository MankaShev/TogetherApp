package com.example.togetherapp.domain.models
import kotlinx.serialization.Serializable

@Serializable
data class Place( val id: Int,
                  val name: String,
                  val latitude: Double,
                  val longitude: Double)
