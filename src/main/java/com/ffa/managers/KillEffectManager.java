package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

/**
 * KillEffectManager
 * Plays a random particle burst + sound at the death location
 * whenever a rank holder gets a kill.
 *
 * Effects rotate randomly each kill so it never feels the same.
 */
public class KillEffectManager {

    private final FFAPlugin plugin;
    private final Random rng = new Random();

    // Each entry: particle type, count, spread (offsetX/Y/Z), speed
    private record Effect(Particle particle, int count, double spread, double speed,
                          Sound sound, float soundVolume, float soundPitch) {}

    private static final List<Effect> EFFECTS = List.of(
        new Effect(Particle.FLAME,            80,  0.6, 0.15, Sound.ENTITY_BLAZE_DEATH,        1.0f, 1.0f),
        new Effect(Particle.LIGHTNING,         6,  0.8, 0.0,  Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 1.2f),
        new Effect(Particle.EXPLOSION,        12,  1.0, 0.3,  Sound.ENTITY_GENERIC_EXPLODE,    0.8f, 1.0f),
        new Effect(Particle.SOUL_FIRE_FLAME,  60,  0.5, 0.12, Sound.BLOCK_SOUL_SAND_PLACE,     1.0f, 0.8f),
        new Effect(Particle.DRAGON_BREATH,    70,  0.7, 0.1,  Sound.ENTITY_ENDER_DRAGON_DEATH, 0.6f, 1.5f),
        new Effect(Particle.LAVA,             50,  0.5, 0.05, Sound.BLOCK_LAVA_EXTINGUISH,     0.9f, 1.1f),
        new Effect(Particle.END_ROD,          90,  0.8, 0.2,  Sound.ENTITY_ENDERMAN_DEATH,     0.8f, 1.3f),
        new Effect(Particle.WITCH,            60,  0.6, 0.08, Sound.ENTITY_WITCH_DEATH,        1.0f, 0.9f)
    );

    public KillEffectManager(FFAPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Plays a random kill effect at the victim's death location.
     * Only fires if the killer has an active rank.
     *
     * @param killer  the player who got the kill
     * @param victim  the player who died (for death location)
     */
    public void playKillEffect(Player killer, Player victim) {
        if (!plugin.getRankManager().hasRank(killer.getUniqueId())) return;

        Location loc = victim.getLocation().add(0, 1, 0);
        Effect effect = EFFECTS.get(rng.nextInt(EFFECTS.size()));

        // Spawn particles — visible to everyone nearby
        loc.getWorld().spawnParticle(
            effect.particle(),
            loc,
            effect.count(),
            effect.spread(), effect.spread(), effect.spread(),
            effect.speed()
        );

        // Play sound
        loc.getWorld().playSound(loc, effect.sound(), effect.soundVolume(), effect.soundPitch());
    }
}
