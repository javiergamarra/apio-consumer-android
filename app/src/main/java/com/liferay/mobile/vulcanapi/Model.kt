package com.liferay.mobile.vulcanapi

import com.google.gson.annotations.SerializedName

//FIXME either

data class Collection<out T : Model>(
    @SerializedName("@id") override val id: String,
    val totalItems: Int,
    val numberOfItems: Int,
    val members: List<T>,
    @SerializedName("@type") override val type: Array<String>) : Model {
  override val relationships: List<Model> = members

  override fun toString() = type.joinToString()
}

data class BlogPosting(
    @SerializedName("@id") override val id: String,
    val headline: String,
    val creator: Person,
    @SerializedName("@type") override val type: Array<String>) : Model {
  override val relationships: List<Model> = listOf(creator)

  override fun toString() = type.joinToString()
}

interface Model {
  val id: String
  val type: Array<String>
  val relationships: List<Model>
}

data class Person(@SerializedName("@id") override val id: String, val name: String,
    @SerializedName("@type") override val type: Array<String>) : Model {
  override val relationships: List<Model> = listOf()
}
