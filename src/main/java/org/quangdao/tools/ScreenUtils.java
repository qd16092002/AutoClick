package org.quangdao.tools;

import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

@Log4j2
public class ScreenUtils {
    private Rectangle scanArea;

    private final int margin;
    private final Robot robot = new Robot();

    public ScreenUtils(int margin) throws AWTException {
        this.margin = margin;
        //TODO:
        scanArea = new Rectangle(10000, 10000);
    }

    private static void sleep(long milis) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignore){}
    }

    public synchronized Point fillText(SubImage look, Point clickOffset, String text, Point hint, Rectangle subArea) throws InterruptedException {
        Point pt = find(look, hint, subArea);
        if(pt == null) return null;

        StringSelection stringSelection = new StringSelection(text);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, stringSelection);

        robot.mouseMove(clickOffset.x+pt.x, clickOffset.y+pt.y);
        sleep(100);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        sleep(100);
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_V);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        sleep(100);
        return pt;
    }

    public synchronized Point click(SubImage look, Point target, Point hint, Rectangle subArea) {
        Point pt = find(look, hint, subArea);
        if(pt == null) return null;

        robot.mouseMove(target.x+pt.x, target.y+pt.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return pt;
   }

    public synchronized Point find(SubImage look, Point hint, Rectangle subArea)  {
        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        Rectangle r = new Rectangle(0, 0, d.width, d.height);
        SubImage background = new SubImage(robot.createScreenCapture(r), r);

        if (hint != null) {
            Point pt = look.findIn(background, new Rectangle(hint.x, hint.y, 1, 1));
            if(pt!=null) return pt;
            pt = look.findIn(background, new Rectangle(hint.x-margin, hint.y-margin, 1+margin*2,1+2*margin));
            if(pt!=null) return pt;
        }

        Point pt = look.findIn(background, scanArea.intersection(subArea));
        if(pt == null) return null;
        return pt;
    }
}
