package com.chocohead.sm.api;

import net.fabricmc.loader.api.metadata.Person;

public interface DescriptivePerson extends Person {
	@Override
	PersonalContact getContact();
}