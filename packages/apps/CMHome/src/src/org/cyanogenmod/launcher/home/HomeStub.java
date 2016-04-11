/*
 * Copyright (C) 2014 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cyanogenmod.launcher.home;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AccelerateInterpolator;

import com.android.launcher.home.Home;

import it.gmariotti.cardslib.library.view.listener.dismiss.DefaultDismissableManager;
import org.cyanogenmod.launcher.cardprovider.DashClockExtensionCardProvider;
import org.cyanogenmod.launcher.cardprovider.ICardProvider;
import org.cyanogenmod.launcher.cardprovider.ICardProvider.CardProviderUpdateResult;
import org.cyanogenmod.launcher.cards.SimpleMessageCard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;

public class HomeStub implements Home {

    private static final String TAG = "HomeStub";
    private static final String NO_EXTENSIONS_CARD_ID = "noExtensions";
    private HomeLayout mHomeLayout;
    private Context mHostActivityContext;
    private Context mCMHomeContext;
    private boolean mShowContent = false;
    private SimpleMessageCard mNoExtensionsCard;
    private List<ICardProvider> mCardProviders = new ArrayList<ICardProvider>();
    private CMHomeCardArrayAdapter mCardArrayAdapter;

    private HashMap<String, AsyncTask> mUpdateTasks = new HashMap<String, AsyncTask>();

    private final AccelerateInterpolator mAlphaInterpolator;

    private final ICardProvider.CardProviderUpdateListener mCardProviderUpdateListener =
            new ICardProvider.CardProviderUpdateListener() {
                @Override
                public void onCardProviderUpdate(String cardId) {
                    refreshCard(cardId);
                }
            };

    public HomeStub() {
        super();
        mAlphaInterpolator = new AccelerateInterpolator();
    }

    @Override
    public void setHostActivityContext(Context context) {
        mHostActivityContext = context;
    }

    @Override
    public void onStart(Context context) {
        mCMHomeContext = context;
        if(mShowContent) {
            // Add any providers we wish to include, if we should show content
            initProvidersIfNeeded(context);
        }
    }

    @Override
    public void setShowContent(Context context, boolean showContent) {
        mShowContent = showContent;
        if(mShowContent) {
            // Add any providers we wish to include, if we should show content
            initProvidersIfNeeded(context);
            if(mHomeLayout != null) {
                loadCardsFromProviders(context);
            }
        } else {
            for(ICardProvider cardProvider : mCardProviders) {
                cardProvider.onHide(context);
            }
            mCardProviders.clear();
            if(mHomeLayout != null) {
                removeAllCards(context);
                // Make sure that the Undo Bar is hidden if no content is to be shown.
                hideUndoBar();
            }
        }
    }

    @Override
    public void onDestroy(Context context) {
        mHomeLayout = null;
    }

    @Override
    public void onResume(Context context) {
    }

    @Override
    public void onPause(Context context) {
    }

    @Override
    public void onShow(Context context) {
        if (mHomeLayout != null) {
            mHomeLayout.setAlpha(1.0f);

            if(mShowContent) {
                for(ICardProvider cardProvider : mCardProviders) {
                    cardProvider.onShow();
                    cardProvider.requestRefresh();
                }
            } else {
                hideUndoBar();
            }
        }
    }

    @Override
    public void onScrollProgressChanged(Context context, float progress) {
        if (mHomeLayout != null) {
            mHomeLayout.setAlpha(mAlphaInterpolator.getInterpolation(progress));
        }
    }

    @Override
    public void onHide(Context context) {
        if (mHomeLayout != null) {
            mHomeLayout.setAlpha(0.0f);
        }
        for(ICardProvider cardProvider : mCardProviders) {
            cardProvider.onHide(context);
        }
    }

    @Override
    public void onInvalidate(Context context) {
        if (mHomeLayout != null) {
            mHomeLayout.removeAllViews();
        }
    }

    @Override
    public void onRequestSearch(Context context, int mode) {
        
    }

    @Override
    public View createCustomView(Context context) {
        if(mHomeLayout == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            mHomeLayout = (HomeLayout) inflater.inflate(R.layout.home_layout, null);
        }
        hideUndoBar();

        return mHomeLayout;
    }

    @Override
    public String getName(Context context) {
        return "HomeStub";
    }

    @Override
    public int getNotificationFlags() {
        return Home.FLAG_NOTIFY_ALL;
    }

    @Override
    public int getOperationFlags() {
        return Home.FLAG_OP_MASK;
    }

    private void hideUndoBar() {
        View undoLayout = mHomeLayout.findViewById(R.id.list_card_undobar);
        undoLayout.setVisibility(View.GONE);
    }

    public void initProvidersIfNeeded(Context context) {
        if (mCardProviders.size() == 0) {
            mCardProviders.add(new DashClockExtensionCardProvider(context, mHostActivityContext));

            for (ICardProvider cardProvider : mCardProviders) {
                cardProvider.addOnUpdateListener(mCardProviderUpdateListener);
            }
        }
    }

    /*
     * Gets a list of all cards provided by each provider,
     * and updates the UI to show them.
     */
    private void loadCardsFromProviders(Context context) {
        // If cards have been initialized already, just update them
        if(mCardArrayAdapter != null
           && mCardArrayAdapter.getCards().size() > 0
           && mHomeLayout != null) {
            refreshCards(true);
        } else {
            new LoadAllCardsTask(context).execute();
        }
    }

    /**
     * Creates a card with a message to inform the user they have no extensions
     * installed to publish content.
     */
    private Card getNoExtensionsCard(final Context context) {
        if (mNoExtensionsCard == null) {
            mNoExtensionsCard = new SimpleMessageCard(context);
            mNoExtensionsCard.setTitle(context.getResources().getString(R.string.no_extensions_card_title));
            mNoExtensionsCard.setBody(context.getResources().getString(R.string.no_extensions_card_body));
            mNoExtensionsCard.setId(NO_EXTENSIONS_CARD_ID);
        }

        return mNoExtensionsCard;
    }

    /**
     * Refresh all cards by asking the providers to update them.
     * @param addNew If providers have new cards that have not
     * been displayed yet, should they be added?
     */
    public void refreshCards(boolean addNew) {
        new RefreshAllCardsTask(addNew).execute();
    }

    public void refreshCard(String cardId) {
        AsyncTask mTask = mUpdateTasks.get(cardId);
        // Nothing to refresh if the card adapter hasn't been
        // initialized yet or the ID we've been given is null.
        if (cardId != null && mCardArrayAdapter != null) {
            if (mTask != null) {
                mTask.cancel(true);
            }
            AsyncTask<Void, Void, Card> newTask =
                    new LoadSingleCardTask(cardId, mCMHomeContext);
            mUpdateTasks.put(cardId, newTask);
            newTask.execute();
        }
    }

    private void removeAllCards(Context context) {
        CardListView cardListView = (CardListView) mHomeLayout.findViewById(R.id.cm_home_cards_list);
        // Set CardArrayAdapter to an adapter with an empty list
        List<Card> cards = new ArrayList<Card>();
        mCardArrayAdapter = new CMHomeCardArrayAdapter(context, cards);
        cardListView.setAdapter(mCardArrayAdapter);
    }

    private class LoadSingleCardTask extends AsyncTask<Void, Void, Card> {
        private String mId;
        private Context mContext;
        private boolean mIsCardNew = false;

        public LoadSingleCardTask(String id, Context context) {
            mId = id;
            mContext = context;
        }

        @Override
        protected Card doInBackground(Void... voids) {
            Card card = mCardArrayAdapter.getCardWithId(mId);

            // The card already exists in the list
            if (card != null) {
                // Allow each provider to update the card (if necessary)
                for (ICardProvider cardProvider : mCardProviders) {
                    cardProvider.updateCard(card);
                }
            } else {
                // The card is brand new, add it
                Card newCard = null;
                for (ICardProvider cardProvider : mCardProviders) {
                    newCard = cardProvider.createCardForId(mId);
                    if (newCard != null) break;
                }

                if (newCard != null) {
                    card = newCard;
                    mIsCardNew = true;
                }
            }
            return card;
        }

        @Override
        protected void onPostExecute(Card card) {
            super.onPostExecute(card);

            if (card != null) {
                if (mIsCardNew) {
                    mCardArrayAdapter.add(card);
                    // Remove the "no cards" card, if it's there.
                    mCardArrayAdapter.remove(getNoExtensionsCard(mContext));
                    mCardArrayAdapter.notifyDataSetChanged();
                } else {
                    mCardArrayAdapter.updateCardViewIfVisible(card);
                }
            }
            mUpdateTasks.remove(mId);
        }
    }

    private class LoadAllCardsTask extends AsyncTask<Void, Void, List<Card>> {
        private Context mContext;

        public LoadAllCardsTask(Context context) {
            mContext = context;
        }

        @Override
        protected List<Card> doInBackground(Void... voids) {
            List<Card> cards = new ArrayList<Card>();
            for (ICardProvider provider : mCardProviders) {
                for (Card card : provider.getCards()) {
                    cards.add(card);
                }
            }

            // If there aren't any cards, show the user a message about how to fix that!
            if (cards.size() == 0) {
                cards.add(getNoExtensionsCard(mContext));
            }
            return cards;
        }

        @Override
        protected void onPostExecute(List<Card> cards) {
            super.onPostExecute(cards);
            CardListView cardListView =
                    (CardListView) mHomeLayout.findViewById(R.id.cm_home_cards_list);

            if(cardListView != null) {
                mCardArrayAdapter = new CMHomeCardArrayAdapter(mContext, cards);
                mCardArrayAdapter.setEnableUndo(true, mHomeLayout);
                mCardArrayAdapter.setDismissable(new RightDismissableManager());
                cardListView.setAdapter(mCardArrayAdapter);
            }
        }
    }

    private class RefreshAllCardsTask extends AsyncTask<Void, Void, CardProviderUpdateResult> {
        private boolean mAddNew = false;
        private int mFinalCardCount = 0;
        private boolean mNoExtensionsCardExists = false;

        public RefreshAllCardsTask(boolean addNew) {
            mAddNew = addNew;
        }

        @Override
        protected CardProviderUpdateResult doInBackground(Void... voids) {
            List<Card> originalCards = mCardArrayAdapter.getCards();

            CardProviderUpdateResult updateResult = null;
            // Allow each provider to update it's cards
            for (ICardProvider cardProvider : mCardProviders) {
                updateResult = cardProvider.updateAndAddCards(originalCards);
            }

            mNoExtensionsCardExists = originalCards.contains(mNoExtensionsCard);
            mFinalCardCount = originalCards.size();

            if (updateResult != null) {
                mFinalCardCount += updateResult.getCardsToAdd().size();
                mFinalCardCount -= updateResult.getCardsToRemove().size();
            }
            return updateResult;
        }

        @Override
        protected void onPostExecute(CardProviderUpdateResult updateResult) {
            super.onPostExecute(updateResult);

            if (updateResult != null) {
                if (mAddNew) {
                    mCardArrayAdapter.addAll(updateResult.getCardsToAdd());
                }
                for (Card card : updateResult.getCardsToRemove()) {
                    mCardArrayAdapter.remove(card);
                }
            }

            if (mNoExtensionsCardExists && mFinalCardCount > 1) {
                mCardArrayAdapter.remove(mNoExtensionsCard);
            }

            mCardArrayAdapter.notifyDataSetChanged();
        }
    }

    public class CMHomeCardArrayAdapter extends CardArrayAdapter {

        public CMHomeCardArrayAdapter(Context context, List<Card> cards) {
            super(context, cards);
        }

        public List<Card> getCards() {
            List<Card> cardsToReturn = new ArrayList<Card>();
            for(int i = 0; i < getCount(); i++) {
                cardsToReturn.add(getItem(i));
            }
            return cardsToReturn;
        }

        public Card getCardWithId(String id) {
            Card theCard = null;
            for(int i = 0; i < getCount(); i++) {
                Card card = getItem(i);
                if (card.getId().equals(id)) {
                    theCard = card;
                    break;
                }
            }
            return theCard;
        }

        /**
         * Find the CardView displaying the card that has changed
         * and update it, if it is currently on screen. Otherwise,
         * do nothing.
         * @param card The card object to re-draw onscreen.
         */
        public void updateCardViewIfVisible(Card card) {
            CardListView listView = getCardListView();
            int start = listView.getFirstVisiblePosition();
            int last = listView.getLastVisiblePosition();
            for (int i = start; i <= last; i++) {
                if (card == listView.getItemAtPosition(i)) {
                    View cardView = listView.getChildAt(i - start);
                    getView(i, cardView, listView);
                    break;
                }
            }
        }
    }

    /**
     * A DismissableManager implementation that only allows cards to be swiped to the right.
     */
    private class RightDismissableManager extends DefaultDismissableManager {
        @Override
        public SwipeDirection getSwipeDirectionAllowed() {
            return SwipeDirection.RIGHT;
        }
    }
}
