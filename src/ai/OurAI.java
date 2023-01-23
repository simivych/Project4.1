package ai;

import game.Game;
import metadata.ai.heuristics.Heuristics;
import metadata.ai.heuristics.terms.*;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import other.AI;
import other.GameLoader;
import other.concept.Concept;
import other.context.Context;
import other.move.Move;
import search.mcts.MCTS;
import search.mcts.playout.MAST;
import utils.AIFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OurAI extends AI{

    String friendlyName;
    int player;
    AI selectedAI;
    Evaluator evaluator;
    Map<String, Integer> clusters;
    Map<String, Integer> predictions;
    Map<String, Double> lengths = new HashMap<>();

    // for reading directly from pmml model
    double[][] pcacomponents;
    double[][] scalecomponents;


    double grad = 0.0;
    double time = 0.5;
    int timeTech = 1; //0 is uniform, 1 is increasing, 2 is decreasing
    int turn =1;
    boolean clusteringbool;

    public  OurAI() {
        this.friendlyName = "Our AI";
        this.clusters = getClusters();
        this.clusteringbool = false;
        this.predictions = getPredictions();
        readGameLengths();
        //this.evaluator = getModel();
        //this.pcacomponents = getPCACompenents();
        //this.scalecomponents = getScaleComponents();
    }

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
//        System.out.println("Turn - "+ turn +" time - "+time);
        Move move = selectedAI.selectAction(game, context, time, maxIterations, maxDepth);
        time += grad;
        turn ++;
        return move;
    }

    @Override
    public void initAI(Game game, int playerID)
    {
        this.player = playerID;
        this.selectedAI = selectAI(game, playerID);
        this.selectedAI.initAI(game, playerID);
        //setTimeGrad(game);
    }

    //decision structure for selecting the AI
    public AI selectAI(Game game, int playerID){
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
                case 3:
                    foundAI = AIFactory.createAI("MAST");
                case 4:
                    foundAI = new AlphaBetaNoHeuristics();
                default:
                    foundAI = MCTS.createUCT();
            }
            return foundAI;
        } else {
            int agentid = this.predictions.get(game.name());
            System.out.println("agent selected:" + agentid);
            AI foundAI;
            switch (agentid) {
                case 0:
                    foundAI = MCTS.createUCT();
                    break;
                case 1:
                    foundAI = new PNSMCTS();
                    break;
                case 2:
                    return AIFactory.createAI("MAST");
                case 3:
                    return AIFactory.createAI("MC-GRAVE");
                case 4:
                    return new AlphaBetaNoHeuristics();
                default:
                    foundAI = MCTS.createUCT();
            }
            return foundAI;
        }
    }

    // retrieves the clusterid for a certain game
    public int getClusterIdFromModel(Game game){
        var clusterid = this.clusters.get(game.name());
        // this is probably not necessary though
        if(clusterid == null){
            clusterid = 2;
        }
        return (int) clusterid;
    }

    //loads the clustering model from a file
    public Evaluator getModel() {

        try {
            //Build model from ppml file
            this.evaluator = new LoadingModelEvaluatorBuilder()
                    .load(new File("Python\\modelscikit.pmml"))
                    .build();
            //self check
            this.evaluator.verify();

        } catch(Exception e){
            System.out.println("Couldnt load model correctly");
        }
        return evaluator;
    }

    // OLD VERSION OF GETTING CLUSTERS
    public String getClusterIdFromModelOld(Game game){
        // first get the necessary components from the game
        //double[][] values = getNecessaryComponents(game);
        double[][] values = readConcepts(game.name());

        // standardize based on the python columns
        double[][] standardized = standardize(values);

        // multiply times our pcacomponents to get the right pca'd values
        double[][] pca = multiplyMatrices(standardized, pcacomponents);

        // select the columns based on feature selection
//        double[][] rightcols = getSelectedColumns(pca);
//        computeDistances(pca);
        Map<String, Double> pcamap = pcaToMap(pca);
        //evaluate
        Map<String, ?> results = evaluator.evaluate(pcamap);

        // Decoupling results from the JPMML-Evaluator runtime environment
        results = EvaluatorUtil.decodeAll(results);

        //get cluster from results
        return (String) results.get("cluster");
    }

    //gets the game rules and components necessary to do the clustering from a game
    public double[][] getNecessaryComponents(Game game){

        //init concepts map
        HashMap<String, Double> concepts = new HashMap<>();
        //get bool and non bool concepts
        BitSet boolconcepts = game.booleanConcepts();
        Map<Integer, String> nonboolconcepts =  game.nonBooleanConcepts();
        other.concept.Concept[] var15 = Concept.values();


        //get inputfields so we can retrieve names later
        List<InputField> inputfields = evaluator.getInputFields();
        double[][] conceptsarray = new double[1][scalecomponents.length];
        //loop over all input fields
        for(int i=0;i<inputfields.size();i++){
            //retrieve name
            String name = inputfields.get(i).getName();

            //check 1: in bool list (converts to int)
            int val = (boolconcepts.get(i)) ? 1 : 0;
            if(val == 1) {
                concepts.put(name, 1.0);
                conceptsarray[0][i] = 1.0;
                //check 2: non boolean concepts
            } else {
                String nonbools = nonboolconcepts.get(i);
                //if it exists in our nonbool list add it, otherwise add 0
                if(nonbools!=null) {
                    concepts.put(name, Double.parseDouble(nonbools));
                    conceptsarray[0][i] = Double.parseDouble(nonbools);
                } else {
                    concepts.put(name, 0.0);
                    conceptsarray[0][i] = 0.0;
                }

            }

        }
        return conceptsarray;
    }


    // used for PCA (copied from the itnernet)
    double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix) {
        double[][] result = new double[firstMatrix.length][secondMatrix[0].length];

        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                result[row][col] = multiplyMatricesCell(firstMatrix, secondMatrix, row, col);
            }
        }

        return result;
    }
    // used for PCA (copied from the internet)
    double multiplyMatricesCell(double[][] firstMatrix, double[][] secondMatrix, int row, int col) {
        double cell = 0;
        for (int i = 0; i < secondMatrix.length; i++) {
            cell += firstMatrix[row][i] * secondMatrix[i][col];
        }
        return cell;
    }

    // gets pca components from a file
    public double[][] getPCACompenents(){
        double[][] pcamatrix = new double[666][150];
        try {
            File file = new File("Python\\components.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            int col = 0;
            while ((line = br.readLine()) != null){
                if(col!=0) {
                    String[] values = line.split(",");
                    for (int i = 1; i < values.length; i++) {
                        pcamatrix[col-1][i-1] = Double.parseDouble(values[i]);
                    }
                }
                col++;
            }

        } catch(Exception e){
            System.out.println(e);
            System.out.println(e.getStackTrace());
            System.out.println("Couldnt load components correctly");
        }
        return pcamatrix;
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


    // reads scale components from a file (to reverse standardize)
    public double[][] getScaleComponents(){
        double[][] scalematrix = new double[666][150];
        try {
            File file = new File("Python\\scale.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            int col = 0;
            while ((line = br.readLine()) != null){
                if(col!=0) {
                    String[] values = line.split(",");
                    for (int i = 1; i < values.length; i++) {
                        scalematrix[col-1][i-1] = Double.parseDouble(values[i]);
                    }
                }
                col++;
            }

        } catch(Exception e){
            System.out.println(e);
            System.out.println(e.getStackTrace());
            System.out.println("Couldnt load scale components correctly");
        }
        return scalematrix;
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

    // convert pca to a map
    public Map<String, Double> pcaToMap(double[][] pca){
        HashMap<String, Double> map = new HashMap<>();
        for(int i=0;i<pca[0].length; i++){
            map.put(Integer.toString(i), pca[0][i]);
        }
        return map;
    }

    // compute distances from centroids from clustering
    public void computeDistances(double[][] pcamatrix){
        double[] cluster0 = new double[]{-0.14391111111111124,0.3977777777777777,0.7207999999999998,-0.18093333333333333,0.5535777777777775,0.5548222222222222,1.157733333333333,0.7581333333333335,-0.016466666666666685,0.12264444444444453,0.3030888888888889,0.42706666666666643,0.4353111111111111,0.43179999999999996,-0.1766444444444445,-0.015711111111111,0.1884444444444444,-0.22913333333333336,-0.09751111111111106,0.10508888888888897,-0.8791555555555554,0.05122222222222218,-0.06108888888888873,0.12264444444444444,0.02484444444444452,0.5432666666666666,0.08997777777777773,0.3019555555555556,-0.43648888888888887,-0.2099111111111111,0.10188888888888901,-0.2262,-0.11995555555555554,0.6915777777777778,-0.17568888888888892,-0.16679999999999995,-0.3996000000000001,-1.0455333333333332,0.17468888888888887,0.07368888888888886,-0.21426666666666655,-0.026399999999999983,0.29155555555555546,0.022933333333333406,-0.07848888888888887,0.5818444444444444,1.4989999999999997,-1.1075777777777776,1.391488888888889,0.07057777777777777,-0.3091111111111112,-0.10042222222222226,-0.3179333333333335,0.15333333333333338,0.33613333333333334,-0.09297777777777783,-0.2926222222222223,0.12237777777777771,-0.3341999999999998,-0.7084666666666667,0.3163555555555556,0.5009333333333332,2.183666666666667,0.14295555555555547,1.5057555555555555,11.107244444444445,-0.1832444444444445,-0.44628888888888874,0.012799999999999978,0.020688888888888986,0.05948888888888885,-0.028177777777777753,0.09142222222222221,-0.0020444444444444225,-0.007444444444444441};
        double[] cluster1 = new double[]{1.3821875,1.6815625,1.1057500000000002,-0.6936249999999999,9.620812500000001,-0.47896874999999994,-0.6886875,-0.30803125000000003,-0.03390625000000004,-0.6676874999999999,1.7742812499999996,0.25475000000000003,0.1806249999999999,0.8376874999999995,-0.19587499999999994,0.6544062499999997,-1.5191875000000001,-0.24034375000000008,0.14178125000000003,-3.334437500000001,1.8703124999999992,0.3661562499999999,0.93634375,-0.3290937500000002,-0.15831250000000002,-0.49387499999999984,-0.3318125,-0.3611562500000001,0.25843750000000015,0.4935624999999998,0.30384374999999997,0.14871874999999998,-0.2849375,1.5245312499999992,0.007093749999999826,0.47484374999999984,0.11543749999999998,2.82228125,0.41312500000000024,-0.5046875,-0.17187500000000006,0.7935625000000002,-0.16100000000000006,0.44121875000000005,-0.2252500000000002,0.1451562499999999,-2.757875,0.10943749999999998,-1.3078437500000004,-0.1834687500000001,0.7377187499999999,0.09315625000000001,0.36287499999999995,-0.10618749999999999,-0.4975312499999996,0.46303125000000017,0.4356875,-0.08837500000000002,0.7809375000000003,0.7243125,-0.31649999999999995,-0.01728125000000002,-2.9713125,0.11503125,-1.024875,2.19021875,0.3666875,-0.10481250000000007,0.15331249999999996,0.5932812500000003,-0.24024999999999994,0.16546874999999997,-0.38303125,-0.11115624999999996,-0.22409375000000004};
        double[] cluster2 = new double[]{0.057244663382594554,0.09608702791461415,0.1388669950738916,-0.14795073891625615,-0.5320262725779967,0.02199671592775033,0.0738604269293925,-0.03209852216748766,0.04784564860426933,-0.13736945812807877,-0.24149589490968806,-0.03432348111658459,-0.06529064039408851,0.1599343185550078,0.06768472906403956,0.05139408866995068,-0.07026600985221666,-0.03970771756978655,-0.03039573070607553,-0.29571921182266026,0.30653201970443344,-0.02112315270935961,0.21734811165845644,-0.027495894909688002,0.059760262725779945,0.22716912972085382,0.042735632183908016,0.01806075533661748,0.04684729064039408,-0.18828899835796398,0.15020525451559924,7.783251231526873E-4,0.024029556650246246,0.46171100164203593,0.02789655172413794,-0.10180459770114929,0.005198686371100154,-0.6061001642036128,-0.06284893267651886,-0.029827586206896607,0.05217077175697872,-0.037986863711001385,-0.010307060755336637,0.03554844006568151,0.005266009852216628,-0.08675862068965515,0.48380788177339934,0.06512151067323481,0.18541543513957326,0.022154351395730475,0.11003284072249594,-0.022041050903119818,-0.024550082101806227,-0.017192118226600984,0.008758620689655184,0.0035172413793103274,-0.11346141215106731,0.0072742200328408035,-0.03407881773399019,0.04495566502463053,0.057559934318555044,0.032487684729064,0.11054679802955676,-0.031724137931034485,-0.04745320197044335,-1.7576896551724142,0.005328407224958968,-0.1258390804597701,-0.025362889983579565,0.035093596059113275,0.01821018062397374,-0.028937602627257787,0.025482758620689674,0.026192118226600954,-0.05029556650246302};
        double[] cluster3 = new double[]{-0.14176098901098916,-0.46162362637362625,-0.4558406593406599,0.32509615384615387,-0.17103846153846142,-0.07611538461538474,-0.2491318681318682,-0.08387087912087915,-0.00350549450549446,0.21315109890109873,0.7740604395604394,-0.10563461538461529,0.058505494505494506,-0.26811813186813155,-0.06885164835164835,-0.016909340659340664,-0.46773626373626337,0.11850549450549455,-0.07418956043956046,0.40654120879120903,-0.6814560439560443,0.018109890109890097,-0.4178296703296699,0.06611813186813187,-0.19453846153846158,-0.3816291208791211,-0.010118131868131906,-0.05854945054945055,-0.007491758241758087,0.21619505494505512,-0.19477472527472522,-0.010041208791208766,-0.010758241758241752,-1.2220686813186814,-0.05398351648351651,0.17909065934065954,0.0160494505494505,0.9980357142857145,-0.08906043956043964,0.04276373626373673,-0.013140109890109922,-0.006203296703296741,-0.017505494505494493,-0.14258241758241744,-1.7857142857144776E-4,0.042109890109890115,-0.27753296703296687,0.04023626373626367,-0.17871703296703306,0.35905769230769236,-0.03589285714285714,-0.01767582417582416,0.07647527472527478,0.019714285714285743,0.012354395604395595,-0.013582417582417633,0.153162087912088,0.004035714285714315,-0.12147252747252733,-0.032425824175824305,-0.03326923076923087,-0.10479670329670322,-0.10596153846153834,0.02019505494505494,-0.08819230769230782,1.2552692307692301,-0.041810439560439554,0.14939285714285747,-0.025189560439560426,-0.01756593406593407,-0.012038461538461562,0.04657417582417579,-7.664835164835192E-4,-0.03557142857142856,0.14012912087912088};
        double[] cluster4 = new double[]{-0.389425925925926,0.6998888888888888,0.25068518518518523,0.03903703703703708,0.9904444444444445,0.08631481481481487,0.28968518518518505,0.4782222222222221,-0.48231481481481475,0.40575925925925904,-3.7984444444444447,0.5922037037037037,-0.12788888888888877,-0.8528333333333333,-0.035870370370370386,-0.8401111111111111,4.688555555555556,-0.01737037037037033,0.840388888888889,2.483166666666667,0.761037037037037,-0.14359259259259266,-0.1385370370370368,-0.04283333333333342,0.7102777777777778,-0.14962962962962958,-0.29205555555555546,0.15337037037037032,-0.2675555555555556,0.5490555555555557,-0.6462592592592591,0.15905555555555562,0.0704444444444444,1.550759259259259,0.19174074074074074,-0.2013148148148148,0.09790740740740754,-0.6930740740740737,0.9189074074074071,0.2858333333333333,-0.219462962962963,0.021777777777777695,0.08651851851851856,0.2797222222222222,0.14066666666666672,0.1235185185185185,-3.2002037037037048,-0.14759259259259266,-1.2710740740740742,-2.620351851851852,-1.1786111111111113,0.3961296296296296,-0.18877777777777768,-0.003814814814814781,-0.16744444444444448,-0.14518518518518517,0.23283333333333336,-0.15888888888888886,1.01912962962963,-0.12738888888888883,-0.5009259259259259,-0.06707407407407406,-0.5913333333333332,0.034351851851851835,0.4819074074074074,0.8074074074074074,0.15703703703703703,0.8460555555555557,0.35433333333333333,-0.6461481481481482,-0.031518518518518474,-0.06205555555555555,-0.13144444444444447,0.012166666666666683,-0.238074074074074};
        double[][] clusters = new double[][]{cluster0, cluster1, cluster2, cluster3, cluster4};
        double[] dist = new double[5];
        List<InputField> inputfields = evaluator.getInputFields();
        for(int i=0;i<clusters.length;i++){
            for(int j=0;j<clusters[0].length;j++){
                int index = Integer.parseInt(inputfields.get(j).getName());
                dist[i] += Math.pow((pcamatrix[0][j] - clusters[i][j]), 2);
            }
            System.out.println(dist[i]);
        }

    }
    // get columns that turned out to be the best from the featureselection
    public double[][] getSelectedColumns(double[][] pca){
        int[] columns =  new int[]{38,  31,   9,  40,  26,  20,  41,  46,  87,  72,  45,  56,  71,
                51,  24,  29,  33,  54,  43,  55,  96,  18,  80,  63,  35,  23,
                30,  65,  78,  81,  75, 112,  60,  37,  10,  59,  16, 110,  57,
                119,  53,  27,  91,  14,  44,  73,  39,  88,  66,  58,  52,  61,
                4,  85, 123,  48, 100, 107, 124,  77, 138,  36,  99,   6,  76,
                15,  70,  32,  68,  19, 101, 105,   2,  42, 113};
        double[][] newpca = new double[pca.length][columns.length];

        for(int i=0;i<columns.length;i++){
            int index = columns[i];
            newpca[0][i] = pca[0][index];
        }
        return newpca;
    }

    // normalize our features based on other games (has to be imported through python)
    public double[][] standardize(double[][] pca){
        double[][] newarray = new double[1][pca[0].length];
        for(int i=0;i<pca[0].length; i++){
            newarray[0][i] = (pca[0][i]-scalecomponents[i][0])/scalecomponents[i][1];
        }
        return newarray;

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
            case 48:
                heur = new RegionProximity(null, 1.0f, 5, null);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + heuristicId);
        }
        return heur;
    }
    //from a game name, loops through endconcepts and gets the right heuristics from that. (NOT FINISHED)
    public Heuristics getHeuristicsPerGame(String gamename){
        Game game = GameLoader.loadGameFromName(gamename+ ".lud");
        BitSet concepts = game.booleanConcepts();
        int[] endconcepts = new int[]{492, 494, 500, 502, 516, 518, 524, 526, 508, 510, 483, 485, 467, 469};
        int[][] endconceptheurs = new int[][]{{}};
        for(int i=0;i<endconcepts.length;i++){
            if(concepts.get(endconcepts[i])){
                int[] vals = endconceptheurs[i];
                HeuristicTerm heur1 = getHeuristicFromId(vals[0]);
                HeuristicTerm heur2 = getHeuristicFromId(vals[1]);
                return new Heuristics(new HeuristicTerm[]{heur1, heur2});
            }
        }
        //basic heuristics
        return new Heuristics(new HeuristicTerm[]{new Material(null, 1.0f, null, null), new MobilitySimple(null, 0.001f)});
    }

    public static Heuristics[] createHeuristicVariations(HeuristicTerm heur1, HeuristicTerm heur2){
        Heuristics equal = new Heuristics(new HeuristicTerm[]{heur1, heur2});
        heur1.setWeight(100);
        Heuristics firstbigger = new Heuristics(new HeuristicTerm[]{heur1, heur2});
        heur1.setWeight(1);
        heur2.setWeight(100);
        Heuristics secondbigger = new Heuristics(new HeuristicTerm[]{heur1, heur2});
        Heuristics nosecond = new Heuristics(new HeuristicTerm[]{heur1});
        return new Heuristics[]{equal, firstbigger, secondbigger, nosecond};
    }

    public double[] ifStatementHeurstics(double[] concepts){
        int noOwnPiecesWin = 492;
        int noOwnPiecesLoss = 494;
        int FillWin = 500;
        int FillLoss = 502;
        int ScoringWin = 516;
        int ScoringLoss = 518;
        int NoMovesWin = 524;
        int NoMovesLoss = 526;
        int ReachWin = 508;
        int ReachLoss = 510;
        int EliminatePiecesWin = 483;
        int EliminatePiecesLoss = 485;
        int NoTargetPieceWin = 474;
        int NoTargetPieceLoss = 476;
        int CheckmateWin = 467; //all chesslike games
        int CheckmateLoss = 469;

        //four heuristics

        //1. default 1 point for material 0.5 for mobility

        //2. -1 point for material 0.5 for mobility
        int[] twoindex = new int[]{FillLoss, EliminatePiecesLoss,noOwnPiecesWin};
        //3. 1 point material 0.75 for mobility
        int[] threeindix = new int[]{NoMovesLoss, ReachWin};
        //4. 2 points for material 0.5 for mobility
        int[] fourindex = new int[]{noOwnPiecesLoss, EliminatePiecesWin};
        //5 -1 point for material -0.5 point for mobility
        int[] fiveindex = new int[]{ScoringLoss, ReachLoss};
        //6 1 point for material -0.5 for mobility
        int[] sixindix = new int[]{NoMovesWin};

        for(int index: twoindex){
            if(concepts[index] == 1.0){
                return new double[]{-1, 0.5};
            }
        }
        for(int index: threeindix){
            if(concepts[index] == 1.0){
                return new double[]{1, 0.75};
            }
        }
        for(int index: fourindex){
            if(concepts[index] == 1.0){
                return new double[]{2, 0.5};
            }
        }
        for(int index: fiveindex){
            if(concepts[index] == 1.0){
                return new double[]{-1, -0.5};
            }
        }
        for(int index: sixindix){
            if(concepts[index] == 1.0){
                return new double[]{1, -0.5};
            }
        }
        return new double[]{1, 0.5};
    }

    private void setTimeGrad(Game game) {
        System.out.println(game.name());
        if(lengths.containsKey(game.name())){
            System.out.println("time changed");
            double gameLen = lengths.get(game.name());
            System.out.println(gameLen);
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
            }
        }
    }
}