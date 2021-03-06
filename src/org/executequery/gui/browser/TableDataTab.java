/*
 * TableDataTab.java
 *
 * Copyright (C) 2002-2017 Takis Diakoumis
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

package org.executequery.gui.browser;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.Types;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.Timer;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DateTimePicker;
import com.github.lgooddatepicker.components.TimePicker;
import org.apache.commons.lang.StringUtils;
import org.executequery.Constants;
import org.executequery.EventMediator;
import org.executequery.GUIUtilities;
import org.executequery.components.CancelButton;
import org.executequery.databasemediators.QueryTypes;
import org.executequery.databasemediators.spi.DefaultStatementExecutor;
import org.executequery.databasemediators.spi.StatementExecutor;
import org.executequery.databaseobjects.DatabaseObject;
import org.executequery.databaseobjects.DatabaseTable;
import org.executequery.databaseobjects.TableDataChange;
import org.executequery.event.ApplicationEvent;
import org.executequery.event.DefaultUserPreferenceEvent;
import org.executequery.event.UserPreferenceEvent;
import org.executequery.event.UserPreferenceListener;
import org.executequery.gui.BaseDialog;
import org.executequery.gui.ExecuteQueryDialog;
import org.executequery.gui.editor.ResultSetTableContainer;
import org.executequery.gui.editor.ResultSetTablePopupMenu;
import org.executequery.gui.resultset.*;
import org.executequery.log.Log;
import org.executequery.util.ThreadUtils;
import org.underworldlabs.jdbc.DataSourceException;
import org.underworldlabs.swing.*;
import org.underworldlabs.swing.plaf.UIUtils;
import org.underworldlabs.swing.table.SortableHeaderRenderer;
import org.underworldlabs.swing.table.TableSorter;
import org.underworldlabs.swing.toolbar.PanelToolBar;
import org.underworldlabs.swing.util.SwingWorker;
import org.underworldlabs.util.MiscUtils;
import org.underworldlabs.util.SystemProperties;

/**
 * @author Takis Diakoumis
 * @version $Revision: 1780 $
 * @date $Date: 2017-09-03 15:52:36 +1000 (Sun, 03 Sep 2017) $
 */
