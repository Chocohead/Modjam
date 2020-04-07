package com.chocohead.sm.api;

import net.fabricmc.loader.api.metadata.Person;

import com.chocohead.sm.loader.PreMixinClassloaded;

@PreMixinClassloaded
public interface DescriptivePerson extends Person {
	@Override
	PersonalContact getContact();
}