package au.com.ausstaker.cage.model;

/**
 * Processing modes
 *
 * fix - generates single position and exits thread, requires observations pre-selected
 * track - generates online track of position, takes new or updates and removes observations dynamically
 *
 * @author edge2ipi (https://github.com/Ausstaker)
 */
public enum MissionMode {
    fix,track
}
