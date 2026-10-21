package dev.fimkov.echoesbeneath.record;

import dev.fimkov.echoesbeneath.item.EchoChartItem;
import dev.fimkov.echoesbeneath.memory.EchoMemoryRecord;
import dev.fimkov.echoesbeneath.memory.timeline.SoundTimeline;
import dev.fimkov.echoesbeneath.network.RecordSoundPayload;
import dev.fimkov.echoesbeneath.platform.EchoPlatforms;
import dev.fimkov.echoesbeneath.runtime.EchoServerState;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;

public final class EchoRecordPlayback {
	private static final Identifier EMPTY = Identifier.withDefaultNamespace("intentionally_empty");
	private final EchoServerState state;
	private final Map<UUID, Session> sessions = new HashMap<>();

	public EchoRecordPlayback(EchoServerState state) {
		this.state = state;
	}

	public void jukeboxChanged(JukeboxBlockEntity box) {
		if (!(box.getLevel() instanceof ServerLevel level)) {
			return;
		}
		stopAt(level, box.getBlockPos());
		if (!(box.getTheItem().getItem() instanceof EchoChartItem) || !box.getSongPlayer().isPlaying()) {
			return;
		}
		EchoRecordData edit = EchoChartItem.recordData(box.getTheItem(), state.storage());
		if (edit == null) {
			box.getSongPlayer().stop(level, box.getBlockState());
			return;
		}
		start(level, box.getBlockPos(), edit, null, box, 0);
	}

	public void observeJukebox(JukeboxBlockEntity box) {
		if (!(box.getLevel() instanceof ServerLevel level)
				|| !(box.getTheItem().getItem() instanceof EchoChartItem) || !box.getSongPlayer().isPlaying()) {
			return;
		}
		boolean known = sessions.values().stream().anyMatch(session -> session.box == box);
		if (!known) {
			EchoRecordData edit = EchoChartItem.recordData(box.getTheItem(), state.storage());
			if (edit != null) {
				start(level, box.getBlockPos(), edit, null, box,
						(int) box.getSongPlayer().getTicksSinceSongStarted());
			}
		}
	}

	public boolean preview(ServerPlayer player, EchoRecordData edit) {
		stopPreview(player.getUUID());
		return start(player.level(), player.blockPosition(), edit, player.getUUID(), null, 0);
	}

	private boolean start(ServerLevel level, BlockPos speaker, EchoRecordData edit, UUID preview,
			JukeboxBlockEntity box, int elapsed) {
		var record = state.storage().load(edit.memoryId());
		if (record.isEmpty() || edit.endTick() > record.get().durationTicks()) {
			if (box != null) {
				box.getSongPlayer().stop(level, box.getBlockState());
			}
			return false;
		}
		Session session = new Session(level, speaker.immutable(), record.get(), edit, preview, box, elapsed);
		sessions.put(session.id, session);
		return true;
	}

	public int previewTick(UUID owner) {
		return sessions.values().stream().filter(session -> owner.equals(session.preview))
				.mapToInt(session -> session.elapsed).findFirst().orElse(-1);
	}

	public void stopPreview(UUID owner) {
		for (Session session : List.copyOf(sessions.values())) {
			if (owner.equals(session.preview)) {
				stop(session);
			}
		}
	}

	public void stopAt(ServerLevel level, BlockPos position) {
		for (Session session : List.copyOf(sessions.values())) {
			if (session.box != null && session.level == level && session.speaker.equals(position)) {
				stop(session);
			}
		}
	}

	public void stopAll() {
		List.copyOf(sessions.values()).forEach(this::stop);
	}

	public void stopIn(ServerLevel level) {
		for (Session session : List.copyOf(sessions.values())) {
			if (session.level == level) {
				stop(session);
			}
		}
	}

	public void tick() {
		for (Session session : List.copyOf(sessions.values())) {
			if (!session.valid() || session.elapsed >= session.edit.durationTicks()) {
				stop(session);
				continue;
			}
			int tick = session.edit.startTick() + session.elapsed;
			if (!session.primed) {
				session.primed = true;
				for (int index : session.initialSongs.values()) { emit(session, index); }
			}
			while (session.cursor < session.sounds.size() && session.sounds.tickAt(session.cursor) <= tick) {
				emit(session, session.cursor++);
			}
			session.elapsed++;
		}
	}

