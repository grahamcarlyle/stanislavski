package com.timgroup.stanislavski.magic.matchers;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.timgroup.stanislavski.interpreters.AddressesProperty;
import com.timgroup.stanislavski.interpreters.MethodCallInterpreter;
import com.timgroup.stanislavski.matchers.AMatcher;
import com.timgroup.stanislavski.reflection.MethodCall;

import static org.hamcrest.MatcherAssert.assertThat;

public class MagicJavaBeanMatcherTest {

    public static final class Person {
        public final String name;
        private final int age;
        public final boolean hasSuperpowers;

        public Person(String name, int age, boolean hasSuperpowers) {
            this.name = name;
            this.age = age;
            this.hasSuperpowers = hasSuperpowers;
        }
        
        public int getAge() {
            return age;
        }
    }
    
    public static interface PersonMatcher extends Matcher<Person> {
        public PersonMatcher withName(String name);
        public PersonMatcher with_the_name_of(String name);
        public PersonMatcher withName(Matcher<? super String> nameMatcher);
        
        @AddressesProperty("name") @MatchesWith(ForenameSurnameMatcher.class)
        public PersonMatcher named(String forename, String surname);
        @AddressesProperty("age") public PersonMatcher aged(int age);
        @AddressesProperty("age") public PersonMatcher aged(Matcher<? super Integer> ageMatcher);
        
        @AddressesProperty("hasSuperpowers") @ChecksBoolean(true)
        public PersonMatcher is_a_superhero();
        
        @AddressesProperty("hasSuperpowers") @ChecksBoolean(false)
        public PersonMatcher is_a_civilian();
    }
    
    public static final class ForenameSurnameMatcher implements MethodCallInterpreter<Matcher<?>> {
        @Override
        public Matcher<?> apply(MethodCall methodCall) {
            String fullName = Joiner.on(" ").join(methodCall.argumentValues());
            return JavaBeanPropertyMatcherMaker.forAnyClass()
                                               .make(methodCall, Matchers.is(fullName));
        }
    }
    
    private final Person person = new Person("Julius Caesar", 42, false);

    private PersonMatcher matcher() {
        return MagicJavaBeanMatcher.matching(Person.class)
                .using(PersonMatcher.class);
    }
    
    @Test public void
    matches_a_property_identified_by_the_method_name() {
        assertThat(matcher().withName(Matchers.equalTo("Julius Caesar")),
                   AMatcher.that_matches(person)
                           .with_the_description("A Person whose name is \"Julius Caesar\""));
    }
    
    @Test public void
    dsecribes_multiple_properties() {
        assertThat(matcher().withName("Julius Caesar").aged(42),
                   AMatcher.that_matches(person)
                           .with_the_description("A Person whose name is \"Julius Caesar\" and whose age is <42>"));
    }
    
    @Test public void
    supports_underscore_formatted_method_names() {
        assertThat(matcher().with_the_name_of("Julius Caesar"),
                   AMatcher.that_matches(person)
                           .with_the_description("A Person whose name is \"Julius Caesar\""));
    }
    @Test public void
    converts_literal_parameters_into_matchers() {
        assertThat(matcher().withName("Julius Caesar"),
                   AMatcher.that_matches(person)
                           .with_the_description("A Person whose name is \"Julius Caesar\""));
    }
    
    @Test public void
    can_override_a_method_name_with_an_alias() {
        assertThat(matcher().aged(42),
                   AMatcher.that_matches(person)
                           .with_the_description("A Person whose age is <42>"));
    }
    
    @Test public void
    supports_special_cases_using_the_matches_with_annotation() {
        assertThat(matcher().named("Julius", "Caesar"),
                AMatcher.that_matches(person)
                        .with_the_description("A Person whose name is \"Julius Caesar\""));
    }
    
    @Test public void
    gives_a_meaningful_mismatch_description() {
        assertThat(matcher().withName("Tiberius Caesar").aged(17),
                AMatcher.that_fails_to_match(person)
                        .with_the_mismatch_description("was a Person whose name was \"Julius Caesar\" and whose age was <42>"));
    }
    
    @Test public void
    supports_shorthand_for_checking_booleans() {
        Person elkman = new Person("Elkman", 37, true);
        assertThat(matcher().is_a_civilian(), AMatcher.that_matches(person));
        assertThat(matcher().is_a_superhero(), AMatcher.that_matches(elkman));
        assertThat(matcher().is_a_civilian(), AMatcher.that_fails_to_match(elkman)
                                                      .with_the_mismatch_description("was a Person whose hasSuperpowers was <true>"));
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test public void
    does_the_right_thing_when_given_the_wrong_class() {
        Object theWrongSortOfThing = "The Wrong Sort Of Thing";
        assertThat((Matcher) matcher().is_a_civilian(), (Matcher) AMatcher.that_fails_to_match(theWrongSortOfThing)
                   .with_the_mismatch_description(String.format("was not a <%s>", Person.class)));
    }
    
    @Test public void
    can_match_null_literals() {
        Person nemo = new Person(null, 0, false);
        assertThat(matcher().with_the_name_of(null), AMatcher.that_matches(nemo));
    }
}
