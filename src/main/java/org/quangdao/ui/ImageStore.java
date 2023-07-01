package org.quangdao.ui;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.image.BufferedImage;

@Getter
@Log4j2
public class ImageStore {
    private Robot robot = new Robot();
    private JPanel root;
    private JLabel title;
    private JButton viewButton;
    private JButton changeButton;

    @Setter
    private BufferedImage image;
    @Setter
    private int x, y;
    @Setter
    private Rectangle rect = new Rectangle(0,0,0,0);

    @Nullable
    public static Image getImageFromClipboard() {
        Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
        if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor))
        {
            try {
                return (Image) transferable.getTransferData(DataFlavor.imageFlavor);
            }
            catch (Exception e) {
                log.error("--------------------Clipboard failed----------------------");
                log.error(e.getMessage(), e);
                return null;
            }
        }
        else {
            log.warn("------------------------Clipboard not a image--------------------");
            return null;
        }
    }
    public ImageStore() throws AWTException {
        init();
    }

    public class CustomJPanel extends JPanel {
        protected int xx, yy;
        protected Rectangle rrect = new Rectangle(rect);
        public CustomJPanel() {
            super();
            xx = x;
            yy = y;

            addAncestorListener(new AncestorListener() {
                @Override
                public void ancestorAdded(AncestorEvent ancestorEvent) {
                    JPanel panel = (JPanel)ancestorEvent.getComponent();
                    panel.setPreferredSize(new Dimension(image.getWidth(panel), image.getHeight(panel)));
                    ((JFrame)ancestorEvent.getAncestor()).pack();
                }
                public void ancestorRemoved(AncestorEvent ancestorEvent) { }
                public void ancestorMoved(AncestorEvent ancestorEvent) { }
            });
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Rectangle rrect = new Rectangle(this.rrect);
            if(rrect.width < 0) {
                rrect.x += rrect.width;
                rrect.width = -rrect.width;
            }
            if(rrect.height < 0) {
                rrect.y += rrect.height;
                rrect.height = -rrect.height;
            }

            Graphics2D graphics2D = (Graphics2D) g;
            graphics2D.drawImage(image, 0, 0, this);
            graphics2D.setColor(new Color(255,100,100));
            graphics2D.setStroke(new BasicStroke(3));
            graphics2D.drawLine(xx-15, yy, xx+15, yy);
            graphics2D.drawLine(xx, yy-15, xx, yy+15);
            graphics2D.setColor(new Color(100,255,100));
            graphics2D.drawRect(rrect.x, rrect.y, rrect.width, rrect.height);
        }
    }
    public class EditCustomJPanel extends CustomJPanel {
        private boolean isBtn2 = false;
        public EditCustomJPanel() {
            super();
            addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if(isBtn2) {
                        rrect.width = e.getX() - rrect.x;
                        rrect.height = e.getY() - rrect.y;
                        repaint();
                    }

                }
            });
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    if(mouseEvent.getButton() == MouseEvent.BUTTON1) {
                        xx = mouseEvent.getX();
                        yy = mouseEvent.getY();
                        mouseEvent.getComponent().repaint();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if(e.getButton()==MouseEvent.BUTTON3) {
                        log.info(rrect);
                        isBtn2 = true;
                        rrect.x = e.getX();
                        rrect.y = e.getY();
                        rrect.width = 0;
                        rrect.height = 0;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(e.getButton()==MouseEvent.BUTTON3)
                        isBtn2 = false;
                }
            });
        }
        public void save() {
            x = xx;
            y = yy;
            if(rrect.width < 0) {
                rrect.x += rrect.width;
                rrect.width = -rrect.width;
            }
            if(rrect.height < 0) {
                rrect.y += rrect.height;
                rrect.height = -rrect.height;
            }
            rect = new Rectangle(rrect);
        }
    }

    public void init() {
        viewButton.addActionListener((ev-> {
            if (image != null) {
                try {
                    JFrame jFrame = new JFrame();
                    jFrame.setContentPane(new CustomJPanel());
                    jFrame.pack();
                    Point pt = viewButton.getLocationOnScreen();
                    jFrame.setLocation(pt);
                    jFrame.setVisible(true);
                } catch (Throwable t) {
                    log.error(t.getMessage(), t);
                }
            }
            else {
                JOptionPane.showMessageDialog(null, "Chưa có ảnh");
            }
        }));

        changeButton.addActionListener((actionEvent -> {
            int ch = JOptionPane.showOptionDialog(null, "Chọn nguồn ảnh", "Chọn",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, new Object[]{"Màn hình", "Clipboard"}, null);
            if(ch==JOptionPane.NO_OPTION) {
                Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    try {
                        this.image = toBufferedImage((Image)transferable.getTransferData(DataFlavor.imageFlavor));
                    }
                    catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "getImageFromClipboard: Không phải ảnh!");
                        return;
                    }
                }
                else {
                    JOptionPane.showMessageDialog(this.root, "getImageFromClipboard: Không phải ảnh!");
                    return;
                }
            }
            else {
                Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
                this.image = robot.createScreenCapture(new Rectangle(0, 0, d.width, d.height));
            }
            if(this.image != null) {
                JFrame jFrame = new JFrame();
                EditCustomJPanel jPanel = new EditCustomJPanel();
                jFrame.setContentPane(jPanel);
                jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                jFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        int ch = JOptionPane.showConfirmDialog(null, "Đóng cửa sổ ?", "Xác nhận", JOptionPane.YES_NO_CANCEL_OPTION);
                        if(ch == JOptionPane.OK_OPTION) {
                            jPanel.save();
                            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        }
                        else if(ch == JOptionPane.NO_OPTION) {
                            jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        }
                        else if(ch == JOptionPane.CANCEL_OPTION) {
                            jFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        }
                    }
                });
                Point pt = viewButton.getLocationOnScreen();
                jFrame.setLocation(pt);
                jFrame.setVisible(true);
            }
            else {
                JOptionPane.showMessageDialog(null, "Không có ảnh trong clipboard");
            }
        }));
    }

    public static BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();
        return bimage;
    }
}
