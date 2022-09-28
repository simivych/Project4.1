package main;

import app.StartDesktopApp;
import manager.ai.AIRegistry;
import mcts.ExampleDUCT;
import mcts.ExampleUCT;
import mcts.OurAI;
import random.RandomAI;

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
		// Register our example AIs
		AIRegistry.registerAI("Our AI", () -> {return new OurAI();}, (game) -> {return true;});

		// Run Ludii
		StartDesktopApp.main(new String[0]);
	}

}
