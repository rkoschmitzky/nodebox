package nodebox.client;

import nodebox.node.NodeLibrary;
import nodebox.ui.Theme;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ExamplesBrowser extends JFrame {

    private static final Image DEFAULT_EXAMPLE_IMAGE;
    private static final File examplesFolder = new File("examples");
    private static final Font EXAMPLE_TITLE_FONT = new Font(Font.DIALOG, Font.BOLD, 12);
    private static final Color EXAMPLE_TITLE_COLOR = new Color(60, 60, 200);
    private static final Pattern NUMBERS_PREFIX_PATTERN = Pattern.compile("^[0-9]+\\s");

    static {
        try {
            DEFAULT_EXAMPLE_IMAGE = ImageIO.read(ExamplesBrowser.class.getResourceAsStream("/default-example.png"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Category currentCategory;
    private SubCategory currentSubCategory;

    private Map<String, File> categoryFolderMap = new HashMap<String, File>();

    private final JPanel categoriesPanel;
    private final JPanel subCategoriesPanel;
    private final JPanel examplesPanel;

    public ExamplesBrowser() {
        super("Examples");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);


        categoriesPanel = new CategoriesPanel();
        categoriesPanel.setBackground(Color.WHITE);

        subCategoriesPanel = new SubCategoriesPanel();

        examplesPanel = new JPanel(new ExampleLayout(10, 10));
        examplesPanel.setBackground(Color.WHITE);


        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(categoriesPanel, BorderLayout.NORTH);
        mainPanel.add(subCategoriesPanel, BorderLayout.WEST);
        mainPanel.add(examplesPanel, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setJMenuBar(new NodeBoxMenuBar());


        mainPanel.getActionMap().put("Reload", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                reload();
            }
        });

        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(PlatformUtils.getKeyStroke(KeyEvent.VK_R), "Reload");

        reload();
    }

    /**
     * Refresh the examples browser by loading everything from disk.
     */
    private void reload() {
        final List<Category> categories = parseCategories(examplesFolder);

        categoriesPanel.removeAll();
        for (final Category category : categories) {
            final CategoryButton b = new CategoryButton(category);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (Component c : categoriesPanel.getComponents()) {
                        CategoryButton b = (CategoryButton) c;
                        b.setSelected(false);
                    }
                    b.setSelected(true);
                    selectCategory(category);
                }
            });
            categoriesPanel.add(b);
        }
        categoriesPanel.validate();
        categoriesPanel.repaint();
        ((CategoryButton) categoriesPanel.getComponent(0)).setSelected(true);
        selectCategory(categories.get(0));
    }


    private void selectCategory(Category category) {
        subCategoriesPanel.removeAll();
        final List<SubCategory> subCategories = category.subCategories;
        for (final SubCategory subCategory : subCategories) {
            final SubCategoryButton b = new SubCategoryButton(subCategory);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    for (Component c : subCategoriesPanel.getComponents()) {
                        SubCategoryButton b = (SubCategoryButton) c;
                        b.setSelected(false);
                    }
                    b.setSelected(true);
                    selectSubCategory(subCategory);
                }
            });
            subCategoriesPanel.add(b);
        }
        subCategoriesPanel.validate();
        subCategoriesPanel.repaint();
        ((SubCategoryButton) subCategoriesPanel.getComponent(0)).setSelected(true);
        selectSubCategory(subCategories.get(0));
    }

    private void selectSubCategory(SubCategory subCategory) {
        List<Example> examples = subCategory.examples;
        examplesPanel.removeAll();
        for (final Example e : examples) {
            ExampleButton b = new ExampleButton(e.title, new ImageIcon(e.thumbnail));
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    openExample(e);
                }
            });
            examplesPanel.add(b);
        }
        examplesPanel.validate();
        examplesPanel.repaint();
    }


    private void openExample(Example example) {
        Application.getInstance().openDocument(example.file);
    }

    public static List<Category> parseCategories(File parentDirectory) {
        File[] directories = parentDirectory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });

        ArrayList<Category> categories = new ArrayList<Category>();
        for (File d : directories) {
            String name = fileToTitle(d);
            List<SubCategory> subCategories = parseSubCategories(d);
            categories.add(new Category(name, d, subCategories));
        }
        return categories;
    }

    private static List<SubCategory> parseSubCategories(File directory) {
        File[] directories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory() && !file.isHidden();
            }
        });

        ArrayList<SubCategory> subCategories = new ArrayList<SubCategory>();
        for (File d : directories) {
            String name = fileToTitle(d);
            List<Example> examples = parseExamples(d);
            subCategories.add(new SubCategory(name, d, examples));
        }
        return subCategories;
    }

    public static List<Example> parseExamples(File directory) {
        File[] directories = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File projectDirectory) {
                return projectDirectory.isDirectory() && nodeBoxFileForDirectory(projectDirectory).exists();
            }
        });

        ArrayList<Example> examples = new ArrayList<Example>();
        for (File projectDirectory : directories) {
            File nodeBoxFile = nodeBoxFileForDirectory(projectDirectory);
            Map<String, String> propertyMap = NodeLibrary.parseHeader(nodeBoxFile);
            examples.add(Example.fromNodeLibrary(nodeBoxFile, propertyMap));
        }
        return examples;
    }

    public static String fileToTitle(File file) {
        String baseName = FileUtils.getBaseName(file.getName());
        return NUMBERS_PREFIX_PATTERN.matcher(baseName).replaceFirst("");
    }

    public static File nodeBoxFileForDirectory(File projectDirectory) {
        return new File(projectDirectory, projectDirectory.getName() + ".ndbx");
    }

    public static Image thumbnailForLibraryFile(File nodeBoxFile) {
        if (nodeBoxFile == null) return DEFAULT_EXAMPLE_IMAGE;
        File projectDirectory = nodeBoxFile.getParentFile();
        String baseName = FileUtils.getBaseName(nodeBoxFile.getName());
        File imageFile = new File(projectDirectory, baseName + ".png");
        if (imageFile.exists()) {
            try {
                return ImageIO.read(imageFile);
            } catch (IOException e) {
                return DEFAULT_EXAMPLE_IMAGE;
            }
        } else {
            return DEFAULT_EXAMPLE_IMAGE;
        }
    }

    private static String getProperty(Map<String, String> propertyMap, String key, String defaultValue) {
        if (propertyMap.containsKey(key)) {
            return propertyMap.get(key);
        } else {
            return defaultValue;
        }
    }

    public static final class Category {
        public final String name;
        public final File directory;
        public final List<SubCategory> subCategories;

        public Category(String name, File directory, List<SubCategory> subCategories) {
            this.name = name;
            this.directory = directory;
            this.subCategories = subCategories;
        }
    }

    public static final class SubCategory {
        public final String name;
        public final File directory;
        public final List<Example> examples;

        public SubCategory(String name, File directory, List<Example> examples) {
            this.name = name;
            this.directory = directory;
            this.examples = examples;
        }
    }

    public static class Example {

        public final File file;
        public final String title;
        public final String description;
        public final Image thumbnail;

        public static Example fromNodeLibrary(File nodeBoxFile, Map<String, String> propertyMap) {
            String title = getProperty(propertyMap, "title", fileToTitle(nodeBoxFile));
            String description = getProperty(propertyMap, "description", "");
            Image thumbnail = thumbnailForLibraryFile(nodeBoxFile);
            return new Example(nodeBoxFile, title, description, thumbnail);
        }

        public Example(File file, String title, String description, Image thumbnail) {
            this.file = file;
            this.title = title;
            this.description = description;
            this.thumbnail = thumbnail;
        }
    }

    private static class CategoriesPanel extends JPanel {
        private CategoriesPanel() {
            super(new FlowLayout(FlowLayout.LEADING, 0, 0));
            setSize(300, 32);
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(new Color(240, 240, 240));
            g.fillRect(0, 0, getWidth(), getHeight() - 1);
            g.setColor(new Color(200, 200, 200));
            g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
        }
    }

    private static class SubCategoriesPanel extends JPanel {
        private SubCategoriesPanel() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setSize(300, 32);
            setMinimumSize(new Dimension(150, 32));
            setMaximumSize(new Dimension(150, 1000));
            setPreferredSize(new Dimension(150, 500));
        }
    }

    private static class CategoryButton extends JToggleButton {

        private CategoryButton(Category category) {
            super(category.name);
            setSize(150, 32);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            if (isSelected()) {
                g2.setColor(new Color(2, 164, 228));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                g2.setColor(new Color(255, 255, 255));
                g2.drawLine(0, 0, 0, getHeight() - 2);
                g2.setColor(new Color(210, 210, 210));
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight() - 2);
            }

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
            if (isSelected()) {
                g2.setColor(Color.WHITE);
            } else {
                g2.setColor(new Color(160, 160, 160));
            }
            g2.drawString(getText(), 10, 20);

            //g2.setColor(Color.GREEN);
            //g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }

    }

    private class SubCategoryButton extends JToggleButton {
        public SubCategoryButton(SubCategory subCategory) {
            super(subCategory.name);
            forceSize(this, 150, 32);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected()) {
                g.setColor(new Color(2, 164, 228));
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            g.setFont(Theme.SMALL_FONT);
            if (isSelected()) {
                g.setColor(Color.WHITE);
            } else {
                g.setColor(Color.BLACK);
            }
            g.drawString(getText(), 5, 20);
        }
    }


    private static class ExampleButton extends JButton {
        private ExampleButton(String title, Icon icon) {
            super(title, icon);
            setSize(150, 125);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            getIcon().paintIcon(this, g, 0, 0);
            g2.setFont(EXAMPLE_TITLE_FONT);
            g2.setColor(EXAMPLE_TITLE_COLOR);
            g2.drawString(getText(), 1, 117);
        }
    }

    private static class ExampleLayout implements LayoutManager {

        private ArrayList<Component> components = new ArrayList<Component>();

        private int hGap, vGap;

        private ExampleLayout(int hGap, int vGap) {
            this.hGap = hGap;
            this.vGap = vGap;
        }

        @Override
        public void addLayoutComponent(String s, Component component) {
            components.add(component);

        }

        @Override
        public void removeLayoutComponent(Component component) {
            components.remove(component);
        }

        @Override
        public Dimension preferredLayoutSize(Container container) {
            return container.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container container) {
            return container.getSize();
        }

        @Override
        public void layoutContainer(Container container) {
            int y = vGap;
            int x = hGap;
            int maxHeightForRow = 0;
            for (Component c : container.getComponents()) {
                int width = c.getWidth();
                int height = c.getHeight();
                maxHeightForRow = Math.max(maxHeightForRow, height);
                if (x > container.getWidth() - width) {
                    x = hGap;
                    y += maxHeightForRow + vGap;
                    maxHeightForRow = 0;
                }
                c.setBounds(x, y, width, height);
                x += width + hGap;
            }
        }
    }

    private static void forceSize(Component c, int width, int height) {
        Dimension d = new Dimension(width, height);
        c.setSize(d);
        c.setMinimumSize(d);
        c.setMaximumSize(d);
        c.setPreferredSize(d);
    }

    public static void main(String[] args) {
        ExamplesBrowser browser = new ExamplesBrowser();
        browser.setVisible(true);
        browser.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

}
