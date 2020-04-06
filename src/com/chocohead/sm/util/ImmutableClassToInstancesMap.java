package com.chocohead.sm.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ImmutableClassToInstancesMap<T> extends ForwardingMap<Class<? extends T>, List<? extends T>> implements ClassToInstancesMap<T> {
	private static final ImmutableClassToInstancesMap<Object> EMPTY = new ImmutableClassToInstancesMap<>(ImmutableMap.of());

	@SuppressWarnings("unchecked")
	public static <T> ImmutableClassToInstancesMap<T> empty() {
		return (ImmutableClassToInstancesMap<T>) EMPTY;
	}

	public static <T, R extends T> ImmutableClassToInstancesMap<T> of(Class<R> type, List<R> value) {
		return new ImmutableClassToInstancesMap<>(ImmutableMap.of(type, value));
	}

	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	public static final class Builder<T> {
		private final ImmutableMap.Builder<Class<? extends T>, List<? extends T>> builder = ImmutableMap.builder();
		private final Map<Class<? extends T>, List<? extends T>> typeMap = new IdentityHashMap<>();
		private boolean immutify = false;

		Builder() {
		}

		public <R extends T> Builder<T> put(Class<R> key, R value) {
			@SuppressWarnings("unchecked")
			List<R> list = (List<R>) typeMap.get(key);

			if (list == null) {
				list = new ArrayList<>();
				put(key, list);
			}

			list.add(value);
			return this;
		}

		public <R extends T> Builder<T> put(Class<R> key, List<R> value) {
			typeMap.put(key, value);
			builder.put(key, value);
			return this;
		}

		public <R extends T> Builder<T> putAll(Map<Class<? extends R>, ? extends List<R>> map) {
			for (Entry<Class<? extends R>, ? extends List<R>> entry : map.entrySet()) {
				typeMap.put(entry.getKey(), entry.getValue());
				builder.put(entry);
			}

			return this;
		}

		public Builder<T> immutified() {
			immutify = true;
			return this;
		}

		public ImmutableClassToInstancesMap<T> build() {
			Map<Class<? extends T>, List<? extends T>> map = builder.build();

			if (map.isEmpty()) {
				return empty();
			} else {
				return new ImmutableClassToInstancesMap<>(immutify ? immutify(map) : map);
			}
		}

		private static <T> Map<Class<? extends T>, List<? extends T>> immutify(Map<Class<? extends T>, List<? extends T>> map) {
			ImmutableMap.Builder<Class<? extends T>, List<? extends T>> builder = ImmutableMap.builder();

			for (Entry<Class<? extends T>, List<? extends T>> entry : map.entrySet()) {
				builder.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
			}

			return builder.build();
		}
	}

	public static <T, R extends T> ImmutableClassToInstancesMap<T> copyOf(Map<Class<? extends R>, ? extends List<R>> map) {
		return new Builder<T>().putAll(map).build();
	}


	private final Map<Class<? extends T>, List<? extends T>> delegate;

	private ImmutableClassToInstancesMap(Map<Class<? extends T>, List<? extends T>> delegate) {
		this.delegate = delegate;
	}

	@Override
	protected Map<Class<? extends T>, List<? extends T>> delegate() {
		return delegate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends T> List<R> get(Class<R> type) {
		return (List<R>) delegate.get(Objects.requireNonNull(type));
	}

	@Deprecated
	@Override
	public <R extends T> List<R> put(Class<R> type, List<R> value) {
		throw new UnsupportedOperationException();
	}

}