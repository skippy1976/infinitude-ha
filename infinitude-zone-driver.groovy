/*
    Infinitude Zone driver

* Intial release: 0.0.1
*/

import groovy.json.JsonOutput

def version() { return '0.0.1' }

metadata {
    definition (
            name: "Infinitude Zone",
            namespace: "Infinitude",
            author: "skippy76",
            importUrl: 'https://raw.githubusercontent.com/skippy1976/infinitude-ha/refs/heads/main/infinitude-zone-driver.groovy'
    ) {
        capability "Refresh"
        capability "Initialize"
        capability "Thermostat"
        capability "RelativeHumidityMeasurement"

        attribute "CurrentActivity", "STRING"
        attribute "DamperPosition", "NUMBER"
        attribute "Enabled", "STRING"
        attribute "FanMode", "STRING"
        attribute "Hold", "STRING"
        attribute "Occupancy", "STRING"
        attribute "OccupancOverride", "STRING"
        attribute "OverrideTemperatureModeReset", "STRING"
        attribute "TemperatureUnit", "STRING"
        attribute "ZoneConditioning", "STRING"

        // Commands needed to change internal attributes of virtual device.
        command "setTemperature", ["NUMBER"]
        command "setThermostatOperatingState", ["ENUM"]
        command "setThermostatSetpoint", ["NUMBER"]
        command "setHumiditySetpoint", ["NUMBER"]
        command "setSupportedThermostatFanModes", ["JSON_OBJECT"]
        command "setSupportedThermostatModes", ["JSON_OBJECT"]
    }

    preferences {
        input( name: "enableDebug", type:"bool", title: "Enable debug logging", defaultValue: false)
    }
}

// ***** Hubitat System Methods *****

def installed() {
    ifDebug('Infinitude zone device added (installed)...')
    // It's good practice to initialize on install.
}

def uninstalled() {
    ifDebug('Infinitude zone driver Uninstalled...')
}

def updated() {
    // called when settings updated for device (zone)
    ifDebug('Infinitude zone device updated')
    ifDebug("debug logging is: ${logEnable == true}")
    if (logEnable) runIn(1800,logsOff)
    initialize()
}

def subscribe() {
    ifDebug('Subscribe method.')
}

String label() {
    ifDebug('Label method.')
    return getName()
}

// ***** Capability Command Methods *****

def initialize() {
    ifDebug('Initialize')
    // called when system startup occurs (capability.initialize)
}

def refresh() {
    // called when the driver refresh occurs (capability.refresh)
    ifDebug('Infinitude zone driver refresh...')
}

def auto() {
    // called when changing mode to auto : Capability Thermostat
    ifDebug('auto() was called')
    setThermostatMode('auto')
}

def cool() {
    // called when changing mode to cool : Capability Thermostat
    ifDebug('cool() was called')
    setThermostatMode('cool')
}

def emergencyHeat() {
    // called when changing mode to emergencyHeat : Capability Thermostat
    ifDebug('heat() was called')
    setThermostatMode('heat')
}

def fanAuto() {
    // called when changing fan mode to auto : Capability Thermostat
    ifDebug('fanAuto() was called')
    setThermostatFanMode('auto')
}

def fanCirculate() {
    // called when (not sure) : Capability Thermostat
    ifDebug('fanCirculate() was called')
    setThermostatFanMode('circulate')
}

def fanOn() {
    // called when turning fan on : Capability Thermostat
    ifDebug('fanOn() was called')
    setThermostatFanMode('on')
}

def heat() {
    // called when changing mode to heat : Capability Thermostat
    ifDebug('heat() was called')
    setThermostatMode('heat')
}

def off() {
    // called when changing mode to off : Capability Thermostat
    ifDebug('off() was called')
    setThermostatMode('off')
}

def setCoolingSetpoint(temperature) {
    // called to set cooling setpoint : Capability Thermostat
    ifDebug("setCoolingSetpoint(${setpoint}) was called")
}

def setHeatingSetpoint(temperature) {
    // called to set heating setpoint : Capability Thermostat
    ifDebug("setHeatingSetpoint(${setpoint}) was called")
}

def setThermostatFanMode(fanmode) {
    // called to set thermostat fan mode : Capability Thermostat
    ifDebug('setThermostatFanMode() was called')
}

def setThermostatMode(thermostatmode) {
    // called to set thermostat mode : Capability Thermostat
    ifDebug("setThermostatMode(${mode}) was called")
    setThermostatOperatingState ('idle')
}

// *** Other methods

def setHumiditySetpoint(setpoint) {
    ifDebug "setHumiditySetpoint(${setpoint}) was called"
}

def setThermostatOperatingState(operatingState) {
    ifDebug("setThermostatOperatingState (${operatingState}) was called")
}

def setThermostatSetpoint(setpoint) {
    ifDebug("setThermostatSetpoint(${setpoint}) was called")
}

