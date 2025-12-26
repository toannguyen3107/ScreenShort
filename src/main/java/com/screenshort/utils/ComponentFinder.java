package com.screenshort.utils;

import java.awt.Component;
import java.awt.Container;
import java.awt.MouseInfo;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * Utilities for finding Swing components by name or position.
 */
public final class ComponentFinder {

    private ComponentFinder() {
        // Utility class - prevent instantiation
    }

    /**
     * Finds a component with the specified name under the current mouse position.
     *
     * @param name   The component name to search for
     * @param parent The parent component to search within
     * @return The found component, or null if not found
     */
    public static Component findComponentUnderMouse(String name, Component parent) {
        if (parent == null) {
            return null;
        }
        try {
            Point location = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(location, parent);
            Component deepest = SwingUtilities.getDeepestComponentAt(parent, location.x, location.y);

            if (deepest != null) {
                Component current = deepest;
                while (current != null) {
                    if (Objects.equals(name, current.getName())) {
                        return current;
                    }
                    if (current == parent) {
                        break;
                    }
                    current = current.getParent();
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets a component by name, searching recursively from the given component.
     *
     * @param component The root component to start searching from
     * @param name      The component name to find
     * @return The found component, or null if not found
     */
    public static Component getComponentByName(Component component, String name) {
        if (component == null) {
            return null;
        }
        if (Objects.equals(name, component.getName())) {
            return component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                Component result = getComponentByName(child, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    /**
     * Finds all components with the specified name.
     *
     * @param parent The parent component to search within
     * @param name   The component name to find
     * @return List of matching components (may be empty)
     */
    public static List<Component> findAllComponentsByName(Component parent, String name) {
        List<Component> matchingComponents = new ArrayList<>();
        if (parent == null) {
            return matchingComponents;
        }
        if (Objects.equals(name, parent.getName())) {
            matchingComponents.add(parent);
        }
        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                matchingComponents.addAll(findAllComponentsByName(child, name));
            }
        }
        return matchingComponents;
    }

    /**
     * Finds the JRootPane within a container.
     *
     * @param container The container to search within
     * @return The found JRootPane, or null if not found
     */
    public static JRootPane findJRootPane(Container container) {
        if (container == null) {
            return null;
        }
        for (Component component : container.getComponents()) {
            if (component instanceof JRootPane) {
                return (JRootPane) component;
            } else if (component instanceof Container) {
                JRootPane found = findJRootPane((Container) component);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Recursively finds a component by name within a parent component.
     *
     * @param parent The parent component to search within
     * @param name   The component name to find
     * @return The found component, or null if not found
     */
    public static Component findByNameRecursively(Component parent, String name) {
        if (parent == null || name == null) {
            return null;
        }
        if (name.equals(parent.getName())) {
            return parent;
        }
        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                Component found = findByNameRecursively(child, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
