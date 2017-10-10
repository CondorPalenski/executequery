/*
 * AbstractLobRecordDataItem.java
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

package org.executequery.gui.resultset;

public abstract class AbstractLobRecordDataItem extends AbstractRecordDataItem
												implements LobRecordDataItem {

	private byte[] data;
	String tableName;

	public AbstractLobRecordDataItem(String table,String name, int dataType, String dataTypeName,int row) {

		super(name, dataType, dataTypeName,row);
		tableName=table;
	}

	@Override
    public int length() {

		return (data == null ? 0 : data.length);
	}

	@Override
    public byte[] getData() {

		if (data == null) {

			data = readLob();
		}

		return data;
	}

	abstract byte[] readLob();

    @Override
    public String asBinaryString() {

        char space = ' ';
        String stripPrefix = "ffffff";

        int defaultBytesToProcess = 496;
        int bytesToProcess = Math.min(data.length, defaultBytesToProcess);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytesToProcess; i++) {

            String hexString = Integer.toHexString(data[i]);
            if (hexString.startsWith(stripPrefix)) {

                hexString = hexString.substring(stripPrefix.length());
            }

            if (hexString.length() == 1) {

                sb.append('0');
            }

            sb.append(hexString.toUpperCase()).append(space);
        }

        if (bytesToProcess == defaultBytesToProcess) {

            sb.append("..");
        }

        return sb.toString();
    }

    @Override
    public boolean isLob() {

        return true;
    }

}






