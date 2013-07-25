/*
 * ALMA - Atacama Large Millimiter Array
 * Copyright (c) European Southern Observatory, 2011 
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 */
package alma.acs.monitoring.blobber;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.descriptive.SummaryStatistics;

import alma.acs.monitoring.DAO.ComponentData;
import alma.acs.monitoring.DAO.ComponentStatistics;

/**
 * Extends the ComponentData type with a list of monitor data
 * that is used only during processing by the upper blobber layer.
 * <p>
 * Up to ACS 12.0, this was an inner class of CollectorList, where it did not really fit.
 * 
 * @author hsommer
 */
class BlobData extends ComponentData
{
	final List<Object> dataList = new ArrayList<Object>();

	@Override
	public void reset() {
		super.reset();
		dataList.clear();
	}

	/**
	 * Calculates the statistics and stores it in {@link #statistics}
	 * if our monitor point data is represented as BigDecimal objects;
	 * otherwise this call is ignored.
	 * 
	 * @param inDataList
	 */
	void calculateStatistics() {
		if (dataList.size() > 0 && dataList.get(0) instanceof BigDecimal) {
			
			SummaryStatistics stat = new SummaryStatistics();
			
			for (Object blobData : dataList) {
				BigDecimal value = (BigDecimal) blobData;
				stat.addValue(value.doubleValue());
			}

			statistics = new ComponentStatistics();
			statistics.min = BigDecimal.valueOf(stat.getMin());
			statistics.max = BigDecimal.valueOf(stat.getMax());
			statistics.mean = BigDecimal.valueOf(stat.getMean());
			statistics.stdDev = BigDecimal.valueOf(stat.getStandardDeviation());
		}
	}

}