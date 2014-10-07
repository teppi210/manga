package com.danilov.manga.core.repository;

import android.util.Log;
import com.danilov.manga.core.http.*;
import com.danilov.manga.core.model.Manga;
import com.danilov.manga.core.model.MangaChapter;
import com.danilov.manga.core.model.MangaSuggestion;
import com.danilov.manga.core.util.IoUtils;
import com.danilov.manga.core.util.ServiceContainer;
import com.danilov.manga.core.util.Utils;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Semyon Danilov on 17.05.2014.
 */

/**
 * Engine for russian most popular manga site, geh
 */
public class ReadmangaEngine implements RepositoryEngine {

    private static final String TAG = "ReadmangaEngine";

//    private String baseSearchUri = "http://readmanga.me/search?q=";
//    private String baseSuggestionUri = "http://readmanga.me/search/suggestion?query=";
//    public static final String baseUri = "http://readmanga.me";

//  ADULT
    private String baseSearchUri = "http://adultmanga.ru/search?q=";
    private String baseSuggestionUri = "http://adultmanga.ru/search/suggestion?query=";
    public static final String baseUri = "http://adultmanga.ru";

    @Override
    public String getLanguage() {
        return "Русский";
    }

    @Override
    public List<MangaSuggestion> getSuggestions(final String query) throws RepositoryException {
        HttpBytesReader httpBytesReader = ServiceContainer.getService(HttpBytesReader.class);
        List<MangaSuggestion> suggestions = null;
        if (httpBytesReader != null) {
            Exception ex = null;
            try {
                String uri = baseSuggestionUri + URLEncoder.encode(query, Charset.forName(HTTP.UTF_8).name());
                byte[] response = httpBytesReader.fromUri(uri);
                String responseString = IoUtils.convertBytesToString(response);
                suggestions = parseSuggestionsResponse(Utils.toJSON(responseString));
            } catch (UnsupportedEncodingException e) {
                ex = e;
            } catch (HttpRequestException e) {
                ex = e;
            } catch (JSONException e) {
                ex = e;
            }
            if (ex != null) {
                throw new RepositoryException(ex.getMessage());
            }
        }
        return suggestions;
    }

