package org.antlr.works.debugger.input;

import org.antlr.runtime.Token;
import org.antlr.tool.Grammar;
import org.antlr.works.awtree.AWTreePanel;
import org.antlr.works.debugger.Debugger;
import org.antlr.works.debugger.tree.DBTreeNode;
import org.antlr.works.debugger.tree.DBTreeToken;
import org.antlr.works.prefs.AWPrefs;
import org.antlr.works.prefs.AWPrefsDialog;
import org.antlr.xjlib.foundation.notification.XJNotificationCenter;
import org.antlr.xjlib.foundation.notification.XJNotificationObserver;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
/*

[The "BSD licence"]
Copyright (c) 2005-2006 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

public class DBInputProcessorTree implements DBInputProcessor, XJNotificationObserver {

    public AWTreePanel treePanel;
    public Debugger debugger;

    public InputTreeNode rootNode;
    public InputTreeNode currentNode;
    public InputTreeNode lastNode;

    /** Map of token to tree node information */
    public Map<Integer,NodeInfo> nodeInfoForToken = new HashMap<Integer, NodeInfo>();

    /** Last position in the grammar received from the parser */
    public int line, pos;

    /** Node colors */
    public Color nonConsumedColor;
    public Color consumedColor;
    public Color ltColor;

    public DBInputProcessorTree(AWTreePanel treePanel, Debugger debugger) {
        this.treePanel = treePanel;
        this.debugger = debugger;

        createColors();

        XJNotificationCenter.defaultCenter().addObserver(this, AWPrefsDialog.NOTIF_PREFS_APPLIED);
    }

    public void close() {
        XJNotificationCenter.defaultCenter().removeObserver(this);
    }

    public void updateTreePanel() {
        treePanel.refresh();
        treePanel.scrollNodeToVisible(lastNode);
    }

    public void createColors() {
        nonConsumedColor = AWPrefs.getNonConsumedTokenColor();
        consumedColor = AWPrefs.getConsumedTokenColor();
        ltColor = AWPrefs.getLookaheadTokenColor();
    }

    public void applyColor(Color c) {
        for (NodeInfo info : nodeInfoForToken.values()) {
            if (info.node != null)
                info.node.setColor(c);
        }
    }

    public void reset() {
        nodeInfoForToken.clear();

        rootNode = createNode(null);
        treePanel.setRoot(rootNode);

        currentNode = rootNode;
        lastNode = currentNode;
    }

    public void removeAllLT() {
    }

    public void rewind(int i) {
    }

    public void rewindAll() {
        applyColor(nonConsumedColor);

        currentNode = rootNode;
        lastNode = currentNode;
    }

    public void LT(Token token) {
        InputTreeNode node = processToken(token);
        if(node != null) {
            lastNode = node;
            node.setColor(ltColor);
        }
    }

    public void consumeToken(Token token, int flavor) {
        InputTreeNode node = processToken(token);
        if(node != null) {
            lastNode = node;
            node.setColor(consumedColor);
        }
    }

    public InputTreeNode processToken(Token token) {
        /** Check to see if the token has already been processed */
        NodeInfo info = getNode(token);
        if(info != null) {
            /** Set the current position to the one when the node has been created */
            currentNode = info.currentNode;
            /** Return the node itself */
            return info.node;
        }

        /** The token hasn't been yet processed */
        info = new NodeInfo(token);

        switch(token.getType()) {
            case Token.DOWN:
                currentNode = (InputTreeNode)currentNode.getLastChild();
                info.currentNode = currentNode;
                break;

            case Token.UP:
                if(currentNode == rootNode) {
                    debugger.warning(this, "UP token applied to the root node!");
                }
                if(currentNode == null) {
                    debugger.warning(this, "Warning: currentNode is null, use rootNode instead.");
                    currentNode = rootNode;
                }
                currentNode = (InputTreeNode)currentNode.getParent();
                info.currentNode = currentNode;
                break;

            default:
                if(currentNode == null) {
                    debugger.warning(this, "Warning: currentNode is null, use rootNode instead.");
                    currentNode = rootNode;
                }
                currentNode.add(info.node = createNode(token));
                info.currentNode = currentNode;
                break;
        }

        /** Add all new node to the map using their unique ID */
        DBTreeToken tt = (DBTreeToken)token;
        nodeInfoForToken.put(tt.ID, info);

        return info.node;
    }

    public InputTreeNode createNode(Token token) {
        InputTreeNode node = new InputTreeNode((DBTreeToken)token, debugger.getGrammar().getANTLRGrammar());
        node.setPosition(line, pos);
        return node;
    }

    public NodeInfo getNode(Token token) {
        DBTreeToken tt = (DBTreeToken)token;
        return nodeInfoForToken.get(tt.ID);
    }

    public void setLocation(int line, int pos) {
        this.line = line;
        this.pos = pos;
    }

    public int getCurrentTokenIndex() {
        return 0;
    }

    public DBInputTextTokenInfo getTokenInfoAtTokenIndex(int index) {
        return null;
    }

    public DBInputTextTokenInfo getTokenInfoForToken(Token token) {
        //return rootNode.findNodeWithToken(token);
        return null;
    }

    public void notificationFire(Object source, String name) {
        if(name.equals(AWPrefsDialog.NOTIF_PREFS_APPLIED)) {
            createColors();
        }
    }

    public boolean isBreakpointAtToken(Token token) {
        NodeInfo info = getNode(token);
        return !(info == null || info.node == null) && info.node.breakpoint;
    }

    public static class NodeInfo {

        /** Token */
        public Token token;

        /** Node associated with the token. null if associated with token UP or DOWN */
        public InputTreeNode node;

        /** Current node when the associated node has been created */
        public InputTreeNode currentNode;

        public NodeInfo(Token token) {
            this.token = token;
        }

    }
    
    public class InputTreeNode extends DBTreeNode {

        public boolean breakpoint = false;

        public InputTreeNode(DBTreeToken token, Grammar grammar) {
            super(token, grammar);
        }

        public void toggleBreakpoint() {
            breakpoint = !breakpoint;
            /** Repaint the node to reflect the new state */
            treePanel.getGraphView().repaintNode(this);
        }

        public Color getColor() {
            if(breakpoint)
                return Color.red;
            else
                return super.getColor();
        }

        public String toString() {
            if(token != null)
                return token.getText(); //+" <"+grammar.getTokenDisplayName(token.getType())+">"
            else
                return "nil";
        }

    }
}
