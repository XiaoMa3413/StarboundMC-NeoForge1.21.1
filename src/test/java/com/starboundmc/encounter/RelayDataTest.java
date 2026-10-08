package com.starboundmc.encounter;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class RelayDataTest {
    private static RelayData approach() {
        var data=new RelayData();data.discover("sys1:lush");
        var snapshot=new CompoundTag();snapshot.putString("Marker","modified station");
        data.beginApproach(new BlockPos(-30,110,75),snapshot);return data;
    }
    @Test void newEncounterStartsUndiscoveredButMalformedSaveIsRejected() {
        var data=new RelayData();assertEquals(RelayData.Phase.UNDISCOVERED,data.phase());
        assertTrue(data.outsideCrew().isEmpty());
        assertThrows(IllegalArgumentException.class,()->RelayData.load(new CompoundTag(),null));
        assertEquals(RelayData.Phase.UNDISCOVERED,RelayData.load(data.save(new CompoundTag(),null),null).phase());
    }
    @Test void approachTransitionOwnsDirtyStateAndCannotSkipDiscovery() {
        var fresh=new RelayData();assertThrows(IllegalStateException.class,()->fresh.beginRouting());
        assertThrows(IllegalStateException.class,()->fresh.beginMaterialization(null));
        var data=approach();assertTrue(data.isDirty());assertEquals(0,data.transaction());
        for(int i=1;i<RelayEncounter.APPROACH_TICKS;i++) assertFalse(data.advanceApproach());
        assertTrue(data.advanceApproach());data.cancelApproach();
        assertEquals(RelayData.Phase.AVAILABLE,data.phase());assertEquals(0,data.approachTicks());
    }
    @Test void restartRetainsCrewJournalAndSnapshotWithoutMutableAliases() {
        var data=approach();var id=UUID.randomUUID();data.setCrewOutside(id,true);
        var saved=data.save(new CompoundTag(),null);saved.putString("Phase","LEAVING");saved.putInt("Transaction",2);
        saved.putBoolean("Recovered",true);saved.putBoolean("Completed",true);
        var restored=RelayData.load(saved,null);
        assertEquals(2,restored.transaction());assertEquals(RelayData.Phase.LEAVING,restored.phase());
        assertEquals(data.origin(),restored.origin());assertTrue(restored.outsideCrew().contains(id));
        assertThrows(UnsupportedOperationException.class,()->restored.outsideCrew().clear());
        restored.snapshot().putString("Marker","changed");
        assertEquals("modified station",restored.snapshot().getString("Marker"));
    }
    @Test void invalidPhaseJournalOrCrewNeverBecomesUndiscovered() {
        for(var phase:new String[]{"BOGUS","UNDISCOVERED","MATERIALIZING","LEAVING","ERROR"}) {
            var tag=approach().save(new CompoundTag(),null);tag.putString("Phase",phase);
            assertThrows(IllegalArgumentException.class,()->RelayData.load(tag,null),phase);
        }
        var crew=approach().save(new CompoundTag(),null);var list=new ListTag();list.add(StringTag.valueOf("broken-uuid"));crew.put("OutsideCrew",list);
        assertThrows(IllegalArgumentException.class,()->RelayData.load(crew,null));
        var journal=approach().save(new CompoundTag(),null);journal.putInt("Transaction",2);
        assertThrows(IllegalArgumentException.class,()->RelayData.load(journal,null));
    }
}
