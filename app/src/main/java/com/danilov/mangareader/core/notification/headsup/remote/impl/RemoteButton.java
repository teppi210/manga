package com.danilov.mangareader.core.notification.headsup.remote.impl;

import android.view.View;
import android.widget.Button;

import com.danilov.mangareader.core.notification.headsup.remote.HRemoteView;

/**
 * Created by semyon on 17.12.14.
 */
public class RemoteButton extends HRemoteView<Button> {

    private View.OnClickListener onClickListener;

    public RemoteButton(final int viewId) {
        super(viewId);
    }

    public void setOnClickListener(final View.OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    public void apply(final Button view) {
        view.setOnClickListener(onClickListener);
    }

}
