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
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.HttpUrl

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val fields = listOf(
        //        "headline",
        "creator");

    val url = createEntryPoint()

    vulcanConsumer<Collection<BlogPosting>>(url, fields) {

      val listView = findViewById(R.id.list_view) as ListView

      val arrayAdapter = BlogPostingAdapter(this@MainActivity, R.layout.blog_posting_row, it.members)
      listView.adapter = arrayAdapter
      arrayAdapter.notifyDataSetChanged()
    }
  }

  private fun createEntryPoint() =
      HttpUrl.Builder()
          .scheme("http")
          .host("192.168.50.33")
          .port(8080)
          .addPathSegments("o/api/group")
          .addPathSegment(20140.toString())
          .addPathSegments("p/blogs")
          .build()
}

class BlogPostingAdapter(context: Context, val layoutId: Int,
    val members: List<BlogPosting>) : ArrayAdapter<BlogPosting>(context, layoutId, members) {

  override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

    val view = convertView ?: LayoutInflater.from(context).inflate(layoutId, parent, false)


    val blogPosting = members[position]

    val OCL = View.OnClickListener {
      val intent = Intent(context, SecondActivity::class.java)
      intent.putExtra("blogPostingId", blogPosting.id)
      context.startActivity(intent)
    }

    view.findViewById(R.id.blog_posting_detail).apply {
      setOnClickListener(OCL)
    }

    (view.findViewById(R.id.headline) as TextView).apply {
      this.text = blogPosting.headline
    }
    (view.findViewById(R.id.author) as TextView).apply {
      setOnClickListener(OCL)
      this.text = blogPosting.creator.name
    }

    return view
  }

}

class SecondActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_second)

    val httpURL = HttpUrl.parse(intent.getStringExtra("blogPostingId"))

    vulcanConsumer<BlogPosting>(httpURL!!) {

    }
  }
}

fun <T> asyncTask(function: () -> T): Deferred<T> = async(CommonPool) { function() }

inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object : TypeToken<T>() {}.type)