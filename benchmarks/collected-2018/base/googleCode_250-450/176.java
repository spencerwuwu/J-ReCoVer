// https://searchcode.com/api/result/8285662/

package name.nanek.gdwprototype.server;

import name.nanek.gdwprototype.client.model.GameDisplayInfo;
import name.nanek.gdwprototype.client.model.GameUpdateInfo;
import name.nanek.gdwprototype.client.service.GameService;
import name.nanek.gdwprototype.server.support.AppEngineUtil;
import name.nanek.gdwprototype.server.support.DbUtil;
import name.nanek.gdwprototype.shared.FieldVerifier;
import name.nanek.gdwprototype.shared.exceptions.GameException;
import name.nanek.gdwprototype.shared.model.Game;
import name.nanek.gdwprototype.shared.model.Marker;
import name.nanek.gdwprototype.shared.model.Player;
import name.nanek.gdwprototype.shared.model.Position;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;

/**
 * Handles game service requests.
 * 
 * @author Lance Nanek
 */
@SuppressWarnings("serial")
public class ServiceImpl extends RemoteServiceServlet implements GameService {
		
	//TODO get all game logic moved into this
	private Engine engine = new Engine();
	
	//TODO get all DB logic moved into this
	private DataAccessor dataAccessor = new DataAccessor();
	
	//TODO reduce repetitive transaction handling by using an interceptor/filter? 
	//or just having one method that handles command objects?
	
	@Override
	public GameUpdateInfo moveMarker(Long gameId, Integer sourceRow, Integer sourceColumn, Integer destRow,
			Integer destColumn, Long markerId) throws GameException {
		
		//System.out.println("GameServiceImpl#moveMarker: " + gameId  + ", " + sourceRow + ", " + sourceColumn + ", " + destRow
		//		 + ", " + destColumn + ", " + markerId);
		
		User user = AppEngineUtil.requireUser();
			
		Objectify em = DbUtil.beginTransaction();
		try {
			GameUpdateInfo update = engine.moveMarker(gameId, sourceRow, sourceColumn, destRow, destColumn, markerId, user, em);
			em.getTxn().commit();
			return update;
		} finally {
			DbUtil.rollbackIfNeeded(em);
		}		
	}

	@Override
	public GameDisplayInfo getDisplayInfo(Long gameId) throws GameException {
		Objectify em = DbUtil.beginTransaction();
		try {
			Game game = dataAccessor.requireGame(this, em, gameId);
			return engine.createDisplayInfo(game, em);

			// Don't bother committing, this was read only anyway.
		} finally {
			DbUtil.rollbackIfNeeded(em);
		}		
	}
	
	//TODO accept an optional move number parameter representing what state the client already knows about
	//return null to indicate no changes if the game's move number hasn't been incremented
	@Override
	public GameUpdateInfo getPositionsByGameId(Long gameId) throws GameException {
		Objectify em = DbUtil.beginTransaction();
		try {
			Game game = dataAccessor.requireGame(this, em, gameId);
			return engine.createPlayInfo(game, em);

			// Don't bother committing, this was read only anyway.
		} finally {
			DbUtil.rollbackIfNeeded(em);
		}
	}

	@Override
	public void surrender(Long gameId, Player surrenderer) throws GameException {
		
		Objectify em = DbUtil.beginTransaction();
		try {
			Game game = dataAccessor.requireGame(this, em, gameId);
			game.setEnded(true);
			game.setWinner(Player.other(surrenderer));
			em.put(game);
			
			em.getTxn().commit();
		} finally {
			DbUtil.rollbackIfNeeded(em);
		}
	}

	@Override
	public void publishMap(Long mapId) throws GameException {
		Objectify em = DbUtil.beginTransaction();
		try {
			Game map = dataAccessor.requireMap(this, em, mapId);
			map.setEnded(true);
			em.put(map);
			
			em.getTxn().commit();
		} finally {
			DbUtil.rollbackIfNeeded(em);
		}
	}
	
	@Override
	public Game getGameListingById(Long gameId) throws GameException {
		
		Objectify em = DbUtil.beginTransaction();
		try {
			Game game = dataAccessor.requireGame(this, em, gameId);
			return game;
		} finally {
			DbUtil.rollbackIfNeeded(em);
		}
	}
	
