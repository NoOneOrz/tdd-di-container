package geektime.tdd.di;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Nested
public class InjectionTest {
    private Dependency dependency = mock(Dependency.class);
    private Provider<Dependency> dependencyProvider = mock(Provider.class);
    private ParameterizedType dependencyProviderType;
    private Context context = mock(Context.class);


    @BeforeEach
    public void setup() throws NoSuchFieldException {
        dependencyProviderType = (ParameterizedType) InjectionTest.class.getDeclaredField("dependencyProvider").getGenericType();
        when(context.get(eq(Dependency.class))).thenReturn(Optional.of(dependency));
        when(context.get(eq(dependencyProviderType))).thenReturn(Optional.of(dependencyProvider));
    }

    @Nested
    public class ConstructorInjection {

        @Nested
        class Injection {

            static class DefaultConstructor {
            }

            @Test
            public void should_call_default_constructor_if_no_inject_constructor() {
                DefaultConstructor instance = new InjectionProvider<>(DefaultConstructor.class).get(context);

                assertNotNull(instance);
            }

            static class InjectConstructor {
                Dependency dependency;

                @Inject
                public InjectConstructor(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_constructor() {
                InjectConstructor instance = new InjectionProvider<>(InjectConstructor.class).get(context);

                assertSame(dependency, instance.dependency);
            }

            @Test
            public void should_include_dependency_from_inject_constructor() {
                InjectionProvider<InjectConstructor> provider = new InjectionProvider<>(InjectConstructor.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            @Test
            public void should_include_provider_type_from_inject_constructor() {
                InjectionProvider<ProviderInjectConstructor> provider = new InjectionProvider<>(ProviderInjectConstructor.class);
                assertArrayEquals(new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new));
            }

            static class ProviderInjectConstructor {
                Provider<Dependency> dependency;

                @Inject
                public ProviderInjectConstructor(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_provider_via_inject_constructor() {
                ProviderInjectConstructor instance = new InjectionProvider<>(ProviderInjectConstructor.class).get(context);

                assertSame(dependencyProvider, instance.dependency);

            }
        }

        @Nested
        class IllegalInjectConstructors {

            static class MultiInjectConstructor {
                @Inject
                public MultiInjectConstructor(String name, Double value) {
                }

                @Inject
                public MultiInjectConstructor(String name) {
                }
            }

            static class NoInjectConstructorNorDefaultConstructor {
                public NoInjectConstructorNorDefaultConstructor(String name) {
                }
            }

            @Test
            public void should_throw_exception_if_component_is_abstract() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(ConstructorInjection.AbstractComponent.class));
            }

            @Test
            public void should_throw_exception_if_component_is_interface() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(Component.class));
            }

            @Test
            public void should_throw_exception_if_multi_inject_constructors_provided() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(MultiInjectConstructor.class));
            }

