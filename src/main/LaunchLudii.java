package main;

import ai.AlphaBetaNoHeuristics;
import ai.OurAI;
import game.Game;
import kilothon.Kilothon;
import manager.ai.AIRegistry;
import metadata.ai.agents.Agent;
import metadata.ai.heuristics.Heuristics;
import other.GameLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * The main method of this launches the Ludii application with its GUI, and registers
 * the example AIs from this project such that they are available inside the GUI.
 *
 * @author Dennis Soemers
 */
public class LaunchLudii
{

	/**
	 * The main method
	 * @param args
	 */
	public static void main(final String[] args)
	{
		// Register alpha beta no heuristics
		AIRegistry.registerAI("OurAI", () -> {return new OurAI();}, (game) -> {return true;});
		Kilothon.main(new String[]{"OurAI", "OurAI"});
	}

	public static void getHeuristic(){
		Game game = GameLoader.loadGameFromName( "Sudoku"+ ".lud");
		metadata.ai.Ai aiMetadata = game.metadata().ai();
		Heuristics md = game.metadata().ai().trainedHeuristics();
		System.out.println(12);
	}
	public static void countHeuristics(){
		OurAI ai = new OurAI();
		Map<String, Integer> clusters = ai.getClusters();
		Map<Integer, Map<String, Integer>> saved = new HashMap<>();
		int noheuristics =0;
		int heuristics = 0;
		int alphabetaheuristics = 0;
		for (var entry : clusters.entrySet()) {
			Game game = GameLoader.loadGameFromName(entry.getKey() + ".lud");
			Heuristics h = game.metadata().ai().heuristics();
			if (h == null) {
				noheuristics++;
			} else {
				if(game.metadata().ai().agent() != null) {
					System.out.println(game.metadata().ai().agent().toString());
					if (game.metadata().ai().agent().toString().contains("Alpha")) {
						alphabetaheuristics++;
					} else {
						heuristics++;
					}
				}
			}
		}
		System.out.println(noheuristics);
	}

	public static Map<Integer, Map<String, Integer>>  countClusterAIs(){
		OurAI ai = new OurAI();
		Map<String, Integer> clusters = ai.getClusters();
		Map<Integer, Map<String, Integer>> saved = new HashMap<>();

		for (var entry : clusters.entrySet()) {
			Game game = GameLoader.loadGameFromName( entry.getKey()+ ".lud");
			Agent agent = game.metadata().ai().agent();
			if(agent!=null && agent.toString() != null){
				if(saved.containsKey(entry.getValue())){
					if(saved.get(entry.getValue()).containsKey(agent.toString())){
						int newvalue = saved.get(entry.getValue()).get(agent.toString()) + 1;
						saved.get(entry.getValue()).put(agent.toString(), newvalue);
					} else {
						saved.get(entry.getValue()).put(agent.toString(), 1);
					}
				} else {
					Map<String, Integer> map = new HashMap<>();
					map.put(agent.toString(), 1);
					saved.put(entry.getValue(), map);
				}
			}
		}
		for (var entry : saved.keySet()) {
			System.out.println("Cluster: " + entry);
			Map<String, Integer> map = saved.get(entry);
			for(var key : map.keySet()){
				System.out.println(key + " count: " +  map.get(key));
			}
		}
		return saved;
	}

}
