package org.cubewhy.celestial.gui.pages;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import lombok.extern.slf4j.Slf4j;
import org.cubewhy.celestial.gui.dialogs.ArgsConfigDialog;
import org.cubewhy.celestial.gui.layouts.VerticalFlowLayout;
import org.cubewhy.celestial.utils.GuiUtils;
import org.cubewhy.celestial.utils.SystemUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import static org.cubewhy.celestial.Celestial.config;
import static org.cubewhy.celestial.Celestial.f;
import static org.cubewhy.celestial.gui.GuiLauncher.statusBar;

@Slf4j
public class GuiSettings extends JScrollPane {
    private static final JPanel panel = new JPanel();
    private final Set<String> claimed = new HashSet<>();

    public GuiSettings() {
        super(panel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        this.setBorder(new TitledBorder(null, f.getString("gui.settings.title"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.orange));
        panel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.LEFT));
        this.getVerticalScrollBar().setUnitIncrement(30);
        this.initGui();
    }

    private void initGui() {
        // config
        // jre
        panel.add(new JLabel(f.getString("gui.settings.warn.restart")));
        JPanel panelVM = new JPanel();
        panelVM.setLayout(new VerticalFlowLayout(VerticalFlowLayout.LEFT));
        panelVM.setBorder(new TitledBorder(null, f.getString("gui.settings.jvm"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.orange));

        String customJre = config.getValue("jre").getAsString();
        JButton btnSelectPath = new JButton((customJre.isEmpty()) ? SystemUtils.getCurrentJavaExec().getPath() : customJre);
        JButton btnUnset = new JButton(f.getString("gui.settings.jvm.jre.unset"));
        btnSelectPath.addActionListener((e) -> {
            File file = GuiUtils.chooseFile(new FileNameExtensionFilter("Java Executable", "exe"));
            if (file != null) {
                JButton source = (JButton) e.getSource();
                statusBar.setText(String.format(f.getString("gui.settings.jvm.jre.success"), file));
                config.setValue("jre", file.getPath());
                source.setText(file.getPath());
            }
        });
        btnUnset.addActionListener((e) -> {
            if (JOptionPane.showConfirmDialog(this, f.getString("gui.settings.jvm.jre.unset.confirm"), "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                return;
            }
            File java = SystemUtils.getCurrentJavaExec();
            btnSelectPath.setText(java.getPath());
            config.setValue("jre", "");
            statusBar.setText(f.getString("gui.settings.jvm.jre.unset.success"));
        });
        // jre settings
        JPanel p1 = new JPanel();
        p1.add(new JLabel(f.getString("gui.settings.jvm.jre")));
        p1.add(btnSelectPath);
        p1.add(btnUnset);
        panelVM.add(p1);
        // ram settings
        JPanel p2 = new JPanel();
        p2.add(new JLabel(f.getString("gui.settings.jvm.ram")));
        JSlider ramSlider = new JSlider(JSlider.HORIZONTAL, 0, SystemUtils.getTotalMem(), config.getValue("ram").getAsInt());
        ramSlider.setPaintTicks(true);
        ramSlider.setMajorTickSpacing(1024); // 1G
        p2.add(ramSlider);
        DecimalFormat decimalFormat = new DecimalFormat("#.##");

        JLabel labelRam = new JLabel(decimalFormat.format((float) ramSlider.getValue() / 1024F) + "GB");
        ramSlider.addChangeListener((e) -> {
            JSlider source = (JSlider) e.getSource();
            if (!source.getValueIsAdjusting()) {
                // save value
                log.info("Set ram -> " + source.getValue());
                config.setValue("ram", source.getValue());
            }
            labelRam.setText(decimalFormat.format((float) source.getValue() / 1024F) + "GB");
        });
        p2.add(labelRam);
        panelVM.add(p2);

        JPanel p3 = new JPanel();
        p3.add(new JLabel(f.getString("gui.settings.jvm.wrapper")));
        JTextField wrapperInput = getAutoSaveTextField("wrapper", config.getConfig());
        p3.add(wrapperInput);
        JButton btnSetVMArgs = new JButton(f.getString("gui.settings.jvm.args"));
        btnSetVMArgs.addActionListener((e) -> {
            new ArgsConfigDialog("vm-args", config.getConfig()).setVisible(true);
        });
        panelVM.add(btnSetVMArgs);
        panelVM.add(p3);

        claim("jre", panelVM);
        claim("ram");
        claim("vm-args");
        claim("wrapper");

        claim("game"); // config in GuiVersionSelect
        claim("javaagents"); // config in GuiAddonManager

        JPanel panelLauncher = new JPanel();
        panelLauncher.setLayout(new VerticalFlowLayout(VerticalFlowLayout.LEFT));
        panelLauncher.setBorder(new TitledBorder(null, f.getString("gui.settings.launcher"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.orange));
        panelLauncher.add(getAutoSaveCheckBox(config.getConfig(), "data-sharing", f.getString("gui.settings.launcher.data-sharing")));
        // theme
        JPanel p4 = new JPanel();
        p4.add(new JLabel(f.getString("gui.settings.launcher.theme")));
        p4.add(getAutoSaveComboBox(config.getConfig(), "theme", List.of(new String[]{"dark", "light"})));
        panelLauncher.add(p4);
        // language
        JPanel p5 = new JPanel();
        p5.add(new JLabel(f.getString("gui.settings.launcher.language")));
        p5.add(getAutoSaveComboBox(config.getConfig(), "language", List.of(new String[]{"zh", "en"})));
        panelLauncher.add(p5);
        // max threads
        JPanel p6 = new JPanel();
        p6.add(new JLabel(f.getString("gui.settings.launcher.max-threads")));
        p6.add(getAutoSaveSpinner(config.getConfig(), "max-threads", 1, 256));
        panelLauncher.add(p6);

        claim("data-sharing", panelLauncher);
        claim("theme");
        claim("language");
        claim("max-threads");

        JPanel panelUnclaimed = new JPanel();
        panelUnclaimed.setBorder(new TitledBorder(null, f.getString("gui.settings.unclaimed"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.orange));
        panelUnclaimed.setLayout(new VerticalFlowLayout(VerticalFlowLayout.LEFT));
        addUnclaimed(panelUnclaimed, config.getConfig());
        panel.add(panelUnclaimed);
    }

    private void addUnclaimed(JPanel basePanel, @NotNull JsonObject json) {
        for (Map.Entry<String, JsonElement> s : json.entrySet()) {
            if (!claimed.contains(s.getKey())) {
                // unclaimed
                if (s.getValue().isJsonPrimitive()) {
                    JPanel p = getSimplePanel(json, s.getKey());
                    basePanel.add(p);
                }
                if (s.getValue().isJsonObject()) {
                    JPanel subPanel = new JPanel();
                    subPanel.setBorder(new TitledBorder(null, s.getKey(), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, Color.orange));
                    subPanel.setLayout(new VerticalFlowLayout(VerticalFlowLayout.LEFT));
                    basePanel.add(subPanel);
                    addUnclaimed(subPanel, s.getValue().getAsJsonObject());
                }
                if (s.getValue().isJsonArray()) {
                    // TODO valueList
                }
            }
        }
    }

    private JComboBox<String> getAutoSaveComboBox(JsonObject json, String key, @NotNull List<String> items) {
        JComboBox<String> cb = new JComboBox<>();
        for (String item : items) {
            cb.addItem(item);
        }
        cb.setSelectedItem(json.get(key).getAsString());
        cb.addActionListener((e) -> {
            JComboBox<String> source = (JComboBox<String>) e.getSource();
            String v = (String) source.getSelectedItem();
            json.addProperty(key, v);
            config.save();
        });
        return cb;
    }

    private @NotNull JPanel getSimplePanel(@NotNull JsonObject json, String key) {
        JPanel panel = new JPanel();
        JsonPrimitive value = json.getAsJsonPrimitive(key);
        if (value.isBoolean()) {
            JCheckBox cb = getAutoSaveCheckBox(json, key, key);
            panel.add(cb);
        } else if (value.isString()) {
            panel.add(new JLabel(key));
            JTextField input = getAutoSaveTextField(key, json);
            panel.add(input);
        } else if (value.isNumber()) {
            panel.add(new JLabel(key));
            JSpinner spinner = getAutoSaveSpinner(json, key, Double.MIN_VALUE, Double.MAX_VALUE);
            panel.add(spinner);

        }
        return panel;
    }

    @NotNull
    private static JSpinner getAutoSaveSpinner(@NotNull JsonObject json, String key, double min, double max) {
        JsonPrimitive value = json.getAsJsonPrimitive(key);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value.getAsDouble(), min, max, 0.01));
        spinner.setAutoscrolls(true);
        JComponent editor = spinner.getEditor();
        JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
        spinner.addChangeListener((e) -> {
            JSpinner source = (JSpinner) e.getSource();
            Number v = (Number) source.getValue();
            json.addProperty(key, v);
            config.save();
        });
        textField.setColumns(20);
        return spinner;
    }

    @NotNull
    private static JCheckBox getAutoSaveCheckBox(@NotNull JsonObject json, String key, String text) {
        JCheckBox cb = new JCheckBox(text);
        JsonPrimitive value = json.getAsJsonPrimitive(key);
        cb.setSelected(value.getAsBoolean());
        cb.addActionListener((e) -> {
            JCheckBox source = (JCheckBox) e.getSource();
            json.addProperty(key, source.isSelected());
            config.save();
        });
        return cb;
    }

    @NotNull
    private static JTextField getAutoSaveTextField(String key, @NotNull JsonObject json) {
        JsonPrimitive value = json.getAsJsonPrimitive(key);
        JTextField input = new JTextField(value.getAsString());
        input.addActionListener((e) -> {
            JTextField source = (JTextField) e.getSource();
            // save value
            json.addProperty(key, source.getText());
            config.save();
        });
        input.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                JTextField source = (JTextField) e.getSource();
                // save value
                json.addProperty(key, source.getText());
                config.save();
            }
        });
        return input;
    }

    /**
     * Mark a key as claimed and add the panel
     *
     * @param key      key in celestial.json
     * @param cfgPanel a panel to config this value
     */
    private void claim(String key, JPanel cfgPanel) {
        claim(key);
        panel.add(cfgPanel); // add the panel
    }

    private void claim(String key) {
        if (claimed.add(key)) {
            log.debug("Claimed " + key);
        } else {
            log.warn("Failed to claim " + key + " : always claimed.");
        }
    }
}
