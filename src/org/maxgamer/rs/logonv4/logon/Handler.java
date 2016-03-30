package org.maxgamer.rs.logonv4.logon;

import org.maxgamer.rs.network.io.stream.RSInputBuffer;

/**
 * A handler changes depending on the state of a Game Server connection. It starts as AuthHandler, which
 * validates that the connection is allowed (Eg, correct logon server password). Then it can go to a 
 * GameDecoder which interprets packets received from the Game Server.
 * 
 * @author netherfoam
 */
public interface Handler {
	public abstract void handle(RSInputBuffer in);
}