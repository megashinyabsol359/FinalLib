package com.example.finallib.search

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.finallib.R
import com.example.finallib.model.Book
import com.example.finallib.model.Tag
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var btnToggleTagFilter: Button
    private lateinit var spinnerLanguage: Spinner
    private lateinit var tvSearchResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var rvTagFilters: RecyclerView
    private lateinit var progressBarTags: ProgressBar
    private lateinit var tagFilterContainer: FrameLayout
    private lateinit var bookAdapter: BookSearchAdapter
    private lateinit var tagFilterAdapter: TagFilterAdapter
    private val tagFilterList = mutableListOf<TagFilterItem>()
    private val db = FirebaseFirestore.getInstance()
    private var isTagFilterVisible = false
    private var selectedLanguage = ""
    private val languages = mutableListOf("Tất cả ngôn ngữ")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_search)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle("Tìm Kiếm")

        etSearch = findViewById(R.id.et_search)
        btnSearch = findViewById(R.id.btn_search)
        btnToggleTagFilter = findViewById(R.id.btn_toggle_tag_filter)
        spinnerLanguage = findViewById(R.id.spinner_language)
        tvSearchResult = findViewById(R.id.tv_search_result)
        progressBar = findViewById(R.id.progressBar_search)
        rvSearchResults = findViewById(R.id.rv_search_results)
        rvTagFilters = findViewById(R.id.rv_tag_filters)
        progressBarTags = findViewById(R.id.progressBar_tags)
        tagFilterContainer = findViewById(R.id.tag_filter_container)

        // Setup RecyclerView Books
        bookAdapter = BookSearchAdapter(emptyList())
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = bookAdapter

        // Setup RecyclerView Tags with GridLayoutManager (3 columns)
        tagFilterAdapter = TagFilterAdapter(tagFilterList) {
            // Khi tag filter thay đổi, tự động load lại danh sách sách
            val keyword = etSearch.text.toString().trim()
            if (keyword.isNotEmpty()) {
                searchBooks(keyword)
            } else {
                loadDefaultBooks()
            }
        }
        rvTagFilters.layoutManager = GridLayoutManager(this, 2)
        rvTagFilters.adapter = tagFilterAdapter

        // Fetch danh sách tags
        fetchTags()

        // Toggle tag filter visibility
        btnToggleTagFilter.setOnClickListener {
            isTagFilterVisible = !isTagFilterVisible
            if (isTagFilterVisible) {
                tagFilterContainer.layoutParams.height = 600 // Tăng chiều cao
                tagFilterContainer.visibility = FrameLayout.VISIBLE
                btnToggleTagFilter.text = "Ẩn bộ lọc tags"
            } else {
                tagFilterContainer.visibility = FrameLayout.GONE
                btnToggleTagFilter.text = "Hiện bộ lọc tags"
            }
        }

        btnSearch.setOnClickListener {
            val searchKeyword = etSearch.text.toString().trim()
            if (searchKeyword.isNotEmpty()) {
                searchBooks(searchKeyword)
            } else {
                // Hiển thị 15-20 sách ngẫu nhiên khi không có keyword
                loadDefaultBooks()
            }
        }

        // Tìm kiếm khi nhấn Enter trên bàn phím
        etSearch.setOnEditorActionListener { _, _, _ ->
            btnSearch.performClick()
            true
        }

        // Setup Language Spinner
        fetchLanguages()

        spinnerLanguage.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                selectedLanguage = if (position == 0) "" else languages[position]
                val keyword = etSearch.text.toString().trim()
                if (keyword.isNotEmpty()) {
                    searchBooks(keyword)
                } else {
                    loadDefaultBooks()
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Load sách mặc định lúc khởi động
        loadDefaultBooks()
    }

    private fun fetchLanguages() {
        db.collection("books")
            .get()
            .addOnSuccessListener { documents ->
                val languageSet = mutableSetOf<String>()
                for (document in documents) {
                    val book = document.toObject(Book::class.java)
                    if (book.language.isNotEmpty()) {
                        languageSet.add(book.language)
                    }
                }
                languages.addAll(languageSet.sorted())
                setupLanguageSpinner()
            }
    }

    private fun setupLanguageSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLanguage.adapter = adapter
    }

    private fun fetchTags() {
        db.collection("tags")
            .get()
            .addOnSuccessListener { documents ->
                tagFilterList.clear()
                for (document in documents) {
                    val tag = document.toObject(Tag::class.java)
                    tagFilterList.add(TagFilterItem(tag))
                }
                progressBarTags.visibility = ProgressBar.GONE
                tagFilterAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                progressBarTags.visibility = ProgressBar.GONE
            }
    }

    private fun loadDefaultBooks() {
        progressBar.visibility = ProgressBar.VISIBLE
        tvSearchResult.visibility = TextView.GONE
        rvSearchResults.visibility = RecyclerView.GONE

        val includedTags = tagFilterAdapter.getIncludedTags()
        val excludedTags = tagFilterAdapter.getExcludedTags()

        db.collection("books")
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                val bookList = mutableListOf<Book>()
                
                for (document in documents) {
                    val book = document.toObject(Book::class.java)

                    // Filter theo ngôn ngữ
                    if (selectedLanguage.isNotEmpty() && book.language != selectedLanguage) {
                        continue
                    }

                    // Áp dụng include tags filter
                    if (includedTags.isNotEmpty()) {
                        val hasIncludedTag = includedTags.any { includedTag ->
                            book.tags.any { it.equals(includedTag, ignoreCase = true) }
                        }
                        if (!hasIncludedTag) continue
                    }

                    // Áp dụng exclude tags filter
                    if (excludedTags.isNotEmpty()) {
                        val hasExcludedTag = excludedTags.any { excludedTag ->
                            book.tags.any { it.equals(excludedTag, ignoreCase = true) }
                        }
                        if (hasExcludedTag) continue
                    }

                    bookList.add(book)
                }

                progressBar.visibility = ProgressBar.GONE

                if (bookList.isNotEmpty()) {
                    bookAdapter.updateList(bookList)
                    rvSearchResults.visibility = RecyclerView.VISIBLE
                    tvSearchResult.visibility = TextView.GONE
                } else {
                    tvSearchResult.text = "Không có sách nào"
                    rvSearchResults.visibility = RecyclerView.GONE
                    tvSearchResult.visibility = TextView.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.GONE
                tvSearchResult.text = "Lỗi tải sách: ${e.message}"
                rvSearchResults.visibility = RecyclerView.GONE
                tvSearchResult.visibility = TextView.VISIBLE
            }
    }

    private fun searchBooks(keyword: String) {
        progressBar.visibility = ProgressBar.VISIBLE
        tvSearchResult.visibility = TextView.GONE
        rvSearchResults.visibility = RecyclerView.GONE

        val includedTags = tagFilterAdapter.getIncludedTags()
        val excludedTags = tagFilterAdapter.getExcludedTags()

        db.collection("books")
            .get()
            .addOnSuccessListener { documents ->
                val bookList = mutableListOf<Book>()

                for (document in documents) {
                    val book = document.toObject(Book::class.java)

                    // Filter theo ngôn ngữ
                    if (selectedLanguage.isNotEmpty() && book.language != selectedLanguage) {
                        continue
                    }

                    // Tìm kiếm theo tiêu đề, tác giả, ngôn ngữ hoặc tags
                    val matchesKeyword = book.title.contains(keyword, ignoreCase = true) ||
                        book.author.contains(keyword, ignoreCase = true) ||
                        book.language.contains(keyword, ignoreCase = true) ||
                        book.tags.any { it.contains(keyword, ignoreCase = true) }

                    if (!matchesKeyword) continue

                    // Áp dụng include tags filter
                    if (includedTags.isNotEmpty()) {
                        val hasIncludedTag = includedTags.any { includedTag ->
                            book.tags.any { it.equals(includedTag, ignoreCase = true) }
                        }
                        if (!hasIncludedTag) continue
                    }

                    // Áp dụng exclude tags filter
                    if (excludedTags.isNotEmpty()) {
                        val hasExcludedTag = excludedTags.any { excludedTag ->
                            book.tags.any { it.equals(excludedTag, ignoreCase = true) }
                        }
                        if (hasExcludedTag) continue
                    }

                    bookList.add(book)
                }

                progressBar.visibility = ProgressBar.GONE

                if (bookList.isNotEmpty()) {
                    bookAdapter.updateList(bookList)
                    rvSearchResults.visibility = RecyclerView.VISIBLE
                    tvSearchResult.visibility = TextView.GONE
                } else {
                    tvSearchResult.text = "Không tìm thấy sách với từ khóa: \"$keyword\""
                    rvSearchResults.visibility = RecyclerView.GONE
                    tvSearchResult.visibility = TextView.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = ProgressBar.GONE
                tvSearchResult.text = "Lỗi tìm kiếm: ${e.message}"
                rvSearchResults.visibility = RecyclerView.GONE
                tvSearchResult.visibility = TextView.VISIBLE
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

