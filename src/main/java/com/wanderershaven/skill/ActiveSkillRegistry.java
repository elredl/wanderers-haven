package com.wanderershaven.skill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** Registry of active-skill handlers keyed by skill ID. */
public final class ActiveSkillRegistry {

	@FunctionalInterface
	public interface Handler {
		void execute(ServerPlayer player);
	}

	private final Map<String, Handler> handlers = new ConcurrentHashMap<>();

	public void register(String skillId, Handler handler) {
		handlers.put(skillId, handler);
	}

	public boolean execute(ServerPlayer player, String skillId) {
		Handler handler = handlers.get(skillId);
		if (handler == null) return false;
		handler.execute(player);
		return true;
	}
}
