package com.screenshort;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class ComponentFinder {
    public static Component findComponentUnderMouse(String name, Component parent) {
        Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, parent);
        Component deepest = SwingUtilities.getDeepestComponentAt(parent, location.x, location.y);
        return SwingUtilities.getAncestorNamed(name, deepest);
    }

    public static Component getComponentByName(Component component, String name) {
        if (component.getName() != null && component.getName().equals(name)) {
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

    public static List<Component> findAllComponentsByName(Component parent, String name) {
        List<Component> matchingComponents = new ArrayList<>();
        if (name.equals(parent.getName())) {
            matchingComponents.add(parent);
        }
        if (parent instanceof Container) {
            for (Component child : ((Container) parent).getComponents()) {
                matchingComponents.addAll(findAllComponentsByName(child, name));
            }
        }
        return matchingComponents;
    }
}
