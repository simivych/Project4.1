package experiments;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ai.AlphaBetaNoHeuristics;
import ai.OurAI;
import game.Game;
import manager.ai.AIRegistry;
import ai.PNSMCTS;
import metadata.ai.heuristics.Heuristics;
import metadata.ai.heuristics.terms.HeuristicTerm;
import other.AI;
import other.GameLoader;
import other.context.Context;
import other.model.Model;
import other.trial.Trial;
import ai.RandomAI;
import search.mcts.MCTS;
import search.pns.ProofNumberSearch;
import utils.AIFactory;


/**
 * Adapted form of RunCustomMatch from the example AI code for Ludii, written by Dennis Soemers.
 * This version of RunCustomMatch actually ensures that agents take turn for who gets to go first and that the agent
 * is re-initialized every game.
 *
 * To run experiments:
 * - Make sure you have a file called "Experiment Games - Untested.csv" that contains the NAMES of the games you wish to
 * play, each on a separate line without separators (see file in Git for example).
 * - Choose which agents to use near the top of main
 */
public class RunCustomMatch
{

	//-------------------------------------------------------------------------

	/** Experiment + file settings */
	static final int NUM_GAMES = 5;
	static final String fileName = "Experiment Data.csv";
	static final String testedfileName = "Experiment Games - Tested.csv";
	static final String untestedfileName = "Experiment Games - Untested.csv";
	static final boolean includeHeader = true; // This boolean determines if you want to add the names of the columns, set to false if run before

	/** Command line print settings */
	static boolean showProgressBar = true;
	static String completed = "x";
	static String incomplete = "-";

	/** Heuristic settings */
	static int heuristic1 = 9;
	static int heuristic2 = 26;

	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 */
	private RunCustomMatch()
	{
		// do not instantiate
	}

	//-------------------------------------------------------------------------

