package com.codecool.klondike;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Pane;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.*;

public class Game extends Pane {

    private List<Card> deck = new ArrayList<>();
    private List<Card> remainCards = new ArrayList<>();
    private List<Card> iterateCards = new ArrayList<>();

    private Pile stockPile;
    private Pile discardPile;
    private List<Pile> foundationPiles = FXCollections.observableArrayList();
    private List<Pile> tableauPiles = FXCollections.observableArrayList();

    private double dragStartX, dragStartY;
    private List<Card> draggedCards = FXCollections.observableArrayList();

    private static double STOCK_GAP = 0.2;
    private static double FOUNDATION_GAP = 0;
    private static double TABLEAU_GAP = 25;


    private EventHandler<MouseEvent> onMouseClickedHandler = e -> {
        Card card = (Card) e.getSource();
        if (card.getContainingPile().getPileType() == Pile.PileType.STOCK) {
            card.moveToPile(discardPile);
            card.flip();
            card.setMouseTransparent(false);
            System.out.println("Placed " + card + " to the waste.");
        }
    };

    private EventHandler<MouseEvent> stockReverseCardsHandler = e -> {
        refillStockFromDiscard();
    };

    private EventHandler<MouseEvent> onMousePressedHandler = e -> {
        dragStartX = e.getSceneX();
        dragStartY = e.getSceneY();
    };

    private EventHandler<MouseEvent> onMouseDraggedHandler = e -> {
        Card card = (Card) e.getSource();
        Pile activePile = card.getContainingPile();
        if (activePile.getPileType() == Pile.PileType.STOCK)
            return;
        double offsetX = e.getSceneX() - dragStartX;
        double offsetY = e.getSceneY() - dragStartY;

        draggedCards.clear();
        draggedCards.add(card);


        card.getDropShadow().setRadius(20);
        card.getDropShadow().setOffsetX(10);
        card.getDropShadow().setOffsetY(10);

        card.toFront();
        card.setTranslateX(offsetX);
        card.setTranslateY(offsetY);
    };

    private EventHandler<MouseEvent> onMouseReleasedHandler = e -> {
        if (draggedCards.isEmpty())
            return;
        Card card = (Card) e.getSource();
        Pile pile = getValidIntersectingPile(card, tableauPiles);
        //TODO
        if (pile != null) {
            handleValidMove(card, pile);
            draggedCards.clear();
        } else {
            draggedCards.forEach(MouseUtil::slideBack);
            draggedCards = null;
            draggedCards.clear();
        }
    };

    public boolean isGameWon() {
        //TODO
        return false;
    }

    public Game() {
        deck = Card.createNewDeck();
        System.out.println(deck);
        initPiles();
        dealCards();
    }

    public void addMouseEventHandlers(Card card) {
        card.setOnMousePressed(onMousePressedHandler);
        card.setOnMouseDragged(onMouseDraggedHandler);
        card.setOnMouseReleased(onMouseReleasedHandler);
        card.setOnMouseClicked(onMouseClickedHandler);
    }

    public void refillStockFromDiscard() {
        remainCards = discardPile.getCards();
        Collections.reverse(remainCards);
        Iterator<Card> deckIterators = remainCards.iterator();
        deckIterators.forEachRemaining(card -> {
            card.flip();
            stockPile.addCard(card);
            addMouseEventHandlers(card);});
        discardPile.clear();
        System.out.println("Stock refilled from discard pile.");
    }

    public boolean isMoveValid(Card card, Pile destPile) {
        //TODO
        Card top = destPile.getTopCard();
        if(top == null){
            System.out.println("Its valid move");
            return true;
        }else {
            boolean valid1;
            valid1 = Card.isOppositeColor(card,top);
            int rankTop = top.getRank();
            System.out.println("Its valid move?" + valid1 + "card top rank:" + rankTop + "card rank:" + card.getRank());
            int differenceRank = top.getRank() - card.getRank();
            if(differenceRank == 1 && valid1){
                return true;
            } else {
                return false;
            }

        }
    }
    private Pile getValidIntersectingPile(Card card, List<Pile> piles) {
        Pile result = null;
        for (Pile pile : piles) {
            if (!pile.equals(card.getContainingPile()) &&
                    isOverPile(card, pile) &&
                    isMoveValid(card, pile))
                result = pile;
        }
        return result;
    }

