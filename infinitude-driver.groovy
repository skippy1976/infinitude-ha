/*
 * Infinitude driver
 *
 * Intial release:
 */

import groovy.json.JsonOutput

def version() { return "Infinitude Hubitat Driver 0.0.1" }

metadata {
    definition(name: "Infinitude System", 
               namespace: "Infinitude", 
               author: "skippy76", 
               importUrl: "") {
        		capability "Polling"
    }

    preferences {
        section("Infinitude Connection") {
			def pollRate = ["0" : "Disabled", "1" : "Poll every minute", "5" : "Poll every 5 minutes", "10" : "Poll every 10 minutes", "15" : "Poll every 15 minutes", "30" : "Poll every 30 minutes (not recommended)"]
			input ("poll_Rate", "enum", title: "Device Poll Rate", options: pollRate, defaultValue: 0)            
        }
    }
}

void installed()
{
	ifDebug("Infinitude driver Installed...")
}

def updated() {
    ifDebug("updated...")
	switch(poll_Rate) {
		case "0" :
			ifDebug("Infinitude Polling is Disabled")
			break
		case "1" :
			runEvery1Minute(poll)
			ifDebug("Poll Rate set at 1 minute")
			break
		case "5" :
			runEvery5Minutes(poll)
			ifDebug("Poll Rate set at 5 minutes")
			break
		case "10" :
			runEvery10Minutes(poll)
			ifDebug("Poll Rate set at 10 minutes")
			break
		case "15" :
			runEvery15Minutes(poll)
			ifDebug("Poll Rate set at 15 minutes")
			break
		case "30" :
			runEvery30Minutes(poll)
			ifDebug("Poll Rate set at 30 minutes")
			break
	}
}

def poll() {
    ifDebug("polling... ")
    
    parent.updated()
}

def createZone(zone, idx, zoneIdx, status, system){
    def name = "infinitude-${idx}-${zoneIdx}"
    
	ifDebug("Creating ${name} with label '${zone.name[0]}' with deviceNetworkId = ${name}")
    addChildDevice("hubitat", "Virtual Thermostat", name, [name: name, isComponent: true, label: zone.name[0]])    
    
    def zoneDevice = getChildDevice(name)
    
    zoneDevice.setSupportedThermostatFanModes(JsonOutput.toJson(["low","medium","high","auto"]))
    zoneDevice.setSupportedThermostatModes(JsonOutput.toJson(["off", "fan only", "auto", "heat", "cool"]))
    
    updateZone(zone, name, status, system)
}

def updateZone(zone, name, status, system) {
    ifDebug("Setting thermostat properties ${name}")
    def zoneDevice = getChildDevice(name)
    
	zoneDevice.setTemperature(status.rt[0]) // real temperature status.rt[0]
    
    // setpoint depends on mode
    if (system.mode[0] == "cool") {
    	zoneDevice.setThermostatSetpoint(status.clsp[0]) // cool setpoint status.clsp[0]
    }
    
    if (system.mode[0] == "heat") {
    	zoneDevice.setThermostatSetpoint(status.htsp[0])  // heat setpoint status.htsp[0]
    }
    
    zoneDevice.setCoolingSetpoint(status.clsp[0])
	zoneDevice.setHeatingSetpoint(status.htsp[0])
    
    zoneDevice.setHumidity(status.rh[0]) // relative humidity status.rh[0]
    zoneDevice.setThermostatMode(system.mode[0]) // system mode system.mode[0]
    if (status.fan[0] == "off") {
    	zoneDevice.setThermostatFanMode("auto") // zone fan mode status.fan[0]
    } else {
    	zoneDevice.setThermostatFanMode(status.fan[0]) // zone fan mode status.fan[0]
    }
    
    zoneDevice.setThermostatOperatingState(status.zoneconditioning[0]) // zone conditioning state status.zoneconditioning[0]
}

def removeZone(zoneInfo){
	ifDebug("Removing ${zoneInfo.zoneName} with deviceNetworkId = ${zoneInfo.deviceNetworkId}")
	deleteChildDevice(zoneInfo.deviceNetworkId)
}

private ifDebug(msg){
	parent.ifDebug('Connection Driver: ' + (msg ?: ""))
}
