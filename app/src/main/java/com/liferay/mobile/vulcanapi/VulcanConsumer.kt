package com.liferay.mobile.vulcanapi

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

fun <T : Model> vulcanConsumer(
    url: HttpUrl, fields: Map<String, List<String>> = emptyMap(), embedded: List<String> = emptyList(),
    convert: (String) -> T, onComplete: (T) -> Unit) {

  launch(UI) {

    asyncTask {
      val okHttp = OkHttpClient()
      val urlBuilder = url.newBuilder()

      //FIXME local graph
      //FIXME event oriented

      configureSelectedFields(urlBuilder, fields)
      configureEmbeddedParameters(urlBuilder, embedded)

      val credential = createAuthentication()

      val request = Request.Builder()
          .url(urlBuilder.build())
          .addHeader("Authorization", credential)
          .addHeader("Accept", "application/ld+json")
          .build()
      val response = okHttp.newCall(request).execute()

      val result = convert(response.body()!!.string())

      if (graph[result.id] != null) {
        graph[result.id]!!.apply {

          value = value?.let {
            val model = it.merge(result)
            model.attributes = model.attributes.union(model.type.flatMap { fields[it] ?: emptyList() })

            relationships = model.relationships().map {
              if (it is Left) {
                Node<Model>(it.value)
              }
              else {
                (it as Right).let { Node<Model>(it.value.id, it.value) }
              }
            }

            relationships.forEach { graph.put(it.id, it) }

            model
          }

        }
      } else {
        val node = Node<Model>(result.id, result, recursive(result, fields))
        graph.put(result.id, node)
        result.attributes = result.type.flatMap { fields[it] ?: emptyList() }.toSet()
        traverseRelationships(node)
      }

      result
    }.await().let(onComplete)
  }
}

private fun recursive(result: Model, fields: Map<String, List<String>>): List<Node<*>> =
    result.relationships().map {
      if (it is Left) {
        Node<Model>(it.value)
      }
      else {
        (it as Right).value.let {
          it.attributes = it.type.flatMap { fields[it] ?: emptyList() }.toSet()
          Node<Model>(it.id, it, recursive(it, fields))
        }
      }
    }

private fun <T : Model> traverseRelationships(node: Node<T>) {
  node.relationships.forEach {
    graph.put(it.id, it)
    traverseRelationships(it)
  }
}

class Node<T : Model>(val id: String, var value: Model? = null, var relationships: List<Node<*>> = emptyList()) {
}

var graph: MutableMap<String, Node<*>> = mutableMapOf()

fun configureSelectedFields(
    httpUrl: HttpUrl.Builder, fields: Map<String, List<String>>) {
  fields.forEach { (type, values) ->
    httpUrl.addQueryParameter("fields[$type]", values.joinToString(separator = ","))
  }
}

fun configureEmbeddedParameters(httpUrl: HttpUrl.Builder, embedded: List<String>) {
  httpUrl.addQueryParameter("embedded", embedded.joinToString(","))
}

fun createAuthentication(): String? {
  val credential = Credentials.basic("test@liferay.com", "test")
  return credential
}

//inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)