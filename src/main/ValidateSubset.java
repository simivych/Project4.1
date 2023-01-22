package main;

import com.kitfox.svg.A;
import game.Game;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import other.GameLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

public class ValidateSubset {

    static ArrayList<Double> kilothonRes = new ArrayList<>();

    static ArrayList<Double> subset1 = new ArrayList<>();
    static ArrayList<Double> subset2 = new ArrayList<>();
    static ArrayList<Double> subset3 = new ArrayList<>();
    static ArrayList<Double> subset4 = new ArrayList<>();
    static ArrayList<Double> subset5 = new ArrayList<>();
    static ArrayList<Double> subset6 = new ArrayList<>();
    static ArrayList<Double> subset7 = new ArrayList<>();
    static ArrayList<Double> subset8 = new ArrayList<>();
    static ArrayList<Double> subset9 = new ArrayList<>();
    static ArrayList<Double> subset10 = new ArrayList<>();
    static ArrayList<Double> subset11 = new ArrayList<>();
    static ArrayList<Double> subset12 = new ArrayList<>();

    static String[] results = new String[]{"results/resultsAlphaBeta.txt", "results/resultsUCT.txt", "results/resultsPNMCTS.txt",
            "results/resultsGRAVE.txt", "results/resultsMAST.txt"};

    static String[] subsets = new String[]{"subsets/Birch/subset_5_filtered.csv","subsets/Birch/subset_5.csv",
            "subsets/Birch/subset_5_filtered_PCA.csv","subsets/Birch/subset_5_PCA.csv", "subsets/K_means/subset_5.csv",
            "subsets/K_means/subset_5_filtered.csv", "subsets/K_means/subset_5_filtered_random.csv",
            "subsets/K_means/subset_5_random.csv", "subsets/K_means/subset_5_PCA.csv", "subsets/K_means/subset_5_PCA_filtered_random.csv",
            "subsets/K_means/subset_5_PCA_filtered.csv", "subsets/K_means/subset_5_PCA_random.csv"};



    public static void main(String[] a) {


        getScores();

        printScores();


    }

    private static void printScores() {
        System.out.println("Kilothon scores - ");
        System.out.println(Arrays.toString(kilothonRes.toArray()));
        System.out.println(subsets[0] + " scores - ");
        System.out.println(Arrays.toString(subset1.toArray()));
        System.out.println(subsets[1] + " scores - ");
        System.out.println(Arrays.toString(subset2.toArray()));
        System.out.println(subsets[2] + " scores - ");
        System.out.println(Arrays.toString(subset3.toArray()));
        System.out.println(subsets[3] + " scores - ");
        System.out.println(Arrays.toString(subset4.toArray()));
        System.out.println(subsets[4] + " scores - ");
        System.out.println(Arrays.toString(subset5.toArray()));
        System.out.println(subsets[5] + " scores - ");
        System.out.println(Arrays.toString(subset6.toArray()));
        System.out.println(subsets[6] + " scores - ");
        System.out.println(Arrays.toString(subset7.toArray()));
        System.out.println(subsets[7] + " scores - ");
        System.out.println(Arrays.toString(subset8.toArray()));
        System.out.println(subsets[8] + " scores - ");
        System.out.println(Arrays.toString(subset9.toArray()));
        System.out.println(subsets[9] + " scores - ");
        System.out.println(Arrays.toString(subset10.toArray()));
        System.out.println(subsets[10] + " scores - ");
        System.out.println(Arrays.toString(subset11.toArray()));
        System.out.println(subsets[11] + " scores - ");
        System.out.println(Arrays.toString(subset12.toArray()));

        System.out.println("Correlations - ");
        System.out.println(Arrays.toString(calculatePearson().toArray()));
    }