	private void emit(Session session, int index) {
		SoundTimeline sounds = session.sounds;
		int voice = index;
		int action = RecordSoundPayload.PLAY;
		if (sounds.isJukeboxAt(index)) {
			BlockPos origin = BlockPos.containing(sounds.relativeXAt(index), sounds.relativeYAt(index),
					sounds.relativeZAt(index));
			Integer previous = session.jukeboxVoices.remove(origin);
			if (previous != null) {
				send(session, new RecordSoundPayload(session.id, session.speaker, previous, EMPTY,
						0.0F, 1.0F, RecordSoundPayload.STOP_VOICE, 0), true);
			}
			if (sounds.isStopAt(index)) {
				return;
			}
			session.jukeboxVoices.put(origin, voice);
		}
		send(session, new RecordSoundPayload(session.id, session.speaker, voice, sounds.soundAt(index),
				Math.clamp(sounds.volumeAt(index), 0.0F, 1.0F) * session.edit.gain(),
				sounds.pitchAt(index), action, Math.max(0, session.edit.startTick() + session.elapsed - sounds.tickAt(index))), false);
	}

	private void send(Session session, RecordSoundPayload payload, boolean allListeners) {
		if (session.preview != null) {
			ServerPlayer player = state.server().getPlayerList().getPlayer(session.preview);
			if (player != null && player.level() == session.level) {
				EchoPlatforms.network().sendToPlayer(player, payload);
			}
			return;
		}
		for (ServerPlayer player : state.server().getPlayerList().getPlayers()) {
			boolean nearby = player.level() == session.level
					&& player.position().distanceToSqr(session.speaker.getX() + 0.5D,
							session.speaker.getY() + 0.5D, session.speaker.getZ() + 0.5D) <= 64.0D * 64.0D;
			if ((allListeners && session.listeners.contains(player.getUUID())) || nearby) {
				session.listeners.add(player.getUUID());
				EchoPlatforms.network().sendToPlayer(player, payload);
			}
		}
	}

	private void stop(Session session) {
		sessions.remove(session.id);
		send(session, new RecordSoundPayload(session.id, session.speaker, 0, EMPTY,
				0.0F, 1.0F, RecordSoundPayload.STOP_ALL, 0), true);
		if (session.box != null && !session.box.isRemoved() && session.box.getSongPlayer().isPlaying()) {
			session.box.getSongPlayer().stop(session.level, session.box.getBlockState());
			session.box.setChanged();
		}
	}

	private final class Session {
		private final UUID id = UUID.randomUUID();
		private final ServerLevel level;
		private final BlockPos speaker;
		private final EchoRecordData edit;
		private final SoundTimeline sounds;
		private final UUID preview;
		private final JukeboxBlockEntity box;
		private final Set<UUID> listeners = new HashSet<>();
		private final Map<BlockPos, Integer> jukeboxVoices = new HashMap<>();
		private int cursor;
		private int elapsed;
		private boolean primed;
		private final Map<BlockPos, Integer> initialSongs = new HashMap<>();

		private Session(ServerLevel level, BlockPos speaker, EchoMemoryRecord memory, EchoRecordData edit,
				UUID preview, JukeboxBlockEntity box, int elapsed) {
			this.level = level;
			this.speaker = speaker;
			this.edit = edit;
			this.sounds = memory.soundTimeline();
			this.preview = preview;
			this.box = box;
			this.elapsed = Math.clamp(elapsed, 0, edit.durationTicks());
			while (cursor < sounds.size() && sounds.tickAt(cursor) < edit.startTick() + this.elapsed) {
				if (sounds.isJukeboxAt(cursor)) {
					BlockPos origin = BlockPos.containing(sounds.relativeXAt(cursor), sounds.relativeYAt(cursor), sounds.relativeZAt(cursor));
					if (sounds.isStopAt(cursor)) { initialSongs.remove(origin); }
					else { initialSongs.put(origin, cursor); }
				}
				cursor++;
			}
		}

		private boolean valid() {
			if (box != null) {
				return !box.isRemoved() && level.hasChunkAt(speaker) && box.getSongPlayer().isPlaying()
						&& box.getTheItem().getItem() instanceof EchoChartItem;
			}
			ServerPlayer player = state.server().getPlayerList().getPlayer(preview);
			return player != null && player.level() == level;
		}
	}
}
