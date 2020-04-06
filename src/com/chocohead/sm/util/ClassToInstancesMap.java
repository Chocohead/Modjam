package com.chocohead.sm.util;

import java.util.List;
import java.util.Map;

public interface ClassToInstancesMap<T> extends Map<Class<? extends T>, List<? extends T>> {
	<R extends T> List<R> get(Class<R> type);

	default <R extends T> List<R> getOrDefault(Class<R> type, List<R> defaultValue) {
		List<R> v;
        return (v = get(type)) != null || containsKey(type) ? v : defaultValue;
    }

	@Override
	@Deprecated //Probably don't want to be using this over the other get
	List<? extends T> get(Object key);

	@Override
	@Deprecated //Probably don't want to be using this over the other getOrDefault
	default List<? extends T> getOrDefault(Object key, List<? extends T> defaultValue) {
		return Map.super.getOrDefault(key, defaultValue);
	}

	<R extends T> List<R> put(Class<R> type, List<R> value);
}