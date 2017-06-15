package com.liferay.mobile.vulcanapi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : AppCompatActivity() {

  inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    launch(UI) {

      asyncTask {
        val okHttp = OkHttpClient()

        val group = 20140

        val credential = Credentials.basic("test@liferay.com", "test")


        val httpUrl = HttpUrl.Builder().scheme("http").host("192.168.50.33").port(8080).addPathSegment("o")
            .addPathSegment("api").addPathSegment("group").addPathSegment(group.toString()).addPathSegment(
            "p").addPathSegment("blogs")

        httpUrl.addQueryParameter("embedded", "creator")

        val request = Request.Builder()
            .url(httpUrl.build())
            .addHeader("Authorization", credential)
            .addHeader("Accept", "application/ld+json")
            .build()
        val response = okHttp.newCall(request).execute()

        Gson().fromJson<Collection<BlogPosting>>(response.body()!!.string())
      }.await().let {
        val listView = findViewById(R.id.list_view) as ListView

        val arrayAdapter = BlogPostingAdapter(this@MainActivity, R.layout.blog_posting_row, it.members)
        listView.adapter = arrayAdapter
        arrayAdapter.notifyDataSetChanged()
      }
    }
  }
}

class BlogPostingAdapter(context: Context, val layoutId: Int,
    val members: List<BlogPosting>) : ArrayAdapter<BlogPosting>(context, layoutId, members) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

    val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)

    val blogPosting = members.get(position)

    (view.findViewById(R.id.headline) as TextView).apply { this.text = blogPosting.headline }
    (view.findViewById(R.id.author) as TextView).apply {
      setOnClickListener { context.startActivity(Intent(context, SecondActivity::class.java)) }
      this.text = blogPosting.creator.name
    }

    return view;
  }

}

class SecondActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_second)
  }
}

data class Collection<T>(
    val totalItems: Int,
    val numberOfItems: Int,
    val members: List<T>,
    @SerializedName("@type")
    val type: Array<String>) {
  override fun toString() = type.joinToString { it }
}

data class BlogPosting(val headline: String, val creator: Person, @SerializedName("@type") val type: Array<String>) {
  override fun toString() = type.joinToString { it }
}

data class Person(val name: String) {

}

fun <T> asyncTask(function: () -> T): Deferred<T> = async(CommonPool) { function() }