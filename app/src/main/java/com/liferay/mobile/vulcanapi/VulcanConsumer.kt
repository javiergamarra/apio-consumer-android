package com.liferay.mobile.vulcanapi

import android.app.Activity
import com.google.gson.Gson
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

inline fun <reified T : Model> vulcanConsumer(activity: Activity, url: HttpUrl, fields: List<String> = emptyList(),
    crossinline onComplete: (Collection<BlogPosting>) -> Unit) {

  println("Hola!");

//  launch(UI) {

  Thread() {
    //    asyncTask {
    val okHttp = OkHttpClient()

    val urlBuilder = url.newBuilder();

    //FIXME local graph
    //FIXME event oriented

    configureSelectedFields(urlBuilder, fields)
    configureEmbeddedParameters(urlBuilder)

    val credential = createAuthentication()

    val request = Request.Builder()
        .url(urlBuilder.build())
        .addHeader("Authorization", credential)
        .addHeader("Accept", "application/ld+json")
        .build()
    val response = okHttp.newCall(request).execute()

    val result = Gson().fromJson<Collection<BlogPosting>>(response.body()!!.string())

    val node = Node(result)
    graph.put(result.id, node)
//      result.relationships.forEach { graph.put(it.id, Node(it)) }

//    result
    activity.runOnUiThread({ onComplete(result) })

  }.start()
//        .await().let(onComplete)
//  }
}

class Node<out T>(val value: T, val relationships: List<Node<*>> = emptyList())

var graph: MutableMap<String, Node<*>> = mutableMapOf()

object kotlin {
  val value = graph;
}

fun configureSelectedFields(httpUrl: HttpUrl.Builder, fields: List<String>) {
  httpUrl.addQueryParameter("fields[BlogPosting]", fields.joinToString(separator = ","))
}

fun configureEmbeddedParameters(httpUrl: HttpUrl.Builder) {
  httpUrl.addQueryParameter("embedded", "creator")
}

fun createAuthentication(): String? {
  val credential = Credentials.basic("test@liferay.com", "test")
  return credential
}