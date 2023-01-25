package ai;

import game.Game;
import metadata.ai.heuristics.Heuristics;
import metadata.ai.heuristics.terms.*;
import other.AI;
import other.context.Context;
import other.move.Move;
import search.mcts.MCTS;
import utils.AIFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

public class OurAI extends AI{

    String friendlyName;
    int player;
    AI selectedAI;

    Map<String, Integer> clusters;
    Map<String, Integer> predictions;
    Map<String, Double> lengths = new HashMap<>();

    double grad = 0.0;
    double time = 0.5;
    int timeTech = 0; //0 is uniform, 1 is increasing, 2 is decreasing
    int turn =1;
    boolean clusteringbool; // if true use clustering else ML predictions

    public  OurAI() {
        this.friendlyName = "Our AI";
        this.clusters = getClusters();
        this.clusteringbool = false;
        this.predictions = getPredictions();
//        readGameLengths();
       if (timeTech == 0) {
            System.out.println("Uniform Time");
        } else if (timeTech == 1) {
            System.out.println("Increasing Time");
        } else if (timeTech == 2) {
            System.out.println("Decreasing Time");
        } else{
            System.out.println("No Time");
        }
    }

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        Move move = selectedAI.selectAction(game, context, time, maxIterations, maxDepth);
        time += grad;
        turn ++;
        return move;
    }

    @Override
    public void initAI(Game game, int playerID)
    {
        this.player = playerID;
        this.selectedAI = selectAI(game);
        this.selectedAI.initAI(game, playerID);
    }

    //decision structure for selecting the AI
    public AI selectAI(Game game){
        if(clusteringbool) {
            int clusterid = getClusterIdFromModel(game);
            System.out.println("cluster selected:" + clusterid);
            AI foundAI;
            switch (clusterid) {
                case 0:
                    foundAI = new PNSMCTS();
                    break;
                case 1:
                    foundAI = new AlphaBetaNoHeuristics();
                    break;
                case 2:
                    foundAI = new AlphaBetaNoHeuristics();
                    break;
                case 3:
                    foundAI = AIFactory.createAI("MAST");
                    break;
                case 4:
                    foundAI = new AlphaBetaNoHeuristics();
                    break;
                default:
                    foundAI = MCTS.createUCT();
            }
            return foundAI;
        } else {
            int agentid = this.predictions.get(game.name());
            System.out.println("agent selected: " );
            switch (agentid) {
                case 0:
                    System.out.println("UCT");
                    return MCTS.createUCT();
                case 1:
                    System.out.println("PNMCTS");
                    return new PNSMCTS();
                case 2:
                    System.out.println("MAST");
                    return AIFactory.createAI("MAST");
                case 3:
                    System.out.println("GRAVE");
                    return AIFactory.createAI("MC-GRAVE");
                case 4:
                    // get automatic
                    System.out.println("Alpha Beta");
                    Heuristics heurs = getHeuristicsPerGame(game);
                    return new AlphaBetaNoHeuristics(heurs);
                default:
                    return MCTS.createUCT();
            }
        }
    }

    // retrieves the clusterid for a certain game
    public int getClusterIdFromModel(Game game){
        var clusterid = this.clusters.get(game.name());
        // this is probably not necessary though
        if(clusterid == null){
            clusterid = 2;
        }
        return clusterid;
    }

    // reads concepts of a game from a csv
    public double[][] readConcepts(String gamename){
        try {
            File file = new File("Python\\data.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            double[][] doublevalues = null;
            while ((line = br.readLine()) != null){
                if(line.contains(gamename)){
                    String[] values = line.split(",");
                    doublevalues = new double[1][values.length-5];
                    for(int i=7;i<values.length-1; i++){
                        if(values[i].length() !=0) {
                            doublevalues[0][i - 7] = Double.parseDouble(values[i]);
                        } else {
                            doublevalues[0][i - 7] = 0;
                        }
                    }
                }
            }
            return doublevalues;

        } catch(Exception e){
            System.out.println(e);
            System.out.println(e.getStackTrace());
            System.out.println("Couldnt load concepts correctly");
        }
        return null;
    }

    public void readGameLengths(){
        try {
            File file = new File("Python\\game_lengths.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            while ((line = br.readLine()) != null){
                String[] values = line.split(",");
                String name = values[2];
                double length = Double.parseDouble(values[3]);
                lengths.put(name, length);
            }

        } catch(Exception e){
            System.out.println(e);
            System.out.println(e.getStackTrace());
            System.out.println("Couldnt load components correctly");
        }
    }

    // read clusters from a file
    public Map<String, Integer> getClusters(){
        HashMap<String, Integer> clusters = new HashMap<>();
        try {
            File file = new File("Python\\data_labelled.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            boolean firsttime = true;
            while ((line = br.readLine()) != null){
                if(!firsttime) {
                    String[] values = line.split(",");
                    clusters.put(values[3], Integer.parseInt(values[1]));
                }
                firsttime=false;

            }
            return clusters;

        } catch(Exception e){
            System.out.println(e);
            System.out.println(e.getStackTrace());
            System.out.println("Couldnt load clusters correctly");
        }
        return null;
    }

    //reads the predicted agent (result from ML model) from a file
    public Map<String, Integer> getPredictions() {
        HashMap<String, Integer> clusters = new HashMap<>();
        try {
            File file = new File("Python\\predictions.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            boolean firsttime = true;
            while ((line = br.readLine()) != null) {
                if (!firsttime) {
                    String[] values = line.split(",");
                    clusters.put(values[1], Integer.parseInt(values[2]));
                }
                firsttime = false;

            }
            return clusters;

        } catch (Exception e) {
            System.out.println(e);
            System.out.println(e.getStackTrace());
            System.out.println("Couldnt load clusters correctly");
        }
        return null;
    }

    // gets the HeuristicTerm object from the id
    public static HeuristicTerm getHeuristicFromId(int heuristicId){
        HeuristicTerm heur;
        switch (heuristicId) {
            case 1:
                heur = new Material(null, 1.0f, null, null);
                break;
            case 2:
                heur = new Influence(null, 1.0f);
                break;
            case 4:
                heur = new InfluenceAdvanced(null, 1.0f);
                break;
            case 5:
                heur = new UnthreatenedMaterial(null, -1.0f, null);
                break;
            case 6:
                heur = new MobilityAdvanced(null, 1.0f);
                break;
            case 8:
                heur = new NullHeuristic();
                break;
            case 9:
                heur = new UnthreatenedMaterial(null, 1.0f, null);
                break;
            case 10:
                heur = new LineCompletionHeuristic(null, 1.0f, null);
                break;
            case 11:
                heur = new CornerProximity(null, -1.0f, null);
                break;
            case 12:
                heur = new MobilitySimple(null, -1.0f);
                break;
            case 13:
                heur = new CentreProximity(null, -1.0f, null);
            case 14:
                heur = new Material(null, -1.0f, null, null);
                break;
            case 15:
                heur = new Influence(null, -1.0f);
                break;
            case 16:
                heur = new MobilitySimple(null, 1.0f);
                break;
            case 17:
                heur = new CentreProximity(null, 1.0f, null);
                break;
            case 18:
                heur = new CornerProximity(null, 1.0f, null);
                break;
            case 20:
                heur = new SidesProximity(null, 1.0f, null);
                break;
            case 21:
                heur = new SidesProximity(null, -1.0f, null);
                break;
            case 23:
                heur = new RegionProximity(null, 1.0f, 3, null);
                break;
            case 26:
                heur = new Score(null, 1.0f);
                break;
            case 30:
                heur = new OwnRegionsCount(null, -1.0f);
                break;
            case 31:
                heur = new RegionProximity(null, 1.0f, 1, null);
                break;
            case 38:
                heur = new PlayerSiteMapCount(null, 1.0f);
                break;
            case 42:
                heur = new RegionProximity(null, 1.0f, 2, null);
                break;
            case 43:
                heur = new RegionProximity(null, 1.0f, 3, null);
                break;
            case 48:
                heur = new RegionProximity(null, 1.0f, 5, null);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + heuristicId);
        }
        return heur;
    }
    //from a game name, loops through endconcepts and gets the right heuristics from that. (NOT FINISHED)
    public Heuristics getHeuristicsPerGame(Game game){
        // read concepts from file
        double[][] values = readConcepts(game.name());

        // define variables and heuristcs
        int[] endconcepts = new int[]{408, 392, 508, 476, 474};
        int[] endconceptheur1 = new int[]{ 16, 48, 5, 12, 9};
        int[] endconceptheur2 = new int[]{ 10, 10, 14, 15, 1};
        int[] weight1 = new int[]{ 1, 1, 100, 1, 100};
        int[] weight2 = new int[]{ 100, 1, 1, 1, 1};
        //loop over all possible endconcepts
        for(int i=0;i<endconcepts.length;i++){

            //if an endconcept is existant in current game
            if(values[0][endconcepts[i]] > 0.01){
                //init heuristics from their id and set their weights
                HeuristicTerm heur1 = getHeuristicFromId(endconceptheur1[i]);
                heur1.setWeight(weight1[i]);
                HeuristicTerm heur2 = getHeuristicFromId(endconceptheur2[i]);
                heur2.setWeight(weight2[i]);
                return new Heuristics(new HeuristicTerm[]{heur1, heur2});
            }
        }
        //basic heuristics
        return new Heuristics(new HeuristicTerm[]{new Material(null, 1.0f, null, null), new MobilitySimple(null, 0.001f)});
    }


    private void setTimeGrad(Game game) {
        System.out.println(game.name());
        if(lengths.containsKey(game.name())){
            double gameLen = lengths.get(game.name());
            double maxTime = (120.0 / gameLen) - 0.1;
            grad = (maxTime - 0.1) / gameLen;
            if (timeTech == 0) {
                time = 60/gameLen;
                grad = 0;
            } else if (timeTech == 1) {
                time = 0.1;
            } else if (timeTech == 2) {
                time = maxTime;
                grad = -grad;
            } else{
                time = 0.5;
                grad = 0;
            }
        }
    }
}