/**
 * The MIT License
 * Copyright (c) 2016 Regis Leray
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package factoryduke;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import factoryduke.builder.HookBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import org.assertj.core.groups.Tuple;
import org.junit.Before;
import org.junit.Test;

import model.Address;
import model.Role;
import model.User;

public class FactoryDukeTest {

	private Mock mock = mock(Mock.class);

	@Before
	public void initialize() {
		FactoryDuke.load().addAfterHook(o -> mock.afterCall());
		reset(mock);
	}

	@Test
	public void build_consumer_definition() {
		assertThat(FactoryDuke.build(User.class).toOne()).extracting(User::getName, User::getLastName)
				.contains("Malcom", "Scott");

		verify(mock, times(1)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void build_consumer_chained_definition() {
		HookBuilder<User> user = FactoryDuke.build(User.class, new ArrayList<>(Arrays.asList(
				"user_with_fr_address",
				"admin_user")));
		assertThat(user.toOne()).extracting(User::getName, User::getLastName)
				.contains("Malcom", "John");

		verify(mock, times(3)).afterCall(); // todo: check why 3 calls..
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void adress_default() {
		assertThat(FactoryDuke.build(Address.class).toOne()).extracting(Address::getCity, Address::getStreet).contains("Montreal", "prince street");
		verify(mock, times(1)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void factory_call_other_factory() {
		assertThat(FactoryDuke.build(User.class, "user_with_fr_address").toOne()).extracting("name", "addr.city").contains("Malcom", "Paris");
		verify(mock, times(3)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void repeat_1_user() {
		assertThat(FactoryDuke.build(User.class).toOne()).extracting(User::getName, User::getLastName)
				.contains("Malcom", "Scott");

		verify(mock, times(1)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void repeat_2_users() {
		assertThat(FactoryDuke.build(User.class).times(2).toList())
				.hasSize(2)
				.extracting(User::getName, User::getLastName)
				.containsExactly(Tuple.tuple("Malcom", "Scott"), Tuple.tuple("Malcom", "Scott"));

		verify(mock, times(2)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void repeat_2_users_as_set() {
		assertThat(FactoryDuke.build(User.class).times(2).toSet())
				.hasSize(2)
				.extracting(User::getName, User::getLastName)
				.containsExactly(Tuple.tuple("Malcom", "Scott"), Tuple.tuple("Malcom", "Scott"));

		verify(mock, times(2)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void repeat_2_users_with_identifier() {
		assertThat(FactoryDuke.build(User.class, "user_with_fr_address").times(2).toList())
				.hasSize(2)
				.extracting(User::getName, User::getLastName, u -> u.getAddr().getCity())
				.containsExactly(Tuple.tuple("Malcom", "Scott", "Paris"), Tuple.tuple("Malcom", "Scott", "Paris"));

		verify(mock, times(6)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void repeat_2_users_with_override() {
		assertThat(FactoryDuke.build(User.class, u -> {
			u.setRole(Role.USER);
			u.setId(1L);
		}).times(2).toList())
				.hasSize(2)
				.extracting(User::getId, User::getLastName, User::getRole)
				.containsExactly(Tuple.tuple(1L, "Scott", Role.USER), Tuple.tuple(1L, "Scott", Role.USER));

		verify(mock, times(2)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void repeat_2_users_with_identifier_and_override() {
		assertThat(FactoryDuke.build(User.class, "user_with_fr_address", u -> {
			u.getAddr().setCity("NY");
			u.setRole(Role.USER);
		}).times(2).toList())
				.hasSize(2)
				.extracting(User::getName, User::getLastName, User::getRole, u -> u.getAddr().getCity())
				.containsExactly(Tuple.tuple("Malcom", "Scott", Role.USER, "NY"), Tuple.tuple("Malcom", "Scott", Role.USER, "NY"));

		verify(mock, times(6)).afterCall();
		verifyNoMoreInteractions(mock);
	}

	@Test
	public void skip_callback(){
		assertThat(FactoryDuke.build(User.class).skipAfterHook(true).toOne()).extracting(User::getName, User::getLastName)
				.contains("Malcom", "Scott");

		verifyZeroInteractions(mock);
	}

	@Test
	public void no_interaction_with_callback_if_reset(){
		FactoryDuke.reset();

		FactoryDuke.define(User.class, u -> {
			u.setLastName("Scott");
			u.setName("Malcom");
			u.setRole(Role.USER);
		});

		assertThat(FactoryDuke.build(User.class).toOne()).extracting(User::getName, User::getLastName)
				.contains("Malcom", "Scott");

		verifyZeroInteractions(mock);
	}

	@Test
	public void load_specific_factory(){
		FactoryDuke.reset();
		FactoryDuke.load("custom");

		assertThat(FactoryRuntimeHolder.getRuntime().getTemplates()).hasSize(1);

		assertThat(FactoryDuke.build(User.class).toOne())
				.extracting(User::getName, User::getRole)
				.contains("Empty", Role.ADMIN);

	}

	@Test
	public void call_before_and_after_hooks(){
		FactoryDuke.reset();
		FactoryDuke.load().addAfterHook(o -> mock.afterCall()).addBeforeHook(() -> mock.beforeCall());

		assertThat(FactoryDuke.build(User.class).toOne()).isNotNull();

		verify(mock, times(1)).beforeCall();
		verify(mock, times(1)).afterCall();
		verifyNoMoreInteractions(mock);

	}

	@Test
	public void skip_before_hook(){
		FactoryDuke.reset();
		FactoryDuke.load().addAfterHook(o -> mock.afterCall()).addBeforeHook(() -> mock.beforeCall());

		assertThat(FactoryDuke.build(User.class).skipBeforeHook(true).toOne()).isNotNull();

		verify(mock, times(0)).beforeCall();
		verify(mock, times(1)).afterCall();
		verifyNoMoreInteractions(mock);

	}

	@Test
	public void skip_after_hook(){
		FactoryDuke.reset();
		FactoryDuke.load().addAfterHook(o -> mock.afterCall()).addBeforeHook(() -> mock.beforeCall());

		assertThat(FactoryDuke.build(User.class).skipAfterHook(true).toOne()).isNotNull();

		verify(mock, times(1)).beforeCall();
		verify(mock, times(0)).afterCall();
		verifyNoMoreInteractions(mock);

	}

	public interface Mock {
		void beforeCall();
		void afterCall();
	}
}
