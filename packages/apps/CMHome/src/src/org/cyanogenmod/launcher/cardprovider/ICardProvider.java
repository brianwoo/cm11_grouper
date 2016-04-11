package org.cyanogenmod.launcher.cardprovider;

import android.content.Context;

import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;

/**
 * An interface for classes that can manage data for and provide Cards to be displayed.
 */
public interface ICardProvider {
    public void onHide(Context context);
    public void onShow();
    public void requestRefresh();

    /**
     * Given a list of cards, update any card for which
     * there is new data available.
     * @param cards Cards to update
     * @return A list of cards that must be added
     */
    public CardProviderUpdateResult updateAndAddCards(List<Card> cards);

    /**
     * Given a card, update it to the freshest data.
     * @param card The card to update.
     */
    public void updateCard(Card card);

    /**
     * Given an ID known to this ICardProvider,
     * generate a card to represent the latest data
     * for that Card ID.
     * @param id An ID string known to this card provider, such as
     *           passed in CardProviderUpdateListener.onCardProviderUpdate
     */
    public Card createCardForId(String id);
    public List<Card> getCards();
    public void addOnUpdateListener(CardProviderUpdateListener listener);

    public interface CardProviderUpdateListener {
        public void onCardProviderUpdate(String cardId);
    }

    public class CardProviderUpdateResult {
        List<Card> mCardsToAdd;
        List<Card> mCardsToRemove;

        CardProviderUpdateResult(List<Card> cardsToAdd, List<Card> cardsToRemove) {
            mCardsToAdd = cardsToAdd;
            mCardsToRemove = cardsToRemove;
        }

        public List<Card> getCardsToAdd() {
            return mCardsToAdd;
        }

        public List<Card> getCardsToRemove() {
            return mCardsToRemove;
        }
    }
}
