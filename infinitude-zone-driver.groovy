/*
    Infinitude Zone driver

* Intial release: 0.0.1
* Revised: 0.0.2
*/

import groovy.json.JsonOutput
import java.time.LocalTime
import java.time.format.DateTimeFormatter

def version() { return '0.0.2' }

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
        attribute "ZoneIndex", "NUMBER"

        // Commands needed to change internal attributes of device
        command "fanOnly", []
        command "setThermostatOperatingState", ["ENUM"]
        command "setHumiditySetpoint", ["NUMBER"]
    }

    preferences {
        input( name: "enableDebug", type:"bool", title: "Enable debug logging", defaultValue: true)
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
    log.info('Infinitude zone device updated')
    log.info("debug logging is: ${device.getSetting("enableDebug")}")
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
    // Subscribe to location mode changes to trigger mode mapping.
    subscribe(location, "mode", modeUpdateHandler)
}

def refresh() {
    // called when the driver refresh occurs (capability.refresh)
    ifDebug('Infinitude zone driver refresh...')
}

def fanAuto() {
    // called when changing fan mode to auto : Capability Thermostat
    ifDebug('fanAuto() was called')
    setThermostat(0, 'auto')
}

def fanCirculate() {
    // called when (not sure) : Capability Thermostat
    ifDebug('fanCirculate() was called')
    setThermostat(0, 'circulate')
}

def fanOn() {
    // called when turning fan on : Capability Thermostat
    ifDebug('fanOn() was called')
    setThermostat(0, 'on')
}

def setThermostatFanMode(fanmode) {
    // called to set thermostat fan mode : Capability Thermostat
    ifDebug('setThermostatFanMode() was called with mode ${fanmode}')

    setThermostat(0, fanmode)
}

def setCoolingSetpoint(temperature) {
    // called to set cooling setpoint : Capability Thermostat
    ifDebug("setCoolingSetpoint(${temperature}) was called")
    setThermostat(temperature, "cool")
}

def setHeatingSetpoint(temperature) {
    // called to set heating setpoint : Capability Thermostat
    ifDebug("setHeatingSetpoint(${temperature}) was called")
    setThermostat(temperature, "heat")
}

def off() {
    // called when changing mode to off : Capability Thermostat
    ifDebug('off() was called')
    setThermostatMode('off')
}

def fanOnly() {
    // called when changing mode to fan only : - custom
    ifDebug('fanOnly() was called')
    setThermostatMode('fan only')
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

def heat() {
    // called when changing mode to heat : Capability Thermostat
    ifDebug('heat() was called')
    setThermostatMode('heat')
}

def emergencyHeat() {
    // called when changing mode to emergencyHeat : Capability Thermostat
    ifDebug('heat() was called')
    setThermostatMode('heat')
}

def setThermostatMode(thermostatmode) {
    // called to set thermostat mode : Capability Thermostat
    ifDebug("setThermostatMode(${mode}) was called")
    parent.setInfinitudeSystemMode(thermostatmode)
}

// *** Other methods

def setHumiditySetpoint(setpoint) {
    ifDebug "setHumiditySetpoint(${setpoint}) was called"
}

def setThermostatOperatingState(operatingState) {
    ifDebug("setThermostatOperatingState (${operatingState}) was called")
}

private setThermostat(setpoint, mode) {
    ifDebug("setThermostat(${setpoint}, ${mode}) was called")

    def systemIndex = parent.currentValue("SystemIndex").intValue()
    def zoneIndex = device.currentValue("ZoneIndex").intValue()

    def systems = parent.getSystems()

    // get the activity index
    def activityIndex = findManualIdIndex(systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].activities[0].activity)

    if (!systems) {
        log.error "Could not retrieve systems to set mode."
        return
    }

    if ((mode == "auto") || (mode == "circulate") || (mode == "on")) {
        ifDebug("Attempting to set Thermostat Fan mode system mode to '${mode}' for system index ${systemIndex} and zone ${zoneIndex}")

        if (mode == "auto") {
            systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].activities[0].activity[activityIndex].fan[0] = "off"
        } else if (mode == "circulate") {
            systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].activities[0].activity[activityIndex].fan[0] = "low"
        } else if (mode == "on") {
            systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].activities[0].activity[activityIndex].fan[0] = "high"
        }

    } else {
        ifDebug("Attempting to set Thermostat Setpoint system mode to '${mode}' for system index ${systemIndex} and zone ${zoneIndex}")

        if (mode.toLowerCase() == "cool") {
            systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].activities[0].activity[activityIndex].clsp[0] = setpoint.toString()
        } else if  (mode.toLowerCase() == "heat") {
            systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].activities[0].activity[activityIndex].htsp[0] = setpoint.toString()
        } else {
            return log.warn("Invalid mode for setpoint ${mode}")
        }
    }

    // check if zone is not already in hold
    if (systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].hold[0] == "off") {
        // need to ensure hold is on
        systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].hold[0] = "on"
        systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].holdActivity[0] = ["manual"]
        systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].setback[0] = ["off"]
    }

    // check if holdDuration is 0, if so this means hold forever
    if (parent.getSetting("holdDuration").toInteger() == 0) {
        ifDebug("Hold forever")
        systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].otmr[0] = {}
    } else {
        // Calculate and set the end time for the hold using the app's setting
        String holdTime = getFutureTimePlusMinutes(parent.getSetting("holdDuration").toInteger())
        ifDebug("Hold end time set to ${holdTime}")
        systems.system[systemIndex].config[0].zones[0].zone[zoneIndex].otmr[0] = holdTime
    }

    // Post the entire modified configuration back.
    parent.updateSystems(systems)
}

