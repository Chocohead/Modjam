package com.chocohead.sm.impl;

import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public class SortedModDependency extends SimpleModDependency {
	@PreMixinClassloaded
	public enum Ordered {
		BEFORE, AFTER;
	}

	private final Ordered ordering;

	public SortedModDependency(String modID, String type, Ordered ordering) {
		super(modID, type);

		this.ordering = ordering;
	}

	public Ordered getOrdering() {
		return ordering;
	}
}