    private boolean isOverPile(Card card, Pile pile) {
        if (pile.isEmpty())
            return card.getBoundsInParent().intersects(pile.getBoundsInParent());
        else
            return card.getBoundsInParent().intersects(pile.getTopCard().getBoundsInParent());
    }

    private void handleValidMove(Card card, Pile destPile) {
        String msg = null;
        if (destPile.isEmpty()) {
            if (destPile.getPileType().equals(Pile.PileType.FOUNDATION))
                msg = String.format("Placed %s to the foundation.", card);
            if (destPile.getPileType().equals(Pile.PileType.TABLEAU))
                msg = String.format("Placed %s to a new pile.", card);
        } else {
            msg = String.format("Placed %s to %s.", card, destPile.getTopCard());
        }
        System.out.println(msg);
        MouseUtil.slideToDest(draggedCards, destPile);
        draggedCards.clear();
    }


    private void initPiles() {
        stockPile = new Pile(Pile.PileType.STOCK, "Stock", STOCK_GAP);
        stockPile.setBlurredBackground();
        stockPile.setLayoutX(75);
        stockPile.setLayoutY(20);
        stockPile.setOnMouseClicked(stockReverseCardsHandler);
        getChildren().add(stockPile);

        discardPile = new Pile(Pile.PileType.DISCARD, "Discard", STOCK_GAP);
        discardPile.setBlurredBackground();
        discardPile.setLayoutX(265);
        discardPile.setLayoutY(20);
        getChildren().add(discardPile);

        for (int i = 0; i < 4; i++) {
            Pile foundationPile = new Pile(Pile.PileType.FOUNDATION, "Foundation " + i, FOUNDATION_GAP);
            //System.out.println(foundationPile);
            foundationPile.setBlurredBackground();
            foundationPile.setLayoutX(585 + i * 180);
            foundationPile.setLayoutY(25);
            foundationPiles.add(foundationPile);
            getChildren().add(foundationPile);
        }
        for (int i = 0; i < 7; i++) {
            Pile tableauPile = new Pile(Pile.PileType.TABLEAU, "Tableau " + i, TABLEAU_GAP);
            tableauPile.setBlurredBackground();
            tableauPile.setLayoutX(50 + i * 180);
            tableauPile.setLayoutY(275);
            tableauPiles.add(tableauPile);
            getChildren().add(tableauPile);
        }
    }

    public void dealCards() {
        //TODO

        for (int i = 0; i < 52; i++) {
            if (i == 0) {
                tableauPiles.get(0).addCard(deck.get(i));
                deck.get(i).flip();
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else if (i > 0 && i <= 2) {
                tableauPiles.get(1).addCard(deck.get(i));
                if (i == 2) {
                    tableauPiles.get(1).addCard(deck.get(i));
                    deck.get(i).flip();
                }
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else if (i > 2 && (i <= 5)) {
                tableauPiles.get(2).addCard(deck.get(i));
                if (i == 5) {
                    tableauPiles.get(2).addCard(deck.get(i));
                    deck.get(i).flip();
                }
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else if (i > 5 && i <= 9) {
                tableauPiles.get(3).addCard(deck.get(i));
                if (i == 9) {
                    tableauPiles.get(3).addCard(deck.get(i));
                    deck.get(i).flip();
                }
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else if (i > 9 && i <= 14) {
                tableauPiles.get(4).addCard(deck.get(i));
                if (i == 14) {
                    tableauPiles.get(4).addCard(deck.get(i));
                    deck.get(i).flip();
                }
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else if (i > 14 && i <= 20) {
                tableauPiles.get(5).addCard(deck.get(i));
                if (i == 20) {
                    tableauPiles.get(5).addCard(deck.get(i));
                    deck.get(i).flip();
                }
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else if (i > 20 && i <= 27) {
                tableauPiles.get(6).addCard(deck.get(i));
                if (i == 27) {
                    tableauPiles.get(6).addCard(deck.get(i));
                    deck.get(i).flip();
                }
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
            else {
                stockPile.addCard(deck.get(i));
                addMouseEventHandlers(deck.get(i));
                getChildren().add(deck.get(i));
            }
        }
    }

    public void setTableBackground(Image tableBackground) {
        setBackground(new Background(new BackgroundImage(tableBackground,
                BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT,
                BackgroundPosition.CENTER, BackgroundSize.DEFAULT)));
    }

}