	public static void main(final String[] args) {

		/**
		 * The block below is where you select which AI to test against which other AI
		 * Options:
		 * UCT
		 * Alpha-Beta Search
		 * PN-MCTS
		 * MAST
		 * GRAVE
		 * Random
		 *
		 * To add more agents, add their string name here and add your agent to the getAgentFromName() method.
		 * For more info on how, check the method.
		 */

		ArrayList<String> agents = new ArrayList<>();
//		agents.add("UCT");
//		agents.add("Alpha-Beta Search");
		agents.add("equal");
		agents.add("firsthigher");
		agents.add("secondhigher");
		agents.add("nosecond");
		agents.add("basicheuristics");
//		agents.add("PN-MCTS");
//		agents.add("MAST");
//		agents.add("GRAVE");
//		agents.add("Random");

		String opponent = "basicheuristics";
//		String opponent = "Random";

		// This is how you can add extra games to be played without going through the untested file.
		ArrayList<String> games = new ArrayList<>();
//		games.add("Tic-Tac-Toe");
//		games.add("Amazons");
//		games.add("Awari");
//		games.add("Lines of Action");
//		games.add("Breakthrough");
//		games.add("2048");

		try{
			BufferedReader br = new BufferedReader(new FileReader(untestedfileName));
			br.readLine();
			String line = "";
			while((line = br.readLine()) != null){
				games.add(line);
			}
		} catch (Exception e){
			System.out.println("Error opening untested file");
		}

		for (String game : games) {
			try {
				System.out.println("Testing game " + game);
				GameLoader.loadGameFromName(game + ".lud");
				System.out.println("Load successful");
			} catch (NullPointerException e) {
				System.out.println("Couldn't load game <-----------------------------");
			}
		}

		System.out.println("-------------------- Agent Testing Experiments --------------------");
		System.out.println();
		System.out.println("Agents being tested: ");
		for(String agent : agents){
			System.out.println("- "+agent);
		}
		System.out.println();
		System.out.println("Agent playing against: "+opponent);
		System.out.println("Number of games per experiment: "+NUM_GAMES);
		System.out.println();

		if (includeHeader){
			String firstline = "GameName,";
			for (String agent : agents) {
				firstline += agent + ",";
			}

			try {
				FileWriter fw = new FileWriter(fileName, true);
				fw.write(firstline + "\n");
				fw.close();
			}catch (Exception e){}
		}

		/**
		 * Main loop that goes through each game in the untested experiment games file.
		 */
		int experiment = 0;
		for (String gameName : games){
//			System.out.println(gameName);

			Game game = GameLoader.loadGameFromName(gameName + ".lud");
			double start = System.currentTimeMillis();
			System.out.println("-------------------- Experiment "+ (experiment+1) +": "+gameName+" -----------------------");

			ArrayList<String> csvData = new ArrayList<>();
			csvData.add(game.name());

			/**
			 * Loop that goes through each agent and runs NUM_GAMES number of games against the opponent agent. Winrate
			 * is saved to the Experiment Data.csv after each GAME (not after each agent).
			 */

			for (String agent : agents){

				// Check if agent can even play game, if not -> winrate -1
				if (getAgentFromName(agent, game) == null){
					csvData.add("-1.0");
					continue;
				}

				// Exception Handling for OOM errors
				try {
					int agentWins = 0;

					for (int gameCounter = 0; gameCounter < NUM_GAMES; ++gameCounter) {

						if (showProgressBar) {
							System.out.print("\r" + agent + ": [" + completed.repeat(gameCounter) + incomplete.repeat(NUM_GAMES - gameCounter) + "]");
						}

						List<AI> ais = new ArrayList<AI>();
						ais.add(null);
						for (int pid = 1; pid <= game.players().count(); ++pid) {
							if (pid == 1) {
								AI ai = getAgentFromName(agent, game);
								ais.add(ai);
							} else if (pid == 2) {
								ais.add(getAgentFromName(opponent, game));
							} else {
								ais.add(new utils.RandomAI());
							}
						}

						// play a game with kilothon rules
						game.setMaxMoveLimit(game.players().count() * 500);
						Trial trial = new Trial(game);
						Context context = new Context(game, trial);
						game.start(context);
						for (int p = 1; p < ais.size(); ++p) {
							ais.get(p).initAI(game, p);
						}

						Model model = context.model();
						double remainingTimeP1 = 60000.0D;
						double remainingTimeP2 = 60000.0D;

						double timeUsed;
						while (!trial.over()) {
							int mover = context.state().mover();
							double thinkingTime = mover == 1 ? ((AI) ais.get(1)).maxSecondsPerMove() : (mover == 2 ? ((AI) ais.get(2)).maxSecondsPerMove() : 1.0D);
							double time = (double) System.currentTimeMillis();
							model.startNewStep(context, ais, thinkingTime);
							timeUsed = (double) System.currentTimeMillis() - time;
							if (remainingTimeP1 > 0.0D && mover == 1) {
								remainingTimeP1 -= timeUsed;
								if (remainingTimeP1 <= 0.0D) {
									System.out.print("r1!"); //+ trial.numberRealMoves());
									((AI) ais.get(1)).closeAI();
									ais.set(1, new utils.RandomAI());
									((AI) ais.get(1)).initAI(game, 1);
								}
							}

							if (remainingTimeP2 > 0.0D && mover == 2) {
								remainingTimeP2 -= timeUsed;
								if (remainingTimeP2 <= 0.0D) {
									System.out.print("r2!"); //+ trial.numberRealMoves());
									((AI) ais.get(2)).closeAI();
									ais.set(2, new utils.RandomAI());
									((AI) ais.get(2)).initAI(game, 2);
								}
							}

						}

						if (context.trial().status().winner() == 1) {
							agentWins++;
						}
					}

					if (showProgressBar) {
						System.out.print("\r" + agent + ": [" + completed.repeat(NUM_GAMES) + "]");
					}

					double winrate = (double) agentWins / NUM_GAMES;
					System.out.println("\n" + agent + " agent winrate: " + winrate + " (" + agentWins + ")");
					System.out.println();

					csvData.add(String.valueOf(winrate));
				} catch (Exception e){
					System.out.println(e.getMessage());
				}
			}

			/**
			 * Code that writes results to CSV and adds Games to the Tested file for convenience
			 */
			String csvLine = "";
			for (String item : csvData){
				csvLine += item + ",";
			}

			try
			{
				FileWriter fw = new FileWriter(fileName, true);
				fw.write(csvLine + "\n");
				fw.close();
			}catch (Exception e){}

			double time = ( (System.currentTimeMillis() - start) / 1000.0 );
			System.out.println("\nTime taken: " + time);

			try
			{
				FileWriter fw = new FileWriter(testedfileName, true);
				fw.write(gameName + "," + time + "\n");
				fw.close();
			}catch (Exception e){}

			experiment++;

		}
		System.out.println("------------------------------------------------------------------");

	}

