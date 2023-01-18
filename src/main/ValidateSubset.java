package main;

import game.Game;
import other.GameLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class ValidateSubset {
    public static void main(String[] a){
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
}
