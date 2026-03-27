package com.wanderershaven.classsystem;

@FunctionalInterface
public interface ClassPrerequisite {
	boolean isMet(PrerequisiteContext context);
}
