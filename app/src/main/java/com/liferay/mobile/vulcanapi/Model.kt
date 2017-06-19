package com.liferay.mobile.vulcanapi

import com.google.gson.annotations.SerializedName

sealed class Either<out L, out R>

data class Left<out T>(val value: T) : Either<T, Nothing>()
data class Right<out T>(val value: T) : Either<Nothing, T>()

fun <T> Either<*, T>.ifRight(f: (T) -> Unit) {
  if (this is Right) {
    f(this.value)
  }
}

fun <T> Either<T, *>.ifLeft(f: (T) -> Unit) {
  if (this is Left) {
    f(this.value)
  }
}

data class Collection<out T : Model>(
    @SerializedName("@id") override val id: String,
    val totalItems: Int,
    val numberOfItems: Int,
    val members: List<T>,
    @SerializedName("@type") override val type: Array<String>) : Model {
  override var attributes = setOf<String>()

  override fun merge(value: Model): Model {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun relationships() = members.map { Right(it) }

  override fun toString() = type.joinToString()
}

data class BlogPosting(
    @SerializedName("@id") override val id: String,
    val headline: String,
    val creator: Either<String, Person>,
    @SerializedName("@type") override val type: Array<String>) : Model {
  override fun merge(value: Model) = BlogPosting(id, (value as BlogPosting).headline, value.creator, this.type)

  override fun relationships(): List<Either<String, Model>> = listOf(creator)
  override var attributes = setOf<String>()

  override fun toString() = type.joinToString()
}

interface Model {
  val id: String
  val type: Array<String>
  fun relationships(): List<Either<String, Model>>
  fun merge(value: Model): Model
  var attributes: Set<String>
}

data class Person(@SerializedName("@id") override val id: String, val name: String,
    @SerializedName("@type") override val type: Array<String>) : Model {
  override var attributes = setOf<String>()
  override fun merge(value: Model): Model {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun relationships(): List<Either<String, Model>> = emptyList()
}
