package org.quangdao.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.quangdao.tools.AI;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

@Getter
@Log4j2
public class ConfigWindow {

    public ConfigWindow() throws AWTException {
        init();
    }
    private JPanel root;
    private JPanel left;
    private JTable table1;
    private JButton startButton;
    private JButton addPart;
    private JButton stopButton;
    private final List<Map<String, ImageStore>> imageStoreMapList = new ArrayList<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, String> properties = new TreeMap<>();
    static private final String[] propList = new String[] {"io.input", "io.output", "io.dump", "exec.wait.max", "exec.wait.min", "exec.cache.box"};
    static private final String[] imageStates = new String[] {"email", "pass", "failed", "cap", "submit", "valid", "loading", "scr", "reload"};

    private void addPart(String name, Map<String, ImageStore> mp) {
        JPanel jPanel = new JPanel();
        var ll = new JLabel(name);
        jPanel.add(ll);
        for(var i : mp.entrySet())
            jPanel.add(i.getValue().getRoot());
        var btn = new Button("Xóa");
        btn.addActionListener(l->{
            imageStoreMapList.remove(mp);
            left.remove(jPanel);
            left.validate();
            left.repaint();
        });
        jPanel.add(btn);
        left.add(jPanel);
        root.validate();
        root.repaint();
    }
    private void init() throws AWTException {
        loadFile();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        for(var i : propList)
            if(!properties.containsKey(i))
                properties.put(i, null);

        {
            int i = 0;
            for (var mp : imageStoreMapList) {
                addPart("" + i++, mp);
            }
        }

        addPart.addActionListener(ll->{
            var mp = new HashMap<String, ImageStore>();
            for(var i : imageStates) {
                try {
                    var st = new ImageStore();
                    st.getTitle().setText(i);
                    mp.put(i, st);
                } catch (AWTException e) {
                    throw new RuntimeException(e);
                }
            }
            imageStoreMapList.add(mp);
            addPart(""+imageStoreMapList.size()+1, mp);
        });

        String[][] data = properties.entrySet().stream().map((kv) -> new String[]{kv.getKey(), kv.getValue()}).toList().toArray(new String[0][]);
        var tableModel = new DefaultTableModel(data , new String[] {"Key", "Value"} ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }

            public void setValueAtSlient(Object aValue, int row, int column) {
                Vector<Object> rowVector = (Vector)this.dataVector.elementAt(row);
                rowVector.setElementAt(aValue, column);
            }
        };
        tableModel.addTableModelListener(tableModelEvent -> {
            if(tableModelEvent.getType()==TableModelEvent.UPDATE) {
                int id = JOptionPane.showConfirmDialog(root, "Lưu ?", "Xác nhận", JOptionPane.OK_CANCEL_OPTION);
                if (id == JOptionPane.OK_OPTION) {
                    properties.put((String) tableModel.getValueAt(tableModelEvent.getFirstRow(), 0), (String) tableModel.getValueAt(tableModelEvent.getFirstRow(), 1));
                } else if (id == JOptionPane.CANCEL_OPTION) {
                    tableModel.setValueAtSlient(properties.get((String) tableModel.getValueAt(tableModelEvent.getFirstRow(), 0)), tableModelEvent.getFirstRow(), 1);
                }
            }
        });
        table1.setModel(tableModel);
        table1.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        startButton.addActionListener(ac->{
            try {
                AI ai = new AI(properties, imageStoreMapList);
                ai.run();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                this.ai = ai;
            } catch (Throwable ex) {
                log.error(ex.getMessage(), ex);
            }
        });

        stopButton.addActionListener(ac->{
            this.ai.stop();
            this.ai = null;
            this.stopButton.setEnabled(false);
            this.startButton.setEnabled(true);
        });

    }

    private AI ai;

    public void saveToFile() {
        log.info("Start writing to file");
        try {
            Path path = Files.createTempFile("tmp", "tmp");
            try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(path.toFile()))) {
                List<Map<String, PicSaveInfo>> info = imageStoreMapList.stream().map(lt -> {
                    Map<String, PicSaveInfo> mp = new HashMap<>();
                    for (var kv : lt.entrySet()) {
                        var i = kv.getValue();
                        if (i.getImage() != null) {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            try {
                                ImageIO.write(i.getImage(), "png", bos);
                                mp.put(kv.getKey(), new PicSaveInfo(new String(Base64.getEncoder().encode(bos.toByteArray()), StandardCharsets.UTF_8), i.getX(), i.getY(), i.getRect()));
                            } catch (Exception e) {
                                log.error(e.getMessage(), e);
                            }
                        }
                    }
                    return mp;
                }).toList();
                gson.toJson(new Options(info, properties), bufferedWriter);
            }
            log.info("Write completed");
            Files.move(path, Path.of("config.json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable ex) {
            log.error("--------------Error in saving configuration file------------------");
            log.error(ex.getMessage(), ex);
        }
    }

    public void loadFile() {
        try {
            Path path = Path.of("config.json");
            if(!Files.exists(path) || !path.toFile().isFile()) return;
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(path.toFile()))) {
                Options options = gson.fromJson(bufferedReader, TypeToken.get(Options.class));
                for(var i : options.imageDatas) {
                    HashMap<String, ImageStore> mp = new HashMap<>();
                    for (var j : imageStates) {
                        var saveInfo = i.get(j);
                        var store = new ImageStore();
                        if (saveInfo != null) {
                            var img = ImageIO.read(new ByteArrayInputStream(Base64.getDecoder().decode(saveInfo.imgData())));
                            var format = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
                            Graphics2D bGr = format.createGraphics();
                            bGr.drawImage(img, 0, 0, null);
                            bGr.dispose();

                            store.setImage(format);
                            store.setX(saveInfo.x);
                            store.setY(saveInfo.y);
                            if (saveInfo.rectangle != null) store.setRect(new Rectangle(saveInfo.rectangle));
                        }
                        store.getTitle().setText(j);
                        mp.put(j, store);
                    }
                    imageStoreMapList.add(mp);
                }
                properties.putAll(options.props());
            }
            log.info("Write completed");
        } catch (Throwable ex) {
            log.error("--------------Error in saving configuration file------------------");
            log.error(ex.getMessage(), ex);
        }
    }

    private record Options(List<Map<String, PicSaveInfo>> imageDatas, Map<String, String> props) {};

    private record PicSaveInfo(String imgData, int x, int y, Rectangle rectangle) {};
}
