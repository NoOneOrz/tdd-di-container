package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.internal.util.collections.Sets;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.*;

@Nested
class ContextTest {
    ContextConfig config;

    @BeforeEach
    public void setup() {
        config = new ContextConfig();
    }


    @Nested
    public class DependenciesSelection {

        @Nested
        public class ProviderType {
        }

        @Nested
        public class Qualifier {
        }
    }

    @Nested
    public class LifecycleManagement {

    }

    @Nested
    public class TypeBinding {
        @Test
        public void should_bind_type_to_a_specific_instance() {

            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();
            assertSame(instance, context.get(ComponentRef.of(TestComponent.class)).get());

        }

        @Test
        public void should_return_empty_if_component_not_defined() {
            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @ParameterizedTest(name = "supporting {0}")
        @MethodSource
        public void should_bind_type_to_an_injectable_component(Class<? extends TestComponent> componentType) {
            Dependency dependency = new Dependency() {
            };
            config.bind(Dependency.class, dependency);
            config.bind(TestComponent.class, componentType);

            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));

            assertTrue(component.isPresent());
            assertSame(dependency, component.get().dependency());
        }

        public static Stream<Arguments> should_bind_type_to_an_injectable_component() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", ConstructorInjection.class)),
                    Arguments.of(Named.of("Filed Injection", FieldInjection.class)),
                    Arguments.of(Named.of("Method Injection", MethodInjection.class))
            );
        }

        static class ConstructorInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public ConstructorInjection(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency dependency() {
                return dependency;
            }
        }

        static class FieldInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            void install(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency dependency() {
                return dependency;
            }
        }

        static class MethodInjection implements TestComponent {
            private Dependency dependency;

            @Inject
            public void install(Dependency dependency) {
                this.dependency = dependency;
            }

            public Dependency dependency() {
                return dependency;
            }
        }

        @Test
        public void should_retrieve_empty_for_unbind_type() {
            Context context = config.getContext();
            Optional<TestComponent> component = context.get(ComponentRef.of(TestComponent.class));
            assertTrue(component.isEmpty());
        }

        @Test
        public void should_retrieve_bind_type_as_provider() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            Provider<TestComponent> provider = context.get(new ComponentRef<Provider<TestComponent>>() {
            }).get();
            assertSame(instance, provider.get());
        }

        @Test
        public void should_not_retrieve_bind_type_as_unsupported_container() {
            TestComponent instance = new TestComponent() {
            };
            config.bind(TestComponent.class, instance);

            Context context = config.getContext();

            assertFalse(context.get(new ComponentRef<List<TestComponent>>() {
            }).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_bind_instance_with_multi_qualifiers() {
                TestComponent instance = new TestComponent() {
                };
                config.bind(TestComponent.class, instance, new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

                Context context = config.getContext();

                TestComponent chosenOne = context.get(ComponentRef.of(TestComponent.class, new NamedLiteral("ChosenOne"))).get();
                TestComponent skywalker = context.get(ComponentRef.of(TestComponent.class, new SkywalkerLiteral())).get();

                assertSame(instance, chosenOne);
                assertSame(instance, skywalker);
            }

            @Test
            public void should_bind_component_with_multi_qualifiers() {
                Dependency dependency = new Dependency() {
                };
                config.bind(Dependency.class, dependency);
                config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class,
                        new NamedLiteral("ChosenOne"), new SkywalkerLiteral());

                Context context = config.getContext();

                InjectionTest.ConstructorInjection.Injection.InjectConstructor chosenOne = context.get(ComponentRef.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new NamedLiteral("ChosenOne"))).get();
                InjectionTest.ConstructorInjection.Injection.InjectConstructor skywalker = context.get(ComponentRef.of(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new SkywalkerLiteral())).get();

                assertSame(dependency, chosenOne.dependency);
                assertSame(dependency, skywalker.dependency);
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_instance() {
                TestComponent instance = new TestComponent() {
                };
                assertThrows(IllegalComponentException.class, () -> config.bind(TestComponent.class, instance, new TestLiteral()));
            }

            @Test
            public void should_throw_exception_if_illegal_qualifier_given_to_component() {
                assertThrows(IllegalComponentException.class, () -> config.bind(InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, InjectionTest.ConstructorInjection.Injection.InjectConstructor.class, new TestLiteral()));
            }
        }
    }

    @Nested
    public class DependencyCheck {
        @ParameterizedTest
        @MethodSource
        public void should_throw_exception_if_dependency_not_found(Class<? extends TestComponent> component) {
            config.bind(TestComponent.class, component);

            DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

            assertEquals(Dependency.class, exception.getDependency().type());
            assertEquals(TestComponent.class, exception.getComponent().type());

        }

        public static Stream<Arguments> should_throw_exception_if_dependency_not_found() {
            return Stream.of(
                    Arguments.of(Named.of("Constructor Injection", MissingDependencyConstructor.class)),
                    Arguments.of(Named.of("Filed Injection", MissingDependencyField.class)),
                    Arguments.of(Named.of("Method Injection", MissingDependencyMethod.class)),
                    Arguments.of(Named.of("Provider in Inject Constructor", MissingDependencyProviderConstructor.class)),
                    Arguments.of(Named.of("Provider in Inject Filed", MissingDependencyProviderFiled.class)),
                    Arguments.of(Named.of("Provider in Inject Method", MissingDependencyProviderMethod.class))
            );
        }

        static class MissingDependencyConstructor implements TestComponent {
            @Inject
            public MissingDependencyConstructor(Dependency dependency) {
            }
        }

        static class MissingDependencyField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class MissingDependencyMethod implements TestComponent {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class MissingDependencyProviderConstructor implements TestComponent {
            @Inject
            public MissingDependencyProviderConstructor(Provider<Dependency> dependency) {
            }
        }

        static class MissingDependencyProviderFiled implements TestComponent {
            @Inject
            Provider<Dependency> dependency;
        }

        static class MissingDependencyProviderMethod implements TestComponent {
            @Inject
            void install(Provider<Dependency> dependency) {
            }
        }

        @ParameterizedTest(name = "cyclic dependency between {0} and {1}")
        @MethodSource
        public void should_throw_exception_if_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                        Class<? extends Dependency> dependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            Set<Class<?>> classes = Sets.newSet(exception.getComponents());

            assertEquals(2, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
        }

        public static Stream<Arguments> should_throw_exception_if_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (Named dependency : List.of(Named.of("Inject Constructor", CyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", CyclicDependencyInjectField.class),
                        Named.of("Inject Method", CyclicDependencyInjectMethod.class))) {
                    arguments.add(Arguments.of(component, dependency));
                }
            }
            return arguments.stream();
        }

        static class CyclicComponentInjectConstructor implements TestComponent {
            @Inject
            public CyclicComponentInjectConstructor(Dependency dependency) {
            }
        }

        static class CyclicComponentInjectField implements TestComponent {
            @Inject
            Dependency dependency;
        }

        static class CyclicComponentInjectMethod implements TestComponent {
            @Inject
            public void install(Dependency dependency) {
            }
        }

        static class CyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public CyclicDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class CyclicDependencyInjectField implements Dependency {
            @Inject
            TestComponent component;
        }

        static class CyclicDependencyInjectMethod implements Dependency {
            @Inject
            public void install(TestComponent component) {
            }
        }


        @ParameterizedTest(name = "indirect cyclic dependency between {0},{1} and {2}")
        @MethodSource
        public void should_throw_exception_if_transitive_cyclic_dependencies_found(Class<? extends TestComponent> component,
                                                                                   Class<? extends Dependency> dependency,
                                                                                   Class<? extends AnotherDependency> anotherDependency) {
            config.bind(TestComponent.class, component);
            config.bind(Dependency.class, dependency);
            config.bind(AnotherDependency.class, anotherDependency);

            CyclicDependenciesFoundException exception = assertThrows(CyclicDependenciesFoundException.class, () -> config.getContext());

            List<Class<?>> classes = Arrays.asList(exception.getComponents());
            assertEquals(3, classes.size());
            assertTrue(classes.contains(TestComponent.class));
            assertTrue(classes.contains(Dependency.class));
            assertTrue(classes.contains(AnotherDependency.class));

        }

        public static Stream<Arguments> should_throw_exception_if_transitive_cyclic_dependencies_found() {
            List<Arguments> arguments = new ArrayList<>();
            for (Named component : List.of(Named.of("Inject Constructor", CyclicComponentInjectConstructor.class),
                    Named.of("Inject Field", CyclicComponentInjectField.class),
                    Named.of("Inject Method", CyclicComponentInjectMethod.class))) {
                for (Named dependency : List.of(Named.of("Inject Constructor", IndirectCyclicDependencyInjectConstructor.class),
                        Named.of("Inject Field", IndirectCyclicDependencyInjectField.class),
                        Named.of("Inject Method", IndirectCyclicDependencyInjectMethod.class))) {
                    for (Named anotherDependency : List.of(Named.of("Inject Constructor", IndirectCyclicAnotherDependencyInjectConstructor.class),
                            Named.of("Inject Field", IndirectCyclicAnotherDependencyInjectField.class),
                            Named.of("Inject Method", IndirectCyclicAnotherDependencyInjectMethod.class))) {
                        arguments.add(Arguments.of(component, dependency, anotherDependency));
                    }
                }
            }
            return arguments.stream();
        }

        static class IndirectCyclicDependencyInjectConstructor implements Dependency {
            @Inject
            public IndirectCyclicDependencyInjectConstructor(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicDependencyInjectField implements Dependency {
            @Inject
            AnotherDependency anotherDependency;
        }

        static class IndirectCyclicDependencyInjectMethod implements Dependency {
            @Inject
            public void install(AnotherDependency anotherDependency) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectConstructor implements AnotherDependency {
            @Inject
            public IndirectCyclicAnotherDependencyInjectConstructor(TestComponent component) {
            }
        }

        static class IndirectCyclicAnotherDependencyInjectField implements AnotherDependency {
            @Inject
            TestComponent component;
        }

        static class IndirectCyclicAnotherDependencyInjectMethod implements AnotherDependency {
            @Inject
            public void install(TestComponent component) {
            }
        }

        static class CyclicDependencyProviderConstructor implements Dependency {
            @Inject
            public CyclicDependencyProviderConstructor(Provider<TestComponent> component) {
            }
        }

        @Test
        public void should_not_throw_exception_if_cyclic_dependency_via_provider() {
            config.bind(TestComponent.class, CyclicComponentInjectConstructor.class);
            config.bind(Dependency.class, CyclicDependencyProviderConstructor.class);

            Context context = config.getContext();
            assertTrue(context.get(ComponentRef.of(TestComponent.class)).isPresent());
        }

        @Nested
        public class WithQualifier {
            @Test
            public void should_throw_exception_if_dependency_with_qualifier_not_found() {
                config.bind(Dependency.class, new Dependency() {
                });
                config.bind(InjectConstructor.class, InjectConstructor.class, new NamedLiteral("Owner"));

                DependencyNotFoundException exception = assertThrows(DependencyNotFoundException.class, () -> config.getContext());

                assertEquals(new Component(InjectConstructor.class, new NamedLiteral("Owner")), exception.getComponent());
                assertEquals(new Component(Dependency.class, new SkywalkerLiteral()), exception.getDependency());

            }

            static class InjectConstructor {
                @Inject
                public InjectConstructor(@Skywalker Dependency dependency) {

                }
            }

            static class SkywalkerDependency implements Dependency {
                @Inject
                public SkywalkerDependency(@jakarta.inject.Named("ChosenOne") Dependency dependency) {

                }
            }

            static class NotCyclicDependency implements Dependency {
                @Inject
                public NotCyclicDependency(@Skywalker Dependency dependency) {

                }
            }

            @Test
            public void should_not_throw_cyclic_exception_if_component_with_same_type_taged_with_different_qualifier() {
                Dependency instance = new Dependency() {
                };
                config.bind(Dependency.class, instance, new NamedLiteral("ChosenOne"));
                config.bind(Dependency.class, SkywalkerDependency.class, new SkywalkerLiteral());
                config.bind(Dependency.class, NotCyclicDependency.class);

                assertDoesNotThrow(() -> config.getContext());
            }

        }
    }
}

record NamedLiteral(String value) implements jakarta.inject.Named {
    @Override
    public Class<? extends Annotation> annotationType() {
        return jakarta.inject.Named.class;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof jakarta.inject.Named named) return Objects.equals(value, named.value());
        return false;
    }

    @Override
    public int hashCode() {
        return "value".hashCode() * 127 ^ value.hashCode();
    }
}


@java.lang.annotation.Documented
@java.lang.annotation.Retention(RUNTIME)
@jakarta.inject.Qualifier
@interface Skywalker {
}

record SkywalkerLiteral() implements Skywalker {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Skywalker.class;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Skywalker;
    }
}

record TestLiteral() implements Test {
    @Override
    public Class<? extends Annotation> annotationType() {
        return Test.class;
    }
}
