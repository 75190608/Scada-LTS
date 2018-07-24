package br.org.scadabr.web.dwr;

import java.util.List;

import br.org.scadabr.db.dao.ScriptDao;
import br.org.scadabr.rt.scripting.ScriptRT;
import br.org.scadabr.vo.scripting.ContextualizedScriptVO;
import br.org.scadabr.vo.scripting.ScriptVO;

import com.serotonin.mango.Common;
import com.serotonin.mango.vo.DataPointExtendedNameComparator;
import com.serotonin.mango.vo.DataPointVO;
import com.serotonin.mango.web.dwr.BaseDwr;
import com.serotonin.web.dwr.DwrResponseI18n;
import org.apache.commons.lang3.tuple.MutablePair;
import org.scada_lts.dao.DAO;
import org.scada_lts.mango.service.DataPointService;

public class ScriptsDwr extends BaseDwr {

	public List<DataPointVO> getPoints() {
		List<DataPointVO> allPoints = new DataPointService().getDataPoints(
				DataPointExtendedNameComparator.instance, false);
		return allPoints;
	}

	public List<ScriptVO<?>> getScripts() {
		List<ScriptVO<?>> scripts = new ScriptDao().getScripts();
		return scripts;
	}

	public ScriptVO<?> getScript(int id) {
		if (id == Common.NEW_ID) {
			ContextualizedScriptVO vo = new ContextualizedScriptVO();
			vo.setXid(generateUniqueXid(ScriptVO.XID_PREFIX, "scripts"));
			return vo;
		}

		return new ScriptDao().getScript(id);
	}

	public DwrResponseI18n saveScript(int id, String xid, String name,
			String script, List<MutablePair<Integer, String>> pointsOnContext,
			List<MutablePair<Integer, String>> objectsOnContext) {

		ContextualizedScriptVO vo = new ContextualizedScriptVO();
		vo.setId(id);
		vo.setXid(xid);
		vo.setName(name);
		vo.setScript(script);
		vo.setPointsOnContext(pointsOnContext);
		vo.setObjectsOnContext(objectsOnContext);
		vo.setUserId(Common.getUser().getId());

		DwrResponseI18n response = new DwrResponseI18n();

		vo.validate(response);

		if (!response.getHasMessages())
			new ScriptDao().saveScript(vo);

		response.addData("seId", vo.getId());
		return response;
	}

	private String generateUniqueXid(String prefix, String tableName) {
		String xid = Common.generateXid(prefix);
		while (!isXidUnique(xid, -1, tableName)) {
			xid = Common.generateXid(prefix);
		}
		return xid;
	}

	protected boolean isXidUnique(String xid, int excludeId, String tableName) {

		return DAO.getInstance().getJdbcTemp().queryForInt("select count(*) from " + tableName
				+ " where xid=? and id<>?", xid, excludeId) == 0;

	}

	public void deleteScript(int scriptId) {
		new ScriptDao().deleteScript(scriptId);
	}

	public boolean executeScript(int scriptId) {
		ScriptVO<?> script = new ScriptDao().getScript(scriptId);

		try {
			if (script != null) {
				ScriptRT rt = script.createScriptRT();
				rt.execute();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
}
