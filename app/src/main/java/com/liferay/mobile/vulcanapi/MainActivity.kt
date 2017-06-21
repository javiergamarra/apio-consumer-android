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
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.async
import okhttp3.HttpUrl
import java.io.EOFException
import java.io.IOException
import java.lang.reflect.Type


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

    vulcanConsumer<Collection<BlogPosting>>(url, fields, embedded,
        {
          //          fromJson<Collection<BlogPosting>>(JsonReader(StringReader(it)), object : TypeToken<Collection<BlogPosting>>() {}.type)!!

          val type = TypeToken.getParameterized(Either::class.java, String::class.java, Person::class.java).type

          val gson = GsonBuilder().registerTypeAdapter(type,
              EitherTypeAdapter()).create()

          gson.fromJson(it, object : TypeToken<Collection<BlogPosting>>() {}.type)
        }) {
      val listView = findViewById(R.id.list_view) as ListView

      val arrayAdapter = BlogPostingAdapter(this@MainActivity, R.layout.blog_posting_row, it.members)
      listView.adapter = arrayAdapter
      arrayAdapter.notifyDataSetChanged()
    }
  }

  @Throws(JsonIOException::class, JsonSyntaxException::class)
  fun <T> fromJson(reader: JsonReader, typeOfT: Type): T? {
    var isEmpty = true
    val oldLenient = reader.isLenient
    reader.isLenient = true
    try {
      reader.peek()
      isEmpty = false
      val typeToken = TypeToken.get(typeOfT) as TypeToken<T>
      val typeAdapter = Gson().getAdapter<T>(typeToken)
      val `object` = typeAdapter.read(reader)
      return `object`
    } catch (e: EOFException) {
      /*
       * For compatibility with JSON 1.5 and earlier, we return null for empty
       * documents instead of throwing.
       */
      if (isEmpty) {
        return null
      }
      throw JsonSyntaxException(e)
    } catch (e: IllegalStateException) {
      throw JsonSyntaxException(e)
    } catch (e: IOException) {
      // TODO(inder): Figure out whether it is indeed right to rethrow this as JsonSyntaxException
      throw JsonSyntaxException(e)
    } finally {
      reader.isLenient = oldLenient
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
      blogPosting.creator.ifRight {
        this.text = it.name
      }
      blogPosting.creator.ifLeft {
        vulcanConsumer<Person>(HttpUrl.parse(it)!!,
            convert = { Gson().fromJson(it, object : TypeToken<Person>() {}.type) }) {
          this.text = it.name
        }
      }

    }

    return view
  }

}

class SecondActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_second)


    val httpURL = HttpUrl.parse(intent.getStringExtra("blogPostingId"))

    vulcanConsumer<BlogPosting>(httpURL!!, embedded = listOf("creator"),
        convert = { Gson().fromJson(it, object : TypeToken<BlogPosting>() {}.getType()) }) {
      (findViewById(R.id.headline) as TextView).apply {
        text = (graph[it.id]!!.value as BlogPosting).headline
      }
    }
  }
}

fun <T> asyncTask(function: () -> T): Deferred<T> = async(CommonPool) { function() }

class EitherTypeAdapter : TypeAdapter<Either<String, Model>>() {

  override fun write(out: JsonWriter?, value: Either<String, Model>?) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  @Throws(IOException::class)
  override fun read(jsonReader: JsonReader): Either<String, Model> {

    if (jsonReader.peek() == JsonToken.BEGIN_OBJECT) {
      val adapter = Gson().getAdapter(Model::class.java)
      return Right(adapter.read(jsonReader))
    } else {
      return Left(jsonReader.nextString())
    }
  }

}