public class TableDataTab extends JPanel
        implements ResultSetTableContainer, TableModelListener, UserPreferenceListener {

    private ResultSetTableModel tableModel;

    private ResultSetTable table;

    private JScrollPane scroller;

    private DatabaseObject databaseObject;

    private boolean executing = false;

    private SwingWorker worker;

    private boolean cancelled;

    private GridBagConstraints scrollerConstraints;

    private GridBagConstraints errorLabelConstraints;

    private GridBagConstraints rowCountPanelConstraints;

    private GridBagConstraints canEditTableNoteConstraints;

    private DisabledField rowCountField;

    private JPanel rowCountPanel;

    private final boolean displayRowCount;

    private List<TableDataChange> tableDataChanges;

    private JPanel canEditTableNotePanel;

    private JLabel canEditTableLabel;

    private boolean alwaysShowCanEditNotePanel;

    private InterruptibleProcessPanel cancelPanel;

    private JPanel buttonsEditingPanel;

    StatementExecutor querySender;

    public TableDataTab(boolean displayRowCount) {

        super(new GridBagLayout());
        this.displayRowCount = displayRowCount;

        try {

            init();

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    private void init() throws Exception {

        if (displayRowCount) {

            initRowCountPanel();
        }
        createButtonsEditingPanel();

        canEditTableNotePanel = createCanEditTableNotePanel();
        canEditTableNoteConstraints = new GridBagConstraints(1, 1, 1, 1, 1.0, 0,
                GridBagConstraints.NORTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 5), 0, 0);

        scroller = new JScrollPane();
        scrollerConstraints = new GridBagConstraints(1, 2, 1, 1, 1.0, 1.0,
                GridBagConstraints.SOUTHEAST,
                GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 5), 0, 0);

        rowCountPanelConstraints = new GridBagConstraints(1, 3, 1, 1, 1.0, 0,
                GridBagConstraints.SOUTHWEST,
                GridBagConstraints.HORIZONTAL,
                new Insets(0, 5, 5, 5), 0, 0);

        errorLabelConstraints = new GridBagConstraints(1, 1, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER,
                GridBagConstraints.BOTH,
                new Insets(5, 5, 5, 5), 0, 0);

        tableDataChanges = new ArrayList<TableDataChange>();
        alwaysShowCanEditNotePanel = SystemProperties.getBooleanProperty(
                Constants.USER_PROPERTIES_KEY, "browser.always.show.table.editable.label");

        cancelPanel = new InterruptibleProcessPanel("Executing query for data...");

        EventMediator.registerListener(this);
    }

    private JPanel createCanEditTableNotePanel() {

        final JPanel panel = new JPanel(new GridBagLayout());

        canEditTableLabel = new UpdatableLabel();
        JButton hideButton = new LinkButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                panel.setVisible(false);
            }
        });
        hideButton.setText("Hide");

        JButton alwaysHideButton = new LinkButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                panel.setVisible(false);
                alwaysShowCanEditNotePanel = false;

                SystemProperties.setBooleanProperty(Constants.USER_PROPERTIES_KEY,
                        "browser.always.show.table.editable.label", false);

                EventMediator.fireEvent(new DefaultUserPreferenceEvent(TableDataTab.this, null, UserPreferenceEvent.ALL));

            }
        });
        alwaysHideButton.setText("Always Hide");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx++;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(canEditTableLabel, gbc);
        gbc.gridx++;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        panel.add(hideButton, gbc);
        gbc.gridx++;
        gbc.insets.left = 15;
        gbc.insets.right = 10;
        panel.add(alwaysHideButton, gbc);

        panel.setBorder(UIUtils.getDefaultLineBorder());

        return panel;
    }

    private Timer timer;

    public void loadDataForTable(final DatabaseObject databaseObject) {

        addInProgressPanel();

        if (timer != null) {

            timer.cancel();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {

                load(databaseObject);
            }
        }, 600);

    }

    private void load(final DatabaseObject databaseObject) {

        ConnectionsTreePanel treePanel = (ConnectionsTreePanel) GUIUtilities.getDockedTabComponent(ConnectionsTreePanel.PROPERTY_KEY);
        synchronized (treePanel) {
            treePanel.getTree().setEnabled(false);
            if (worker != null) {

                cancel();
                worker.interrupt();
            }

            worker = new SwingWorker() {

                public Object construct() {
                    try {
                        executing = true;
                        return setTableResultsPanel(databaseObject);

                    } catch (Exception e) {

                        addErrorLabel(e);
                        return "done";
                    }
                }

                public void finished() {

                    executing = false;
                    cancelled = false;

                    ConnectionsTreePanel treePanel = (ConnectionsTreePanel) GUIUtilities.getDockedTabComponent(ConnectionsTreePanel.PROPERTY_KEY);
                    treePanel.getTree().setEnabled(true);
                }

            };
            worker.start();
        }
    }

    private void addInProgressPanel() {

        ThreadUtils.invokeLater(new Runnable() {

            @Override
            public void run() {

                removeAll();
                add(cancelPanel, scrollerConstraints);

                repaint();
                cancelPanel.start();
            }
        });

    }

    private void cancel() {

        if (executing) {
            try {

                Log.debug("Cancelling open statement for data tab for table - " + databaseObject.getName());
                cancelStatement();

            } finally {

                cancelled = true;
            }
        }

    }

    private List<String> primaryKeyColumns = new ArrayList<String>(0);
    private List<String> foreignKeyColumns = new ArrayList<String>(0);
    List<org.executequery.databaseobjects.impl.ColumnConstraint> foreigns;

    private Object setTableResultsPanel(DatabaseObject databaseObject) {
        querySender = new DefaultStatementExecutor(databaseObject.getHost().getDatabaseConnection(), true);
        tableDataChanges.clear();
        primaryKeyColumns.clear();
        foreignKeyColumns.clear();

        this.databaseObject = databaseObject;
        try {

            initialiseModel();
            tableModel.setCellsEditable(false);
            tableModel.removeTableModelListener(this);

            if (isDatabaseTable()) {

                DatabaseTable databaseTable = asDatabaseTable();
                if (databaseTable.hasPrimaryKey()) {

                    primaryKeyColumns = databaseTable.getPrimaryKeyColumnNames();
                    canEditTableLabel.setText("This table has a primary key(s) and data may be edited here");
                }

                if (databaseTable.hasForeignKey()) {

                    foreignKeyColumns = databaseTable.getForeignKeyColumnNames();
                    foreigns = databaseTable.getForeignKeys();
                } else foreigns = new ArrayList<>();

                if (primaryKeyColumns.isEmpty()) {

                    canEditTableLabel.setText("This table has no primary keys defined and is not editable here");
                }

                canEditTableNotePanel.setVisible(alwaysShowCanEditNotePanel);
            }

            if (!isDatabaseTable()) {

                canEditTableNotePanel.setVisible(false);
                buttonsEditingPanel.setVisible(false);
            }

            Log.debug("Retrieving data for table - " + databaseObject.getName());
            try {
                ResultSet resultSet = databaseObject.getData(true);
                tableModel.createTable(resultSet);

            } catch (Exception e) {
                Log.error("Error retrieving data for table - " + databaseObject.getName() + ". Try to rebuild table model.");
                ResultSet resultSet = databaseObject.getMetaData();
                tableModel.createTableFromMetaData(resultSet, databaseObject.getHost().getDatabaseConnection());
            }


            if (table == null) {

                createResultSetTable();
            }

            tableModel.setNonEditableColumns(primaryKeyColumns);

            TableSorter sorter = new TableSorter(tableModel);
            table.setModel(sorter);
            sorter.setTableHeader(table.getTableHeader());

            if (isDatabaseTable()) {

                SortableHeaderRenderer renderer = new SortableHeaderRenderer(sorter) {

                    private ImageIcon primaryKeyIcon = GUIUtilities.loadIcon(BrowserConstants.PRIMARY_COLUMNS_IMAGE);
                    private ImageIcon foreignKeyIcon = GUIUtilities.loadIcon(BrowserConstants.FOREIGN_COLUMNS_IMAGE);

                    @Override
                    public Component getTableCellRendererComponent(JTable table,
                                                                   Object value, boolean isSelected, boolean hasFocus,
                                                                   int row, int column) {

                        DefaultTableCellRenderer renderer = (DefaultTableCellRenderer) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                        Icon keyIcon = iconForValue(value);
                        if (keyIcon != null) {

                            Icon icon = renderer.getIcon();
                            if (icon != null) {

                                BufferedImage image = new BufferedImage(icon.getIconWidth() + keyIcon.getIconWidth() + 2,
                                        Math.max(keyIcon.getIconHeight(), icon.getIconHeight()), BufferedImage.TYPE_INT_ARGB);

                                Graphics graphics = image.getGraphics();
                                keyIcon.paintIcon(null, graphics, 0, 0);
                                icon.paintIcon(null, graphics, keyIcon.getIconWidth() + 2, 5);

                                setIcon(new ImageIcon(image));

                            } else {

                                setIcon(keyIcon);
                            }

                        }

                        return renderer;
                    }

                    private ImageIcon iconForValue(Object value) {

                        if (value != null) {

                            String name = value.toString();
                            if (primaryKeyColumns.contains(name)) {

                                return primaryKeyIcon;

                            } else if (foreignKeyColumns.contains(name)) {

                                return foreignKeyIcon;
                            }

                        }

                        return null;
                    }


                };
                sorter.setTableHeaderRenderer(renderer);

            }

            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            if (foreigns != null)
                if (foreigns.size() > 0)
                    for (org.executequery.databaseobjects.impl.ColumnConstraint key : foreigns) {
                        Vector items = itemsForeign(key);
                        table.setComboboxColumn(tableModel.getColumnIndex(key.getColumnName()), items);
                    }


            scroller.getViewport().add(table);
            removeAll();

            add(/*canEditTableNotePanel*/buttonsEditingPanel, canEditTableNoteConstraints);
            add(scroller, scrollerConstraints);

            if (displayRowCount && SystemProperties.getBooleanProperty("user", "browser.query.row.count")) {

                add(rowCountPanel, rowCountPanelConstraints);
                rowCountField.setText(String.valueOf(sorter.getRowCount()));
            }

        } catch (DataSourceException e) {

            if (!cancelled) {

                addErrorLabel(e);

            } else {

                addCancelledLabel();
            }

        } finally {

            tableModel.addTableModelListener(this);
        }

        setTableProperties();
        validate();
        repaint();

        return "done";
    }

    Vector itemsForeign(org.executequery.databaseobjects.impl.ColumnConstraint key) {
        String query = "SELECT distinct " + key.getReferencedColumn() + " FROM " + key.getReferencedTable() + " order by 1";
        Vector items = new Vector();
        try {
            ResultSet rs = querySender.execute(QueryTypes.SELECT, query).getResultSet();
            while (rs.next()) {
                items.add(rs.getObject(1));
            }
        } catch (Exception e) {
            Log.error(e.getMessage());
        }
        items.add(null);
        return items;
    }

    private void initialiseModel() {

        if (tableModel == null) {

            tableModel = new ResultSetTableModel(SystemProperties.getIntProperty("user", "browser.max.records"));
            tableModel.setHoldMetaData(false);
        }

    }

    private boolean isDatabaseTable() {

        return this.databaseObject instanceof DatabaseTable;
    }

    private void addErrorLabel(Throwable e) {

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p><center>Error retrieving object data");
        String message = e.getMessage();
        if (StringUtils.isNotBlank(message)) {

            sb.append("<br />[ ").append(message);
        }

        sb.append(" ]</center></p><p><center><i>(Note: Data will not always be available for all object types)</i></center></p></body></html>");

        addErrorPanel(sb);
    }

    private void addErrorPanel(StringBuilder sb) {

        removeAll();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 20, 10, 20);
        panel.add(new JLabel(sb.toString()), gbc);
        panel.setBorder(UIUtils.getDefaultLineBorder());

        add(panel, errorLabelConstraints);
    }

    private void addCancelledLabel() {

        StringBuilder sb = new StringBuilder();
        sb.append("<html><body><p><center>Statement execution cancelled at user request.");
        sb.append("</center></p><p><center><i>(Note: Data will not always be available for all object types)</i></center></p></body></html>");

        addErrorPanel(sb);
    }

    private void createResultSetTable() {

        table = new ResultSetTable();
        table.addMouseListener(new ResultSetTablePopupMenu(table, this));
        setTableProperties();
    }

    void insert_record(List<JComponent> components, List<Integer> types, List<ResultSetColumnHeader> rschs, BaseDialog dialog) {
        String query = "INSERT INTO " + databaseObject.getNameForQuery();
        String columns = "(";
        String values = " VALUES (";
        for (int i = 0; i < components.size(); i++) {
            String value = "";
            String component_value;
            JComponent component = components.get(i);
            int sqlType;
            ResultSetColumnHeader rsch = rschs.get(i);
            columns += rsch.getName();
            if (i != components.size() - 1)
                columns += " , ";
            sqlType = rsch.getDataType();
            int type = types.get(i);
            boolean str = false;
            switch (type) {
                case 2017:
                    component_value = String.valueOf(((JComboBox) component).getSelectedItem());
                    break;
                case Types.DATE:
                    component_value = ((DatePicker) component).getDateStringOrEmptyString();
                    break;
                case Types.TIMESTAMP:
                    component_value = ((EQDateTimePicker) component).getStringValue();
                    break;
                case Types.TIME:
                    component_value =((EQTimePicker) component).getStringValue();//((DateTimePicker) component).timePicker.getTimeStringOrEmptyString();
                    break;
                case Types.BOOLEAN:
                    component_value = ((RDBCheckBox)component).getStringValue();
                    break;
                default:
                    component_value = ((JTextField) component).getText();
                    break;
            }
            switch (sqlType) {

                case Types.LONGVARCHAR:
                case Types.LONGNVARCHAR:
                case Types.CHAR:
                case Types.NCHAR:
                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.CLOB:
                case Types.DATE:
                case Types.TIME:
                case Types.TIMESTAMP:
                    value = "'";
                    str = true;
                    break;
                default:
                    break;
            }
            if (MiscUtils.isNull(component_value))
                value = "NULL";
            else {
                value += component_value;
            }

            if (str && value != "NULL")
                value += "'";
            values = values + " " + value;
            if (i < components.size() - 1)
                values += ",";

        }
        columns += ")";
        values += ")";
        query = query + columns + " " + values;
        ExecuteQueryDialog eqd = new ExecuteQueryDialog("Insert record", query, databaseObject.getHost().getDatabaseConnection(), true);
        eqd.display();
        if (eqd.getCommit()) {
            dialog.finished();
            loadDataForTable(databaseObject);
        }
    }

    void add_record(ActionEvent actionEvent) {
        int cols = tableModel.getColumnCount();
        List<RecordDataItem> row = new ArrayList<>();
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        GridBagConstraints gbcLabel = new GridBagConstraints();
        gbcLabel.gridx = 0;
        gbc.gridx = 1;
        gbcLabel.gridheight = 1;
        gbc.gridheight = 1;
        gbcLabel.gridwidth = 1;
        gbc.gridwidth = 1;
        gbcLabel.weightx = 0;
        gbc.weightx = 1.0;
        gbcLabel.weighty = 0;
        gbc.weighty = 0;
        gbcLabel.gridy = -1;
        gbc.gridy = -1;
        gbcLabel.fill = GridBagConstraints.NONE;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbcLabel.anchor = GridBagConstraints.WEST;
        gbc.anchor = GridBagConstraints.WEST;
        gbcLabel.ipadx = 0;
        gbc.ipadx = 0;
        gbcLabel.ipady = 0;
        gbc.ipady = 0;
        gbcLabel.insets = new Insets(5, 5, 5, 5);
        gbc.insets = new Insets(5, 5, 5, 5);
        List<Integer> fgns = new ArrayList<>();
        List<Vector> f_items = new ArrayList<>();
        if (foreigns != null)
            if (foreigns.size() > 0)
                for (org.executequery.databaseobjects.impl.ColumnConstraint key : foreigns) {
                    f_items.add(itemsForeign(key));
                    fgns.add(tableModel.getColumnIndex(key.getColumnName()));
                }
        List<JComponent> components = new ArrayList<>();
        List<Integer> types = new ArrayList<>();
        List<ResultSetColumnHeader> rschs = new ArrayList<>();
        for (int i = 0; i < cols; i++) {
            ResultSetColumnHeader rsch = tableModel.getColumnHeaders().get(i);
            if (!databaseObject.getColumns().get(i).isGenerated()) {
                rschs.add(rsch);
                int type = rsch.getDataType();
                String typeName = rsch.getDataTypeName();
                String name = rsch.getName();
                JComponent field;
                JLabel label = new JLabel(name);
                gbcLabel.gridy++;
                gbc.gridy++;
                panel.add(label, gbcLabel);
                if (fgns.contains(i)) {
                    field = new JComboBox(new DefaultComboBoxModel(f_items.get(fgns.indexOf(i))));
                    types.add(2017);
                } else {
                    switch (type) {
                        case Types.DATE:
                            field = new DatePicker();
                            break;
                        case Types.TIMESTAMP:
                            field = new EQDateTimePicker();
                            break;
                        case Types.TIME:
                            field=new EQTimePicker();
                            break;
                        case Types.BOOLEAN:
                            field = new RDBCheckBox();
                            break;
                        default:
                            field = new JTextField(14);
                            break;
                    }
                    types.add(rsch.getDataType());
                }
                panel.add(field, gbc);
                components.add(field);
            }
        }

        JScrollPane scroll = new JScrollPane();
        scroll.setViewportView(panel);
        JPanel mainPane = new JPanel(new GridBagLayout());
        mainPane.add(scroll, new GridBagConstraints(0, 0, 1, 1, 1, 1,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        BaseDialog dialog = new BaseDialog("Adding record", true, mainPane);
        gbcLabel.gridy++;
        gbc.gridy++;
        gbcLabel.weightx = 0;
        gbcLabel.fill = GridBagConstraints.HORIZONTAL;
        JButton b_cancel = new JButton("Cancel");
        b_cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                dialog.finished();
            }
        });
        JButton b_ok = new JButton("Ok");
        b_ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                insert_record(components, types, rschs, dialog);
            }
        });
        panel.add(b_cancel, gbcLabel);
        panel.add(b_ok, gbc);
        dialog.display();

        //tableModel.AddRow(row);
    }

    void delete_record() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            String query = "DELETE FROM " + databaseObject.getNameForQuery() + " WHERE ";
            String order="";
            for (int i = 0; i < tableModel.getColumnHeaders().size(); i++) {
                String value = "";
                ResultSetColumnHeader rsch = tableModel.getColumnHeaders().get(i);
                int sqlType = rsch.getDataType();
                boolean str = false;
                switch (sqlType) {

                    case Types.LONGVARCHAR:
                    case Types.LONGNVARCHAR:
                    case Types.CHAR:
                    case Types.NCHAR:
                    case Types.VARCHAR:
                    case Types.NVARCHAR:
                    case Types.CLOB:
                    case Types.DATE:
                    case Types.TIME:
                    case Types.TIMESTAMP:
                        value = "'";
                        str = true;
                        break;
                    default:
                        break;
                }
                String temp = String.valueOf(tableModel.getValueAt(row, i));
                if (temp == null) {
                    value = "NULL";
                } else
                    value += temp;
                if (str && value != "NULL")
                    value += "'";
                //if(value=="'null'")
                if (value == "NULL")
                    query = query + " (" + rsch.getName() + " IS " + value + " )";
                else
                    query = query + " (" + rsch.getName() + " = " + value + " )";
                if (i != tableModel.getColumnHeaders().size() - 1)
                    query += " and";
                else order=rsch.getName();

            }
            query+="\nORDER BY "+order+"\n";
            query+="ROWS 1";
            ExecuteQueryDialog eqd = new ExecuteQueryDialog("Delete record", query, databaseObject.getHost().getDatabaseConnection(), true);
            eqd.display();
            if (eqd.getCommit()) {
                loadDataForTable(databaseObject);
            }
        }
    }

    private void createButtonsEditingPanel() {
        buttonsEditingPanel = new JPanel(new GridBagLayout());
        PanelToolBar bar = new PanelToolBar();
        RolloverButton addRolloverButton = new RolloverButton();
        addRolloverButton.setIcon(GUIUtilities.loadIcon("add_16.png"));
        addRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                add_record(actionEvent);
            }
        });
        bar.add(addRolloverButton);
        RolloverButton deleteRolloverButton = new RolloverButton();
        deleteRolloverButton.setIcon(GUIUtilities.loadIcon("delete_16.png"));
        deleteRolloverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                delete_record();
            }
        });
        bar.add(deleteRolloverButton);
        GridBagConstraints gbc3 = new GridBagConstraints(4, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
        buttonsEditingPanel.add(bar, gbc3);
    }

    private void initRowCountPanel() {

        rowCountField = new DisabledField();
        rowCountPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        rowCountPanel.add(new JLabel("Data Row Count:"), gbc);
        gbc.gridx = 2;
        gbc.insets.bottom = 2;
        gbc.insets.left = 5;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.right = 0;
        rowCountPanel.add(rowCountField, gbc);
    }


    /**
     * Whether a SQL SELECT statement is currently being executed by this class.
     *
     * @return <code>true</code> | <code>false</code>
     */
    public boolean isExecuting() {

        return executing;
    }

    /**
     * Cancels the currently executing statement.
     */
    public void cancelStatement() {

        if (worker != null) {

            worker.interrupt();
        }

        worker = new SwingWorker() {
            @Override
            public Object construct() {

                databaseObject.cancelStatement();
                return "done";
            }
        };
        worker.start();
    }

    /**
     * Sets default table display properties.
     */
    public void setTableProperties() {

        if (table == null) {

            return;
        }

        table.applyUserPreferences();
        table.setCellSelectionEnabled(false);

        tableModel.setMaxRecords(SystemProperties.getIntProperty("user", "browser.max.records"));
    }

    public JTable getTable() {

        return table;
    }

    public boolean isTransposeAvailable() {

        return false;
    }

    public void transposeRow(TableModel tableModel, int row) {

        // do nothing
    }

    public void tableChanged(TableModelEvent e) {

        if (isDatabaseTable()) {

            int row = e.getFirstRow();
            if (row >= 0) {

                List<RecordDataItem> rowDataForRow = tableModel.getRowDataForRow(row);
                for (RecordDataItem recordDataItem : rowDataForRow) {

                    if (recordDataItem.isChanged()) {

                        Log.debug("Change detected in column [ " + recordDataItem.getName() + " ] - value [ " + recordDataItem.getValue() + " ]");

                        asDatabaseTable().addTableDataChange(new TableDataChange(rowDataForRow));
                        return;
                    }
                }
            }
        }
    }

    private DatabaseTable asDatabaseTable() {

        if (isDatabaseTable()) {

            return (DatabaseTable) this.databaseObject;
        }
        return null;
    }

    public boolean hasChanges() {

        if (isDatabaseTable()) {

            return asDatabaseTable().hasTableDataChanges();
        }
        return false;
    }

    public boolean canHandleEvent(ApplicationEvent event) {

        return (event instanceof UserPreferenceEvent);
    }

    public void preferencesChanged(UserPreferenceEvent event) {

        alwaysShowCanEditNotePanel = SystemProperties.getBooleanProperty(
                Constants.USER_PROPERTIES_KEY, "browser.always.show.table.editable.label");
    }


    class InterruptibleProcessPanel extends JPanel implements ActionListener {

        private ProgressBar progressBar;

        public InterruptibleProcessPanel(String labelText) {

            super(new GridBagLayout());

            progressBar = ProgressBarFactory.create();
            ((JComponent) progressBar).setPreferredSize(new Dimension(260, 18));

            JButton cancelButton = new CancelButton();
            cancelButton.addActionListener(this);

            GridBagConstraints gbc = new GridBagConstraints();
            Insets ins = new Insets(0, 20, 10, 20);
            gbc.insets = ins;
            add(new JLabel(labelText), gbc);
            gbc.gridy = 1;
            gbc.insets.top = 5;
            add(((JComponent) progressBar), gbc);
            gbc.gridy = 2;
            add(cancelButton, gbc);

            setBorder(UIUtils.getDefaultLineBorder());
        }

        public void start() {

            progressBar.start();
        }

        public void actionPerformed(ActionEvent e) {

            progressBar.stop();
            cancel();
        }

    }

}

