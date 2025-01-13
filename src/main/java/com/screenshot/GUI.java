package com.screenshot;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import burp.api.montoya.MontoyaApi;
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
        JMenuItem menuItem = new JMenuItem("Take Screenshot");
        JMenuItem menuItem1 = new JMenuItem("Long Component");

        HttpRequestResponse requestResponse = event.messageEditorRequestResponse().isPresent()
                ? event.messageEditorRequestResponse().get().requestResponse()
                : event.selectedRequestResponses().get(0);

        HttpRequestEditor requestEditor = api.userInterface().createHttpRequestEditor();
        requestEditor.setRequest(requestResponse.request());

        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Frame frame = api.userInterface().swingUtils().suiteFrame();
                Point mousePos = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mousePos, frame);
                Component component = SwingUtilities.getDeepestComponentAt(frame, mousePos.x, mousePos.y);
                Component rrvSplitViewerSplitPane = reverseRecurse(component, 50);
                action.takeScreenshot(rrvSplitViewerSplitPane);
            }
        });

        menuItem1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                api.logging().logToOutput("Long Component Action Triggered");
                try {
                    Frame frame = api.userInterface().swingUtils().suiteFrame();
                    // Component rrvSplitViewerSplitPane = getComponetByName(frame,
                    // "rrvSplitViewerSplitPane");
                    // Component reqComp = getComponetByName(rrvSplitViewerSplitPane,
                    // "rrvRequestsPane");
                    // Component syntaxTextAreaReq = getComponetByName(reqComp, "syntaxTextArea");
                    // Component resComp = getComponetByName(rrvSplitViewerSplitPane,
                    // "rrvResponsePane");
                    // Component syntaxTextAreaRes = getComponetByName(resComp, "syntaxTextArea");
                    // action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                    // action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                    // action.takeScreenshot2();
                    Point mousePos = MouseInfo.getPointerInfo().getLocation();
                    SwingUtilities.convertPointFromScreen(mousePos, frame);
                    Component component = SwingUtilities.getDeepestComponentAt(frame, mousePos.x, mousePos.y);
                    Component rrvSplitViewerSplitPane = reverseRecurse(component, 50);
                    Component reqComp = getComponetByName(rrvSplitViewerSplitPane, "rrvRequestsPane");
                    Component syntaxTextAreaReq = getComponetByName(reqComp, "syntaxTextArea");
                    Component resComp = getComponetByName(rrvSplitViewerSplitPane, "rrvResponsePane");
                    Component syntaxTextAreaRes = getComponetByName(resComp, "syntaxTextArea");
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
                    action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
                    action.takeScreenshot2();
                    // printComponentTree(com5, "");
                    // api.logging().logToOutput("Component at mouse position: " + com5);
                } catch (Exception ex) {
                    api.logging().logToError("Error: " + ex.getMessage());
                }

            }
        });

        menuItems.add(menuItem);
        menuItems.add(menuItem1);
        return menuItems;
    }

    Component reverseRecurse(Component component, int num) {
        if (component.getParent() == null) {
            return component;
        }
        if (num == 0 || component.getName() == "rrvSplitViewerSplitPane") {
            return component;
        }

        return reverseRecurse(component.getParent(), num - 1);
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

    public void printComponentTree(Component component, String indent) {
        api.logging().logToOutput(indent + component);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                printComponentTree(child, indent + "  ");
            }
        }
    }
}
