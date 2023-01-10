package ai;

import game.Game;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.EvaluatorUtil;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import other.AI;
import other.context.Context;
import other.move.Move;

import java.io.File;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OurAI extends AI{

    String friendlyName;
    int player;
    AI selectedAI;
    Evaluator evaluator;

    public  OurAI() {
        this.friendlyName = "Our AI" ;
        this.evaluator = getModel();
    }

    @Override
    public Move selectAction(Game game, Context context, double maxSeconds, int maxIterations, int maxDepth) {
        return selectedAI.selectAction(game, context, maxSeconds, maxIterations, maxDepth);
    }

    @Override
    public void initAI(Game game, int playerID)
    {
        this.player = playerID;
        this.selectedAI = selectAI(game, playerID);
    }

    //loads the clustering model from a file
    public Evaluator getModel() {

        try {
            //Build model from ppml file
            this.evaluator = new LoadingModelEvaluatorBuilder()
                    .load(new File("resources\\model.pmml"))
                    .build();
            //self check
            this.evaluator.verify();

        } catch(Exception e){
            System.out.println("Couldnt load model correctly");
        }
        return evaluator;
    }

    // retrieves the clusterid for a certain game
    public String getClusterIdFromModel(Game game){
        // first get the necessary components from the game
        //TODO: let it return actual values
        Map values = getNecessaryComponents(game);

        //evaluate
        Map<String, ?> results = evaluator.evaluate(values);

        // Decoupling results from the JPMML-Evaluator runtime environment
        results = EvaluatorUtil.decodeAll(results);

        //get cluster from results
        return (String) results.get("cluster");
    }

    //gets the game rules and components necessary to do the clustering from a game
    public Map<String, Double> getNecessaryComponents(Game game){

        //init concepts map
        HashMap<String, Double> concepts = new HashMap<>();

        //get bool and non bool concepts
        BitSet boolconcepts = game.booleanConcepts();
        Map<Integer, String> nonboolconcepts =  game.nonBooleanConcepts();

        //get inputfields so we can retrieve names later
        List<InputField> inputfields = evaluator.getInputFields();

        //loop over all input fields
        for(int i=0;i<inputfields.size();i++){
            //retrieve name
            String name = inputfields.get(i).getName();

            //check 1: in bool list (converts to int)
            int val = (boolconcepts.get(i)) ? 1 : 0;
            if(val == 1) {
                concepts.put(name, 1.0);
            //check 2: non boolean concepts
            } else {
                String nonbools = nonboolconcepts.get(Integer.toString(i));
                //if it exists in our nonbool list add it, otherwise add 0
                if(nonbools!=null) {
                    concepts.put(name, Double.parseDouble(nonbools));
                } else {
                    concepts.put(name, 0.0);
                }

            }

        }
        return concepts;
    }

    //decision structure for selecting the AI
    public AI selectAI(Game game, int playerID){
        String clusterid = getClusterIdFromModel(game);
        AI foundAI = new ExampleUCT();
        System.out.println("cluster selected:" + clusterid);
        switch (clusterid){
            case "0":
                foundAI = new ExampleUCT();
                break;
            case "1":
                foundAI = new ExampleDUCT();
                break;
            case "2":
                foundAI = new ExampleUCT();
            case "3":
                foundAI = new ExampleUCT();
            case "4":
                foundAI = new ExampleUCT();

        }
        return foundAI;
    }
}
