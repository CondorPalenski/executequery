/*
 * AddBookmarkCommand.java
 *
 * Copyright (C) 2002-2015 Takis Diakoumis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.executequery.actions.queryeditor;

import java.awt.event.ActionEvent;

import org.executequery.GUIUtilities;
import org.executequery.actions.othercommands.ShowHideResultSetColumnsCommand;
import org.executequery.gui.BaseDialog;
import org.executequery.gui.editor.AddQueryBookmarkPanel;
import org.executequery.util.StringBundle;
import org.executequery.util.SystemResources;

/** 
 * <p>The Query Editor's add bookmark command.
 *
 * @author   Takis Diakoumis
 * @version  $Revision: 1487 $
 * @date     $Date: 2015-08-23 22:21:42 +1000 (Sun, 23 Aug 2015) $
 */
public class AddBookmarkCommand extends AbstractQueryEditorCommand {

    public void execute(ActionEvent e) {

        if (isQueryEditorTheCentralPanel()) {

            if (queryEditor().hasText()) {

                BaseDialog dialog = 
                    new BaseDialog(AddQueryBookmarkPanel.TITLE, true);
        
                dialog.addDisplayComponent(
                        new AddQueryBookmarkPanel(dialog, queryEditor().getEditorText()));
                dialog.display();

            } else {
                
                GUIUtilities.displayErrorMessage(bundleString("errorMessage"));
            }
            
        }

    }
    private static StringBundle bundle() {

        StringBundle   bundle = SystemResources.loadBundle(AddBookmarkCommand.class);

        return bundle;
    }

    private static String bundleString(String key) {
        return bundle().getString("AddBookmarkCommand." + key);
    }
    
}










