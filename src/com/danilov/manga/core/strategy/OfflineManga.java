package com.danilov.manga.core.strategy;

import com.danilov.manga.core.interfaces.MangaShowStrategy;
import com.danilov.manga.core.model.LocalManga;
import com.danilov.manga.core.model.MangaChapter;
import com.danilov.manga.core.repository.RepositoryEngine;
import com.danilov.manga.core.repository.RepositoryException;
import com.danilov.manga.core.view.InAndOutAnim;
import com.danilov.manga.core.view.MangaImageSwitcher;

import java.io.File;
import java.util.List;

/**
 * Created by Semyon Danilov on 21.06.2014.
 */
public class OfflineManga implements MangaShowStrategy {

    private LocalManga manga;
    private MangaImageSwitcher mangaImageSwitcher;
    private InAndOutAnim nextImageAnim;
    private InAndOutAnim prevImageAnim;

    private RepositoryEngine engine = RepositoryEngine.Repository.OFFLINE.getEngine();

    private List<String> uris = null;

    private int currentImageNumber;
    private int currentChapter;

    public OfflineManga(final LocalManga manga, final MangaImageSwitcher mangaImageSwitcher, final InAndOutAnim nextImageAnim, final InAndOutAnim prevImageAnim) {
        this.manga = manga;
        this.mangaImageSwitcher = mangaImageSwitcher;
        this.nextImageAnim = nextImageAnim;
        this.prevImageAnim = prevImageAnim;
    }

    @Override
    public void initStrategy() throws ShowMangaException {
        try {
            engine.queryForChapters(manga);
        } catch (RepositoryException e) {
            throw new ShowMangaException(e.getMessage());
        }
    }

    @Override
    public void showImage(final int i) throws ShowMangaException {
        if (i == currentImageNumber || i >= uris.size() || i < 0) {
            return;
        }
        File imageFile = new File(uris.get(i));
        if (i < currentImageNumber) {
            mangaImageSwitcher.setInAndOutAnim(prevImageAnim);
            mangaImageSwitcher.setPreviousImageDrawable(imageFile.getPath());
        } else if (i > currentImageNumber) {
            mangaImageSwitcher.setInAndOutAnim(nextImageAnim);
            mangaImageSwitcher.setNextImageDrawable(imageFile.getPath());
        }
        currentImageNumber = i;
    }

    @Override
    public void showChapter(final int i) throws ShowMangaException {
        this.currentChapter = i;
        this.currentImageNumber = -1;
        MangaChapter chapter = manga.getChapterByNumber(currentChapter);
        try {
            uris = engine.getChapterImages(chapter);
        } catch (RepositoryException e) {
            throw new ShowMangaException(e.getMessage());
        }
        if (uris != null && uris.size() > 0) {
            showImage(0);
        }
    }

    @Override
    public void next() throws ShowMangaException {
        if (currentImageNumber + 1 >= uris.size()) {
            showChapter(currentChapter + 1);
            return;
        }
        showImage(currentImageNumber + 1);
    }

    @Override
    public void previous() throws ShowMangaException {
        showImage(currentImageNumber - 1);
    }

    public int getCurrentChapter() {
        return currentChapter;
    }

    public int getCurrentImageNumber() {
        return currentImageNumber;
    }

}
