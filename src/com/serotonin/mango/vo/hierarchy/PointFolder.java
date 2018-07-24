/*
    Mango - Open Source M2M - http://mango.serotoninsoftware.com
    Copyright (C) 2006-2011 Serotonin Software Technologies Inc.
    @author Matthew Lohbihler
    
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.serotonin.mango.vo.hierarchy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.org.scadabr.protocol.iec101.common101.information.SRQ;
import com.serotonin.json.JsonArray;
import com.serotonin.json.JsonException;
import com.serotonin.json.JsonObject;
import com.serotonin.json.JsonReader;
import com.serotonin.json.JsonRemoteEntity;
import com.serotonin.json.JsonRemoteProperty;
import com.serotonin.json.JsonSerializable;
import com.serotonin.json.JsonValue;
import com.serotonin.mango.Common;
import com.serotonin.mango.util.LocalizableJsonException;
import com.serotonin.mango.vo.DataPointVO;
import org.apache.commons.lang3.tuple.MutablePair;
import org.scada_lts.mango.service.DataPointService;

/**
 * @author Matthew Lohbihler
 * 
 */
@JsonRemoteEntity
public class PointFolder implements JsonSerializable {
    private int id = Common.NEW_ID;
    @JsonRemoteProperty
    private String name;

    @JsonRemoteProperty(innerType = PointFolder.class)
    private List<PointFolder> subfolders = new ArrayList<PointFolder>();

    private List<MutablePair<Integer, String>> points = new ArrayList<>();

    public PointFolder() {
        // no op
    }

    public PointFolder(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public void addSubfolder(PointFolder subfolder) {
        subfolders.add(subfolder);
    }

    public void addDataPoint(MutablePair point) {
        points.add(point);
    }

    public void removeDataPoint(int dataPointId) {
        for (int i = 0; i < points.size(); i++) {
            if (points.get(i).getKey() == dataPointId) {
                points.remove(i);
                return;
            }
        }
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MutablePair<Integer, String>> getPoints() {
        return points;
    }

    public void setPoints(List<MutablePair<Integer, String>> points) {
        this.points = points;
    }

    public List<PointFolder> getSubfolders() {
        return subfolders;
    }

    public void setSubfolders(List<PointFolder> subfolders) {
        this.subfolders = subfolders;
    }

    boolean findPoint(List<PointFolder> path, int pointId) {
        boolean found = false;
        for (MutablePair point : points) {
            if ((Integer)point.getKey() == pointId) {
                found = true;
                break;
            }
        }

        if (!found) {
            for (PointFolder subfolder : subfolders) {
                found = subfolder.findPoint(path, pointId);
                if (found)
                    break;
            }
        }

        if (found)
            path.add(this);

        return found;
    }

    void copyFoldersFrom(PointFolder that) {
        for (PointFolder thatSub : that.subfolders) {
            PointFolder thisSub = new PointFolder(thatSub.getId(), thatSub.getName());
            thisSub.copyFoldersFrom(thatSub);
            subfolders.add(thisSub);
        }
    }

    public PointFolder getSubfolder(String name) {
        for (PointFolder subfolder : subfolders) {
            if (subfolder.name.equals(name))
                return subfolder;
        }
        return null;
    }

    //
    //
    // Serialization
    //
    @Override
    public void jsonSerialize(Map<String, Object> map) {
        DataPointService dataPointService = new DataPointService();
        List<String> pointList = new ArrayList<String>();
        for (MutablePair p : points) {
            DataPointVO dp = dataPointService.getDataPoint((Integer) p.getKey());
            if (dp != null)
                pointList.add(dp.getXid());
        }
        map.put("points", pointList);
    }

    @Override
    public void jsonDeserialize(JsonReader reader, JsonObject json) throws JsonException {
        JsonArray jsonPoints = json.getJsonArray("points");
        if (jsonPoints != null) {
            points.clear();
            DataPointService dataPointService = new DataPointService();

            for (JsonValue jv : jsonPoints.getElements()) {
                String xid = jv.toJsonString().getValue();

                DataPointVO dp = dataPointService.getDataPoint(xid);
                if (dp == null)
                    throw new LocalizableJsonException("emport.error.missingPoint", xid);

                points.add(new MutablePair<>(dp.getId(), dp.getName()));
            }
        }
    }
}