    private static ArrayList<Double> calculatePearson() {
        ArrayList<Double> correls = new ArrayList<>();
        double[] kilothonres = toArray(kilothonRes);

        double[] subset1arr = toArray(subset1);
        double corr = correlationCoefficient(kilothonres, subset1arr);
        correls.add(corr);

        double[] subset2arr = toArray(subset2);
        corr= correlationCoefficient(kilothonres, subset2arr);
        correls.add(corr);

        double[] subset3arr = toArray(subset3);
        corr = correlationCoefficient(kilothonres, subset3arr);
        correls.add(corr);

        double[] subset4arr = toArray(subset4);
        corr = correlationCoefficient(kilothonres, subset4arr);
        correls.add(corr);

        double[] subset5arr = toArray(subset5);
        corr = correlationCoefficient(kilothonres, subset5arr);
        correls.add(corr);

        double[] subset6arr = toArray(subset6);
        corr = correlationCoefficient(kilothonres, subset6arr);
        correls.add(corr);

        double[] subset7arr = toArray(subset7);
        corr = correlationCoefficient(kilothonres, subset7arr);
        correls.add(corr);

        double[] subset8arr = toArray(subset8);
        corr = correlationCoefficient(kilothonres, subset8arr);
        correls.add(corr);

        double[] subset9arr = toArray(subset9);
        corr = correlationCoefficient(kilothonres, subset9arr);
        correls.add(corr);

        double[] subset10arr = toArray(subset10);
        corr = correlationCoefficient(kilothonres, subset10arr);
        correls.add(corr);

        double[] subset11arr = toArray(subset11);
        corr = correlationCoefficient(kilothonres, subset11arr);
        correls.add(corr);

        double[] subset12arr = toArray(subset12);
        corr = correlationCoefficient(kilothonres, subset12arr);
        correls.add(corr);

        return correls;
    }

    private static double[] toArray(ArrayList<Double> arr) {
        double[] result = new double[arr.size()];
        int index = 0;
        for(double number:arr){
            result[index] = number;
        }
        return result;
    }

    private static void getScores() {
        kilothonRes.add(-1.0);
        subset1.add(-1.0);
        subset2.add(-1.0);
        subset3.add(-1.0);
        subset4.add(-1.0);
        subset5.add(-1.0);
        subset6.add(-1.0);
        subset7.add(-1.0);
        subset8.add(-1.0);
        subset9.add(-1.0);
        subset10.add(-1.0);
        subset11.add(-1.0);
        subset12.add(-1.0);

        kilothonRes.add(1.0);
        subset1.add(1.0);
        subset2.add(1.0);
        subset3.add(1.0);
        subset4.add(1.0);
        subset5.add(1.0);
        subset6.add(1.0);
        subset7.add(1.0);
        subset8.add(1.0);
        subset9.add(1.0);
        subset10.add(1.0);
        subset11.add(1.0);
        subset12.add(1.0);


        for (String result:results){
            double score = calculateScore(result);
            kilothonRes.add(score);
            subset1.add((double) caclcualteSubsetScore(result, getGames(subsets[0])));
            subset2.add((double) caclcualteSubsetScore(result, getGames(subsets[1])));
            subset3.add((double) caclcualteSubsetScore(result, getGames(subsets[2])));
            subset4.add((double) caclcualteSubsetScore(result, getGames(subsets[3])));
            subset5.add((double) caclcualteSubsetScore(result, getGames(subsets[4])));
            subset6.add((double) caclcualteSubsetScore(result, getGames(subsets[5])));
            subset7.add((double) caclcualteSubsetScore(result, getGames(subsets[6])));
            subset8.add((double) caclcualteSubsetScore(result, getGames(subsets[7])));
            subset9.add((double) caclcualteSubsetScore(result, getGames(subsets[8])));
            subset10.add((double) caclcualteSubsetScore(result, getGames(subsets[9])));
            subset11.add((double) caclcualteSubsetScore(result, getGames(subsets[10])));
            subset12.add((double) caclcualteSubsetScore(result, getGames(subsets[11])));


        }

    }

