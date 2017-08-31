/*
 * FeedbackCommand.java
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

package org.executequery.actions.helpcommands;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.executequery.GUIUtilities;
import org.executequery.actions.filecommands.PrintPreviewCommand;
import org.executequery.actions.othercommands.AbstractBaseCommand;
import org.executequery.gui.BaseDialog;
import org.executequery.gui.FeedbackPanel;
import org.executequery.log.Log;
import org.executequery.util.StringBundle;
import org.executequery.util.SystemResources;

/** 
 * Command to open the feedback dialog.
 *
 * @author   Takis Diakoumis
 * @version  $Revision: 1770 $
 * @date     $Date: 2017-08-21 22:01:25 +1000 (Mon, 21 Aug 2017) $
 */
public class FeedbackCommand extends AbstractBaseCommand {
    
    public void execute(ActionEvent e) {

        String actionCommand = e.getActionCommand();
        
        try {

            Method method = getClass().getMethod(
                    actionCommand, new Class[]{ActionEvent.class});
            
            method.invoke(this, e);

        } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e1) {
            
            handleException(e1);
        }
        
    }

    private void handleException(Throwable e) {

        Log.error(bundleString("ErrorExecutingFeedbackCommand"), e);
    }

    public void featureRequest(ActionEvent e) {
        
        showDialog(FeedbackPanel.FEATURE_REQUEST, bundleString("FeatureRequest"));
    }

    public void userComments(ActionEvent e) {
        
        showDialog(FeedbackPanel.USER_COMMENTS, bundleString("UserComments"));
    }

    public void bugReport(ActionEvent e) {
        
        showDialog(FeedbackPanel.BUG_REPORT, bundleString("ReportBug"));
    }

    private void showDialog(int type, String title) {
        
        GUIUtilities.showWaitCursor();

        try {

            BaseDialog dialog = new BaseDialog(title, true, true);

            FeedbackPanel panel = new FeedbackPanel(dialog, type);
            
            dialog.addDisplayComponent(panel);
            dialog.display();

        } finally {

            GUIUtilities.showNormalCursor();
        }
    }
    private StringBundle bundle() {

        StringBundle   bundle = SystemResources.loadBundle(FeedbackCommand.class);

        return bundle;
    }

    private String bundleString(String key) {
        return bundle().getString("FeedbackCommand." + key);
    }

}










