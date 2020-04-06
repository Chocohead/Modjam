package com.chocohead.sm.impl;

public class SortedModDependency extends SimpleModDependency {
	public enum Ordered {
		BEFORE, AFTER;
	}

	private final Ordered ordering;

	public SortedModDependency(String modID, Ordered ordering) {
		super(modID);

		this.ordering = ordering;
	}

	public Ordered getOrdering() {
		return ordering;
	}
}