package com.esabook.auzen.article.readview

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esabook.auzen.App
import com.esabook.auzen.data.db.entity.ArticleEntity
import com.esabook.auzen.extentions.NewsParserUtils
import com.esabook.auzen.extentions.NewsParserUtils.toArticleEntity
import com.esabook.auzen.extentions.collectLatest2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dankito.readability4j.Article
import timber.log.Timber

class ReadVM : ViewModel() {

    var readibilityModeOn = MutableLiveData(false)
    var articleLink: String = ""
    val isUnread = MutableLiveData<Boolean>()
    val isInPlaylist = MutableLiveData<Boolean>()

    val article: MutableLiveData<Article?> = MutableLiveData()
    var selectedFont = fontFamilies[0]

    var articleEntity: ArticleEntity? = null
        private set

    private val articleEntityRaw by lazy {
        App.db.articleDao().findByGuidOrLink(articleLink)
    }

    private fun onCollectArticleEntityRaw(a: ArticleEntity?) {
        if (a?.guid == articleEntity?.guid
            && a?.isUnread == articleEntity?.isUnread
            && a?.isPlayListQueue == articleEntity?.isPlayListQueue
        )
            return

        articleEntity = a
        isUnread.postValue(articleEntity?.isUnread == true)
        isInPlaylist.postValue(articleEntity?.isPlayListQueue == true)
        if (a != null)
            generateArticle()
        else
            generateArticle(false)

    }

    fun generateArticle(isUseLocal: Boolean = true) {
        if (articleLink.isBlank()) {
            return
        }


        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isUseLocal && article.value?.uri != articleLink) {
                    articleEntityRaw.collectLatest2(this@ReadVM::onCollectArticleEntityRaw)
                }

                val link = articleLink

                val article = NewsParserUtils.getArticle(link)

                article?.uri?.also { articleLink = it }

                if (articleEntity == null && article != null) {
                    val newArticleEntity = article.toArticleEntity()
                    articleEntity = newArticleEntity
                    App.db.articleDao().insertAll(newArticleEntity)
                }


                this@ReadVM.article.postValue(article)

            } catch (e: Exception) {
                Timber.e(e)
            }

        }
    }

    fun markUnRead(_isUnRead: Boolean) {
        if (articleEntity?.isUnread == _isUnRead) return
        viewModelScope.launch(Dispatchers.IO) {
            articleEntity?.guid?.let {
                App.db.articleDao().markAsRead(it, _isUnRead)
            }
        }
    }

    fun markInPlaylist(asQueue: Boolean = true) {
        if (articleEntity?.isPlayListQueue == asQueue) return
        viewModelScope.launch(Dispatchers.IO) {
            articleEntity?.guid?.let {
                App.db.articleQueueDao().update(it, asQueue)
            }
        }
    }


    fun title() = article.value?.title

    suspend fun getHtmlContent(content: Article, articleEntity: ArticleEntity?): String {
        return withContext(Dispatchers.IO) {
            var newContent = content.contentWithDocumentsCharsetOrUtf8

            content.title?.let {
                newContent = newContent?.replaceFirst(it.toRegex(), "")
            }

            content.byline?.let {
                newContent = newContent?.replaceFirst(it, "")
            }

            articleEntity?.enclosure?.let {
                newContent = newContent?.replaceFirst(it, "")
            }

            bodyStyle
                .replaceFirst(FONT_FIRST, selectedFont.name)
                .replaceFirst(FONT_URL, selectedFont.fontLink)
                .replaceFirst(TITLE, content.title ?: "")
                .replaceFirst(CONTENT, newContent ?: "")
        }
    }

    data class Font(val name: String, val fontLink: String)

    companion object {
        const val FONT_URL = "%FONT_URL%"
        const val FONT_FIRST = "%FONT_FIRST%"
        const val TITLE = "%TITLE%"
        const val PUB_DATE = "%PUB-DATE%"
        const val CONTENT = "%CONTENT%"
        const val COVER = "%COVER%"

        val fontFamilies: Array<Font> by lazy {
            arrayOf(
                Font(
                    """"Fira Sans", sans-serif""", """
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Fira+Sans&display=swap" rel="stylesheet"> 
                """.trimIndent()
                ),

                Font(
                    """"Crimson Text", serif""", """
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link href="https://fonts.googleapis.com/css2?family=Crimson+Text&display=swap" rel="stylesheet"> 
                """.trimIndent()
                )
            )
        }

        val bodyStyle: String by lazy {
            """
        <html>
          <head>
            $FONT_URL
            <style>
            * {
                font-family: $FONT_FIRST; 
                letter-spacing: 0px;
                }
            p {
                color: #737373
            }
            
            h1, h2, h3, h4, h5, h6, p b, p i, i, strong, p strong {
                font-family: $FONT_FIRST; 
                color:#353535
                letter-spacing: 0.25px;
                }
            a {
                text-decoration: none;color:#4b8b26; 
                }
            body {
                 margin-left: 16px;
                 margin-right: 16px;
                 background-color: #ffffff;
                 overflow-x: hidden; 
                 }
            figure {
                 margin: 0px;
                }
            iframe {
                 width: calc(100% + 32px) !important;
                 height: auto !important;
                 margin-left: -16px !important;
                 margin-right: -16px !important;
                 }
            video, img {
                width: 100%;
                max-width: 100%;
                max-inline-size: 100%;
                height: auto !important;
                block-size: auto
                }
            img[src=""],img[src="null"] {
               display: none !important;
            }
           
            </style>
          </head>
          <body>
          <h2>$TITLE</h2>
          <br/>
          $CONTENT
          </body>
        </html>

    """.trimIndent()
        }
    }

}