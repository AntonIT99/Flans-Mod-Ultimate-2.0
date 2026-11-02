package com.wolffsmod.npcs.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;

/** Exposes the evaluator and search budget so NPC workers can use an independent path finder. */
@Mixin(PathFinder.class)
public interface PathFinderAccessor
{
    @Accessor("nodeEvaluator")
    NodeEvaluator wolffsmodnpcsNodeEvaluator();

    @Accessor("maxVisitedNodes")
    int wolffsmodnpcsMaxVisitedNodes();
}
