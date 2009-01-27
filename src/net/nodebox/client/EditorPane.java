package net.nodebox.client;

import net.nodebox.node.Node;
import net.nodebox.node.NodeTypeLibrary;
import net.nodebox.node.PythonNodeTypeLibrary;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;

public class EditorPane extends Pane {

    private PaneHeader paneHeader;
    private NetworkAddressBar networkAddressBar;
    private CodeArea codeArea;
    private Node node;

    public EditorPane(NodeBoxDocument document) {
        this();
        setDocument(document);
    }

    public EditorPane() {
        setLayout(new BorderLayout(0, 0));
        paneHeader = new PaneHeader(this);
        networkAddressBar = new NetworkAddressBar(this);
        paneHeader.add(networkAddressBar);
        codeArea = new CodeArea();
        codeArea.getInputMap().put(PlatformUtils.getKeyStroke(KeyEvent.VK_R), new ReloadAction());
        add(paneHeader, BorderLayout.NORTH);
        add(codeArea, BorderLayout.CENTER);
    }

    @Override
    public void setDocument(NodeBoxDocument document) {
        super.setDocument(document);
        if (document == null) return;
        setNode(document.getActiveNode());
    }

    public Pane clone() {
        return new EditorPane(getDocument());
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node == node) return;
        this.node = node;
        networkAddressBar.setNode(node);
        if (node == null) return;

        NodeTypeLibrary library = node.getNodeType().getLibrary();
        if (library instanceof PythonNodeTypeLibrary) {
            PythonNodeTypeLibrary pythonLibrary = (PythonNodeTypeLibrary) library;
            File moduleFile = pythonLibrary.getPythonModuleFile();
            if (moduleFile != null) {
                String code = FileUtils.readFile(moduleFile);
                codeArea.setText(code);
                return;
            }
        }
        codeArea.setText("# The source code for this node is not available.");
    }

    private void reload() {
        NodeTypeLibrary library = node.getNodeType().getLibrary();
        if (library instanceof PythonNodeTypeLibrary) {
            PythonNodeTypeLibrary pythonLibrary = (PythonNodeTypeLibrary) library;
            File moduleFile = pythonLibrary.getPythonModuleFile();
            if (moduleFile != null) {
                String code = codeArea.getText();
                FileUtils.writeFile(moduleFile, code);
            }
        }
        getDocument().reloadActiveNode();
    }

    @Override
    public void activeNodeChanged(Node activeNode) {
        setNode(activeNode);
    }

    private class ReloadAction extends AbstractAction {
        private ReloadAction() {
        }

        public void actionPerformed(ActionEvent e) {
            reload();
        }
    }
}
