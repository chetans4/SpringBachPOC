package com.chetan.sbatch.processor;

import org.springframework.batch.item.ItemProcessor;

import com.chetan.sbatch.entity.User;

public class UserProcessor implements ItemProcessor<User, User> {

	@Override
	public User process(User item) throws Exception {
		System.out.println("Processor starts .................");
		final String firstName = item.getFirstName().toUpperCase();
		final String lastName = item.getLastName().toUpperCase();

		final User transformedUser = new User(firstName, lastName);

		return transformedUser;
	}

}
