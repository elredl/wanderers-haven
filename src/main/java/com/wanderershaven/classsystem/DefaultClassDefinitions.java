package com.wanderershaven.classsystem;

import java.util.List;

public final class DefaultClassDefinitions {
	private DefaultClassDefinitions() {
	}

	public static List<ClassDefinition> create() {
		return DefaultClassContent.all().stream()
			.map(ClassContentDefinition::classDefinition)
			.toList();
	}
}
