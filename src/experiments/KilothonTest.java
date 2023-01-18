package experiments;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import game.Game;
import main.Constants;
import main.FileHandling;
import other.AI;
import other.GameLoader;
import other.RankUtils;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import search.mcts.MCTS;
import utils.AIFactory;

import ai.PNSMCTS;

/**
 * To start a kilothon without a GUI (beat a weak ai on all games and send report to a mail).
 * Note: All games except, match, hidden information, simultaneous games or simulation games.
 *
 * @author Eric.Piette
 */
public class KilothonTest
{
    /**
     * Method to run a kilothon
     * @param args 1-Login 2-AgentName
     */
    @SuppressWarnings("resource")
    public static void main(final String[] args)
    {
        final String login = args.length == 0 ? "No Name" : args[0];
        final String agentName = args.length < 2 ? "UCT" : args[1];
        final double startTime = System.currentTimeMillis();
        final double timeToThink = 60000; // Time for the challenger to think smartly (in ms).
        final int movesLimitPerPlayer = 500; // Max number of moves per player.
        final int numGamesToPlay = Constants.INFINITY; // Infinity is all games.
        double sumUtilities = 0;
        int numWins = 0;
        int sumNumMoves = 0;
        int sumP1Moves = 0;

        System.out.println("Kilothon is starting. Login = " + login + " Agent = " + agentName + "\n");

        // Get the list of games.
        final String[] gameList = FileHandling.listGames();
        ArrayList<String> validChoices = new ArrayList<>();

        for (final String s : gameList)
        {
            if (s.contains("/lud/plex"))
                continue;

            if (s.contains("/lud/wip"))
                continue;

            if (s.contains("/lud/wishlist"))
                continue;

            if (s.contains("/lud/reconstruction"))
                continue;

            if (s.contains("/lud/WishlistDLP"))
                continue;

            if (s.contains("/lud/test"))
                continue;

            if (s.contains("/res/lud/bad"))
                continue;

            if (s.contains("/res/lud/bad_playout"))
                continue;

            validChoices.add(s);
        }

        System.out.print(validChoices.size());

        // Random order for the games.
        Collections.shuffle(validChoices);

        // Creates a FileWriter
        FileWriter file;
        BufferedWriter buf = null;
        try {
            buf = new BufferedWriter(new FileWriter("resultUCT.txt", true));
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        int idGame = 0; // index of the game.
        try
        {

            String header = "";
            header+="GAME NAME, "; // game name
            header+="NUM PLAYERS, "; // num players
            header+="WIN?, "; // 1 if winning
            header+="RANK P1, "; // ranking of P1
            header+="UTILITY P1, "; // reward of P1
            header+="NUM MOVES, "; // game length
            header+="NUM P1 MOVES"; // number of P1 moves
            buf.write(header);
            buf.newLine();
            buf.close();

            ArrayList<String> done = finished(validChoices);


            for (final String gameName : validChoices)
            {
                final Game game = GameLoader.loadGameFromName(gameName);
                if(!done.contains(game.name())&& MCTS.createUCT().supportsGame(game)){


                    final int numPlayers = game.players().count();

                    // look only the games we want in the Kilothon.
                    if(!game.hasSubgames() && !game.hiddenInformation() && !game.isSimultaneousMoveGame() && !game.isSimulationMoveGame())
                    {
                        idGame++;
                        System.out.println("game " + idGame + ": " + game.name() + " is running");

                        // Set the AIs.
                        final List<AI> ais = new ArrayList<AI>();
                        ais.add(null);
                        for(int pid = 1; pid <= numPlayers; pid++)
                        {
                            if(pid == 1)
                            {
                                final AI challenger = MCTS.createUCT();
                                challenger.setMaxSecondsPerMove(0.5);
                                ais.add(challenger);
                            }
                            else if(pid == 2)
                            {
                                AI UCT = AIFactory.createAI("UCT");
                                UCT.setMaxSecondsPerMove(0.5);
                                ais.add(UCT);
                            }
                            else
                                ais.add(new utils.RandomAI());
                        }

                        // Start the game.
                        game.setMaxMoveLimit(numPlayers*movesLimitPerPlayer); // Limit of moves per player.
                        final Context context = new Context(game, new Trial(game));
                        final Trial trial = context.trial();
                        game.start(context);

                        // Init the ais.
                        for (int p = 1; p <= game.players().count(); ++p)
                            ais.get(p).initAI(game, p);
                        final Model model = context.model();

                        // Run the game.
                        int numP1Moves = 0;
                        double remainingTimeP1 = timeToThink; // One minute
                        double remainingTimeP2 = timeToThink; // One minute
                        while (!trial.over())
                        {
                            final int mover = context.state().mover();
                            final double thinkingTime = (mover == 1) ? ais.get(1).maxSecondsPerMove() : (mover == 2) ? ais.get(2).maxSecondsPerMove() : 1.0;
                            final double time = System.currentTimeMillis();
                            model.startNewStep(context, ais, thinkingTime);
                            final double timeUsed = System.currentTimeMillis() - time;

                            // We check the remaining time to be able to think smartly for the challenger.
                            if(remainingTimeP1 > 0)
                            {
                                if(mover == 1)
                                {
                                    remainingTimeP1 = remainingTimeP1 - timeUsed;
                                    if(remainingTimeP1 <= 0)
                                    {
                                        System.out.println("switch P1 to Random at move number " + trial.numberRealMoves());
                                        ais.get(1).closeAI();
                                        ais.set(1, new utils.RandomAI());
                                        ais.get(1).initAI(game, 1);
                                    }
                                }
                            }

                            // We check the remaining time to be able to think smartly for the "smart" opponent.
                            if(remainingTimeP2 > 0)
                            {
                                if(mover == 2)
                                {
                                    remainingTimeP2 = remainingTimeP2 - timeUsed;
                                    if(remainingTimeP2 <= 0)
                                    {
                                        System.out.println("switch P2 to Random at move number " + trial.numberRealMoves());
                                        ais.get(2).closeAI();
                                        ais.set(2, new utils.RandomAI());
                                        ais.get(2).initAI(game, 2);
                                    }
                                }
                            }

                            if(context.state().mover() == 1)
                                numP1Moves++;
                        }

                        // Get results.
                        final double numMoves = trial.numberRealMoves();
                        final double rankingP1 = trial.ranking()[1];
                        final boolean win = rankingP1 == 1;
                        final double rewardP1 = RankUtils.rankToUtil(rankingP1, numPlayers);
                        System.out.println("Reward of P1 = " + rewardP1 + " (ranking = " + rankingP1 + ") finished in " + trial.numberRealMoves() + " moves.");
                        System.out.println("\n**************NEXT GAME***********\n");
                        sumUtilities += rewardP1;
                        sumNumMoves += numMoves;
                        sumP1Moves += numP1Moves;
                        if(win)
                            numWins++;
                        String lineToWrite = "";
                        lineToWrite+= game.name() + ", "; // game name
                        lineToWrite+=game.players().count() + ", "; // num players
                        lineToWrite+=win + ", "; // 1 if winning
                        lineToWrite+=rankingP1 + ", "; // ranking of P1
                        lineToWrite+=rewardP1 + ", "; // reward of P1
                        lineToWrite+=numMoves + ", "; // game length
                        lineToWrite+=numP1Moves + ""; // num P1 moves
                        buf = new BufferedWriter(new FileWriter("resultsUCT.txt", true));
                        buf.write(lineToWrite);
                        buf.newLine();

                        buf.close();
                    }

                    if((idGame + 1) > numGamesToPlay) // To stop the kilothon after a specific number of games (for test).
                        break;
                }else if (done.contains(game.name())){
                    System.out.println(game.name() + " already played");
                }else{
                    System.out.println(game.name() + " not supported");
                }
            }

            final double kilothonTime = System.currentTimeMillis() - startTime;
            int seconds = (int) (kilothonTime / 1000) % 60 ;
            int minutes = (int) ((kilothonTime / (1000*60)) % 60);
            int hours   = (int) ((kilothonTime / (1000*60*60)) % 24);
            System.out.println("\nKilothon done in " + hours + " hours " + minutes + " minutes " + seconds + " seconds.");
            System.out.println("........Computation of the results....");


            String bodyMsg = "Kilothon run by " + login;
            bodyMsg += "\nAgent name = " + agentName;
            bodyMsg += "\nSmart thinking time (in ms) = " + timeToThink;
            bodyMsg += "\nMoves limit per player = " + movesLimitPerPlayer;
            bodyMsg += "\nGames played = " + idGame;
            bodyMsg += "\nAVG utility = " + (sumUtilities/idGame);
            bodyMsg += "\nNum Wins = " + numWins;
            bodyMsg += "\nAVG Wins = " + ((double)numWins/(double)idGame) *100.0 + " %";
            bodyMsg += "\nNum Moves = " + sumNumMoves;
            bodyMsg += "\nAVG Moves = " + (sumNumMoves/idGame);
            bodyMsg += "\nNum P1 Moves = " + sumP1Moves;
            bodyMsg += "\nAVG P1 Moves = " + (sumP1Moves/idGame);
            bodyMsg += "\nDone in " + hours + " hours " + minutes + " minutes " + seconds + " seconds.";


            buf = new BufferedWriter(new FileWriter("resultsUCT.txt", true));
            // Writes the string to the file
            buf.write(bodyMsg);

            // Closes the writer
            buf.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

    }

    private static ArrayList<String> finished(ArrayList<String> validChoices) {
        ArrayList<String> done = new ArrayList<>();
        try{
            // Creates a FileReader
            FileReader file = new FileReader("resultsUCT.txt");
            BufferedReader br = new BufferedReader(file);
            String line;
            while( (line = br.readLine()) != null){
                String[] l = line.split(",");
                String game = l[0];

                done.add(game);
            }
            br.close();
        }
        catch(Exception e){

        }
        System.out.println(done.size());
        return done;
    }
}