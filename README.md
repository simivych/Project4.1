To get code to compile download the ludii.jar file

In IntelliJ upload by File -> Project Structur -> Modules -> Dependencies -> + -> JARs -> select downloaded .jar file

USEFUL COMMANDS

get all games

final String[] games = FileHandling.listGames();

create new game

Game game = GameLoader.loadGameFromName("Tic-Tac-Four.lud");

get moves

FastArrayList<Move> legalMoves = game.moves(context).moves();
for games when players can play at the same time 
if(!game.isAlternatingMoveGame()){legalMoves = AIUtils.extractMovesForMover(legalMoves,player);}
	
Stochastic

final long gameFlags = game.gameFlags();
final boolean isStochastic = ((gameFlags & GameType.Stochastic) != 0L);


