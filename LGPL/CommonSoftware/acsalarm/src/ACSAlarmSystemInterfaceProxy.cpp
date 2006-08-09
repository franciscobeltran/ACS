/*******************************************************************************
* ALMA - Atacama Large Millimiter Array
* (c) European Southern Observatory, 2006 
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
*
* "@(#) $Id$"
*
* who       when      what
* --------  --------  ----------------------------------------------
* acaproni  2006-07-12  created 
*/

#include "vltPort.h"

#include "ACSAlarmSystemInterfaceProxy.h"

static char *rcsId="@(#) $Id$"; 
static void *use_rcsId = ((void)&use_rcsId,(void *) &rcsId);

ACSAlarmSystemInterfaceProxy::ACSAlarmSystemInterfaceProxy(string name): AlarmSystemInterface() {
	setSourceName(name);
	// Get the logger
	m_logger= new LoggingProxy(0,0,31);
	LoggingProxy::init (m_logger);
}

/**
 * Push a fault state.
 * @param state the fault state change to push.
 */
 void ACSAlarmSystemInterfaceProxy::push(laserSource::FaultState & state) {
	char msgA[16];
	sprintf(msgA,"%d",state.getCode());
	string msg="Alarm sent: <";
	msg+=state.getFamily();
	msg+=",";
	msg+=state.getMember();
	msg+=",";
	msg+=msgA;
	msg+="> ";
	msg+=state.getDescriptor();
	ACS_SHORT_LOG((LM_ALERT, msg.c_str()));
}

/**
 * Push a collection of fault states.
 * @param states
 */
void ACSAlarmSystemInterfaceProxy::push(vector<laserSource::FaultState> & states) {
	for (unsigned int t=0; t<states.size(); t++) {
		laserSource::FaultState fs = states[t];
		push(fs);
	}
}

/**
 * Push the set of active fault states.
 * @param activeFaults the active fault states.
 */
void ACSAlarmSystemInterfaceProxy::pushActiveList(vector<laserSource::FaultState> & activeFaults) {
	push(activeFaults);
}
