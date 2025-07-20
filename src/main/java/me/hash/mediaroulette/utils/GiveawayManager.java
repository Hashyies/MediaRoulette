package me.hash.mediaroulette.utils;

import me.hash.mediaroulette.service.GiveawayService;
import me.hash.mediaroulette.model.Giveaway;

import java.util.List;

/**
 * Manages giveaway lifecycle and startup recovery
 */
public class GiveawayManager {
    private static GiveawayService giveawayService;
    
    public static void initialize() {
        giveawayService = new GiveawayService();
        
        // Check for giveaways that ended while bot was offline
        resumeGiveaways();
    }
    
    /**
     * Resume giveaways that may have ended while bot was offline
     */
    private static void resumeGiveaways() {
        try {
            List<Giveaway> activeGiveaways = giveawayService.getActiveGiveaways();
            
            int endedCount = 0;
            for (Giveaway giveaway : activeGiveaways) {
                if (giveaway.isExpired() && !giveaway.isCompleted()) {
                    System.out.println("Ending giveaway that expired while offline: " + giveaway.getId());
                    giveawayService.endGiveaway(giveaway);
                    endedCount++;
                }
            }
            
            if (endedCount > 0) {
                System.out.println("Resumed and ended " + endedCount + " expired giveaways");
            }
            
        } catch (Exception e) {
            System.err.println("Error resuming giveaways: " + e.getMessage());
        }
    }
    
    public static GiveawayService getGiveawayService() {
        return giveawayService;
    }
    
    public static void shutdown() {
        if (giveawayService != null) {
            giveawayService.shutdown();
        }
    }
}