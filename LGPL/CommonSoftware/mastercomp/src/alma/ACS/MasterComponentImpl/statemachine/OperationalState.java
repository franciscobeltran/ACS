/*******************************************************************************
 * ALMA - Atacama Large Millimeter Array
 * Copyright (c) ESO - European Southern Observatory, 2011
 * (in the framework of the ALMA collaboration).
 * All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307  USA
 *******************************************************************************/
package alma.ACS.MasterComponentImpl.statemachine;

import alma.ACS.SUBSYSSTATE_OPERATIONAL;
import alma.acs.genfw.runtime.sm.AcsSimpleState;
import alma.acs.genfw.runtime.sm.AcsState;

public class OperationalState extends AvailableSubStateAbstract implements AcsSimpleState
{

	public OperationalState(AlmaSubsystemContext superContext,
			AvailableState context) {
		super(superContext, context);

	}

	public String stateName() {
		return SUBSYSSTATE_OPERATIONAL.value;
	}

	public void activate(String eventName) {
		synchronized (m_superContext) {		
			m_context.setSubstate(this, eventName);
		}
	}
	

	public void entry() {
	}

	public void start() {
		m_context.setSubstate(m_superContext.m_stateOperational, "start");
	}

	public void stop() {
		m_context.setSubstate(m_superContext.m_stateOnline, "stop");
	}

	/**
	 * @see alma.ACS.MasterComponentImpl.statemachine.AcsState#getStateHierarchy()
	 */
	public AcsState[] getStateHierarchy() {
		return new AcsState[] {this};
	}

}