    private static ArrayList<String> getGames(String subset) {
        ArrayList<String> games = new ArrayList<String>();
        try{
            FileReader file1 = new FileReader(subset);
            BufferedReader br = new BufferedReader(file1);
            String line = br.readLine();
            while((line = br.readLine()) != null){
                String[] vals = line.split(",");
                String name = vals[3];
                games.add(name);
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return games;
    }

    private static float caclcualteSubsetScore(String results, ArrayList<String> subset) {
        float totalScore = 0;
        int numGames = 0;
        try{
            FileReader file = new FileReader(results);
            BufferedReader br = new BufferedReader(file);
            String line = br.readLine();
            while((line = br.readLine()) != null){
                String[] vals = line.split(", ");
                String name = vals[0];
                if(isIn(subset, name)) {
                    float score = Float.parseFloat(vals[4]);
                    totalScore += score;
                    numGames++;
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return totalScore/numGames;
    }

    private static boolean isIn(ArrayList<String> subset, String name) {
        for(String game:subset){
            if(game.equals(name)){
                return true;
            }
        }
        return false;
    }

    private static float calculateScore(String results) {
        float totalScore = 0;
        int numGames = 0;
        try{
            FileReader file = new FileReader(results);
            BufferedReader br = new BufferedReader(file);
            String line = br.readLine();
            while((line = br.readLine()) != null){
                String[] vals = line.split(", ");
                float score = Float.parseFloat(vals[4]);
                totalScore += score;
                numGames++;

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        return totalScore/numGames;
    }

    public static void makeFileAB(){
        try{
            // Creates a FileReader
            FileReader file = new FileReader("results/KilothonResults1.txt");
            BufferedReader br = new BufferedReader(file);
            BufferedWriter bw = new BufferedWriter(new FileWriter("results/resultsAlphaBeta.txt"));
            String line;
            String header = "";
            header+="GAME NAME, "; // game name
            header+="NUM PLAYERS, "; // num players
            header+="WIN?, "; // 1 if winning
            header+="RANK P1, "; // ranking of P1
            header+="UTILITY P1, "; // reward of P1
            header+="NUM MOVES, "; // game length
            header+="NUM P1 MOVES"; // number of P1 moves
            bw.write(header);
            while( (line = br.readLine()) != null){
                if(line.startsWith("game")){
                    String[] split = line.split(": ");
                    String gameName = split[1].replace(" is running", "");
                    line = br.readLine();
                    while(line.startsWith("switch")){
                        line = br.readLine();
                    }
                    String[] res = line.split(" = ");
                    String reward = res[1].replace(" (ranking","");
                    String ranking = res[2].split("\\)")[0];
                    String length = line.split("in ")[1].replace(" moves.","");
                    // GAME NAME, NUM PLAYERS, WIN?, RANK P1, UTILITY P1, NUM MOVES, NUM P1 MOVES,
                    Game game = GameLoader.loadGameFromName(gameName+".lud");
                    int numPlayers = game.players().count();
                    boolean win = Float.parseFloat(ranking)==1;
                    String info = gameName + ", " + numPlayers + ", " + win + ", " + ranking + ", " + reward + ", " + length + ", ";
                    bw.newLine();
                    bw.write(info);

                }
            }
            br.close();
            bw.close();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        }

    static double correlationCoefficient(double X[], double Y[]) {
        int n = X.length;
        double sum_X = 0;
        double sum_Y = 0;
        double sum_XY = 0;
        double squareSum_X = 0;
        double squareSum_Y = 0;

        for (int i = 0; i < n; i++) {
            sum_X += X[i];
            sum_Y += Y[i];
            sum_XY += (X[i] * Y[i]);
            squareSum_X += X[i] * X[i];
            squareSum_Y += Y[i] * Y[i];
        }

        double corr = (n * sum_XY - sum_X * sum_Y)/
                (Math.sqrt((n * squareSum_X - sum_X * sum_X) * (n * squareSum_Y - sum_Y * sum_Y)));


        return corr;
    }
}