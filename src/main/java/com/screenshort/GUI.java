package com.screenshort;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ToolType;
import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import burp.api.montoya.ui.editor.HttpRequestEditor;

public class GUI implements ContextMenuItemsProvider {
    private final MontoyaApi api;
    private final Action action;

    public GUI(MontoyaApi api) {
        this.api = api;
        this.action = new Action(api);
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        List<Component> menuItems = new ArrayList<>();
        JMenuItem menuItem = new JMenuItem("Normal");
        JMenuItem menuItem1 = new JMenuItem("Full");
        JMenuItem menuItem2 = new JMenuItem("Full - Edited Request");
        JMenuItem menuItem3 = new JMenuItem("Full - Original Request");
        HttpRequestResponse requestResponse = event.messageEditorRequestResponse().isPresent()
                ? event.messageEditorRequestResponse().get().requestResponse()
                : event.selectedRequestResponses().get(0);

        HttpRequestEditor requestEditor = api.userInterface().createHttpRequestEditor();
        requestEditor.setRequest(requestResponse.request());

        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Frame frame = api.userInterface().swingUtils().suiteFrame();
                Component rrvSplitViewerSplitPane = findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
                action.takeScreenshot(rrvSplitViewerSplitPane);
            }
        });

        menuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Frame frame = api.userInterface().swingUtils().suiteFrame();
                    Component rrvSplitViewerSplitPane = findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
                    Component reqComp = getComponetByName(rrvSplitViewerSplitPane, "rrvRequestsPane");
                    Component syntaxTextAreaReq = getComponetByName(reqComp, "syntaxTextArea");
                    Component resComp = getComponetByName(rrvSplitViewerSplitPane, "rrvResponsePane");
                    Component syntaxTextAreaRes = getComponetByName(resComp, "syntaxTextArea");
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                    action.takeScreenshot2();
                } catch (Exception ex) {
                    api.logging().logToError("Error: " + ex.getMessage());
                }

            }
        });

        menuItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Frame frame = api.userInterface().swingUtils().suiteFrame();
                    Component rrvSplitViewerSplitPane = findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
                    // request pane
                    Component reqComp = getComponetByName(rrvSplitViewerSplitPane, "rrvRequestsPane");
                    List<Component> components = findAllComponentsByName(reqComp, "syntaxTextArea");
                    if (components.size() < 2) {
                        api.logging().logToOutput("No syntaxTextArea found");
                        return;
                    }
                    Component syntaxTextAreaReq = components.get(1);
                    // request pane
                    Component resComp = getComponetByName(rrvSplitViewerSplitPane, "rrvResponsePane");
                    components = findAllComponentsByName(resComp, "syntaxTextArea");
                    if (components.size() < 2) {
                        Component syntaxTextAreaRes = getComponetByName(resComp, "syntaxTextArea");
                        action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                        action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                        action.takeScreenshot2();
                        return;
                    }
                    Component syntaxTextAreaRes = components.get(1);
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                    action.takeScreenshot2();
                } catch (Exception ex) {
                    api.logging().logToError("Error: " + ex.getMessage());
                }
            }
        });

        // this function prepares the screenshot, if original image is under edited
        menuItem3.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Frame frame = api.userInterface().swingUtils().suiteFrame();
                    Component rrvSplitViewerSplitPane = findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
                    // request pane
                    Component reqComp = getComponetByName(rrvSplitViewerSplitPane, "rrvRequestsPane");
                    List<Component> components = findAllComponentsByName(reqComp, "syntaxTextArea");
                    if (components.size() < 2) {
                        api.logging().logToOutput("No syntaxTextArea found");
                        action.clearImages();
                        return;
                    }
                    Component syntaxTextAreaReq = components.get(0);
                    // request pane
                    Component resComp = getComponetByName(rrvSplitViewerSplitPane, "rrvResponsePane");
                    components = findAllComponentsByName(resComp, "syntaxTextArea");
                    if (components.size() < 2) {
                        Component syntaxTextAreaRes = getComponetByName(resComp, "syntaxTextArea");
                        action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                        action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                        action.takeScreenshot2();
                        return;
                    }
                    Component syntaxTextAreaRes = components.get(0);
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                    action.takeScreenshot2();
                } catch (Exception ex) {
                    api.logging().logToError("Error: " + ex.getMessage());
                }
            }
        });

        menuItems.add(menuItem);
        menuItems.add(menuItem1);
        if (event.isFromTool(ToolType.PROXY)) {
            menuItems.add(menuItem2);
            menuItems.add(menuItem3);
        }
        return menuItems;
    }

    public List<Component> findAllComponentsByName(Component parent, String name) {
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

    public Component findComponentUnderMouse(String name, Component parent) {
        Point location = MouseInfo.getPointerInfo().getLocation();
        SwingUtilities.convertPointFromScreen(location, parent);
        Component deepest = SwingUtilities.getDeepestComponentAt(parent, location.x, location.y);
        return SwingUtilities.getAncestorNamed(name, deepest);
    }

    Component getComponetByName(Component component, String name) {
        if (component.getName() != null && component.getName().equals(name)) {
            return component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                Component result = (Container) getComponetByName(child, name);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    Component getComponentByClass(Component comp, String classname) {
        if (comp.getClass().getName().equals(classname)) {
            return comp;
        }
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                Component result = getComponentByClass(child, classname);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public void printComponentTree(Component component, String indent) {
        api.logging().logToOutput(indent + component);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                printComponentTree(child, indent + "  ");
            }
        }
    }
}
