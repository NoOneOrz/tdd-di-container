package geektime.tdd.di;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class CyclicDependenciesFoundException extends RuntimeException {
    private Set<Class<?>> components = new HashSet<>();


    public CyclicDependenciesFoundException(Stack<Component> visiting) {
        visiting.forEach(component -> components.add(component.type()));
    }

    public Class<?>[] getComponents() {
        return components.toArray(Class<?>[]::new);
    }
}