    @Override
    public List<Manga> queryRepository(final String query) {
        HttpBytesReader httpBytesReader = ServiceContainer.getService(HttpBytesReader.class);
        List<Manga> mangaList = null;
        if (httpBytesReader != null) {
            try {
                String uri = baseSearchUri + URLEncoder.encode(query, Charset.forName(HTTP.UTF_8).name());
                byte[] response = httpBytesReader.fromUri(uri);
                String responseString = IoUtils.convertBytesToString(response);
                mangaList = parseSearchResponse(Utils.toDocument(responseString));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (HttpRequestException e) {
                e.printStackTrace();
            }
        }
        return mangaList;
    }

    @Override
    public boolean queryForMangaDescription(final Manga manga) throws RepositoryException {
        HttpBytesReader httpBytesReader = ServiceContainer.getService(HttpBytesReader.class);
        if (httpBytesReader != null) {
            String uri = baseUri + manga.getUri();
            byte[] response = null;
            try {
                response = httpBytesReader.fromUri(uri);
            } catch (HttpRequestException e) {
                if (e.getMessage() != null) {
                    Log.d(TAG, e.getMessage());
                } else {
                    Log.d(TAG, "Failed to load manga description");
                }
                throw new RepositoryException(e.getMessage());
            }
            String responseString = IoUtils.convertBytesToString(response);
            return parseMangaDescriptionResponse(manga, Utils.toDocument(responseString));
        }
        return false;
    }

    @Override
    public boolean queryForChapters(final Manga manga) throws RepositoryException {
        HttpBytesReader httpBytesReader = ServiceContainer.getService(HttpBytesReader.class);
        if (httpBytesReader != null) {
            String uri = baseUri + manga.getUri();
            byte[] response = null;
            try {
                response = httpBytesReader.fromUri(uri);
            } catch (HttpRequestException e) {
                Log.d(TAG, e.getMessage());
                throw new RepositoryException(e.getMessage());
            }
            String responseString = IoUtils.convertBytesToString(response);
            List<MangaChapter> chapters = parseMangaChaptersResponse(Utils.toDocument(responseString));
            if (chapters != null) {
                manga.setChapters(chapters);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> getChapterImages(final MangaChapter chapter) throws RepositoryException {
        String uri = baseUri + chapter.getUri() + "?mature=1";
        HttpBytesReader httpBytesReader = ServiceContainer.getService(HttpBytesReader.class);
        HttpStreamReader httpStreamReader = ServiceContainer.getService(HttpStreamReader.class);
        byte[] bytes = new byte[1024];
        List<String> imageUrls = null;
        LinesSearchInputStream inputStream = null;
        try {
            HttpStreamModel model = httpStreamReader.fromUri(uri);
            inputStream = new LinesSearchInputStream(model.stream, "pictures = [", "];");
            int status = LinesSearchInputStream.SEARCHING;
            while (status == LinesSearchInputStream.SEARCHING) {
                status = inputStream.read(bytes);
            }
            bytes = inputStream.getResult();
            String str = IoUtils.convertBytesToString(bytes);
            imageUrls = extractUrls(str);
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
            throw new RepositoryException(e.getMessage());
        } catch (HttpRequestException e) {
            Log.d(TAG, e.getMessage());
            throw new RepositoryException(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.d(TAG, e.getMessage());
                }
            }
        }
        return imageUrls;
    }

    private final Pattern urlPattern = Pattern.compile("url:\"(.*?)\"");

    private List<String> extractUrls(final String str) {
        Log.d(TAG, "a: " + str);
        Matcher matcher = urlPattern.matcher(str);
        List<String> urls = new ArrayList<String>();
        while (matcher.find()) {
            String url = matcher.group(1);
            if (!url.contains("http")) {
                url = baseUri + url;
            }
            urls.add(url);
        }
        return urls;
    }

    //json names

    private String suggestionsElementName = "suggestions";
    private String exclusionValue = "Автор ";
    private String exclusionValue2 = "Переводчик ";
    private String valueElementName = "value";
    private String dataElementName = "data";
    private String linkElementName = "link";


    //!json names

    private List<MangaSuggestion> parseSuggestionsResponse(final JSONObject object) {
        List<MangaSuggestion> mangaSuggestions = new ArrayList<MangaSuggestion>();
        try {
            JSONArray suggestions = object.getJSONArray(suggestionsElementName);
            for (int i = 0; i < suggestions.length(); i++) {
                JSONObject suggestion = suggestions.getJSONObject(i);
                String value = suggestion.getString(valueElementName);
                if (value == null
                        || value.contains(exclusionValue)
                        || value.contains(exclusionValue2)) {
                    continue;
                }
                try {
                    JSONObject data = suggestion.getJSONObject(dataElementName);
                    if (data == null) {
                        continue;
                    }
                    String link = data.getString(linkElementName);
                    MangaSuggestion mangaSuggestion = new MangaSuggestion(value, link, Repository.READMANGA);
                    mangaSuggestions.add(mangaSuggestion);
                } catch (JSONException e ) {
                    Log.d(TAG, e.getMessage());
                    continue;
                }
            }
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
        }
        return mangaSuggestions;
    }

    //html values
    private String searchElementId = "mangaResults";
    private String mangaTileClass = "tile";
    private String mangaCoverClass = "img";
    private String mangaDescClass = "desc";

    //!html values

    private List<Manga> parseSearchResponse(final Document document) {
        List<Manga> mangaList = null;
        Element searchResults = document.getElementById(searchElementId);
        List<Element> mangaLinks = searchResults.getElementsByClass(mangaTileClass);
        mangaList = new ArrayList<Manga>(mangaLinks.size());
        for (Element mangaLink : mangaLinks) {
            String uri = null;
            String mangaName = null;
            Elements tmp = mangaLink.getElementsByClass(mangaDescClass);
            if (!tmp.isEmpty()) {
                tmp = tmp.get(0).getElementsByTag("h3");
                if (!tmp.isEmpty()) {
                    tmp = tmp.get(0).getElementsByTag("a");
                    if (!tmp.isEmpty()) {
                        Element realLink = tmp.get(0);
                        uri = realLink.attr("href");
                        mangaName = realLink.attr("title");
                    }
                }
            }

            Element screenElement = mangaLink.getElementsByClass(mangaCoverClass).get(0);

            tmp = screenElement.getElementsByTag("img");
            String coverUri = null;
            if (!tmp.isEmpty()) {
                Element img = tmp.get(0);
                coverUri = img != null ? img.attr("src") : "";
            }
            Manga manga = new Manga(mangaName, uri, Repository.READMANGA);
            manga.setCoverUri(coverUri);
            mangaList.add(manga);
        }
        return mangaList;
    }

    //html values
    private String descriptionElementClass = "manga-description";
    private String chaptersElementClass = "chapters-link";
    private String coverClassName = "subject-cower";
    private String coverClass = "full-list-pic";
    private String altCoverClass = "nivo-imageLink";

    private boolean parseMangaDescriptionResponse(final Manga manga, final Document document) {
        Elements mangaDescriptionElements = document.getElementsByClass(descriptionElementClass);
        if (mangaDescriptionElements.isEmpty()) {
            return false;
        }
        Element mangaDescription = mangaDescriptionElements.first();
        Elements links = mangaDescription.getElementsByTag("a");
        if (!links.isEmpty()) {
            links.remove();
        }
        String description = mangaDescription.text();
        Elements chaptersElements = document.getElementsByClass(chaptersElementClass);
        int quantity = 0;
        if (chaptersElements.isEmpty()) {
            quantity = 0;
        } else {
            Element chaptersElement = chaptersElements.first();
            links = chaptersElement.getElementsByTag("a");
            quantity = links.size();
        }
        manga.setChaptersQuantity(quantity);
        manga.setDescription(description);
        if (manga.getCoverUri() != null) {
            return true;
        }
        Elements coverContainer = document.getElementsByClass(coverClassName);
        String coverUri = null;
        if (coverContainer.size() >= 1) {
            Element cover = coverContainer.get(0);
            Elements coverUriElements = cover.getElementsByClass(coverClass);
            if (coverUriElements.size() >= 1) {
                //a lot of images
                Element e = coverUriElements.get(0);
                coverUri = e.attr("href");
            } else {
                coverUriElements = cover.getElementsByClass(altCoverClass);
                if (coverUriElements.size() >= 1) {
                    //more than one
                    coverUri = coverUriElements.get(0).attr("href");
                } else {
                    //only one
                    Elements elements = cover.getElementsByTag("img");
                    if (elements.size() >= 1) {
                        coverUri = elements.get(0).attr("src");
                    }
                }
            }
        }
        manga.setCoverUri(coverUri);
        return true;
    }

    //html values
    private String linkValueAttr = "href";

    private List<MangaChapter> parseMangaChaptersResponse(final Document document) {
        Elements chaptersElements = document.getElementsByClass(chaptersElementClass);
        if (chaptersElements.isEmpty()) {
            return null;
        }
        Element chaptersElement = chaptersElements.first();
        Elements links = chaptersElement.getElementsByTag("a");
        if (links.isEmpty()) {
            return null;
        }
        List<MangaChapter> chapters = new ArrayList<MangaChapter>(links.size());
        int number = 0;
        for (int i = links.size() - 1; i >= 0; i--) {
            Element element = links.get(i);
            String link = element.attr(linkValueAttr);
            String title = element.text();
            MangaChapter chapter = new MangaChapter(title, number, link);
            chapters.add(chapter);
            number++;
        }
        return chapters;
    }

    @Override
    public String getBaseSearchUri() {
        return baseSearchUri;
    }

    @Override
    public String getBaseUri() {
        return baseUri;
    }

}