	@Override
	public Game attemptToJoinGame(final Long gameId) throws GameException {

		final User user = AppEngineUtil.requireUser();
	        
		Objectify ofy = DbUtil.beginTransaction();
		try {
			Game game = dataAccessor.requireGame(this, ofy, gameId);
			if ( user.getUserId().equals(game.getFirstPlayerUserId()) ) {
				//User is already player one.
				//TODO switch to hotseat mode?
			} else if ( null == game.getSecondPlayerUserId() || user.getUserId().equals(game.getSecondPlayerUserId()) ) {
				game.setSecondPlayerUserId(user.getUserId());
			}
			ofy.put(game);
			
			ofy.getTxn().commit();
			
			return game;
		} finally {
			DbUtil.rollbackIfNeeded(ofy);
		}
	}
	
	@Override
	public String getLoginUrlIfNeeded(String returnUrl) {
        UserService userService = UserServiceFactory.getUserService();
        User user = userService.getCurrentUser();
        if (user != null) {
        	return null;
        } else {
            return userService.createLoginURL(returnUrl);
        }
	}
	
	@Override
	public Game[] getMapNames() throws GameException {
		Objectify em = DbUtil.createObjectify();
		Game[] listings = engine.getListings(dataAccessor.getMaps(em));
		return listings;
	}
	
	@Override
	public Game[] getJoinableGameNames() throws GameException {
        String username = AppEngineUtil.getUserId();
        
		Objectify em = DbUtil.createObjectify();
		Game[] listings = engine.getListings(dataAccessor.getJoinableGames(em, username));
		return listings;
	}

	@Override
	public Game[] getObservableGameNames() throws GameException {
        String username = AppEngineUtil.getUserId();
        
		Objectify em = DbUtil.createObjectify();
		Game[] listings = engine.getListings(dataAccessor.getObservableGames(em, username));
		return listings;
	}

	@Override
	public Game createMap(String name, Integer carrotGenerationPeriod, Integer boardWidth, Integer boardHeight, Marker[] markers) throws GameException {
		FieldVerifier.validateGameName(name);

		User user = AppEngineUtil.requireUser();
		Objectify createOfy = DbUtil.beginTransaction();
		try {
			Game map = new Game(name, user);
			map.setMap(true);
			map.setBoardWidth(boardWidth);
			map.setBoardHeight(boardHeight);
			map.carrotGenerationPeriod = carrotGenerationPeriod;

			Key<Game> mapKey = createOfy.put(map);
			for ( Marker marker : markers ) {
				marker.setGame(mapKey);
				createOfy.put(marker);
			}		
			createOfy.getTxn().commit();			
			return map;
		} finally {
			DbUtil.rollbackIfNeeded(createOfy);
		}
	}



	@Override
	public Game createGame(String name, Long mapId) throws GameException {
		FieldVerifier.validateGameName(name);

		User user = AppEngineUtil.requireUser();
		
		//Each map or game is in a separate entity group so can't be accessed from the same transaction.
		//So we need two transactions to copy settings from the map into the newly created game.
		Objectify referenceOfy = DbUtil.beginTransaction();
		try {
			Game map = dataAccessor.requireMap(this, referenceOfy, mapId);	

			Objectify createOfy = DbUtil.beginTransaction();
			try {
			
				Game game = new Game(name, user);
				game.setBoardHeight(map.getBoardHeight());
				game.setBoardWidth(map.getBoardWidth());
				game.turnsUntilNextCarrotGenerated = game.carrotGenerationPeriod = map.carrotGenerationPeriod;
				Key<Game> gameKey = createOfy.put(game);
				
				for ( Marker mapMarker :  referenceOfy.query(Marker.class).ancestor(map) ) {
					Marker gameMarker = mapMarker.copy();
					gameMarker.setGame(gameKey);
					Key<Marker> gameMarkerKey = createOfy.put(gameMarker);
					
					for ( Position mapPosition :  referenceOfy.query(Position.class).ancestor(map).filter("marker", mapMarker) ) {
						Position gamePosition = mapPosition.copy();
						gamePosition.setGame(gameKey);
						gamePosition.setMarker(gameMarkerKey);
						createOfy.put(gamePosition);
					}
				}
				
				createOfy.getTxn().commit();			
				return game;
				
			} finally {
				DbUtil.rollbackIfNeeded(createOfy);
			}
		} finally {
			DbUtil.rollbackIfNeeded(referenceOfy);
		}
	}

}