	/**
	 * This code is to safely get agents from a custom reference name. Any new agents can be added as cases in the
	 * switch. If possible, follow the same structure as the other cases. This means checking if the agent
	 * .supportsGame(game). If not, print a message saying this and then return null.
	 *
	 * @param name Name of the agent you wish to create
	 * @param game Name of the game the agent will play
	 * @return AI object of the agent you requested. If agent is not compatible with game, returns null.
	 */

	private static AI getAgentFromName(String name, Game game){
		switch (name){
			case "Alpha-Beta Search":
				if (!search.minimax.AlphaBetaSearch.createAlphaBeta().supportsGame(game)){
					System.out.println("AB-Search cannot play "+game.name());
					return null;
				}
				return search.minimax.AlphaBetaSearch.createAlphaBeta();
			case "UCT":
				if (!MCTS.createUCT().supportsGame(game)){
					System.out.println("UCT cannot play "+game.name());
					return null;
				}
				AI uct = MCTS.createUCT();
				uct.setMaxSecondsPerMove(0.5D);
				return uct;
			case "Random":
				return new utils.RandomAI();
			case "PN-MCTS":
				if (!new PNSMCTS().supportsGame(game)) {
					System.out.println("PN-MCTS cannot play "+game.name());
					return null;
				}
				return new PNSMCTS();
			case "MAST":
				if (!AIFactory.createAI("MAST").supportsGame(game)){
					System.out.println("MAST cannot play "+game.name());
					return null;
				}
				return AIFactory.createAI("MAST");
			case "GRAVE":
				if (!AIFactory.createAI("MC-GRAVE").supportsGame(game)){
					System.out.println("GRAVE cannot play "+game.name());
					return null;
				}
				return AIFactory.createAI("MC-GRAVE");
			case "equal":
				if (!search.minimax.AlphaBetaSearch.createAlphaBeta().supportsGame(game)){
					System.out.println("AB-Search cannot play "+game.name());
					return null;
				}
				HeuristicTerm heur1 = OurAI.getHeuristicFromId(heuristic1);
				HeuristicTerm heur2 = OurAI.getHeuristicFromId(heuristic2);
				return new AlphaBetaNoHeuristics(new Heuristics(new HeuristicTerm[]{heur1, heur2}));
			case "firsthigher":
				if (!search.minimax.AlphaBetaSearch.createAlphaBeta().supportsGame(game)){
					System.out.println("AB-Search cannot play "+game.name());
					return null;
				}
				HeuristicTerm hr1 = OurAI.getHeuristicFromId(heuristic1);
				hr1.setWeight(hr1.weight()*100);
				HeuristicTerm hr2 = OurAI.getHeuristicFromId(heuristic2);
				return new AlphaBetaNoHeuristics(new Heuristics(new HeuristicTerm[]{hr1, hr2}));
			case "secondhigher":
				if (!search.minimax.AlphaBetaSearch.createAlphaBeta().supportsGame(game)){
					System.out.println("AB-Search cannot play "+game.name());
					return null;
				}
				HeuristicTerm hrs1 = OurAI.getHeuristicFromId(heuristic1);
				HeuristicTerm hrs2 = OurAI.getHeuristicFromId(heuristic2);
				hrs1.setWeight(hrs1.weight()*0.01f);
				return new AlphaBetaNoHeuristics(new Heuristics(new HeuristicTerm[]{hrs1, hrs2}));
			case "nosecond":
				if (!search.minimax.AlphaBetaSearch.createAlphaBeta().supportsGame(game)){
					System.out.println("AB-Search cannot play "+game.name());
					return null;
				}
				HeuristicTerm h1 = OurAI.getHeuristicFromId(heuristic1);
				return new AlphaBetaNoHeuristics(new Heuristics(new HeuristicTerm[]{h1}));
			case "basicheuristics":
				if (!search.minimax.AlphaBetaSearch.createAlphaBeta().supportsGame(game)){
					System.out.println("AB-Search cannot play "+game.name());
					return null;
				}
				return new AlphaBetaNoHeuristics();

			default:
				System.out.println("AI not in list");
				return null;
		}
	}

}