def setSupportedThermostatFanModes(fanModes) {
    ifDebug "setSupportedThermostatFanModes(${fanModes}) was called"
    // (auto, circulate, on)
    sendEvent(name: "supportedThermostatFanModes", value: fanModes, descriptionText: getDescriptionText("supportedThermostatFanModes set to ${fanModes}"))
}

def setSupportedThermostatModes(modes) {
    ifDebug "setSupportedThermostatModes(${modes}) was called"
    // (auto, cool, emergency heat, heat, off)
    sendEvent(name: "supportedThermostatModes", value: modes, descriptionText: getDescriptionText("supportedThermostatModes set to ${modes}"))
}

// ***** Custom Methods *****

def logsOff(){
    // method to turn off logging
    ifDebug('debug logging disabled...')
    device.updateSetting('enableDebug', [value: 'false', type:'bool'])
}

def setSchedule(schedule) {
    sendEvent(name: "schedule", value: "${schedule}", descriptionText: getDescriptionText("schedule is ${schedule}"))
}

def parse(String description) {
    ifDebug "$description"
}

private getDescriptionText(msg) {
    def descriptionText = "${device.displayName} ${msg}"
    return descriptionText
}

// Updates an existing zone child device with new data.
public updateZone(zoneConfig, zoneStatus, systemConfig) {
    ifDebug("Updating zone: ${label}")

    // This flag prevents command loops. Set it before sending events.
    state.updatingFromPhysicalThermostat = true

    def tempUnit = parent.TemperatureUnit == 'F' ? '°F' : '°C'

    if (zoneStatus.currentActivity[0]) {
        sendEvent(name: "CurrentActivity", value: zoneStatus.currentActivity[0])
    }
    if (zoneStatus.damperposition[0]) {
        sendEvent(name: "DamperPosition", value: zoneStatus.damperposition[0])
    }
    if (zoneStatus.enabled[0]) {
        sendEvent(name: "Enabled", value: zoneStatus.enabled[0])
    }
    if (zoneStatus.fan[0]) {
        sendEvent(name: "FanMode", value: zoneStatus.fan[0])
    }
    if (zoneStatus.hold[0]) {
        sendEvent(name: "Hold", value: zoneStatus.hold[0])
    }
    if (zoneStatus.occupancy[0]) {
        sendEvent(name: "Occupancy", value: zoneStatus.occupancy[0])
    }
    if (zoneStatus.occupancyOverride[0]) {
        sendEvent(name: "OccupancOverride", value: zoneStatus.occupancyOverride[0])
    }
    if (zoneStatus.hold[0] == "on") {
        sendEvent(name: "OverrideTemperatureModeReset", value: zoneStatus.otmr[0])
    } else {
        sendEvent(name: "OverrideTemperatureModeReset", value: "")
    }
    if (zoneStatus.rh[0]) {
        sendEvent(name: "humidity", value: zoneStatus.rh[0], unit: "%")
    }
    if (zoneStatus.zoneconditioning[0]) {
        sendEvent(name: "ZoneConditioning", value: zoneStatus.zoneconditioning[0])
    }

    sendEvent(name: "TemperatureUnit", value: tempUnit)

    // *** capability Thermostat
    // The API uses 'off' for the fan when it's in auto mode.
    def fanMode = (zoneStatus.fan[0] == 'off') ? 'auto' : zoneStatus.fan[0]
    sendEvent(name: "thermostatFanMode", value: fanMode)
    if (zoneStatus.clsp[0]) {
        sendEvent(name: "coolingSetpoint", value: zoneStatus.clsp[0])
    }
    if (zoneStatus.htsp[0]) {
        sendEvent(name: "heatingSetpoint", value: zoneStatus.htsp[0])
    }
    if (zoneStatus.rt[0]) {
        sendEvent(name: "temperature", value: zoneStatus.rt[0], unit: tempUnit)
    }

    // The main setpoint depends on the current system mode.
    if (systemConfig.mode[0]) {
        def currentMode = systemConfig.mode[0]

        if (currentMode == 'cool') {
            sendEvent(name: "thermostatSetpoint", value: zoneStatus.clsp[0], unit: tempUnit)
        } else if (currentMode == 'heat') {
            sendEvent(name: "thermostatSetpoint", value: zoneStatus.htsp[0], unit: tempUnit)
        } else {
            log.warn "Unknown systemconfig mode ${currentMode}"
        }

        // Map the system mode to a valid thermostat mode.
        sendEvent(name: "thermostatMode", value: currentMode)

        // Map the API's conditioning state to a standard operating state.
        def opState = (zoneStatus.zoneconditioning[0] == 'idle') ? 'idle' : currentMode
        sendEvent(name: "thermostatOperatingState", value: opState)
    }

    // Clear the flag after all events have been sent.
    state.updatingFromPhysicalThermostat = false
}

// Helper method for debug logging.
private ifDebug(msg) {
    if (enableDebug) {
        // The log prefix helps identify messages from this specific device instance.
        log.debug "${device.displayName}: ${msg}"
    }
}
