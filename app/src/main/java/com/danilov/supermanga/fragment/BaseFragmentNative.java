package com.danilov.supermanga.fragment;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.view.View;

import com.danilov.supermanga.core.application.MangaApplication;
import com.danilov.supermanga.core.util.SafeHandler;

/**
 * Created by Semyon on 29.02.2016.
 */
public class BaseFragmentNative extends Fragment {

    protected View view;

    protected SafeHandler handler = new SafeHandler();

    protected final Context applicationContext = MangaApplication.getContext();

    protected <T extends View> T findViewById(final int id) {
        return (T) view.findViewById(id);
    }

    public boolean onBackPressed() {
        return false;
    }

    @NonNull
    public SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(applicationContext);
    }

    public Context getContext() {
        return applicationContext;
    }

}