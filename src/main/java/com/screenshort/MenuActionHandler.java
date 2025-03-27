package com.screenshort;

import java.awt.*;
import burp.api.montoya.MontoyaApi;

public class MenuActionHandler {
    private final MontoyaApi api;
    private final Action action;

    public MenuActionHandler(MontoyaApi api) {
        this.api = api;
        this.action = new Action(api);
    }

    public void handleNormalScreenshot() {
        Frame frame = api.userInterface().swingUtils().suiteFrame();
        Component targetComponent = ComponentFinder.findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
        action.takeScreenshot(targetComponent);
    }

    public void handleFullScreenshot() {
        try {
            Frame frame = api.userInterface().swingUtils().suiteFrame();
            Component targetComponent = ComponentFinder.findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
            Component reqComp = ComponentFinder.getComponentByName(targetComponent, "rrvRequestsPane");
            Component resComp = ComponentFinder.getComponentByName(targetComponent, "rrvResponsePane");
            Component syntaxTextAreaReq = ComponentFinder.getComponentByName(reqComp, "syntaxTextArea");
            Component syntaxTextAreaRes = ComponentFinder.getComponentByName(resComp, "syntaxTextArea");
            
            action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
            action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
            action.takeScreenshot2();
        } catch (Exception ex) {
            api.logging().logToError("Error: " + ex.getMessage());
        }
    }

    public void handleEditedScreenshot() {
        captureScreenshot(1);
    }

    public void handleOriginalScreenshot() {
        captureScreenshot(0);
    }

    private void captureScreenshot(int index) {
        try {
            Frame frame = api.userInterface().swingUtils().suiteFrame();
            Component targetComponent = ComponentFinder.findComponentUnderMouse("rrvSplitViewerSplitPane", frame);
            Component reqComp = ComponentFinder.getComponentByName(targetComponent, "rrvRequestsPane");
            Component resComp = ComponentFinder.getComponentByName(targetComponent, "rrvResponsePane");
            
            java.util.List<Component> reqComponents = ComponentFinder.findAllComponentsByName(reqComp, "syntaxTextArea");
            java.util.List<Component> resComponents = ComponentFinder.findAllComponentsByName(resComp, "syntaxTextArea");
            
            if (reqComponents.size() <= index || resComponents.size() <= index) {
                api.logging().logToOutput("No syntaxTextArea found");
                action.clearImages();
                return;
            }
            
            Component syntaxTextAreaReq = reqComponents.get(index);
            Component syntaxTextAreaRes = resComponents.get(index);
            
            action.takeScreenshotAndGetBufferImage(syntaxTextAreaReq);
            action.takeScreenshotAndGetBufferImage(syntaxTextAreaRes);
            action.takeScreenshot2();
        } catch (Exception ex) {
            api.logging().logToError("Error: " + ex.getMessage());
        }
    }
}
