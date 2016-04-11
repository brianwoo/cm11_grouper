package org.cyanogenmod.launcher.cards;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.cyanogenmod.launcher.home.R;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * A custom card that will show a title and message only.
 * Swipe is also enabled by default.
 */
public class SimpleMessageCard extends Card {
    private String mBody;

    public SimpleMessageCard(Context context) {
        this(context, R.layout.simple_message_card_inner_content);
    }

    public SimpleMessageCard(final Context context, int innerLayout) {
        super(context, innerLayout);
        setSwipeable(true);
    }

    public void setBody(String body) {
        mBody = body;
    }

    public String getBody() {
        return mBody;
    }

    @Override
    public void setupInnerViewElements(ViewGroup parent, View view) {
        TextView title = (TextView)view.findViewById(R.id.simple_message_card_title);
        TextView body = (TextView)view.findViewById(R.id.simple_message_card_text);

        if (!TextUtils.isEmpty(getTitle())) {
            title.setText(getTitle());
        }
        if (!TextUtils.isEmpty(getBody())) {
            body.setText(getBody());
        }
    }
}
