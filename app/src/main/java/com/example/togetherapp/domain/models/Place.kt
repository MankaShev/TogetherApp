package com.example.togetherapp.domain.models
import java.util.UUID
data class Place( val id: UUID,
                  val name: String,
                  val latitude: Double,
                  val longitude: Double,
                  val address: String)
