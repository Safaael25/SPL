package bguspl.set.ex;

import bguspl.set.Env;

import java.util.concurrent.LinkedBlockingDeque;


/**
 * Enum represents the state of a player during the game.
 */
enum PlayerState {
    PointFreeze,
    PenaltyFreeze,
    WaitingForCheck,
    Playing
}

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
/**
 * The Player class represents a player in the game.
 * Each player has a unique ID, a score, and can be either a human or an AI player.
 * The Player class implements the Runnable interface to allow each player to run in a separate thread.
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * Represents the dealer responsible for managing the game.
     */
    private final Dealer dealer;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * The actions performed by the player.
     */
    private LinkedBlockingDeque<Integer> playerActions;

    /**
     * The size of the feature.
     */
    private int featureSize;

    /**
     * Represents the current state of a player in the game.
     */
    public PlayerState status;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        this.status = PlayerState.Playing;
        this.featureSize = env.config.featureSize;
        this.playerActions = new LinkedBlockingDeque<>(featureSize);
    }

    /**
     * The main player thread of each player starts here (main loop for the player
     * thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");

        if (!human) createArtificialIntelligence();
        
        // Notify the dealer that the player has been fully initialized
        synchronized (dealer.getInitializetionLock()) {
            dealer.getInitializetionLock().notifyAll();
        }

        // Check the player's status and perform corresponding action
        while (!terminate) {
            switch (status) {
                case Playing:
                    pressKey();
                    break;
                case WaitingForCheck:
                    sendSetToCheck();
                    break;
                case PenaltyFreeze:
                    penalty();
                    break;
                case PointFreeze:
                    point();
                    break;
            }
        }

        if (!human)
            try {
                aiThread.join();
            } catch (InterruptedException ignored) {}
        terminateAi();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Processes the action of a player selecting a card slot.
     *
     * @param slot the slot number to process the action for
     */
    private void processAction(int slot) {
        if ( terminate || table.slotToCard[slot] == null) {
            return;
        }

        //Attempt to remove the token from the slot
        if (!table.removeToken(id, slot)) {
            // If the removal failed add token and check if the feature is completed
            boolean finished = table.tokenPlacement[id].size() == featureSize;

            if (!finished) {
                table.placeToken(id, slot);

                finished = table.tokenPlacement[id].size() == featureSize;

                if (finished) {
                    status = PlayerState.WaitingForCheck;
                }
            }
        }
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // Simulate pressing a random key
                int press = (int) (Math.random() * env.config.tableSize);
                keyPressed(press);
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        terminate = true;

        try {
            playerThread.interrupt();
            playerThread.join();
        } catch (InterruptedException exception) {}

        playerActions.offer(-1);  // Indicate termination of player actions.
    }

    /**
     * Terminates the AI threads.
     */
    public void terminateAi() {
        terminate = true;
        if (!human && aiThread != null) {
            try {
                aiThread.interrupt();
                aiThread.join();
            } catch (InterruptedException exception) {}
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        try {
            if (status == PlayerState.Playing && !dealer.isTerminationInProgress()) {
                playerActions.put(slot);
            }
        } catch (InterruptedException exception) {}
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        if (!dealer.isTerminationInProgress() && !terminate) {
            int ignored = table.countCards(); // this part is just for demonstration in the unit tests
            env.ui.setScore(id, ++score);
            freezePlayer(env.config.pointFreezeMillis);
            playerActions.clear();
            status = PlayerState.Playing;
        }
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        if (!dealer.isTerminationInProgress() && !terminate) {
            freezePlayer(env.config.penaltyFreezeMillis);
            status = PlayerState.Playing;
        }
    }

    /**
     * Returns the score of the player.
     *
     * @return the current score of the player
     */
    public int score() {
        return score;
    }

    /**
     * Freezes the player for a specified time.
     *
     * @param freezeMillis the duration in milliseconds for which the player should be frozen
     */
    private void freezePlayer(long freezeMillis) {
        final long second = 1000;

        // loop until the freeze time is greater than or equal to second
        while (!dealer.isTerminationInProgress() && !terminate && freezeMillis >= second) {
            env.ui.setFreeze(id, freezeMillis);
            try {
                Thread.sleep(second);
            } catch (InterruptedException exception) {Thread.currentThread().interrupt();}
            freezeMillis -= second;
        }

        // Update the remaining freeze time
        if (!dealer.isTerminationInProgress() && !terminate) {
            env.ui.setFreeze(id, freezeMillis);
            try {
                Thread.sleep(freezeMillis);
            } catch (InterruptedException exception) {Thread.currentThread().interrupt();}
        }
    }

    /**
     * Creates and starts a new thread for the player.
     * The thread's name is set to "playerThread" followed by the player's ID.
     */
    protected void setAPlayerThreads() {
        playerThread = new Thread(this, " player-" + id);
    }


    /**
     * Creates and starts a new thread for the player.
     * The thread's name is set to "playerThread" followed by the player's ID.
     */
    protected void startPlayerThreads() {
        playerThread.start();
    }

    /**
     * Returns an array containing the IDs of cards with the player's tokens.
     *
     * @return an array containing the IDs of cards with the player's tokens.
     */
    public int[] getTokensArray() {
        int setSize = table.tokenPlacement[id].size();
        int[] set = new int[setSize];

        // Add cards to the array based on the player's token slots
        for (int i = 0; i < setSize; i++) {
            int slot = table.tokenPlacement[id].get(i);
            if (table.slotToCard[slot] != null) {
                set[i] = table.slotToCard[slot];
            }
        }
        return (set);
    }

    /**
     * Sends the player's set to be checked for corerect set.
     */
    public void sendSetToCheck() {
        try {
            table.checkSetSemaphore.acquire();
            dealer.checkSet(id);
            table.checkSetSemaphore.release();
        } catch (InterruptedException exception) {table.checkSetSemaphore.release();}
    }

    /**
     *Retrieving the next key press from the playerActions queue the next key press action.
     */
    public void pressKey() {
        if (!dealer.isTerminationInProgress()) {
            try {
                processAction(this.playerActions.take());
            } catch (InterruptedException exception) {}
        }
    }
}