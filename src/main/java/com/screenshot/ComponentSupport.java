package com.screenshot;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

public class ComponentSupport {

    public static List<Component> getComponentsByName(Component component, String name) {
        List<Component> matchingComponents = new ArrayList<>();
        searchComponentsByName(component, name, matchingComponents);
        return matchingComponents;
    }

    private static void searchComponentsByName(Component component, String name, List<Component> matchingComponents) {
        if (component.getName() != null && component.getName().equals(name)) {
            matchingComponents.add(component);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                searchComponentsByName(child, name, matchingComponents);
            }
        }
    }
}