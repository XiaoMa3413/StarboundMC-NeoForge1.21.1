// SPDX-License-Identifier: MPL-2.0
package com.starboundmc.encounter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class RelayDataTest {
    @Test void restartRetainsInstanceProgressOfflineCrewAndTransactionJournal() {
        var data = new RelayData(); data.phase = RelayData.Phase.LEAVING; data.transaction = 2;
        data.origin = new BlockPos(-30, 110, 75); data.homeBody = "sys1:lush";
        data.recovered = true; data.completed = true; data.snapshot.putString("Marker", "modified station");
        var id = UUID.randomUUID(); data.outsideCrew.add(id);
        var saved = data.save(new CompoundTag(), null); var restored = RelayData.load(saved, null);
        assertEquals(data.origin, restored.origin); assertEquals(data.phase, restored.phase);
        assertEquals(2, restored.transaction); assertEquals(data.homeBody, restored.homeBody);
        assertTrue(restored.outsideCrew.contains(id)); assertTrue(restored.completed && restored.recovered);
        assertEquals("modified station", restored.snapshot.getString("Marker"));
        restored.snapshot.putString("Marker", "changed");
        assertEquals("modified station", data.snapshot.getString("Marker"));
    }
    @Test void oldWorldStartsWithUndiscoveredSignalAndNoParticipants() {
        var data = RelayData.load(new CompoundTag(), null);
        assertEquals(RelayData.Phase.UNDISCOVERED, data.phase);
        assertTrue(data.snapshot.isEmpty()); assertTrue(data.outsideCrew.isEmpty()); assertFalse(data.completed);
    }
}
