package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Vector;
import java.util.ArrayList;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * The list of card slots waiting removal from the table.
     */
    private Vector<Integer> slotsWaitingRemoval;

    /**
     * True if there are empty slots on the table where cards can be placed.
     */
    private volatile boolean pendingPlaceCards;

    /**
     * Lock used for synchronization during the initialization of the player threads.
     */
    private Object initializetionLock;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;

        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        slotsWaitingRemoval = new Vector<>();
        initializetionLock = new Object();

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        initializePlayersThreads();
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }

        // Perform final game actions
        announceWinners();
        terminate();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        System.out.println("Dealer.terminate");
        terminate = true;

        // Terminate player threads
        for (int i = players.length; i > 0; i--)
            players[i - 1].terminate();

        // Notify the dealer thread to wake up and terminate
        Thread.currentThread().interrupt();

    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        synchronized (table) {
            // Iterate over the slots waiting for removal
            while (!slotsWaitingRemoval.isEmpty()) {
                Integer playerId = slotsWaitingRemoval.remove(0);
                removeCardsForPlayer(playerId);
            }

            // The player notifies the dealer that the cards have been removed
            pendingPlaceCards = true;
            synchronized (this) {
                notifyAll();
            }

        }
    }

    /**
     * Removes a set of cards with a player's tokens from the table.
     *
     * @param playerId the ID of the player whose set are to be removed
     */
    private void removeCardsForPlayer(int playerId) {
        Vector<Integer> tokens = new Vector<>(table.tokenPlacement[playerId]);
        for (int slot : tokens) {
            table.removeCard(slot);
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        synchronized (table) {
            int currentCardsOnTable = table.countCards();

            // If the table is already full, no need to place more cards
            if (currentCardsOnTable == env.config.tableSize)
                return;

            // If there are not enough cards for a set or if no sets are possible terminate game
            if (deck.size() + currentCardsOnTable < env.config.featureSize || env.util.findSets(getListOfCardsInGame(), 1).isEmpty()) {
                terminate = true;
                return;
            }

            // Randomly select cards from the deck and place them on the table until it is full or the deck is empty
            for (int slot = 0; slot < table.slotToCard.length && currentCardsOnTable != env.config.tableSize && !deck.isEmpty(); slot++) {
                if (table.slotToCard[slot] == null) {
                    int randomIndex = (int) (Math.random() * deck.size());
                    table.placeCard(deck.remove(randomIndex), slot);
                    currentCardsOnTable++;
                }
            }
            pendingPlaceCards = false;


        }
    }


    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     * The method sleeps for a default duration of 900 milliseconds to avoid busy waiting,
     * while picking a deafult value that is close to a second to insure the timer is working correctly.
     */
    private void sleepUntilWokenOrTimeout() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        while (reshuffleTime - System.currentTimeMillis() > 0 && !pendingPlaceCards) {

            long updateTime = 900;
            long currentTime = reshuffleTime - System.currentTimeMillis();
            // If the time left is less than the warning time in seconds then update the timer every 10 milliseconds
            if (currentTime - (currentTime%1000) < env.config.turnTimeoutWarningMillis) updateTime = 10;

            updateTimerDisplay(false);

            synchronized (this) {
                try {
                    wait(updateTime);
                } catch (InterruptedException ignored) {}
            }
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if (reset) {
            // Reset the countdown timer and reshuffle time
            env.ui.setCountdown(env.config.turnTimeoutMillis, false);
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
            return;
        }

        // Update countdown timer display
        long timeCountDisplay = Math.max(reshuffleTime - System.currentTimeMillis(), 0); // Ensure time is not negative
        long remainder = timeCountDisplay%1000; 
        boolean warning = (timeCountDisplay- (timeCountDisplay%1000) <= env.config.turnTimeoutWarningMillis); // Check if warning time is reached

        if (!warning || (warning && timeCountDisplay-remainder < env.config.turnTimeoutWarningMillis))
            env.ui.setCountdown(timeCountDisplay, warning); 
        else
            env.ui.setCountdown(timeCountDisplay-remainder, warning); // Start the warning time countdown

    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        synchronized (table) {
            // Add all the cards from the table to the deck
            for (int slot = 0; slot < table.slotToCard.length; slot++) {
                if (table.slotToCard[slot] != null) {
                    deck.add(table.slotToCard[slot]);
                    table.removeCard(slot);
                }
            }

            // Clear waiting for removal and reset the timer
            slotsWaitingRemoval.clear();
        }

    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int[] winners = getWinners();
        env.ui.announceWinner(winners);
    }

    /**
     * Returns an array of IDs representing the winners of the game.
     *
     * @return an array of winner IDs
     */
    private int[] getWinners() {
        int maxScore = 0;
        int numOfWinners = 0;

        // Find maximum score and count the number of winners
        for (Player player : players) {
            int playerScore = player.score();
            if (playerScore > maxScore) {
                maxScore = playerScore;
                numOfWinners = 1;
            } 
            
            else if (playerScore == maxScore)
                numOfWinners++;
            
        }

        int[] winners = new int[numOfWinners];

        // Add the IDs of the winners to the winners array
        int index = 0;
        for (Player player : players) {
            if (player.score() == maxScore) {
                winners[index++] = player.id;
            }
        }
        return winners;
    }

    /**
     * Initializes and starts threads for all players.
     */
    private void initializePlayersThreads() {
        for (int i = 0; i < players.length; i++) {
            players[i].setAPlayerThreads();
            players[i].startPlayerThreads();
            
            // Wait for notification from the player thread that it has finished initializing
            synchronized(initializetionLock){
                try{initializetionLock.wait();
                }catch(InterruptedException exception) {}
            }

        }
    }

    /**
     * Returns a list of cards that are on the table and in the deck.
     * 
     * @return List of cards that are on the table and in the deck
     */
    private List<Integer> getListOfCardsInGame() {
        List<Integer> cards = new ArrayList<>(deck);

        // Add cards from the table
        for (Integer card : table.slotToCard) {
            if (card != null) {
                cards.add(card);
            }
        }
        return cards;
    }

    /**
     * Checks if termination is in progress.
     *
     * @return true if termination is in progress, false otherwise
     */
    public boolean isTerminationInProgress() {
        return terminate;
    }

    /**
     * Checks if a player's set is valid.
     *
     * @param playerId the ID of the player
     * @return true if the set is valid, false otherwise
     */
    public boolean checkSet(Integer playerId) {
        // handle the case where the player's set is invalid
        if (table.tokenPlacement[playerId] == null || table.tokenPlacement[playerId].size() != env.config.featureSize) {
            players[playerId].status = PlayerState.Playing;
            return false; // Invalid set
        }

        int[] cards = players[playerId].getTokensArray();
        boolean isCorrect = env.util.testSet(cards);

        // handle the case where the player has a correct set
        if (isCorrect) {
            players[playerId].status = PlayerState.PointFreeze;
            slotsWaitingRemoval.add(playerId);
            removeCardsFromTable();
        }

        // handle the case where the player has an incorrect set
        else {
            players[playerId].status = PlayerState.PenaltyFreeze;
        }

        return isCorrect;
    }
   
    /**
     * Returns the initializationLock object.
     *
     * @return the initializationLock object
     */
    public Object getInitializetionLock() {
            return initializetionLock;
        }

}