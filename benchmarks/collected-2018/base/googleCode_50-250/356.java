// https://searchcode.com/api/result/13126616/

package scotlandyard.engine.spec;

/**
 * interface specifications of player object which extends IUser interface
 *
 * @author simon
 * @version 3.0
 */
public interface IPlayer extends IUser {

	/**
	 * get current position of the player on the map,
	 * which node is the player on
	 * @return integer of node of current position of the player
	 */
	int getPosition();


	/**
	 * gets the number of specify transport tokens
	 * @param transport
	 * @return numbers of tokens
	 */
	int getTickets(int transport);


	/**
	 * sets the number of tokens a player uses the transport type,
	 * that move from the current player position to a legal node
	 * @param transport
	 * @param value
	 */
	void setTickets(int transport,int value);


	/**
	 * checks whether player is MrX
	 * @return true if player is MrX, false otherwise
	 */
	boolean isMrx();


	/**
	 * sets the number of tickets that the player use for the move,
	 * reduce 1 token for the transport
	 * @param transport
	 */
	void consumeTicket(int transport);


	/**
	 * sets new position of player
	 * @param newPosition - position that transport can move to
	 */
	void setPosition(int newPosition);
}
