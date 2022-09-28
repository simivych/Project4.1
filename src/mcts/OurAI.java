package mcts;

import game.Game;
import main.collections.FastArrayList;
import other.AI;
import other.context.Context;
import other.move.Move;
import utils.AIUtils;

public class OurAI extends AI{

    String friendlyName;
    int player;

    public  OurAI() {
        this.friendlyName = "Our AI" ;
    }

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        FastArrayList<Move> legalMoves = game.moves(context).moves();
        if(!game.isAlternatingMoveGame()){
            legalMoves = AIUtils.extractMovesForMover(legalMoves,player);
        }

        // time of stopping search
        final long stopTime = System.currentTimeMillis() + (long) (maxSeconds * 1000L);

        int numIterations = 0;

        // Our main loop through MCTS iterations
        while(numIterations < maxIterations && System.currentTimeMillis() < stopTime && !wantsInterrupt){
            // do search
        }

        // To get state from move have to create a trial

        return legalMoves.get(0);
    }

    @Override
    public void initAI(Game game, int playerID)
    {
        this.player = playerID ;
    }
}
