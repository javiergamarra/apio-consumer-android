package com.liferay.mobile.vulcanapi

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.HttpUrl

class BlogPostingAdapter(
    context: Context, val layoutId: Int, val members: List<BlogPosting>) :
    ArrayAdapter<BlogPosting>(context, layoutId, members) {

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