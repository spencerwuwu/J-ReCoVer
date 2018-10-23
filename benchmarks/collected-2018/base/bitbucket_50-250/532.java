// https://searchcode.com/api/result/122824585/

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.fortress.editing;

import com.sun.fortress.nodes.Node;
import java.util.List;
import org.netbeans.modules.fortress.editing.visitors.SignatureVisitor;
import org.netbeans.modules.fortress.editing.visitors.Scope;
import org.netbeans.modules.gsf.api.OffsetRange;
import org.netbeans.modules.gsf.api.ParserFile;
import org.netbeans.modules.gsf.api.ParserResult;

/**
 *
 * @author Caoyuan Deng
 */
public class FortressParserResult extends ParserResult {

    private ParserFile file;
    private AstTreeNode ast;
    private Node rootNode;
    private String source;
    private OffsetRange sanitizedRange = OffsetRange.NONE;
    private String sanitizedContents;
    private FortressParser.Sanitize sanitized;
    private boolean commentsAdded;
    private Scope rootScope;
    private List<Integer> linesOffset;

    public FortressParserResult(FortressParser parser, ParserFile file, Node rootNode, AstTreeNode ast, List<Integer> linesOffset) {
        super(parser, file, FortressMimeResolver.MIME_TYPE);
        this.file = file;
        this.rootNode = rootNode;
        this.ast = ast;
        this.linesOffset = linesOffset;
    }

    public ParserResult.AstTreeNode getAst() {
        return ast;
    }

    public void setAst(AstTreeNode ast) {
        this.ast = ast;
    }

    /** 
     * The root node of the AST produced by the parser.
     */
    public Node getRootNode() {
        return rootNode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Return whether the source code for the parse result was "cleaned"
     * or "sanitized" (modified to reduce chance of parser errors) or not.
     * This method returns OffsetRange.NONE if the source was not sanitized,
     * otherwise returns the actual sanitized range.
     */
    public OffsetRange getSanitizedRange() {
        return sanitizedRange;
    }

    public String getSanitizedContents() {
        return sanitizedContents;
    }

    /**
     * Set the range of source that was sanitized, if any.
     */
    void setSanitized(FortressParser.Sanitize sanitized, OffsetRange sanitizedRange, String sanitizedContents) {
        this.sanitized = sanitized;
        this.sanitizedRange = sanitizedRange;
        this.sanitizedContents = sanitizedContents;
    }

    public FortressParser.Sanitize getSanitized() {
        return sanitized;
    }

    public boolean isCommentsAdded() {
        return commentsAdded;
    }

    public void setCommentsAdded(boolean commentsAdded) {
        this.commentsAdded = commentsAdded;
    }

    public Scope getRootScope() {
        if (rootScope == null) {
            Node node = getRootNode();
            assert node != null : "Attempted to get definition visitor for broken source";

            SignatureVisitor signatureVisitor = new SignatureVisitor(node, linesOffset);
            node.accept(signatureVisitor);
            rootScope = signatureVisitor.getRootScope();
        }

        return rootScope;
    }

    @Override
    public String toString() {
        return "FortressParseResult(file=" + getFile() + ",rootnode=" + rootNode + ")";
    }
}