// ***** Custom Methods *****

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

void modeUpdateHandler(evt) {
    ifDebug("Location mode changed to '${evt.value}'.")
    def settingName = "mode_${evt.value.replaceAll(/\s+/, "")}"
    def infinitudeMode = parent.getModeMapping(settingName)

    if (infinitudeMode) {
        ifDebug("Mapping '${evt.value}' to Infinitude mode '${infinitudeMode}'.")
        setInfinitudeZoneMode(infinitudeMode)
    } else {
        ifDebug("No mapping found for Hubitat mode '${evt.value}'. No action taken.")
    }
}

// This method actually sends the mode change to the Infinitude server.
void setInfinitudeZoneMode(String mode) {
    def systemIndex = parent.currentValue("SystemIndex").intValue()
    def zoneIndex = device.currentValue("ZoneIndex").intValue()
    ifDebug("Attempting to set Infinitude zone mode to '${mode}' for system index ${systemIndex} and zone index ${zoneIndex}")
    def systems = parent.getSystems()
    if (!systems) {
        log.error "Could not retrieve systems to set mode."
        return
    }

    if (mode == "fan only") { mode = "fanonly" }

    systems.system[systemIndex].config[0].mode[0] = [mode.toLowerCase()]

    // Post the entire modified configuration back.
    parent.updateSystems(systems)
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

// ***** Helper Methods *****

// NEW: Calculates a future time based on the current time plus a number of minutes.
String getFutureTimePlusMinutes(int minutes) {
    LocalTime futureTime = LocalTime.now().plusMinutes(minutes)
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern('HH:mm')
    return futureTime.format(formatter)
}

// Finds the index of the 'manual' activity profile in the zone configuration.
int findManualIdIndex(List<Map<String, Object>> listOfMaps) {
    if (listOfMaps == null) return -1
    for (int i = 0; i < listOfMaps.size(); i++) {
        def item = listOfMaps[i]
        // The 'id' key in the JSON holds an array, so we check the first element.
        if (item instanceof Map && item.id == 'manual') {
            return i
        }
    }
    log.warn "Could not find 'manual' activity in list."
    return -1
}

// Helper method for debug logging.
private ifDebug(msg) {
    if (enableDebug) {
        // The log prefix helps identify messages from this specific device instance.
        log.debug "${device.displayName}: ${msg}"
    }
}