            @Test
            public void should_throw_exception_if_no_inject_nor_default_constructor_provided() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(NoInjectConstructorNorDefaultConstructor.class));
            }
        }

        abstract class AbstractComponent implements Component {
            @Inject
            public AbstractComponent() {
            }
        }

    }

    @Nested
    public class FieldInjection {
        @Nested
        class Injection {
            static class ComponentWithFiledInjection {
                @Inject
                Dependency dependency;
            }

            static class SubclassWithFiledInjection extends ComponentWithFiledInjection {
            }

            @Test
            public void should_inject_dependency_via_filed() {
                ComponentWithFiledInjection component = new InjectionProvider<>(ComponentWithFiledInjection.class).get(context);

                assertSame(dependency, component.dependency);

            }

            @Test
            public void should_inject_dependency_via_superclass_inject_filed() {
                SubclassWithFiledInjection component = new InjectionProvider<>(SubclassWithFiledInjection.class).get(context);

                assertSame(dependency, component.dependency);
            }

            @Test
            public void should_include_dependency_from_field_dependency() {
                InjectionProvider<ComponentWithFiledInjection> provider = new InjectionProvider<>(ComponentWithFiledInjection.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            @Test
            public void should_include_provider_type_from_inject_field() {
                InjectionProvider<ProviderInjectField> provider = new InjectionProvider<>(ProviderInjectField.class);
                assertArrayEquals(new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new));
            }


            static class ProviderInjectField {
                @Inject
                Provider<Dependency> dependency;

            }

            @Test
            public void should_inject_provider_via_inject_field() {
                ProviderInjectField instance = new InjectionProvider<>(ProviderInjectField.class).get(context);

                assertSame(dependencyProvider, instance.dependency);

            }
        }


        @Nested
        class IllegalInjectFields {
            static class FinalInjectFiled {
                @Inject
                final Dependency dependency = null;
            }

            @Test
            public void should_throw_exception_if_inject_filed_is_final() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(FinalInjectFiled.class));
            }
        }
    }

    @Nested
    public class MethodInjection {
        @Nested
        class Injection {
            static class InjectMethodWithNoDependency {
                boolean called = false;

                @Inject
                void install() {
                    this.called = true;
                }
            }

            @Test
            public void should_call_inject_method_even_if_no_dependency_declared() {
                InjectMethodWithNoDependency component = new InjectionProvider<>(InjectMethodWithNoDependency.class).get(context);
                assertTrue(component.called);
            }

            static class InjectMethodWithDependency {
                Dependency dependency;

                @Inject
                void install(Dependency dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_dependency_via_inject_method() {
                InjectMethodWithDependency component = new InjectionProvider<>(InjectMethodWithDependency.class).get(context);
                assertSame(dependency, component.dependency);

            }

            static class SuperClassWithInjectMethod {
                int superCalled = 0;

                @Inject
                void install() {
                    superCalled++;
                }
            }

            static class SubclassWithInjectMethod extends SuperClassWithInjectMethod {
                int subCalled = 0;

                @Inject
                void installAnother() {
                    subCalled = superCalled + 1;
                }
            }

            @Test
            public void should_inject_dependencies_via_inject_method_from_superclass() {
                SubclassWithInjectMethod component = new InjectionProvider<>(SubclassWithInjectMethod.class).get(context);
                assertEquals(1, component.superCalled);
                assertEquals(2, component.subCalled);
            }

            static class SubclassOverrideSupperClassWithInject extends SuperClassWithInjectMethod {
                @Inject
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_only_call_once_if_subclass_override_superclass_inject() {
                SubclassOverrideSupperClassWithInject component = new InjectionProvider<>(SubclassOverrideSupperClassWithInject.class).get(context);

                assertEquals(1, component.superCalled);
            }

            static class SubclassOverrideSupperClassWithNoInject extends SuperClassWithInjectMethod {
                void install() {
                    super.install();
                }
            }

            @Test
            public void should_not_call_inject_method_if_override_with_no_inject() {

                SubclassOverrideSupperClassWithNoInject component = new InjectionProvider<>(SubclassOverrideSupperClassWithNoInject.class).get(context);

                assertEquals(0, component.superCalled);
            }

            @Test
            public void should_include_dependencies_from_inject_method() {
                InjectionProvider<InjectMethodWithDependency> provider = new InjectionProvider<>(InjectMethodWithDependency.class);
                assertArrayEquals(new Class<?>[]{Dependency.class}, provider.getDependencies().toArray(Class<?>[]::new));
            }

            @Test
            public void should_include_provider_type_from_inject_method() {
                InjectionProvider<ProviderInjectMethod> provider = new InjectionProvider<>(ProviderInjectMethod.class);
                assertArrayEquals(new Type[]{dependencyProviderType}, provider.getDependencyTypes().toArray(Type[]::new));
            }

            static class ProviderInjectMethod {
                Provider<Dependency> dependency;

                @Inject
                void install(Provider<Dependency> dependency) {
                    this.dependency = dependency;
                }
            }

            @Test
            public void should_inject_provider_via_inject_method() {
                ProviderInjectMethod instance = new InjectionProvider<>(ProviderInjectMethod.class).get(context);

                assertSame(dependencyProvider, instance.dependency);

            }
        }

        @Nested
        class IllegalInjectMethods {
            static class InjectMethodWithTypeParameter {
                @Inject
                <T> void install() {
                }
            }

            @Test
            public void should_throw_exception_if_inject_method_has_type_parameter() {
                assertThrows(IllegalComponentException.class, () -> new InjectionProvider<>(InjectMethodWithTypeParameter.class));
            }
        }
    }
}
