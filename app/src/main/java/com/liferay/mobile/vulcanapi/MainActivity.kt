package com.liferay.mobile.vulcanapi

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import okhttp3.HttpUrl
import java.io.IOException


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fields = mapOf(
            "BlogPosting" to listOf("creator"),
            "Person" to listOf("name")
        )

        val embedded = listOf("creator")

        val url = createEntryPoint()

        val convert: (String) -> Collection<BlogPosting> = {
            val type = TypeToken.getParameterized(
                Either::class.java, String::class.java, Person::class.java).type

            val gson = GsonBuilder().registerTypeAdapter(type, EitherTypeAdapter()).create()

            gson.fromJson(it, object : TypeToken<Collection<BlogPosting>>() {}.type)
        }

        vulcanConsumer(url, fields, embedded,
            convert) {
            val listView = findViewById(R.id.list_view) as ListView

            val arrayAdapter =
                BlogPostingAdapter(this@MainActivity, R.layout.blog_posting_row, it.members)

            listView.adapter = arrayAdapter
            arrayAdapter.notifyDataSetChanged()
        }
    }

    private fun createEntryPoint() =
        HttpUrl.Builder()
            .scheme("http")
            .host("192.168.1.103")
            .port(8080)
            .addPathSegments("o/api/group")
            .addPathSegment(20140.toString())
            .addPathSegments("p/blogs")
            .build()
}

class SecondActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)

        val httpURL = HttpUrl.parse(intent.getStringExtra("blogPostingId"))

        val type = TypeToken.getParameterized(Either::class.java, String::class.java, Person::class.java).type

        val gson = GsonBuilder().registerTypeAdapter(type, EitherTypeAdapter()).create()

        val convert: (String) -> BlogPosting = {
            gson.fromJson(it, object : TypeToken<BlogPosting>() {}.type)
        }

        vulcanConsumer(httpURL!!, embedded = listOf("creator"), convert = convert) {
            (findViewById(R.id.headline) as TextView).apply {
                text = (graph[it.id]!!.value as BlogPosting).headline
            }
        }
    }
}

class EitherTypeAdapter : TypeAdapter<Either<String, Model>>() {

    override fun write(out: JsonWriter?, value: Either<String, Model>?) {
        TODO("not implemented")
    }

    @Throws(IOException::class)
    override fun read(jsonReader: JsonReader): Either<String, Model> {

        if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
            val adapter = Gson().getAdapter(Person::class.java)
            return Right(adapter.read(jsonReader))
        } else {
            return Left(jsonReader.nextString())
        }
    }

}