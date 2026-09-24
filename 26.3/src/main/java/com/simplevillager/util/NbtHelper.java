package com.simplevillager.util;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

public class NbtHelper {

    private static final ProblemReporter.PathElement PATH = new ProblemReporter.RootFieldPathElement("simplevillager");

    public static TagValueOutput createValueOutput(HolderLookup.Provider provider) {
        ProblemReporter.ScopedCollector collector = new ProblemReporter.ScopedCollector(PATH, org.slf4j.LoggerFactory.getLogger("simplevillager"));
        TagValueOutput output = TagValueOutput.createWithContext(collector, provider);
        collector.close();
        return output;
    }

    public static CompoundTag toTag(TagValueOutput output) {
        return output.buildResult();
    }

    public static ValueInput createValueInput(HolderLookup.Provider provider, CompoundTag tag) {
        ProblemReporter.ScopedCollector collector = new ProblemReporter.ScopedCollector(PATH, org.slf4j.LoggerFactory.getLogger("simplevillager"));
        ValueInput input = TagValueInput.create(collector, provider, tag);
        collector.close();
        return input;
    }
}
