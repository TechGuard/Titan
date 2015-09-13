package org.maxgamer.rs.network;

import org.maxgamer.rs.command.CommandSender;
import org.maxgamer.rs.model.entity.mob.persona.player.CheatLog;
import org.maxgamer.rs.model.entity.mob.persona.player.FriendsList;
import org.maxgamer.rs.model.entity.mob.persona.player.Player;
import org.maxgamer.rs.network.io.packet.RSOutgoingPacket;
import org.maxgamer.rs.network.protocol.ProtocolHandler;
import org.maxgamer.rs.structure.YMLSerializable;
import org.maxgamer.rs.structure.configs.ConfigSection;

/**
 * Represents a {@link Player} or {@link LobbyPlayer} which is controlled by a
 * user.
 * @author netherfoam
 */
public interface Client extends YMLSerializable, CommandSender {
	/**
	 * Fetches the session which is manipulating this client
	 * @return the session never null
	 */
	public Session getSession();
	
	/**
	 * Fetches the name of this client
	 * @return the name, never null
	 */
	public String getName();
	
	/**
	 * Fetches the version of the game this user is running
	 * @return the version
	 */
	public int getVersion();
	
	/**
	 * Fetches the friends list available to this client
	 * @return the friends list available to this client not null
	 */
	public FriendsList getFriends();
	
	/**
	 * Fetches the Protocol to use to send data to this client
	 * @return the Protocol to use to send data to this client not null
	 */
	public ProtocolHandler<? extends Client> getProtocol();
	
	/**
	 * The unique identifier generated by the client when it starts. This is
	 * sent in the login block, and changes each time the client restarts, but
	 * not between logging out & in.
	 * @return
	 */
	public int getUUID();
	
	/**
	 * This method is under the contract that it should remove this Client from
	 * the server entirely, such as removing this from the Players list and
	 * removing this from the Lobby.
	 */
	public void destroy();
	
	/**
	 * A logger against this particular client for anything that is suspicious,
	 * such as picking up items they can't see, sending bad packets, using
	 * interfaces they don't have open, and so on.
	 * 
	 * @return the cheat log
	 */
	public CheatLog getCheats();
	
	//TODO: This should not be here, all data should be done throguh the protocol.
	public boolean write(RSOutgoingPacket out);
	
	public abstract void sendMessage(String string);
	
	public ConfigSection getConfig();
}
